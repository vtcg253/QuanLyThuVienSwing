import dao.*;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.time.LocalDate;

public class VerifyDaoTransactions {
    static int commits,rollbacks;
    static boolean missingStock, failReturn, duplicateUsername, readerHasOpenLoan;
    static int quantity=3,lost=0;
    static final List<String> writes=new ArrayList<>();
    static Object proxy(Class<?> type,InvocationHandler handler){return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);}
    static Object fallback(Class<?> type){if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;}
    static ResultSet rows(List<Object[]> values){int[] cursor={-1};return (ResultSet)proxy(ResultSet.class,(o,m,a)->{
        switch(m.getName()){
            case "next":return ++cursor[0]<values.size();
            case "getInt":return ((Number)values.get(cursor[0])[(Integer)a[0]-1]).intValue();
            case "getString":return String.valueOf(values.get(cursor[0])[(Integer)a[0]-1]);
            case "getObject":return values.get(cursor[0])[(Integer)a[0]-1];
        }return fallback(m.getReturnType());
    });}
    static ResultSet query(String sql){
        if(sql.contains("WHERE p.MADG = ? AND c.TRANGTHAI = 'BORROWED'"))
            return rows(readerHasOpenLoan?Collections.singletonList(new Object[]{1}):List.of());
        if(sql.contains("SELECT SOLUONG FROM SACH"))return rows(missingStock?List.of():Collections.singletonList(new Object[]{10}));
        if(sql.contains("SELECT SOLUONG, SOLUONGMAT"))return rows(Collections.singletonList(new Object[]{quantity,lost}));
        if(sql.contains("FROM TAIKHOAN WHERE TENDANGNHAP"))return rows(duplicateUsername?Collections.singletonList(new Object[]{1}):List.of());
        if(sql.contains("FROM TAIKHOAN WHERE MANV"))return rows(List.of());
        return rows(Collections.singletonList(new Object[]{1}));
    }
    static Object statement(String sql,Class<?> type){return proxy(type,(o,m,a)->{
        switch(m.getName()){
            case "executeQuery":return query(sql==null?(String)a[0]:sql);
            case "executeUpdate":writes.add(sql);return failReturn&&sql.contains("SET NGAYTRA")?0:1;
        }return fallback(m.getReturnType());
    });}
    static Connection connection(){return (Connection)proxy(Connection.class,(o,m,a)->{
        switch(m.getName()){
            case "prepareStatement":return statement((String)a[0],PreparedStatement.class);
            case "createStatement":return statement(null,Statement.class);
            case "commit":commits++;return null;
            case "rollback":rollbacks++;return null;
        }return fallback(m.getReturnType());
    });}
    static void reset(){commits=rollbacks=0;writes.clear();missingStock=failReturn=duplicateUsername=readerHasOpenLoan=false;quantity=3;lost=0;}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void expectedFailure(RunnableSQL action)throws Exception{try{action.run();throw new AssertionError("Expected rejection");}catch(SQLException|IllegalArgumentException expected){}}
    interface RunnableSQL{void run()throws Exception;}
    public static void main(String[] args)throws Exception{
        System.setProperty("DB_URL","jdbc:dao-test:memory");System.setProperty("DB_USERNAME","test");System.setProperty("DB_PASSWORD","test");
        DriverManager.registerDriver(new Driver(){
            public boolean acceptsURL(String u){return u.startsWith("jdbc:dao-test:");}
            public Connection connect(String u,Properties p){return acceptsURL(u)?connection():null;}
            public DriverPropertyInfo[] getPropertyInfo(String u,Properties p){return new DriverPropertyInfo[0];}
            public int getMajorVersion(){return 1;}public int getMinorVersion(){return 0;}public boolean jdbcCompliant(){return false;}
            public java.util.logging.Logger getParentLogger(){return java.util.logging.Logger.getGlobal();}
        });
        LoanDAO loans=new LoanDAO();LocalDate today=LocalDate.now();
        reset();loans.createLoan("DG01",today,today.plusDays(14),Map.of("S01",2),"NV01");
        check(commits==1&&rollbacks==0&&writes.size()==2,"Loan and details must commit together");
        reset();missingStock=true;expectedFailure(()->loans.createLoan("DG01",today,today,Map.of("S01",2),"NV01"));
        check(commits==0&&rollbacks==1&&writes.size()==1,"Missing book must rollback inserted loan");
        reset();failReturn=true;expectedFailure(()->loans.returnBook("PM01","S01"));check(commits==0&&rollbacks==1,"Repeated return must rollback");
        reset();loans.reportLost("PM01","S01",1);check(commits==1&&!writes.get(0).contains("'LOST'"),"Partial loss keeps borrowed status");
        reset();loans.reportLost("PM01","S01",3);check(commits==1&&writes.get(0).contains("'LOST'"),"Full loss marks LOST");
        reset();expectedFailure(()->loans.reportLost("PM01","S01",4));check(commits==0&&rollbacks==1&&writes.isEmpty(),"Excess loss rejected");
        EmployeeDAO employees=new EmployeeDAO();String[] person={"Nhân viên thử","2000-01-01","",""};
        reset();employees.saveWithAccount(null,person,"tester","123456","THU_THU",true);check(commits==1&&rollbacks==0&&writes.size()==2,"Employee and account must commit together");
        reset();expectedFailure(()->employees.saveWithAccount(null,person,"tester","123","THU_THU",true));check(commits==0&&rollbacks==1&&writes.size()==1,"Invalid account must rollback employee insert");
        reset();expectedFailure(()->employees.deleteRecord("NV01","NV01"));check(commits==0&&rollbacks==1&&writes.isEmpty(),"Cannot delete signed-in employee");
        reset();expectedFailure(()->new BookDAO().deleteRecord("S01","NV01"));
        check(writes.isEmpty()&&rollbacks==1,"Book with any loan history cannot be deleted");
        reset();expectedFailure(()->new CatalogDAO().deleteCatalog(model.CatalogType.THE_LOAI,"TL01"));
        check(writes.isEmpty(),"Referenced catalog cannot be deleted");
        reset();duplicateUsername=true;expectedFailure(()->employees.saveWithAccount(null,person,"tester","123456","THU_THU",true));
        check(commits==0&&rollbacks==1&&writes.size()==1,"Duplicate username rolls back employee insert");
        reset();expectedFailure(()->loans.createLoan("DG01",today,today,null,"NV01"));
        check(writes.isEmpty(),"Null book request rejected before writes");
        reset();readerHasOpenLoan=true;
        expectedFailure(()->loans.createLoan("DG01",today,today.plusDays(14),Map.of("S01",1),"NV01"));
        check(commits==0&&rollbacks==1&&writes.isEmpty(),"Reader with unreturned books must be rejected before inserting a loan");
        reset();loans.createLoan("DG01",today,today.plusDays(14),Map.of("S01",1),"NV01");
        check(commits==1&&writes.size()==2,"Reader with no borrowed lines can borrow again");
        reset();loans.createLoan("DG01",today,today.plusDays(14),Map.of("S01",3,"S02",2),"NV01");
        check(commits==1&&writes.size()==3,"Exactly five copies across multiple titles accepted");
        reset();expectedFailure(()->loans.createLoan("DG01",today,today.plusDays(14),Map.of("S01",3,"S02",3),"NV01"));
        check(writes.isEmpty()&&commits==0,"Six copies across titles rejected before writes");
        reset();expectedFailure(()->loans.createLoan("DG01",today,today.plusDays(14),Map.of("S01",6),"NV01"));
        check(writes.isEmpty(),"Six copies of one title rejected");
        System.out.println("PASS: 18 transaction and business validation cases (simulated JDBC, no live database writes).");
    }
}
