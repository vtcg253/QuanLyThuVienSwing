package view;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;

public final class RecordFormDialog {
    private RecordFormDialog() {
    }

    public static boolean confirm(Component parent, String title, javax.swing.JComponent content) {
        return dialog(parent, title, content, true, "Lưu");
    }

    public static void showContent(Component parent, String title, javax.swing.JComponent content) {
        dialog(parent, title, content, false, "Đóng");
    }

    public static boolean confirm(Component parent, String title, javax.swing.JComponent content, String action) {
        return dialog(parent, title, content, true, action);
    }

    private static boolean dialog(Component parent, String title, javax.swing.JComponent content, boolean editable, String action) {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(parent);
        if (parent instanceof java.awt.Window window) owner = window;
        JDialog dialog = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel card = LibraryStyle.card(new java.awt.BorderLayout(0, 22));
        card.add(LibraryStyle.label(title, 24, true, LibraryStyle.INK), java.awt.BorderLayout.NORTH);
        style(content);
        card.add(content, java.awt.BorderLayout.CENTER);
        JPanel buttons = LibraryStyle.clear(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        javax.swing.JButton save = LibraryStyle.button(action, "Xóa".equals(action) ? LibraryStyle.RED : LibraryStyle.GREEN, java.awt.Color.WHITE);
        javax.swing.JButton cancel = LibraryStyle.button("Hủy", LibraryStyle.BEIGE, LibraryStyle.INK);
        boolean[] accepted = {false};
        save.addActionListener(event -> { accepted[0] = true; dialog.dispose(); });
        cancel.addActionListener(event -> dialog.dispose());
        buttons.add(save); if (editable) buttons.add(cancel);
        card.add(buttons, java.awt.BorderLayout.SOUTH);
        dialog.setContentPane(card);dialog.getRootPane().setDefaultButton(save);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();dialog.setMinimumSize(new java.awt.Dimension(500, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);dialog.setVisible(true);return accepted[0];
    }

    private static void style(java.awt.Component component) {
        if (component instanceof JPanel panel) panel.setOpaque(false);
        if (component instanceof JTextField field) LibraryStyle.field(field);
        if (component instanceof JLabel label) {
            label.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
            label.setForeground(LibraryStyle.INK);
        }
        if (component instanceof java.awt.Container container)
            for (java.awt.Component child : container.getComponents()) style(child);
    }

}
