package view;

import model.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import static view.LibraryStyle.*;

public final class RecordEditorPanel extends JPanel {
    public record Choice(String id, String name) {
        @Override public String toString() { return name; }
    }
    private final List<Supplier<String>> values = new ArrayList<>();
    private final JLabel message = label(" ", 13, false, RED);
    private final JButton save = button("Lưu", GREEN, Color.WHITE);
    private final JButton cancel = button("Hủy", BEIGE, INK);
    private final JPanel form = clear(new GridBagLayout());
    private int row;

    public RecordEditorPanel(RecordType type, String id, String[] initial, String[] account,
                             Map<CatalogType, TableData> catalogs,
                             BiConsumer<CatalogType, Consumer<Choice>> quickAdd,
                             Consumer<String[]> onSave, Runnable onCancel) {
        super(new BorderLayout()); setOpaque(false);
        String noun = type == RecordType.EMPLOYEES ? "thủ thư" : type.title().toLowerCase(java.util.Locale.ROOT);
        JPanel card = card(new BorderLayout(0, 26));
        card.setBorder(BorderFactory.createEmptyBorder(28,28,26,28));
        JPanel intro = clear(new BorderLayout(0, 10));
        intro.add(label((id == null ? "Thêm " : "Chỉnh sửa ") + noun + (id == null ? " mới" : ""), 26, true, INK), BorderLayout.NORTH);
        intro.add(label(id == null ? "Mã được tạo tự động khi lưu. Điền thông tin bên dưới để hoàn tất." : "Cập nhật thông tin và lưu các thay đổi.", 14, false, MUTED), BorderLayout.SOUTH);
        card.add(intro, BorderLayout.NORTH);
        if (type == RecordType.BOOKS) {
            JTextField name = input(value(initial,0)); fieldAt("Tên sách *", name,0,3);values.add(name::getText);row++;
            CatalogType[] types={CatalogType.THE_LOAI,CatalogType.TAC_GIA,CatalogType.NHA_XUAT_BAN};
            for(int i=0;i<3;i++) {
                CatalogType catalog=types[i];
                JComboBox<Choice> choices=new JComboBox<>();
                choices.addItem(new Choice("", "Chọn " + catalog.title().toLowerCase(Locale.ROOT)));
                TableData data=catalogs.get(catalog);
                if(data!=null)for(Object[] entry:data.rows())choices.addItem(new Choice(entry[0].toString(),entry[1].toString()));
                String selected=value(initial,i+1);
                for(int j=0;j<choices.getItemCount();j++)if(choices.getItemAt(j).id().equals(selected))choices.setSelectedIndex(j);
                choices.setEditable(false);field(choices);
                JPanel group=clear(new BorderLayout(8,0));group.add(choices,BorderLayout.CENTER);
                JButton add=button("+ Thêm",BEIGE,INK);group.add(add,BorderLayout.EAST);
                add.addActionListener(event->quickAdd.accept(catalog, choice->{choices.addItem(choice);choices.setSelectedItem(choice);}));
                fieldAt(catalog.title(),group,i,1);
                values.add(()->{
                    Choice chosen=(Choice)choices.getSelectedItem();
                    return chosen==null?"":chosen.id();
                });
            }
            row++;
            JTextField year=input(value(initial,4));
            JSpinner stock=new JSpinner(new SpinnerNumberModel(parse(value(initial,5)),0,Integer.MAX_VALUE,1));field(stock);
            fieldAt("Năm xuất bản",year,0,1);fieldAt("Số lượng tồn *",stock,1,1);
            values.add(year::getText);values.add(()->{try {stock.commitEdit();}catch(java.text.ParseException e){throw new IllegalArgumentException("Số lượng tồn không hợp lệ.");}return stock.getValue().toString();});row++;
        } else {
            String[] labels={"Họ và tên *","Ngày sinh","Số điện thoại","Địa chỉ"};
            for(int i=0;i<labels.length;i++) {
                if(i==1) {
                    BirthDateField birthday=new BirthDateField(value(initial,i));
                    fieldAt(labels[i],birthday,i%2,1);values.add(birthday::isoValue);
                } else {
                    JTextField input=input(value(initial,i));fieldAt(labels[i],input,i%2,1);values.add(input::getText);
                }
                if(i%2==1)row++;
            }
            if(type==RecordType.EMPLOYEES) {
                JTextField username=input(value(account,0));
                JPasswordField password=new JPasswordField(),confirmation=new JPasswordField();
                field(password);field(confirmation);
                boolean newAccount=id==null||value(account,0).isBlank();
                fieldAt("Tên đăng nhập",username,0,2);row++;
                fieldAt(newAccount?"Mật khẩu (ít nhất 6 ký tự)":"Mật khẩu mới (để trống để giữ nguyên)",password,0,1);
                fieldAt("Nhập lại mật khẩu",confirmation,1,1);row++;
                String role=id!=null&&!value(account,1).isBlank()?value(account,1):"THU_THU";
                boolean enabled=id==null||account==null||"Y".equals(account[2]);
                values.add(username::getText);
                values.add(()->{
                    char[] entered=password.getPassword(),repeated=confirmation.getPassword();
                    try {
                        if(!Arrays.equals(entered,repeated)) {
                            confirmation.requestFocusInWindow();
                            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp.");
                        }
                        String text=new String(entered);
                        if((newAccount||entered.length>0)&&(text.isBlank()||entered.length<6))
                            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự và không được để trống.");
                        return text;
                    } finally {Arrays.fill(entered,'\0');Arrays.fill(repeated,'\0');}
                });
                values.add(()->role);values.add(()->Boolean.toString(enabled));
            }
        }
        JPanel fields=clear(new BorderLayout(0,8));fields.add(form,BorderLayout.NORTH);
        JPanel bottom=clear(new BorderLayout(0,10));
        bottom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0,LINE),
                BorderFactory.createEmptyBorder(18,0,0,0)));
        message.setVisible(false);bottom.add(message,BorderLayout.NORTH);
        JPanel buttons=clear(new FlowLayout(FlowLayout.LEFT,0,0));
        buttons.add(save);buttons.add(Box.createHorizontalStrut(10));buttons.add(cancel);
        bottom.add(buttons,BorderLayout.SOUTH);
        fields.add(bottom,BorderLayout.SOUTH);card.add(fields,BorderLayout.CENTER);
        JPanel wrap=clear(new BorderLayout());wrap.add(card,BorderLayout.NORTH);
        add(scroll(wrap),BorderLayout.CENTER);
        save.addActionListener(event->{
            try {
                String[] result=values.stream().map(Supplier::get).toArray(String[]::new);
                for (int i=0;i<result.length;i++) if(type!=RecordType.EMPLOYEES||i!=5) result[i]=result[i].trim();
                if(result[0].isBlank())throw new IllegalArgumentException("Vui lòng nhập " + (type==RecordType.BOOKS?"tên sách.":"họ và tên."));
                if(type==RecordType.BOOKS&&!result[4].isBlank()) {
                    int year=Integer.parseInt(result[4]);if(year<1)throw new IllegalArgumentException("Năm xuất bản phải là số nguyên dương.");
                }
                message.setVisible(false);onSave.accept(result);
            }catch(RuntimeException ex){message.setVisible(true);message.setText(ex instanceof java.time.format.DateTimeParseException?"Ngày sinh không hợp lệ. Vui lòng nhập dd/MM/yyyy.":ex.getMessage());}
        });
        cancel.addActionListener(event->onCancel.run());
    }
    private void fieldAt(String title,JComponent component,int col,int span) {
        JPanel group=clear(new BorderLayout(0,8));group.add(label(title,13,true,INK),BorderLayout.NORTH);group.add(component,BorderLayout.CENTER);
        GridBagConstraints c=new GridBagConstraints();c.gridx=col;c.gridy=row;c.gridwidth=span;c.weightx=span;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.NORTHWEST;c.insets=new Insets(0,0,20,col+span==3?0:16);form.add(group,c);
    }
    private static JTextField input(String text){JTextField input=new JTextField(text);field(input);return input;}
    private static String value(String[] values,int index){return values==null||index>=values.length||values[index]==null?"":values[index];}
    private static int parse(String value){try{return Math.max(0,Integer.parseInt(value));}catch(NumberFormatException e){return 0;}}
    public void setBusy(boolean busy){save.setEnabled(!busy);cancel.setEnabled(!busy);save.setText(busy?"Đang lưu...":"Lưu");}
    public void showError(String text){message.setVisible(true);message.setText(text);message.setToolTipText(text);setBusy(false);}
}
