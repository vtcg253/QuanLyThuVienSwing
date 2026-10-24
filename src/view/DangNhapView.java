package view;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.BiConsumer;

public final class DangNhapView extends JFrame {
    private static final Color INK = LibraryStyle.INK;
    private static final Color MUTED = LibraryStyle.MUTED;
    private static final Color ACCENT = LibraryStyle.GREEN;
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton login = new JButton("Đăng nhập  →");
    private final JLabel message = new JLabel(" ");
    private final JCheckBox reveal = new JCheckBox("Hiện mật khẩu");
    private boolean busy;

    public DangNhapView() {
        super("Đăng nhập | Quản lý thư viện");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel background = new JPanel(new BorderLayout());
        background.setBackground(LibraryStyle.CANVAS);
        background.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(LibraryStyle.PAPER);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 32, 28, 32));

        JLabel brand = label("THƯ VIỆN  /  LIBRARY", 11, Font.BOLD, ACCENT);
        card.add(brand);
        card.add(Box.createVerticalStrut(24));
        card.add(label("Chào mừng trở lại", 26, Font.BOLD, INK));
        card.add(Box.createVerticalStrut(8));
        card.add(label("Đăng nhập để quản lý thư viện của bạn.", 13, Font.PLAIN, MUTED));
        card.add(Box.createVerticalStrut(28));
        card.add(label("Tên đăng nhập", 13, Font.BOLD, INK));
        card.add(Box.createVerticalStrut(8));
        styleField(username);
        card.add(username);
        card.add(Box.createVerticalStrut(18));
        card.add(label("Mật khẩu", 13, Font.BOLD, INK));
        card.add(Box.createVerticalStrut(8));
        styleField(password);
        card.add(password);
        card.add(Box.createVerticalStrut(10));
        reveal.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        reveal.setForeground(MUTED);
        reveal.setOpaque(false);
        reveal.setAlignmentX(Component.LEFT_ALIGNMENT);
        char echo = password.getEchoChar();
        reveal.addActionListener(event -> password.setEchoChar(reveal.isSelected() ? (char) 0 : echo));
        card.add(reveal);
        card.add(Box.createVerticalStrut(10));
        message.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.setPreferredSize(new Dimension(340, 32));
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        card.add(message);
        card.add(Box.createVerticalStrut(8));

        login.setUI(new BasicButtonUI());
        login.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        login.setForeground(Color.WHITE);
        login.setBackground(ACCENT);
        login.setOpaque(true);
        login.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        login.setFocusPainted(false);
        login.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        login.setAlignmentX(Component.LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        card.add(login);
        card.add(Box.createVerticalStrut(24));
        card.add(label("HỆ THỐNG QUẢN LÝ THƯ VIỆN", 10, Font.PLAIN, MUTED));
        background.add(card);
        setContentPane(background);
        getRootPane().setDefaultButton(login);
        pack();
        setLocationRelativeTo(null);
    }

    private static JLabel label(String text, int size, int weight, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, weight, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void styleField(JTextField field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setForeground(INK);
        field.setCaretColor(ACCENT);
        field.setBackground(new Color(250, 252, 251));
        field.setPreferredSize(new Dimension(340, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(new AbstractBorder() {
            @Override
            public Insets getBorderInsets(Component component) {
                return new Insets(10, 12, 10, 12);
            }

            @Override
            public Insets getBorderInsets(Component component, Insets insets) {
                insets.set(10, 12, 10, 12);
                return insets;
            }

            @Override
            public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.hasFocus() ? ACCENT : new Color(218, 226, 224));
                g.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
                g.dispose();
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) { field.repaint(); }
            @Override
            public void focusLost(FocusEvent event) { field.repaint(); }
        });
    }

    public void setSubmitAction(BiConsumer<String, char[]> action) {
        login.addActionListener(event -> {
            if (!busy) action.accept(username.getText(), password.getPassword());
        });
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        login.setText(busy ? "Đang đăng nhập..." : "Đăng nhập  →");
        login.setEnabled(!busy);
        username.setEnabled(!busy);
        password.setEnabled(!busy);
        reveal.setEnabled(!busy);
    }

    public void showMessage(String text) {
        message.setForeground(busy ? MUTED : new Color(185, 58, 58));
        message.setText(text);
        message.setToolTipText(text);
    }

    public void showLogin() {
        setVisible(true);
        username.requestFocusInWindow();
    }

    public void close() {
        setVisible(false);
        dispose();
    }
}
