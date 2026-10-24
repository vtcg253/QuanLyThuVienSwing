package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Modal host for editor panels with their own save/cancel controls. */
public final class EditorPopup extends JDialog {
    private boolean busy;

    public EditorPopup(Component parent, String title, int width, int height) {
        super(parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent),
                title, Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { closeIfIdle(); }
        });
        getRootPane().registerKeyboardAction(event -> closeIfIdle(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        setResizable(true);
        GraphicsConfiguration configuration=getGraphicsConfiguration();
        Rectangle bounds=configuration.getBounds();
        Insets insets=Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int availableWidth=Math.max(1,bounds.width-insets.left-insets.right-40);
        int availableHeight=Math.max(1,bounds.height-insets.top-insets.bottom-40);
        setMinimumSize(new Dimension(Math.min(Math.min(560,width),availableWidth),Math.min(Math.min(360,height),availableHeight)));
        setSize(Math.min(width,availableWidth),Math.min(height,availableHeight));
        setLocationRelativeTo(parent);
    }

    public void showEditor(JPanel editor) {
        setContentPane(editor);
        try { setVisible(true); } finally { dispose(); }
    }

    public void setBusy(boolean busy) { this.busy=busy; }
    public void closeIfIdle() { if (!busy) dispose(); }
}
