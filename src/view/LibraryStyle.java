package view;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

final class LibraryStyle {
    static final Color PAPER = new Color(255, 253, 249), CANVAS = new Color(245, 243, 236),
            INK = new Color(30, 47, 40), MUTED = new Color(112, 123, 116),
            LINE = new Color(232, 224, 209), GREEN = new Color(32, 112, 84),
            BEIGE = new Color(236, 230, 216), RED = new Color(184, 53, 56);
    static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color); return label;
    }
    static JPanel clear(LayoutManager layout) { JPanel p = new JPanel(layout); p.setOpaque(false); return p; }
    static JPanel card(LayoutManager layout) { return card(layout, PAPER); }
    static JPanel card(LayoutManager layout, Color fill) {
        JPanel panel = new JPanel(layout) {
            @Override protected void paintComponent(Graphics source) {
                super.paintComponent(source);
                Graphics2D g = (Graphics2D) source.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill); g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
                g.setColor(LINE); g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22); g.dispose();
            }
        };
        panel.setOpaque(false);panel.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));return panel;
    }
    static JButton button(String text, Color fill, Color foreground) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics source) {
                Graphics2D g = (Graphics2D) source.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getModel().isPressed() ? fill.darker() : fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13);g.dispose();super.paintComponent(source);
            }
        };
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,14));b.setForeground(foreground);
        b.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));b.setContentAreaFilled(false);
        b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }
    static void field(JComponent field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        field.setForeground(INK);
        field.setBackground(Color.WHITE);
        if (field instanceof JComboBox<?> combo) {
            combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override protected JButton createArrowButton() { return chevron(false); }
                @Override protected javax.swing.ComboBoxEditor createEditor() {
                    return new javax.swing.plaf.basic.BasicComboBoxEditor() {
                        @Override protected JTextField createEditorComponent() {
                            JTextField editor = new JTextField();
                            editor.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());
                            editor.setBorder(BorderFactory.createEmptyBorder());
                            editor.setOpaque(false);
                            editor.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                            editor.setForeground(INK);
                            return editor;
                        }
                    };
                }
            });
            combo.setMinimumSize(new Dimension(60, 44));
            combo.setPreferredSize(new Dimension(160, 44));
        } else if (field instanceof JSpinner spinner) {
            spinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
                @Override protected Component createNextButton() {
                    JButton next = chevron(true);
                    installNextButtonListeners(next);
                    return next;
                }
                @Override protected Component createPreviousButton() {
                    JButton previous = chevron(false);
                    installPreviousButtonListeners(previous);
                    return previous;
                }
            });
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
                editor.setOpaque(false);
                editor.setBorder(BorderFactory.createEmptyBorder());
                JFormattedTextField number = editor.getTextField();
                number.setUI(new javax.swing.plaf.basic.BasicFormattedTextFieldUI());
                number.setBorder(BorderFactory.createEmptyBorder());
                number.setOpaque(false);
                number.setFont(field.getFont());
                number.setForeground(INK);
                number.setHorizontalAlignment(SwingConstants.LEFT);
            }
            spinner.setPreferredSize(new Dimension(160, 44));
        } else if (field instanceof JTextField text) {
            text.setUI(text instanceof JPasswordField ? new javax.swing.plaf.basic.BasicPasswordFieldUI()
                    : new javax.swing.plaf.basic.BasicTextFieldUI());
            text.setCaretColor(GREEN);
            text.setPreferredSize(new Dimension(160, 44));
            text.setMinimumSize(new Dimension(60, 44));
        }
        field.setBorder(new AbstractBorder() {
            private Insets padding(Component c) {
                return c instanceof JComboBox<?> ? new Insets(0, 12, 0, 8) : new Insets(10, 12, 10, 12);
            }
            @Override public Insets getBorderInsets(Component c) { return padding(c); }
            @Override public Insets getBorderInsets(Component c, Insets i) {
                Insets p=padding(c); i.set(p.top,p.left,p.bottom,p.right);return i;
            }
            @Override public void paintBorder(Component c, Graphics source, int x,int y,int w,int h) {
                Graphics2D g=(Graphics2D)source.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused=c.hasFocus() || c instanceof Container container
                        && KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != null
                        && javax.swing.SwingUtilities.isDescendingFrom(
                            KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), container);
                g.setColor(focused?GREEN:new Color(218,211,198));
                g.drawRoundRect(x,y,w-1,h-1,12,12);g.dispose();
            }
        });
    }

    private static JButton chevron(boolean up) {
        JButton arrow=new JButton() {
            @Override protected void paintComponent(Graphics source) {
                Graphics2D g=(Graphics2D)source.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getModel().isRollover()?GREEN:MUTED);
                g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                int x=getWidth()/2,y=getHeight()/2;
                java.awt.geom.Path2D path=new java.awt.geom.Path2D.Float();
                path.moveTo(x-4,y+(up?2:-2));path.lineTo(x,y+(up?-2:2));path.lineTo(x+4,y+(up?2:-2));
                g.draw(path);g.dispose();
            }
        };
        arrow.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        arrow.setBorder(BorderFactory.createEmptyBorder());arrow.setContentAreaFilled(false);
        arrow.setFocusPainted(false);arrow.setFocusable(false);
        arrow.setPreferredSize(new Dimension(24,12));arrow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return arrow;
    }

    static JLabel badge(String text, Color fill, Color foreground) {
        JLabel label = new JLabel(text) {
            @Override protected void paintComponent(Graphics source) {
                Graphics2D g=(Graphics2D)source.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(fill);g.fillRoundRect(0,0,getWidth(),getHeight(),24,24);g.dispose();super.paintComponent(source);
            }
        };
        label.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));label.setForeground(foreground);
        label.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));return label;
    }

    static JScrollPane scroll(Component view) {
        JScrollPane pane=new JScrollPane(new ScrollBody(view));pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(CANVAS);pane.getVerticalScrollBar().setUnitIncrement(20);return pane;
    }
    private static final class ScrollBody extends JPanel implements Scrollable {
        ScrollBody(Component view) { super(new BorderLayout());setOpaque(false);add(view,BorderLayout.CENTER); }
        @Override public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        @Override public int getScrollableUnitIncrement(Rectangle r,int orientation,int direction){return 20;}
        @Override public int getScrollableBlockIncrement(Rectangle r,int orientation,int direction){return Math.max(20,r.height-20);}
        @Override public boolean getScrollableTracksViewportWidth(){return true;}
        @Override public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getPreferredSize().height<getParent().getHeight();}
    }

}
