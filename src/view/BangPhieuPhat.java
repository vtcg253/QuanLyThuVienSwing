package view;

import model.DuLieuBang;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.BitSet;
import java.util.function.Consumer;

import static view.GiaoDienChung.*;

/** Hiển thị từng lần báo mất thành phiếu phạt riêng và theo dõi trạng thái thanh toán. */
final class BangPhieuPhat extends JPanel {
    private final JTextField search = new JTextField();
    private final JTable table = new JTable() {
        @Override public boolean getScrollableTracksViewportWidth() {
            if (!(getParent() instanceof javax.swing.JViewport viewport)) return false;
            int minimumWidth = 0;
            for (int column = 0; column < getColumnCount(); column++)
                minimumWidth += getColumnModel().getColumn(column).getMinWidth();
            return minimumWidth <= viewport.getWidth();
        }
    };
    private final ThanhPhanTrang pagination = new ThanhPhanTrang(this::applyPage);
    private final Consumer<String> reload;
    private final Consumer<Object[]> markPaid;
    private final JLabel total = label("0", 22, true, INK);
    private final JLabel unpaid = label("0", 22, true, RED);
    private final JLabel paid = label("0", 22, true, GREEN);
    private final JLabel outstanding = label("0 VNĐ", 22, true, RED);
    private DuLieuBang data = new DuLieuBang(new String[0], List.of());

    BangPhieuPhat(Consumer<String> reload, Consumer<Object[]> markPaid) {
        super(new BorderLayout(0, 18));
        this.reload = reload;
        this.markPaid = markPaid;
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(26, 26, 24, 26));

        JPanel heading = clear(new BorderLayout(0, 16));
        JPanel intro = clear(new BorderLayout(0, 6));
        intro.add(label("PHIẾU PHẠT", 11, true, GREEN), BorderLayout.NORTH);
        intro.add(label("Quản lý phạt do mất sách", 27, true, INK), BorderLayout.CENTER);
        intro.add(label("Theo dõi đơn giá tại thời điểm lập, nhân viên xử lý và tình trạng thanh toán.",
                14, false, MUTED), BorderLayout.SOUTH);
        heading.add(intro, BorderLayout.NORTH);

        JPanel cards = clear(new java.awt.GridLayout(1, 4, 12, 0));
        cards.add(stat("Tổng phiếu phạt", total, new Color(242, 249, 244)));
        cards.add(stat("Chưa thanh toán", unpaid, new Color(255, 245, 241)));
        cards.add(stat("Đã thanh toán", paid, new Color(242, 249, 244)));
        cards.add(stat("Cần thu", outstanding, new Color(255, 245, 241)));
        heading.add(cards, BorderLayout.CENTER);

