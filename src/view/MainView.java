package view;

import model.CatalogType;
import model.LibraryUser;
import model.RecordType;
import model.TableData;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainView extends JFrame {
    public interface Actions {
        void navigate(String page);
        void reload(String page, String search, String status);
        void add(String page);
        void edit(String page, Object[] row);
        void delete(String page, Object[] row);
        void createLoan();
        void returnBook(Object[] row);
        void reportLost(Object[] row);
        void logout();
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel dashboard = new JPanel(new BorderLayout());
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private static final Color GREEN = new Color(29, 105, 80);
    private static final Color INK = new Color(30, 47, 40);
    private static final Color CANVAS = new Color(245, 243, 236);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final JLabel title = new JLabel("Tổng quan");
    private final JLabel userLabel = new JLabel();
    private final LibraryUser user;
    private Actions actions;

    public MainView(LibraryUser user) {
        super("Quản lý thư viện");
        this.user = user;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        buildShell();
        setJMenuBar(buildMenuBar());
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu system = new JMenu("Hệ thống");
        addMenuItem(system, "Tổng quan", () -> actions.navigate("dashboard"));
        system.addSeparator();
        addMenuItem(system, "Đăng xuất", this::logout);
        menuBar.add(system);

        JMenu management = new JMenu("Quản lý");
        addMenuItem(management, "Danh sách sách", () -> actions.navigate("books"));
        addMenuItem(management, "Thêm sách", () -> actions.add("books"));
        management.addSeparator();
        addMenuItem(management, "Tác giả", () -> actions.navigate("authors"));
        addMenuItem(management, "Thể loại", () -> actions.navigate("genres"));
        addMenuItem(management, "Nhà xuất bản", () -> actions.navigate("publishers"));
        management.addSeparator();
        addMenuItem(management, "Độc giả", () -> actions.navigate("readers"));
        if (user.isAdmin()) {
            addMenuItem(management, "Thủ thư & tài khoản", () -> actions.navigate("employees"));
        }
        menuBar.add(management);

        JMenu loans = new JMenu("Mượn / Trả");
        addMenuItem(loans, "Danh sách phiếu mượn", () -> actions.navigate("loans"));
        addMenuItem(loans, "Lập phiếu mượn", () -> actions.createLoan());
        menuBar.add(loans);

        return menuBar;
    }

    private void addMenuItem(JMenu menu, String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(event -> action.run());
        menu.add(item);
    }

    private void logout() {
        dispose();
        actions.logout();
    }

    public void display(String key, TableData data) {
        Page page = pages.get(key);
        if (page == null) {
            return;
        }
        page.setData(data);
        cards.show(content, key);
        title.setText(page.title);
        selectNavigation(key);
    }

    public void displayDashboard(Map<String, Long> statistics, TableData books) {
        dashboard.removeAll();
        dashboard.add(new DashboardPanel(statistics, books,
                () -> actions.navigate("books"), () -> actions.navigate("loans")), BorderLayout.CENTER);
        dashboard.revalidate();
        dashboard.repaint();
        cards.show(content, "dashboard");
        title.setText("Dashboard quản lý");
        selectNavigation("dashboard");
    }

    private void selectNavigation(String key) {
        navigation.forEach((name, button) -> {
            button.setContentAreaFilled(name.equals(key));
            button.setOpaque(name.equals(key));
            button.setBackground(new Color(65, 107, 91));
        });
    }

    private void buildShell() {
        JPanel root = new JPanel(new BorderLayout());
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics source) {
                super.paintComponent(source);
                java.awt.Graphics2D g = (java.awt.Graphics2D) source.create();
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(21, 58, 47), 0, getHeight(), new Color(30, 111, 84)));
                g.fillRect(0, 0, getWidth(), getHeight());g.dispose();
            }
        };
        sidebar.setBackground(new Color(23, 72, 57));
        sidebar.setPreferredSize(new Dimension(LibraryBrandPanel.EXPANDED_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        addNavigation(sidebar, "dashboard", "⌂   Dashboard");
        addNavigation(sidebar, "books", "▤   Danh sách sách");
        JButton addBook = navButton("+   Thêm sách");
        addBook.addActionListener(event -> actions.add("books"));
        navigation.put("add-books", addBook);
        sidebar.add(addBook);
        sidebar.add(javax.swing.Box.createVerticalStrut(7));
        addNavigation(sidebar, "authors", "✎   Tác giả");
        addNavigation(sidebar, "genres", "▧   Thể loại");
        addNavigation(sidebar, "publishers", "▣   Nhà xuất bản");
        addNavigation(sidebar, "readers", "♙   Độc giả");
        if (user.isAdmin()) addNavigation(sidebar, "employees", "♟   Thủ thư & tài khoản");
        addNavigation(sidebar, "loans", "⇄   Mượn / Trả");

        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setBackground(GREEN);
        top.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        JPanel heading = LibraryStyle.clear(new java.awt.GridBagLayout());
        heading.add(title);
        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        account.setOpaque(false);
        userLabel.setText(user.fullName() + " · " + user.role());
        userLabel.setForeground(Color.WHITE);
        JButton logout = LibraryStyle.button("Đăng xuất", LibraryStyle.BEIGE, INK);
        logout.addActionListener(event -> logout());
        account.add(userLabel);
        account.add(logout);
        top.add(heading, BorderLayout.WEST);
        JPanel accountHolder = LibraryStyle.clear(new java.awt.GridBagLayout());
        accountHolder.add(account);
        top.add(accountHolder, BorderLayout.EAST);

        LibraryBrandPanel brandArea = new LibraryBrandPanel();
        brandArea.addToggleListener(event -> {
            boolean expanded = !sidebar.isVisible();
            sidebar.setVisible(expanded);
            brandArea.setExpanded(expanded);
            root.revalidate();root.repaint();
        });
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0,68));
        header.add(brandArea,BorderLayout.WEST);
        header.add(top,BorderLayout.CENTER);

        content.setBackground(CANVAS);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        addPage("books", "Danh sách sách", RecordType.BOOKS);
        addPage("readers", "Quản lý độc giả", RecordType.READERS);
        addPage("employees", "Thủ thư & tài khoản", RecordType.EMPLOYEES);
        addPage("loans", "Quản lý mượn / trả", null);
        addPage("genres", CatalogType.THE_LOAI);
        addPage("authors", CatalogType.TAC_GIA);
        addPage("publishers", CatalogType.NHA_XUAT_BAN);
        content.add(dashboard, "dashboard");
        root.add(header, BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        selectNavigation("dashboard");
        cards.show(content, "dashboard");
    }

    private void addNavigation(JPanel sidebar, String key, String label) {
        JButton button = navButton(label);
        navigation.put(key, button);
        button.addActionListener(event -> actions.navigate(key));
        sidebar.add(button);
        sidebar.add(javax.swing.Box.createVerticalStrut(7));
    }

    private JButton navButton(String label) {
        JButton button = new JButton(label);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(0f);
        button.setForeground(new Color(233, 243, 235));
        button.setBackground(new Color(23, 72, 57));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 8));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return button;
    }

    private void addPage(String key, String name, RecordType recordType) {
        Page page = new Page(key, name, recordType, null);
        pages.put(key, page);
        content.add(page.panel, key);
    }

    private void addPage(String key, CatalogType catalogType) {
        Page page = new Page(key, catalogType.title(), null, catalogType);
        pages.put(key, page);
        content.add(page.panel, key);
    }

    public void displayForm(String name, String navigationKey, JPanel form) {
        for (java.awt.Component component : content.getComponents()) {
            if ("editor".equals(component.getName())) content.remove(component);
        }
        form.setName("editor");
        content.add(form, "editor");
        cards.show(content, "editor");
        title.setText(name);
        selectNavigation(navigationKey);
        content.revalidate();
        content.repaint();
    }

    public void displayLoanStatistics(Map<String, Long> statistics) {
        pages.get("loans").loansPanel.setStatistics(statistics);
    }

    public java.awt.Component loanActionParent() { return pages.get("loans").loansPanel.actionParent(); }
    public String loanSearch() { return pages.get("loans").loansPanel.searchText(); }
    public String loanStatus() { return pages.get("loans").loansPanel.selectedStatus(); }
    public void setLoanBusy(boolean busy) { pages.get("loans").loansPanel.setDetailsBusy(busy); }
    public void refreshLoanDetails(String loanId, TableData data) { pages.get("loans").loansPanel.refreshDetails(loanId,data); }

    private final class Page {
        private final String title;
        private final JPanel panel = new JPanel(new BorderLayout());
        private BooksPanel recordsPanel;
        private LoansPanel loansPanel;

        private Page(String key, String title, RecordType recordType, CatalogType catalogType) {
            this.title = title;
            panel.setOpaque(false);
            if ("loans".equals(key)) {
                loansPanel = new LoansPanel(() -> actions);
                panel.add(loansPanel);
            } else {
                recordsPanel = new BooksPanel(key, term -> actions.reload(key, term, "ALL"),
                        () -> actions.add(key), row -> actions.edit(key, row), row -> actions.delete(key, row));
                panel.add(recordsPanel, BorderLayout.CENTER);
            }
        }

        private void setData(TableData data) {
            if (loansPanel != null) loansPanel.setData(data);
            else {
                recordsPanel.setData(data);
                panel.revalidate();
            }
        }

    }
}
