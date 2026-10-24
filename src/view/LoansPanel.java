package view;

import model.TableData;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import static view.LibraryStyle.*;

final class LoansPanel extends JPanel {
    private LoanDetailsPanel details;
    private JDialog detailsDialog;
    private String detailsLoanId;
    private final JTextField search = new JTextField();
    private final JComboBox<String> status=new JComboBox<>(new String[]{"Tất cả trạng thái","Đang mượn","Quá hạn","Đã trả","Đã mất"});
    private final JPanel list=clear(new GridBagLayout());
    private final JLabel count=label(" ",13,false,MUTED);
    private final PaginationBar pagination=new PaginationBar(this::applyPage);
    private List<List<Object[]>> grouped=List.of();
    private JScrollPane listScroll;
    private final Map<String,JLabel> totals=new LinkedHashMap<>();
    private final java.util.function.Supplier<MainView.Actions> actions;
    LoansPanel(java.util.function.Supplier<MainView.Actions> actions) {
        super(new BorderLayout());this.actions=actions;setOpaque(false);
        JPanel card=card(new BorderLayout(0,20));
        JPanel top=clear(new BorderLayout(0,18));
        JPanel intro=clear(new BorderLayout(12,0));
        JPanel words=clear(new GridLayout(0,1,0,7));
        words.add(label("NGHIỆP VỤ THƯ VIỆN",11,true,GREEN));
        words.add(label("Quản lý mượn / trả sách",26,true,INK));
        words.add(label("Theo dõi phiếu mượn, tình trạng trả sách và hạn trả.",14,false,MUTED));
        intro.add(words,BorderLayout.CENTER);JButton create=button("+ Lập phiếu mượn",GREEN,Color.WHITE);
        create.addActionListener(event->actions.get().createLoan());JPanel add=clear(new FlowLayout(FlowLayout.RIGHT,0,10));add.add(create);intro.add(add,BorderLayout.EAST);top.add(intro,BorderLayout.NORTH);
        JPanel stats=clear(new GridLayout(1,4,12,0));
        for(String name:new String[]{"Tổng phiếu","Đang mượn","Quá hạn","Đã trả"}) {
            JPanel tile=card(new BorderLayout(0,8),name.equals("Quá hạn")?new Color(255,245,241):new Color(242,249,244));tile.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
            tile.add(label(name,13,true,MUTED),BorderLayout.NORTH);JLabel value=label("—",24,true,name.equals("Quá hạn")?RED:INK);tile.add(value);totals.put(name,value);stats.add(tile);
        }
        top.add(stats,BorderLayout.CENTER);
        JPanel filter=clear(new BorderLayout(10,0));field(search);search.getAccessibleContext().setAccessibleName("Tìm phiếu mượn");search.setToolTipText("Tìm mã phiếu, độc giả, nhân viên hoặc sách");
        filter.add(search,BorderLayout.CENTER);JPanel controls=clear(new FlowLayout(FlowLayout.RIGHT,8,0));field(status);controls.add(status);
        JButton find=button("Lọc phiếu",GREEN,Color.WHITE);controls.add(find);filter.add(controls,BorderLayout.EAST);top.add(filter,BorderLayout.SOUTH);
        Runnable reload=()->actions.get().reload("loans",search.getText(),switch(status.getSelectedIndex()){case 1->"BORROWED";case 2->"OVERDUE";case 3->"RETURNED";case 4->"LOST";default->"ALL";});
        find.addActionListener(event->reload.run());search.addActionListener(event->reload.run());status.addActionListener(event->reload.run());
        card.add(top,BorderLayout.NORTH);
        JPanel results=clear(new BorderLayout(0,12));JPanel heading=clear(new GridLayout(0,1,0,5));heading.add(label("Danh sách phiếu",17,true,INK));heading.add(count);results.add(heading,BorderLayout.NORTH);
        JPanel wrap=clear(new BorderLayout());wrap.add(list,BorderLayout.NORTH);listScroll=scroll(wrap);results.add(listScroll);card.add(results);card.add(pagination,BorderLayout.SOUTH);
        add(card);
    }
    void setStatistics(Map<String,Long> values) {
        totals.forEach((key,label)->label.setText(String.valueOf(values.getOrDefault(key.equals("Tổng phiếu")?"Phiếu mượn":key,0L))));
    }
    void setData(TableData data) {
        Map<String,List<Object[]>> groups=new LinkedHashMap<>();
        for(Object[] row:data.rows())groups.computeIfAbsent(String.valueOf(row[0]),ignored->new ArrayList<>()).add(row.clone());
        grouped=new ArrayList<>(groups.values());
        count.setText(groups.size()+" phiếu phù hợp");
        pagination.setTotal(grouped.size(),detailsDialog != null);
        applyPage();
    }
    private void applyPage() {
        list.removeAll();
        int index=0;
        for(int i=pagination.offset();i<pagination.end();i++) {
            JPanel loan=loanCard(grouped.get(i));GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=index++;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(0,0,12,0);list.add(loan,c);
        }
        if(grouped.isEmpty()){GridBagConstraints c=new GridBagConstraints();list.add(label("Không có phiếu mượn phù hợp.",15,false,MUTED),c);}
        if(listScroll!=null)listScroll.getViewport().setViewPosition(new Point());
        list.revalidate();list.repaint();
    }
    private JPanel loanCard(List<Object[]> rows) {
        Object[] first=rows.get(0);JPanel card=card(new BorderLayout(0,15));card.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel header=clear(new BorderLayout(8,0));JPanel words=clear(new GridLayout(0,1,0,7));
        boolean borrowed=rows.stream().anyMatch(r->"BORROWED".equals(String.valueOf(r[10])));
        boolean lost=rows.stream().anyMatch(r->number(r[8])>0);
        boolean overdue=borrowed&&date(first[4])!=null&&date(first[4]).isBefore(LocalDate.now());
        String state=overdue?"Quá hạn":borrowed?"Đang mượn":rows.stream().allMatch(r->"RETURNED".equals(String.valueOf(r[10])))?"Đã trả":lost?"Đã mất":"Đã xử lý";
        JPanel identity=clear(new FlowLayout(FlowLayout.LEFT,0,0));identity.add(label(first[0]+"     ",16,true,INK));identity.add(badge(state,overdue?new Color(253,229,224):borrowed?new Color(255,234,196):new Color(216,242,229),overdue?RED:borrowed?new Color(142,94,23):GREEN));words.add(identity);
        words.add(label("Độc giả  "+display(first[1])+"     ·     Nhân viên  "+display(first[2]),13,false,INK));
        words.add(label("SĐT độc giả  "+display(first.length>14?first[14]:null),13,false,INK));
        words.add(label("Ngày mượn  "+display(first[3])+"     Hạn trả  "+display(first[4]),12,false,MUTED));header.add(words);
        JButton detail=button("Xem chi tiết →",BEIGE,INK);detail.addActionListener(event->showDetails(rows));JPanel right=clear(new FlowLayout(FlowLayout.RIGHT));right.add(detail);header.add(right,BorderLayout.EAST);card.add(header,BorderLayout.NORTH);
        JPanel lines=clear(new GridBagLayout());int y=0;
        addLine(lines,new JComponent[]{label("MÃ / TÊN SÁCH",11,true,MUTED),label("SL MƯỢN",11,true,MUTED),label("NGÀY TRẢ",11,true,MUTED),label("THAO TÁC",11,true,MUTED)},y++);
        for(Object[] row:rows) {
            JPanel name=clear(new GridLayout(0,1,0,4));JLabel title=label(display(row[6])+"   "+display(row[5]),14,false,INK);title.setToolTipText(display(row[5]));name.add(title);
            int missing=number(row[8]);if(missing>0)name.add(label("Đã mất: "+missing+" cuốn",12,false,RED));
            JPanel controls=clear(new FlowLayout(FlowLayout.LEFT,6,0));
            if("BORROWED".equals(String.valueOf(row[10]))) {
                JButton returned=button("Trả còn lại",GREEN,Color.WHITE);returned.addActionListener(event->actions.get().returnBook(row.clone()));controls.add(returned);
                JButton report=button("Ghi nhận mất",RED,Color.WHITE);report.addActionListener(event->actions.get().reportLost(row.clone()));controls.add(report);
            }else controls.add(label("Đã xử lý",12,false,MUTED));
            addLine(lines,new JComponent[]{name,label(display(row[7]),14,false,INK),label(row[9]==null?"Chưa trả":display(row[9]),12,false,row[9]==null?MUTED:GREEN),controls},y++);
        }
        card.add(lines);return card;
    }
    private void addLine(JPanel panel,JComponent[] cells,int row) {
        double[] weights={0.42,0.10,0.16,0.32};
        for(int i=0;i<cells.length;i++) {
            GridBagConstraints c=new GridBagConstraints();c.gridx=i;c.gridy=row;c.weightx=weights[i];c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(10,0,10,8);
            cells[i].setMinimumSize(new Dimension(i==3?240:40,30));panel.add(cells[i],c);
        }
    }
    String searchText() { return search.getText(); }
    String selectedStatus() {
        return switch(status.getSelectedIndex()) {
            case 1 -> "BORROWED";case 2 -> "OVERDUE";case 3 -> "RETURNED";case 4 -> "LOST";default -> "ALL";
        };
    }
    Component actionParent() { return detailsDialog != null && detailsDialog.isShowing() ? detailsDialog : this; }
    void setDetailsBusy(boolean busy) { if (details != null) details.setBusy(busy); }
    void refreshDetails(String loanId, TableData data) {
        if (detailsDialog == null || !detailsDialog.isDisplayable() || !loanId.equals(detailsLoanId)) return;
        List<Object[]> rows = data.rows().stream().filter(row -> loanId.equals(String.valueOf(row[0]))).toList();
        if (rows.isEmpty()) { detailsDialog.dispose();return; }
        details = detailContent(rows, detailsDialog::dispose);
        detailsDialog.setContentPane(details);
        detailsDialog.revalidate();detailsDialog.repaint();
    }
    private LoanDetailsPanel detailContent(List<Object[]> rows, Runnable onClose) {
        return new LoanDetailsPanel(rows, row -> actions.get().returnBook(row),
                row -> actions.get().reportLost(row), onClose);
    }
    private void showDetails(List<Object[]> rows) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog popup = new JDialog(owner,"Chi tiết phiếu " + rows.get(0)[0],Dialog.ModalityType.APPLICATION_MODAL);
        detailsDialog = popup;
        detailsLoanId = String.valueOf(rows.get(0)[0]);
        details = detailContent(rows, popup::dispose);
        popup.setContentPane(details);
        popup.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        popup.setResizable(true);
        popup.getRootPane().registerKeyboardAction(event -> popup.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        GraphicsConfiguration configuration = owner == null ? getGraphicsConfiguration() : owner.getGraphicsConfiguration();
        Rectangle bounds = configuration == null ? new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()) : configuration.getBounds();
        Insets insets = configuration == null ? new Insets(0,0,0,0) : Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int width = Math.max(1,bounds.width-insets.left-insets.right-40);
        int height = Math.max(1,bounds.height-insets.top-insets.bottom-40);
        popup.setMinimumSize(new Dimension(Math.min(760,width),Math.min(500,height)));
        popup.setSize(Math.min(1320,width),Math.min(760,height));
        popup.setLocationRelativeTo(owner);
        try { popup.setVisible(true); }
        finally { popup.dispose();detailsDialog=null;details=null;detailsLoanId=null; }
    }
    private static int number(Object value){return value==null?0:((Number)value).intValue();}
    private static LocalDate date(Object value){if(value==null)return null;try{return LocalDate.parse(value.toString().substring(0,10));}catch(RuntimeException e){return null;}}
    private static String display(Object value){if(value==null)return "—";LocalDate date=value instanceof java.util.Date?date(value):null;return date==null?value.toString():date.toString();}
}
