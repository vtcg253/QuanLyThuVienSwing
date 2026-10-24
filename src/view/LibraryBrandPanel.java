package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/** The brand contracts to just the menu button when navigation is hidden. */
final class LibraryBrandPanel extends JPanel {
    static final int EXPANDED_WIDTH = 260;
    private final JPanel text = LibraryStyle.clear(new BorderLayout(0,4));
    private final JButton menu;

    LibraryBrandPanel() {
        super(new BorderLayout(10,0));
        setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        menu = LibraryStyle.button("☰",new Color(57,94,78),Color.WHITE);
        menu.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
        menu.setBorder(BorderFactory.createEmptyBorder());
        menu.setPreferredSize(new Dimension(32,32));
        menu.getAccessibleContext().setAccessibleName("Ẩn hoặc hiện menu thư viện");
        JPanel buttonHolder = LibraryStyle.clear(new GridBagLayout());
        buttonHolder.add(menu);
        add(buttonHolder,BorderLayout.WEST);
        text.add(LibraryStyle.label("Quản lý thư viện",16,true,Color.WHITE),BorderLayout.NORTH);
        text.add(LibraryStyle.label("Hệ thống thư viện",11,false,new Color(184,209,196)),BorderLayout.SOUTH);
        JPanel words = LibraryStyle.clear(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();c.weightx=1;c.anchor=GridBagConstraints.WEST;
        words.add(text,c);
        // Hide this whole container, including its reserved layout space.
        textContainer=words;
        add(words,BorderLayout.CENTER);
        setExpanded(true);
    }

    private final JPanel textContainer;
    void addToggleListener(ActionListener listener) { menu.addActionListener(listener); }
    void setExpanded(boolean expanded) {
        textContainer.setVisible(expanded);
        setBackground(expanded?new Color(21,58,47):new Color(29,105,80));
        setPreferredSize(new Dimension(expanded?EXPANDED_WIDTH:64,68));
        menu.setToolTipText(expanded?"Ẩn thanh điều hướng":"Hiện thanh điều hướng");
        menu.putClientProperty("expanded",expanded);
        revalidate();repaint();
    }
}
