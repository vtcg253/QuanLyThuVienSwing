import database.DBConnection;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

/** One-time migration: keep existing IDs; replace DM IDs using normal application prefixes.
 * Run without --apply to preview. No DDL or trigger changes. All writes share one transaction.
 */
public final class NormalizeDemoCodes {
    record Kind(String table, String key, String prefix) {}
    record Change(Kind kind, String oldId, String newId) {}
    static final List<Kind> KINDS = List.of(
        new Kind("THELOAI","MATL","TL"), new Kind("TACGIA","MATG","TG"),
        new Kind("NHAXUATBAN","MANXB","NXB"), new Kind("DOCGIA","MADG","DG"),
        new Kind("NHANVIEN","MANV","NV"), new Kind("SACH","MASACH","S"),
        new Kind("PHIEUMUON","MAPM","PM"));
    static int execute(Connection c, String sql, String... values) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i=0;i<values.length;i++) s.setString(i+1,values[i]);
            return s.executeUpdate();
        }
    }
    static List<String> columns(Connection c, String table) throws SQLException {
        List<String> out=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME=? ORDER BY COLUMN_ID")) {
            s.setString(1,table);try(ResultSet r=s.executeQuery()){while(r.next())out.add(r.getString(1));}
        } return out;
    }
    // Compare every value before/after, translating only actual identifier columns.
    static List<String> snapshot(Connection c,List<Change> changes) throws SQLException {
        List<String> result=new ArrayList<>();
        Map<String,Map<String,String>> maps=new HashMap<>();
        for(Change ch:changes)maps.computeIfAbsent(ch.kind.key,k->new HashMap<>()).put(ch.oldId,ch.newId);
        List<String> tables=new ArrayList<>();for(Kind k:KINDS)tables.add(k.table);
        tables.add("TAIKHOAN");tables.add("CT_PHIEUMUON");
        for(String table:tables)try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM "+table)){
            ResultSetMetaData md=r.getMetaData();
            while(r.next()){
                StringBuilder row=new StringBuilder(table);
                for(int i=1;i<=md.getColumnCount();i++){
                    String value=r.getString(i);Map<String,String> m=maps.get(md.getColumnName(i));
                    if(m!=null)value=m.getOrDefault(value,value);
                    row.append('|').append(value==null?-1:value.length()).append(':').append(value);
                }result.add(row.toString());
            }
        }
        Collections.sort(result);return result;
    }
    public static void main(String[] args) throws Exception {
        boolean apply=Arrays.asList(args).contains("--apply");
        try(Connection c=DBConnection.getConnection()){
            c.setAutoCommit(false);
            try {
                for(String t:List.of("THELOAI","TACGIA","NHAXUATBAN","DOCGIA","NHANVIEN","SACH","PHIEUMUON","TAIKHOAN","CT_PHIEUMUON"))
                    try(Statement s=c.createStatement()){s.execute("LOCK TABLE "+t+" IN EXCLUSIVE MODE NOWAIT");}
                List<Change> changes=new ArrayList<>();
                for(Kind k:KINDS){
                    int max=0;List<String> demo=new ArrayList<>();
                    try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT "+k.key+" FROM "+k.table+" ORDER BY "+k.key)){
                        while(r.next()){
                            String id=r.getString(1);
                            if(id.matches(k.prefix+"[0-9]+"))max=Math.max(max,Integer.parseInt(id.substring(k.prefix.length())));
                            if(id.matches("DM"+k.prefix+"[0-9]+"))demo.add(id);
                        }
                    }
                    for(String old:demo)changes.add(new Change(k,old,k.prefix+String.format("%02d",++max)));
                }
                StringBuilder csv=new StringBuilder("table,column,old_code,new_code\n");
                for(Change ch:changes)csv.append(ch.kind.table).append(',').append(ch.kind.key).append(',').append(ch.oldId).append(',').append(ch.newId).append('\n');
                System.out.print(csv);
                if(!apply){c.rollback();return;}
                List<String> before=snapshot(c,changes);
                for(Change ch:changes){
                    Kind k=ch.kind;List<String> cols=columns(c,k.table);
                    String selection=String.join(",",cols.stream().map(col->col.equals(k.key)?"?":col).toList());
                    if(execute(c,"INSERT INTO "+k.table+" ("+String.join(",",cols)+") SELECT "+selection+" FROM "+k.table+" WHERE "+k.key+"=?",ch.newId,ch.oldId)!=1)throw new SQLException("Cannot copy "+ch.oldId);
                    // Discover actual referencing columns from Oracle metadata.
                    try(PreparedStatement s=c.prepareStatement("SELECT cc.TABLE_NAME,cc.COLUMN_NAME FROM USER_CONSTRAINTS fk JOIN USER_CONSTRAINTS pk ON pk.CONSTRAINT_NAME=fk.R_CONSTRAINT_NAME JOIN USER_CONS_COLUMNS cc ON cc.CONSTRAINT_NAME=fk.CONSTRAINT_NAME WHERE fk.CONSTRAINT_TYPE='R' AND pk.TABLE_NAME=?")){
                        s.setString(1,k.table);try(ResultSet r=s.executeQuery()){
                            while(r.next())execute(c,"UPDATE "+r.getString(1)+" SET "+r.getString(2)+"=? WHERE "+r.getString(2)+"=?",ch.newId,ch.oldId);
                        }
                    }
                    if(execute(c,"DELETE FROM "+k.table+" WHERE "+k.key+"=?",ch.oldId)!=1)throw new SQLException("Cannot remove old code "+ch.oldId);
                }
                if(!before.equals(snapshot(c,List.of())))throw new SQLException("Data changed beyond identifier migration; rolling back");
                if(!changes.isEmpty())Files.writeString(Path.of("bao-cao/bang-doi-ma-demo.csv"),csv.toString(),StandardOpenOption.CREATE_NEW);
                c.commit();System.out.println("COMMITTED: "+changes.size()+" codes; all rows and non-code values verified unchanged.");
            } catch(Exception e){c.rollback();throw e;}
        }
    }
}