        JPanel filter = clear(new BorderLayout(10, 0));
        field(search);
        search.setToolTipText("Tìm mã phạt, phiếu mượn, độc giả hoặc sách");
        JButton find = button("Tìm kiếm", GREEN, Color.WHITE);
        find.addActionListener(event -> reload.accept(search.getText()));
        search.addActionListener(event -> reload.accept(search.getText()));
        filter.add(search, BorderLayout.CENTER);
        filter.add(find, BorderLayout.EAST);
        heading.add(filter, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(58);
        table.setFillsViewportHeight(true);
        table.setUI(new javax.swing.plaf.basic.BasicTableUI());
        table.setForeground(INK);
        table.setBackground(PAPER);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(237, 244, 236));
        table.setSelectionForeground(INK);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 48));
        table.getTableHeader().setDefaultRenderer((source, value, selected, focus, row, column) -> {
            JLabel header = label(String.valueOf(value).toUpperCase(Locale.ROOT), 12, true, MUTED);
            header.setOpaque(true);
            header.setBackground(PAPER);
            header.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, LINE),
                    javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8)));
            return header;
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && table.columnAtPoint(event.getPoint()) != actionColumn()) {
                    showReceipt(selectedRow());
                }
            }
        });
        JScrollPane result = new JScrollPane(table);
        result.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        result.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        result.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        result.getViewport().setBackground(PAPER);
        result.getHorizontalScrollBar().setUnitIncrement(24);
        result.getVerticalScrollBar().setUnitIncrement(20);
        result.setColumnHeaderView(table.getTableHeader());
        add(result, BorderLayout.CENTER);
        add(pagination, BorderLayout.SOUTH);
    }

    String searchText() { return search.getText(); }

    void setData(DuLieuBang data) {
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        table.setRowSorter(null);
        this.data = data;
        long outstandingAmount = 0;
        int unpaidCount = 0;
        int paidCount = 0;
        for (Object[] row : data.rows()) {
            if ("CHUA_THANH_TOAN".equals(value(row, 10))) {
                unpaidCount++;
                outstandingAmount += number(row, 9);
            } else {
                paidCount++;
            }
        }
        total.setText(Integer.toString(data.rows().size()));
        unpaid.setText(Integer.toString(unpaidCount));
        paid.setText(Integer.toString(paidCount));
        outstanding.setText(money(outstandingAmount));

        // Giữ danh sách ngắn gọn; hộp thoại chi tiết hiển thị đầy đủ phiếu phạt.
        String[] columns = {"Mã phạt", "Ngày lập", "Phiếu mượn", "Độc giả", "Tên sách", "Thành tiền (VNĐ)", "Trạng thái", "Thao tác"};
        int[] sourceColumns = {0, 1, 2, 3, 6, 9, 10};
        Object[][] rows = new Object[data.rows().size()][columns.length];
        for (int i = 0; i < rows.length; i++) {
            Object[] source = data.rows().get(i);
            for (int column = 0; column < sourceColumns.length; column++)
                rows[i][column] = sourceColumns[column] < source.length ? source[sourceColumns[column]] : null;
        }
        table.setModel(new DefaultTableModel(rows, columns) {
            @Override public boolean isCellEditable(int row, int column) { return column == actionColumn(); }
        });
        table.setAutoCreateRowSorter(true);
        ((TableRowSorter<?>) table.getRowSorter()).setSortable(actionColumn(), false);
        table.getRowSorter().addRowSorterListener(event -> {
            if (event.getType() == javax.swing.event.RowSorterEvent.Type.SORT_ORDER_CHANGED)
                pagination.firstPage();
        });
        table.setDefaultRenderer(Object.class, cellRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(moneyRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(statusRenderer());
        table.getColumnModel().getColumn(actionColumn()).setCellRenderer(actionsRenderer());
        table.getColumnModel().getColumn(actionColumn()).setCellEditor(new ActionsEditor());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sizeColumnsToContent();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        pagination.setTotal(rows.length);
        applyPage();
    }

    private void applyPage() {
        if (!(table.getRowSorter() instanceof TableRowSorter<?> sorter)) return;
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();

        TableRowSorter<javax.swing.table.TableModel> ordered = new TableRowSorter<>(table.getModel());
        ordered.setSortKeys(sorter.getSortKeys());
        BitSet included = new BitSet(table.getModel().getRowCount());
        for (int i = pagination.offset(); i < pagination.end(); i++)
            included.set(ordered.convertRowIndexToModel(i));
        @SuppressWarnings("unchecked")
        TableRowSorter<javax.swing.table.TableModel> pageSorter =
                (TableRowSorter<javax.swing.table.TableModel>) sorter;
        pageSorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends javax.swing.table.TableModel, ? extends Integer> entry) {
                return included.get(entry.getIdentifier());
            }
        });
        table.clearSelection();
        table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredSize().width,
                Math.max(1, table.getRowCount()) * table.getRowHeight()));
        table.scrollRectToVisible(new java.awt.Rectangle(0, 0, 1, 1));
        revalidate();
        repaint();
    }

    private int actionColumn() { return table.getColumnCount() - 1; }

    private void sizeColumnsToContent() {
        for (int column = 0; column < table.getColumnCount(); column++) {
            javax.swing.table.TableColumn tableColumn = table.getColumnModel().getColumn(column);
            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component header = headerRenderer.getTableCellRendererComponent(table, tableColumn.getHeaderValue(),
                    false, false, -1, column);
            int headerWidth = header.getPreferredSize().width;
            int contentWidth = headerWidth;
            int protectedWidth = headerWidth;
            for (int row = 0; row < table.getRowCount(); row++) {
                Component cell = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                int cellWidth = cell.getPreferredSize().width;
                contentWidth = Math.max(contentWidth, cellWidth);
                if (column == 6 || column == actionColumn())
                    protectedWidth = Math.max(protectedWidth, cellWidth);
            }
            tableColumn.setMinWidth(protectedWidth);
            tableColumn.setPreferredWidth(contentWidth);
            tableColumn.setWidth(contentWidth);
        }
    }

    private JPanel stat(String title, JLabel value, Color background) {
        JPanel card = card(new BorderLayout(0, 6), background);
        card.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.add(label(title, 12, true, MUTED), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private TableCellRenderer cellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                    boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(source, display(value), selected, false, row, column);
                cell.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
                cell.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                cell.setForeground(INK);
                cell.setBackground(selected ? source.getSelectionBackground() : PAPER);
                cell.setToolTipText(display(value));
                return cell;
            }
        };
    }

    private TableCellRenderer moneyRenderer() {
        return (source, value, selected, focus, row, column) -> {
            JLabel cell = (JLabel) cellRenderer().getTableCellRendererComponent(source, money(number(value)), selected, focus, row, column);
            cell.setHorizontalAlignment(SwingConstants.RIGHT);
            return cell;
        };
    }

    private TableCellRenderer statusRenderer() {
        return (source, value, selected, focus, row, column) -> {
            JPanel panel = clear(new FlowLayout(FlowLayout.LEFT, 8, 12));
            panel.setBackground(selected ? source.getSelectionBackground() : PAPER);
            boolean isPaid = "DA_THANH_TOAN".equals(String.valueOf(value));
            panel.add(badge(isPaid ? "Đã thanh toán" : "Chưa thanh toán",
                    isPaid ? new Color(216, 242, 229) : new Color(255, 234, 196),
                    isPaid ? GREEN : new Color(142, 94, 23)));
            return panel;
        };
    }

    private TableCellRenderer actionsRenderer() {
        return (source, value, selected, focus, row, column) -> {
            JPanel panel = actions(rowAt(source, row), false);
            panel.setSize(source.getColumnModel().getColumn(column).getWidth(), source.getRowHeight());
            panel.doLayout();
            return panel;
        };
    }

    private Object[] selectedRow() {
        int row = table.getSelectedRow();
        return row < 0 ? null : rowAt(table, row);
    }

    private Object[] rowAt(JTable source, int viewRow) {
        return data.rows().get(source.convertRowIndexToModel(viewRow));
    }

    private JPanel actions(Object[] row, boolean interactive) {
        JPanel panel = clear(new FlowLayout(FlowLayout.RIGHT, 6, 9));
        panel.setBackground(PAPER);
        JButton detail = button("Chi tiết", BEIGE, INK);
        detail.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 10, 7, 10));
        if (interactive) detail.addActionListener(event -> showReceipt(row));
        else detail.setEnabled(false);
        panel.add(detail);
        if ("CHUA_THANH_TOAN".equals(value(row, 10))) {
            JButton collect = button("Đã thu", GREEN, Color.WHITE);
            collect.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 10, 7, 10));
            if (interactive) collect.addActionListener(event -> markPaid.accept(row.clone()));
            else collect.setEnabled(false);
            panel.add(collect);
        }
        return panel;
    }

    private void showReceipt(Object[] row) {
        if (row == null) return;
        HopThoaiXacNhan.showContent(this, "Chi tiết phiếu phạt · " + value(row, 0), receiptContent(row));
    }

    private JPanel receiptContent(Object[] row) {
        boolean isPaid = "DA_THANH_TOAN".equals(value(row, 10));
        JPanel content = clear(new BorderLayout(0, 18));

        JPanel summary = card(new BorderLayout(16, 0), isPaid ? new Color(242, 249, 244) : new Color(255, 245, 241));
        summary.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel amount = clear(new BorderLayout(0, 4));
        amount.add(label("TỔNG TIỀN PHẠT", 11, true, MUTED), BorderLayout.NORTH);
        amount.add(label(money(number(row, 9)), 24, true, isPaid ? GREEN : RED), BorderLayout.CENTER);
        summary.add(amount, BorderLayout.CENTER);
        summary.add(badge(isPaid ? "Đã thanh toán" : "Chưa thanh toán",
                isPaid ? new Color(216, 242, 229) : new Color(255, 234, 196),
                isPaid ? GREEN : new Color(142, 94, 23)), BorderLayout.EAST);
        content.add(summary, BorderLayout.NORTH);

        JPanel sections = clear(new BorderLayout(0, 12));
        JPanel blocks = clear(new BorderLayout());
        blocks.setLayout(new javax.swing.BoxLayout(blocks, javax.swing.BoxLayout.Y_AXIS));
        blocks.add(detailSection("Thông tin phiếu phạt",
                "Mã phiếu phạt", value(row, 0),
                "Ngày lập", display(row[1]),
                "Phiếu mượn", value(row, 2),
                "Độc giả", value(row, 3)));
        blocks.add(detailSection("Thông tin sách mất",
                "Tên sách", value(row, 6),
                "Mã sách", value(row, 5),
                "Số lượng mất", value(row, 7) + " cuốn",
                "Đơn giá phạt", money(number(row, 8))));
        blocks.add(detailSection("Thanh toán",
                "Nhân viên xử lý", value(row, 4),
                "Ngày thanh toán", display(row[11])));
        sections.add(blocks, BorderLayout.CENTER);
        content.add(sections, BorderLayout.CENTER);
        content.setPreferredSize(new Dimension(590, content.getPreferredSize().height));
        return content;
    }

    private JPanel detailSection(String title, String... fields) {
        JPanel section = clear(new BorderLayout(0, 9));
        section.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, LINE),
                javax.swing.BorderFactory.createEmptyBorder(12, 0, 14, 0)));
        section.add(label(title, 15, true, INK), BorderLayout.NORTH);
        JPanel grid = clear(new java.awt.GridLayout(0, 2, 18, 12));
        for (int i = 0; i + 1 < fields.length; i += 2) {
            JPanel field = clear(new BorderLayout(0, 4));
            field.add(label(fields[i], 11, false, MUTED), BorderLayout.NORTH);
            JLabel value = new JLabel("<html><div style='width:245px'>" + html(fields[i + 1]) + "</div></html>");
            value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            value.setForeground(INK);
            value.setToolTipText(fields[i + 1]);
            field.add(value, BorderLayout.CENTER);
            grid.add(field);
        }
        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    private static String value(Object[] row, int index) {
        return index < row.length && row[index] != null ? row[index].toString() : "—";
    }

    private static long number(Object[] row, int index) { return index < row.length ? number(row[index]) : 0; }
    private static long number(Object value) { return value instanceof Number number ? number.longValue() : 0; }
    private static String money(long amount) { return java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " VNĐ"; }
    private static String display(Object value) {
        if (value == null || value.toString().isBlank()) return "—";
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof LocalDate date) return date.toString();
        return value.toString();
    }
    private static String html(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    private final class ActionsEditor extends AbstractCellEditor implements TableCellEditor {
        @Override public Object getCellEditorValue() { return null; }
        @Override public Component getTableCellEditorComponent(JTable source, Object value, boolean selected,
                int row, int column) {
            Object[] penalty = rowAt(source, row);
            JPanel actions = actions(penalty, true);
            for (Component component : actions.getComponents()) {
                if (component instanceof JButton button) button.addActionListener(event -> fireEditingStopped());
            }
            return actions;
        }
    }
}
