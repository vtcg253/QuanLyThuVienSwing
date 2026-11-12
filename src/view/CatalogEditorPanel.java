package view;

import model.CatalogType;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import static view.LibraryStyle.*;

public final class CatalogEditorPanel extends JPanel {
    private final JButton save=button("Lưu",GREEN,Color.WHITE),cancel=button("Hủy",BEIGE,INK);
    private final JLabel message=label(" ",13,false,RED);
    public CatalogEditorPanel(CatalogType type, boolean editing, String[] initial, Consumer<String[]> onSave, Runnable onCancel) {
        super(new BorderLayout());setOpaque(false);
        JPanel card=card(new BorderLayout(0,20));
        JPanel heading=clear(new BorderLayout(0,8));heading.add(label((editing?"Chỉnh sửa ":"Thêm ")+type.title().toLowerCase(java.util.Locale.ROOT),24,true,INK),BorderLayout.NORTH);
        heading.add(label(editing?"Cập nhật thông tin bên dưới.":"Mã được tạo tự động khi lưu.",13,false,MUTED),BorderLayout.SOUTH);card.add(heading,BorderLayout.NORTH);
        String[] labels=type.formLabels();JTextField[] fields=new JTextField[labels.length];JPanel form=clear(new GridLayout(0,1,0,16));
        for(int i=0;i<labels.length;i++){
            fields[i]=new JTextField(initial==null||initial[i]==null?"":initial[i]);field(fields[i]);
            JPanel group=clear(new BorderLayout(0,8));JLabel caption=label(labels[i]+(i==0?" *":""),13,true,INK);caption.setLabelFor(fields[i]);group.add(caption,BorderLayout.NORTH);group.add(fields[i]);form.add(group);
        }
        card.add(form);JPanel footer=clear(new BorderLayout(0,10));message.setVisible(false);footer.add(message,BorderLayout.NORTH);JPanel controls=clear(new FlowLayout(FlowLayout.LEFT,0,0));controls.add(save);controls.add(Box.createHorizontalStrut(8));controls.add(cancel);footer.add(controls,BorderLayout.SOUTH);card.add(footer,BorderLayout.SOUTH);
        JPanel wrap=clear(new BorderLayout());wrap.add(card,BorderLayout.NORTH);add(scroll(wrap));
        save.addActionListener(event->{String[] values=java.util.Arrays.stream(fields).map(JTextField::getText).map(String::trim).toArray(String[]::new);
            try { model.InputValidation.catalog(type, values); }
            catch (IllegalArgumentException ex) { showError(ex.getMessage()); return; }
            if(values[0].isBlank()){message.setVisible(true);message.setText("Vui lòng nhập "+labels[0].toLowerCase(java.util.Locale.ROOT)+".");return;}message.setVisible(false);onSave.accept(values);});
        cancel.addActionListener(event->onCancel.run());
    }
    public void setBusy(boolean busy){save.setEnabled(!busy);cancel.setEnabled(!busy);save.setText(busy?"Đang lưu...":"Lưu");}
    public void showError(String text){message.setVisible(true);message.setText(text);message.setToolTipText(text);setBusy(false);}
}
