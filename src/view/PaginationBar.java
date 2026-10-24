package view;

import javax.swing.*;
import java.awt.*;
import static view.LibraryStyle.*;

/** Shared page navigation; the owner supplies the ordered items for each page. */
final class PaginationBar extends JPanel {
    private final JLabel count=label("Hiển thị 0 trên 0",13,false,MUTED);
    private final JComboBox<Integer> size=new JComboBox<>(new Integer[]{5,10,20});
    private final JPanel numbers=clear(new FlowLayout(FlowLayout.LEFT,5,0));
    private final JButton previous=button("‹ Trước",BEIGE,INK),next=button("Sau ›",BEIGE,INK);
    private final Runnable onChange;
    private int total;
    private int page;

    PaginationBar(Runnable onChange) {
        super(new BorderLayout(0,12));this.onChange=onChange;setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,LINE),
                BorderFactory.createEmptyBorder(14,0,0,0)));
        JPanel info=clear(new BorderLayout());info.add(count,BorderLayout.WEST);
        JPanel choice=clear(new FlowLayout(FlowLayout.RIGHT,8,0));choice.add(label("Số dòng / trang",12,false,MUTED));
        size.setSelectedItem(10);field(size);size.setPreferredSize(new Dimension(74,34));
        size.getAccessibleContext().setAccessibleName("Số dòng mỗi trang");choice.add(size);info.add(choice,BorderLayout.EAST);add(info,BorderLayout.NORTH);
        JPanel navigation=clear(new FlowLayout(FlowLayout.LEFT,0,0));navigation.add(previous);navigation.add(Box.createHorizontalStrut(6));navigation.add(numbers);navigation.add(Box.createHorizontalStrut(6));navigation.add(next);add(navigation,BorderLayout.SOUTH);
        previous.addActionListener(event->goTo(page-1));next.addActionListener(event->goTo(page+1));
        size.addActionListener(event->{page=0;refresh();onChange.run();});refresh();
    }
    int offset(){return page*pageSize();}
    int end(){return Math.min(total,offset()+pageSize());}
    private int pageSize(){return (Integer)size.getSelectedItem();}
    private int pageCount(){return Math.max(1,(total+pageSize()-1)/pageSize());}
    void setTotal(int total){setTotal(total,false);}
    void setTotal(int total,boolean preservePage){this.total=total;page=preservePage?Math.min(page,pageCount()-1):0;refresh();}
    void firstPage(){page=0;refresh();onChange.run();}
    private void goTo(int target){if(target<0||target>=pageCount()||target==page)return;page=target;refresh();onChange.run();}
    private void refresh(){
        count.setText(total==0?"Hiển thị 0 trên 0":"Hiển thị "+(offset()+1)+"–"+end()+" trên "+total);
        previous.setEnabled(page>0);next.setEnabled(page+1<pageCount());
        previous.setForeground(previous.isEnabled()?INK:MUTED);next.setForeground(next.isEnabled()?INK:MUTED);
        numbers.removeAll();
        int start=Math.max(0,Math.min(page-2,pageCount()-5)),end=Math.min(pageCount(),start+5);
        if(start>0){addNumber(0);if(start>1)numbers.add(label("…",13,false,MUTED));}
        for(int i=start;i<end;i++)addNumber(i);
        if(end<pageCount()){if(end<pageCount()-1)numbers.add(label("…",13,false,MUTED));addNumber(pageCount()-1);}
        numbers.revalidate();numbers.repaint();
    }
    private void addNumber(int index){
        JButton number=button(Integer.toString(index+1),index==page?GREEN:BEIGE,index==page?Color.WHITE:INK);
        number.getAccessibleContext().setAccessibleName("Trang "+(index+1));
        number.putClientProperty("selectedPage",index==page);
        number.addActionListener(event->goTo(index));numbers.add(number);
    }
}
