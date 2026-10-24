package view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.util.function.Consumer;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import static view.LibraryStyle.*;

/** Loan details and per-book actions used by the resizable details popup. */
final class LoanDetailsPanel extends JPanel {
    private final List<Object[]> records;
    private final Consumer<Object[]> returnBook;
    private final Consumer<Object[]> reportLost;
    private final JTable table;
    private boolean busy;

    LoanDetailsPanel(List<Object[]> rows, Consumer<Object[]> returnBook,
                     Consumer<Object[]> reportLost, Runnable onClose) {
        super(new BorderLayout(0, 20));
        if (rows.isEmpty()) throw new IllegalArgumentException("Phiếu mượn không có dữ liệu.");
        records = rows.stream().map(Object[]::clone).toList();
        this.returnBook = returnBook;
        this.reportLost = reportLost;
        setOpaque(false);
        setName("loan-details");
        Object[] first = rows.get(0);
        JPanel card = card(new BorderLayout(0, 22));
        JPanel top = clear(new BorderLayout(0, 20));
        JPanel heading = clear(new BorderLayout(16, 0));
        JPanel title = clear(new GridLayout(0, 1, 0, 8));
        title.add(label("CHI TIẾT MƯỢN / TRẢ", 11, true, GREEN));
        title.add(label("Phiếu mượn " + text(first[0]), 27, true, INK));
        heading.add(title, BorderLayout.CENTER);
        JButton back = button("Đóng", BEIGE, INK);
        back.addActionListener(event -> onClose.run());
        JPanel navigation = clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        navigation.add(back);
        heading.add(navigation, BorderLayout.EAST);
        top.add(heading, BorderLayout.NORTH);

        JPanel info = clear(new GridLayout(0, 2, 24, 14));
        info.add(info("Độc giả", text(first[1]) + " · " + text(first[11])));
        info.add(info("Nhân viên lập", text(first[2]) + " · " + text(first[12])));
        info.add(info("Số điện thoại độc giả", text(value(first,14))));
        info.add(info("Số điện thoại nhân viên", text(value(first,17))));
        info.add(info("Ngày sinh độc giả", BirthDateField.display(value(first,13))));
        info.add(info("Ngày sinh nhân viên", BirthDateField.display(value(first,16))));
        info.add(info("Địa chỉ độc giả", text(value(first,15))));
        info.add(info("Địa chỉ nhân viên", text(value(first,18))));
        info.add(info("Ngày mượn", dateText(first[3])));
        info.add(info("Hạn trả", dateText(first[4])));
        top.add(info, BorderLayout.CENTER);

        long borrowed = 0, returned = 0, lost = 0, remaining = 0;
        Object[][] values = new Object[rows.size()][9];
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            int quantity = number(row[7]), missing = number(row[8]);
            boolean isReturned = "RETURNED".equals(text(row[10]));
            boolean isBorrowed = "BORROWED".equals(text(row[10]));
            int returnedCount = isReturned ? Math.max(0, quantity - missing) : 0;
            int pendingCount = isBorrowed ? Math.max(0, quantity - missing) : 0;
            borrowed += quantity; returned += returnedCount; lost += missing; remaining += pendingCount;
            values[i] = new Object[]{row[6], row[5], quantity, returnedCount, missing, pendingCount,
                    dateText(row[9]), status(row), null};
        }
        JPanel totals = clear(new GridLayout(1, 4, 12, 0));
        totals.add(total("Cuốn đã mượn", borrowed, INK));
        totals.add(total("Cuốn đã trả", returned, GREEN));
        totals.add(total("Cuốn đã mất", lost, RED));
        totals.add(total("Cuốn còn mượn", remaining, INK));
        top.add(totals, BorderLayout.SOUTH);
        card.add(top, BorderLayout.NORTH);

