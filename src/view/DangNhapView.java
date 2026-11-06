package view;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

public final class DangNhapView extends JFrame {
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton login = LibraryStyle.button("Đăng nhập  →", LibraryStyle.GREEN, Color.WHITE);
    private final JLabel message = LibraryStyle.label(" ", 12, false, LibraryStyle.MUTED);
    private final JCheckBox reveal = new JCheckBox("Hiện mật khẩu");
    private boolean busy;

    public DangNhapView() {
        super("Đăng nhập | Quản lý thư viện");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel background = new JPanel(new BorderLayout());
        background.setBackground(LibraryStyle.CANVAS);
        background.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));
        JPanel form = LibraryStyle.card(null);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(30, 32, 28, 32));

        addRow(form, LibraryStyle.label("THƯ VIỆN  /  LIBRARY", 11, true, LibraryStyle.GREEN), 24);
        addRow(form, LibraryStyle.label("Chào mừng trở lại", 26, true, LibraryStyle.INK), 8);
        addRow(form, LibraryStyle.label("Đăng nhập để quản lý thư viện của bạn.", 13, false, LibraryStyle.MUTED), 28);
        addField(form, "Tên đăng nhập", username);
        addField(form, "Mật khẩu", password);

        reveal.setOpaque(false);
        reveal.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        reveal.setForeground(LibraryStyle.MUTED);
        char echo = password.getEchoChar();
        reveal.addActionListener(event -> password.setEchoChar(reveal.isSelected() ? (char) 0 : echo));
        addRow(form, reveal, 10);
        message.setPreferredSize(new Dimension(340, 32));
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        addRow(form, message, 8);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        addRow(form, login, 24);
        addRow(form, LibraryStyle.label("HỆ THỐNG QUẢN LÝ THƯ VIỆN", 10, false, LibraryStyle.MUTED), 0);

        background.add(form);
        setContentPane(background);
        getRootPane().setDefaultButton(login);
        pack();
        setLocationRelativeTo(null);
    }

    private static void addRow(JPanel form, JComponent component, int gap) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(component);
        if (gap > 0) form.add(Box.createVerticalStrut(gap));
    }

    private static void addField(JPanel form, String title, JTextField field) {
        addRow(form, LibraryStyle.label(title, 13, true, LibraryStyle.INK), 8);
        LibraryStyle.field(field);
        field.setPreferredSize(new Dimension(340, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        addRow(form, field, 18);
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
        message.setForeground(busy ? LibraryStyle.MUTED : LibraryStyle.RED);
        message.setText(text);
        message.setToolTipText(text);
    }

    public void showLogin() {
        setVisible(true);
        username.requestFocusInWindow();
    }

    public void close() {
        dispose();
    }
}
