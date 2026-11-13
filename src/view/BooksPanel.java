package view;

import model.TableData;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.function.Consumer;

/** Book catalogue styled to match the library's cream and green design. */
final class BooksPanel extends JPanel {
    private static final Color PAPER = new Color(255, 253, 249);
    private static final Color INK = new Color(30, 47, 40);
    private static final Color MUTED = new Color(112, 123, 116);
    private static final Color LINE = new Color(232, 224, 209);
    private static final Color GREEN = new Color(32, 112, 84);
    private final JTable table = new JTable();
    private final PaginationBar pagination = new PaginationBar(this::applyPage);
    private final Consumer<Object[]> edit;
    private final Consumer<Object[]> delete;
    private int dataColumns;
    private final boolean bookMode;
    private final String page;
    private java.util.List<Object[]> sourceRows = java.util.List.of();

    BooksPanel(Consumer<String> searchAction, Runnable addAction,
               Consumer<Object[]> edit, Consumer<Object[]> delete) {
        this("books", searchAction, addAction, edit, delete);
    }

    BooksPanel(String page, Consumer<String> searchAction, Runnable addAction,
               Consumer<Object[]> edit, Consumer<Object[]> delete) {
        super(new BorderLayout(0, 18));
        this.page = page;
        this.bookMode = "books".equals(page);
        this.edit = edit;
        this.delete = delete;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(26, 26, 24, 26));
        JPanel heading = new JPanel(new BorderLayout(0, 18));
        heading.setOpaque(false);
        JPanel intro = new JPanel(new BorderLayout(0, 14));
        intro.setOpaque(false);
        String title = switch (page) {
            case "readers" -> "Quản lý độc giả";
            case "employees" -> "Quản lý thủ thư & tài khoản";
            case "authors" -> "Quản lý tác giả";
            case "genres" -> "Quản lý thể loại";
            case "publishers" -> "Quản lý nhà xuất bản";
            default -> "Danh sách sách";
        };
        String description = switch (page) {
            case "readers" -> "Quản lý thông tin bạn đọc đăng ký mượn sách tại thư viện.";
            case "employees" -> "Quản lý hồ sơ thủ thư, tài khoản và quyền truy cập thư viện.";
            case "authors" -> "Quản lý thông tin tác giả và các đầu sách trong thư viện.";
            case "genres" -> "Phân loại sách để tra cứu và quản lý dễ dàng hơn.";
            case "publishers" -> "Quản lý thông tin liên hệ các nhà xuất bản.";
            default -> "Tìm và quản lý sách theo mã, tên sách, thể loại, tác giả hoặc nhà xuất bản.";
        };
        intro.add(text(title, 27, true, INK), BorderLayout.NORTH);
        intro.add(text(description,
                15, false, MUTED), BorderLayout.CENTER);
        heading.add(intro, BorderLayout.NORTH);
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JTextField search = new JTextField();
        search.setToolTipText(bookMode ? "Tìm theo mã, tên sách, thể loại, tác giả hoặc nhà xuất bản" : "Tìm theo mã hoặc tên");
        search.getAccessibleContext().setAccessibleName("Tìm kiếm");
        search.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        LibraryStyle.field(search);
        search.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { search.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { search.repaint(); }
        });
        JButton find = new RoundedButton("Tìm kiếm", GREEN, Color.WHITE);
        JButton add = new RoundedButton(switch (page) {
            case "readers" -> "+ Thêm độc giả"; case "employees" -> "+ Thêm thủ thư";
            case "authors" -> "+ Thêm tác giả"; case "genres" -> "+ Thêm thể loại";
            case "publishers" -> "+ Thêm nhà xuất bản"; default -> "+ Thêm sách";
        }, GREEN, Color.WHITE);
        find.addActionListener(event -> searchAction.accept(search.getText()));
        search.addActionListener(event -> searchAction.accept(search.getText()));
        add.addActionListener(event -> addAction.run());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.add(find); controls.add(add);
        toolbar.add(search, BorderLayout.CENTER);
        toolbar.add(controls, BorderLayout.EAST);
        heading.add(toolbar, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        table.setUI(new javax.swing.plaf.basic.BasicTableUI());
        table.setForeground(INK);
        table.setBackground(PAPER);
        table.setRowHeight(58);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(237, 244, 236));
        table.setSelectionForeground(INK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 48));
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, col) -> {
            JLabel label = text(value.toString().toUpperCase(java.util.Locale.ROOT), 12, true, MUTED);
            label.setOpaque(true); label.setBackground(PAPER);
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            return label;
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, false, row, column);
                cell.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (value == null || value.toString().isBlank()) cell.setText("—");
                if (("readers".equals(page) || "employees".equals(page)) && t.convertColumnIndexToModel(column)==2)
                    cell.setText(BirthDateField.display(value));
                cell.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
                cell.setToolTipText(value == null ? null : value.toString());
                return cell;
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        // Keep the footer visible while the rows scroll inside the table.
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(PAPER);
        add(scroll, BorderLayout.CENTER);
        add(pagination, BorderLayout.SOUTH);
    }

    void setData(TableData data) {
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        // Detach the previous page filter before replacing the result set.
        table.setRowSorter(null);
        sourceRows = data.rows();
        dataColumns = "employees".equals(page) ? 5 : data.columns().length;
        String[] headers = "employees".equals(page)
                ? new String[]{"Mã", "Họ và tên", "Ngày sinh", "Số điện thoại", "Tài khoản", ""}
                : java.util.Arrays.copyOf(data.columns(), dataColumns + 1);
        headers[dataColumns] = bookMode ? "" : "Thao tác";
        if (!bookMode) headers[0] = "Mã";
        // Short headings leave space for the title and author columns.
        if (bookMode) { headers[4] = "NXB"; headers[5] = "Năm XB"; }
        Object[][] rows = new Object[data.rows().size()][dataColumns + 1];
        for (int i = 0; i < rows.length; i++) {
            Object[] source = data.rows().get(i);
            if ("employees".equals(page)) {
                System.arraycopy(source, 0, rows[i], 0, 4);
                rows[i][4] = source[5];
            } else System.arraycopy(source, 0, rows[i], 0, dataColumns);
        }
        table.setModel(new DefaultTableModel(rows, headers) {
            @Override public boolean isCellEditable(int row, int col) { return col == dataColumns; }
        });
        table.setAutoCreateRowSorter(true);
        ((TableRowSorter<?>) table.getRowSorter()).setSortable(dataColumns, false);
        int[] widths = {75, 260, 160, 145, 145, 80, 80, 142};
        for (int i = 0; i < dataColumns; i++) table.getColumnModel().getColumn(i).setPreferredWidth(
                bookMode ? widths[i] : i == 0 ? 90 : i == 1 ? 240 : 170);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        TableColumn actions = table.getColumnModel().getColumn(dataColumns);
        actions.setMinWidth(142); actions.setMaxWidth(142);
        actions.setCellRenderer((t, value, selected, focus, row, col) -> {
            JPanel buttons = actionButtons(null, null);
            buttons.setSize(t.getColumnModel().getColumn(col).getWidth(), t.getRowHeight());
            buttons.doLayout();
            return buttons;
        });
        actions.setCellEditor(new ActionEditor());
        if (bookMode) table.getColumnModel().getColumn(6).setCellRenderer((t, value, selected, focus, row, col) -> {
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 15));
            cell.setBackground(selected ? table.getSelectionBackground() : PAPER);
            JLabel badge = new JLabel(value == null ? "0" : value.toString()) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(216, 242, 229));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24); g.dispose();
                    super.paintComponent(graphics);
                }
            };
            badge.setForeground(GREEN); badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            badge.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9)); cell.add(badge);
            cell.setSize(t.getColumnModel().getColumn(col).getWidth(), t.getRowHeight());
            cell.doLayout();
            return cell;
        });
        table.getRowSorter().addRowSorterListener(event -> {
            if (event.getType() == javax.swing.event.RowSorterEvent.Type.SORT_ORDER_CHANGED)
                pagination.firstPage();
        });
        pagination.setTotal(rows.length);
        applyPage();
    }

    private void applyPage() {
        if (!(table.getRowSorter() instanceof TableRowSorter<?> sorter)) return;
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        // Sort the full result first so sorting remains correct across page boundaries.
        TableRowSorter<javax.swing.table.TableModel> ordered = new TableRowSorter<>(table.getModel());
        ordered.setSortKeys(sorter.getSortKeys());
        java.util.BitSet included = new java.util.BitSet(table.getModel().getRowCount());
        for (int i = pagination.offset(); i < pagination.end(); i++)
            included.set(ordered.convertRowIndexToModel(i));
        sorter.setRowFilter(new RowFilter<javax.swing.table.TableModel, Integer>() {
            @Override public boolean include(Entry<? extends javax.swing.table.TableModel, ? extends Integer> entry) {
                return included.get(entry.getIdentifier());
            }
        });
        table.clearSelection();
        table.setPreferredScrollableViewportSize(new Dimension(0,
                Math.max(1, table.getRowCount()) * table.getRowHeight()));
        table.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        revalidate();repaint();
    }

    private Object[] rowData(int viewRow) {
        int row = table.convertRowIndexToModel(viewRow);
        return sourceRows.get(row).clone();
    }

    private JPanel actionButtons(Runnable onEdit, Runnable onDelete) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 11));
        panel.setBackground(PAPER);
        JButton editButton = new RoundedButton("Sửa", new Color(236, 230, 216), INK);
        JButton deleteButton = new RoundedButton("Xóa", new Color(184, 53, 56), Color.WHITE);
        editButton.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        deleteButton.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
        if (onEdit != null) editButton.addActionListener(event -> onEdit.run());
        if (onDelete != null) deleteButton.addActionListener(event -> onDelete.run());
        panel.add(editButton);
        if (delete != null) panel.add(deleteButton);
        return panel;
    }

    private final class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        @Override public Object getCellEditorValue() { return null; }
        @Override public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int row, int col) {
            Object[] record = rowData(row);
            return actionButtons(() -> { fireEditingStopped(); edit.accept(record); },
                    () -> { fireEditingStopped(); delete.accept(record); });
        }
    }

    private static JLabel text(String value, int size, boolean bold, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color); return label;
    }

    private static final class RoundedButton extends JButton {
        private final Color fill;
        RoundedButton(String label, Color fill, Color foreground) {
            super(label); this.fill = fill;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            setForeground(foreground); setContentAreaFilled(false); setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isPressed() ? fill.darker() : fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14); g.dispose();
            super.paintComponent(graphics);
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(PAPER); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
        g.setColor(LINE); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24); g.dispose();
    }
}