        JPanel books = clear(new BorderLayout(0, 12));
        books.add(label("Sách trong phiếu · " + rows.size() + " đầu sách", 17, true, INK), BorderLayout.NORTH);
        table = new JTable(new DefaultTableModel(values, new String[]{"Mã sách", "Tên sách", "SL mượn",
                "Đã trả", "Đã mất", "Còn mượn", "Ngày trả", "Trạng thái", "Thao tác"}) {
            @Override public boolean isCellEditable(int row, int column) {
                return !busy && column == 8 && actionable(records.get(row));
            }
            @Override public Class<?> getColumnClass(int column) {
                return column >= 2 && column <= 5 ? Integer.class : Object.class;
            }
        }) {
            @Override public boolean getScrollableTracksViewportWidth() {
                if (getParent() instanceof JViewport viewport)
                    return viewport.getWidth() >= getMinimumSize().width;
                return super.getScrollableTracksViewportWidth();
            }
        };
        table.setName("loan-detail-books");
        table.setUI(new javax.swing.plaf.basic.BasicTableUI());
        table.setRowHeight(58);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        table.setForeground(INK);table.setBackground(PAPER);
        table.setSelectionBackground(new Color(225, 241, 231));table.setSelectionForeground(INK);
        table.setShowVerticalLines(false);table.setGridColor(LINE);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel header = label(String.valueOf(value), 12, true, MUTED);
            header.setOpaque(true);header.setBackground(PAPER);
            header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,LINE),
                    BorderFactory.createEmptyBorder(0,8,0,8)));
            return header;
        });
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t,value,selected,false,row,column);
                cell.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
                cell.setHorizontalAlignment(value instanceof Number ? SwingConstants.CENTER : SwingConstants.LEFT);
                cell.setToolTipText(String.valueOf(value));return cell;
            }
        };
        table.setDefaultRenderer(Object.class,renderer);table.setDefaultRenderer(Integer.class,renderer);
        int[] widths = {75, 230, 75, 65, 65, 85, 110, 115, 290};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(i == 1 ? 180 : widths[i]);
        }
        ((javax.swing.table.TableRowSorter<?>) table.getRowSorter()).setSortable(8, false);
        table.getColumnModel().getColumn(8).setCellRenderer((t,value,selected,focus,row,col) -> {
            JPanel controls = controls(records.get(t.convertRowIndexToModel(row)), null, null);
            controls.setSize(t.getColumnModel().getColumn(col).getWidth(),t.getRowHeight());
            controls.doLayout();return controls;
        });
        table.getColumnModel().getColumn(8).setCellEditor(new ActionEditor());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.getViewport().setBackground(PAPER);
        scroll.setPreferredSize(new Dimension(0, Math.max(100, Math.min(400, rows.size()*58+42))));
        books.add(scroll, BorderLayout.CENTER);
        card.add(books, BorderLayout.CENTER);
        JPanel body = clear(new BorderLayout());
        body.add(card, BorderLayout.NORTH);
        add(scroll(body));
    }

    void setBusy(boolean busy) {
        this.busy = busy;
        if (table.isEditing()) table.getCellEditor().cancelCellEditing();
        table.setEnabled(!busy);table.repaint();
    }

    private static boolean actionable(Object[] row) {
        return "BORROWED".equals(text(row[10])) && number(row[7]) > number(row[8]);
    }

    private JPanel controls(Object[] row, Runnable onReturn, Runnable onLost) {
        JPanel panel = clear(new FlowLayout(FlowLayout.LEFT, 6, 10));
        if (actionable(row)) {
            JButton returned = button("Trả còn lại", GREEN, Color.WHITE);
            JButton lost = button("Ghi nhận mất", RED, Color.WHITE);
            returned.setEnabled(!busy);lost.setEnabled(!busy);
            if (busy) { returned.setForeground(new Color(210,220,212));lost.setForeground(new Color(230,210,210)); }
            if (onReturn != null) returned.addActionListener(event -> onReturn.run());
            if (onLost != null) lost.addActionListener(event -> onLost.run());
            panel.add(returned);panel.add(lost);
        } else panel.add(label("Đã xử lý", 13, false, MUTED));
        return panel;
    }

    private final class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        @Override public Object getCellEditorValue() { return null; }
        @Override public Component getTableCellEditorComponent(JTable t, Object value, boolean selected, int row, int column) {
            Object[] record = records.get(t.convertRowIndexToModel(row)).clone();
            return controls(record,
                    () -> { fireEditingStopped();returnBook.accept(record); },
                    () -> { fireEditingStopped();reportLost.accept(record); });
        }
    }

    private static JPanel info(String name, String value) {
        JPanel group = clear(new BorderLayout(0, 6));
        group.add(label(name, 12, false, MUTED), BorderLayout.NORTH);
        JLabel content = label(value, 15, true, INK);content.setToolTipText(value);
        group.add(content);return group;
    }
    private static JPanel total(String name, long value, Color color) {
        JPanel tile = card(new BorderLayout(0, 6), new Color(243, 248, 244));
        tile.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        tile.add(label(name,12,false,MUTED),BorderLayout.NORTH);
        tile.add(label(Long.toString(value),24,true,color));return tile;
    }
    private static String status(Object[] row) {
        return switch (text(row[10])) {
            case "RETURNED" -> "Đã trả";
            case "LOST" -> "Đã mất";
            case "BORROWED" -> {
                LocalDate due = date(row[4]);
                yield due != null && due.isBefore(LocalDate.now()) ? "Quá hạn" : "Đang mượn";
            }
            default -> "Chưa xác định";
        };
    }
    private static Object value(Object[] row, int index) { return index < row.length ? row[index] : null; }
    private static int number(Object value) { return value == null ? 0 : ((Number)value).intValue(); }
    private static String text(Object value) { return value == null || value.toString().isBlank() ? "—" : value.toString(); }
    private static LocalDate date(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate local) return local;
        if (value instanceof java.sql.Date sql) return sql.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        try { return LocalDate.parse(value.toString().substring(0,10)); }
        catch (RuntimeException exception) { return null; }
    }
    private static String dateText(Object value) { LocalDate date = date(value);return date == null ? "—" : date.toString(); }
}
