package view;

import model.TableData;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static view.LibraryStyle.*;

public final class LoanEditorPanel extends JPanel {
    public record Request(String readerId, LocalDate borrowed, LocalDate due, Map<String, Integer> books) {
    }

    private final JPanel lines = clear(new GridBagLayout());
    private final List<BookLine> entries = new ArrayList<>();
    private final TableData books;
    private final JButton add = button("+ Thêm dòng sách", BEIGE, INK);
    private boolean refreshing;
    private boolean busy;
    private final JLabel message = label(" ", 13, false, RED);
    private final JButton save = button("Lập phiếu mượn", GREEN, Color.WHITE), cancel = button("Hủy", BEIGE, INK);

    public LoanEditorPanel(TableData readers, TableData books, Consumer<Request> onSave, Runnable onCancel) {
        super(new BorderLayout());
        setOpaque(false);
        this.books = books;
        JPanel card = card(new BorderLayout(0, 22));
        JPanel intro = clear(new GridLayout(0, 1, 0, 10));
        intro.add(label("Lập phiếu mượn mới", 27, true, INK));
        intro.add(label("Chọn độc giả, thời hạn mượn và các sách cần mượn.", 14, false, MUTED));
        card.add(intro, BorderLayout.NORTH);
        JPanel body = clear(new BorderLayout(0, 22));
        JPanel fields = clear(new GridLayout(1, 3, 16, 0));
        JComboBox<RecordEditorPanel.Choice> reader = new JComboBox<>();
        reader.addItem(new RecordEditorPanel.Choice("", ""));
        for (Object[] row : readers.rows())
            reader.addItem(new RecordEditorPanel.Choice(row[0].toString(), row[1] + " · " + row[0]));
        field(reader);
        JTextField borrowed = new JTextField(LocalDate.now().toString()), due = new JTextField(LocalDate.now().plusDays(14).toString());
        field(borrowed);
        field(due);
        fields.add(group("Độc giả *", reader));
        fields.add(group("Ngày mượn (YYYY-MM-DD)", borrowed));
        fields.add(group("Hạn trả (YYYY-MM-DD)", due));
        body.add(fields, BorderLayout.NORTH);
        JPanel bookSection = clear(new BorderLayout(0, 14));
        JPanel heading = clear(new BorderLayout());
        heading.add(label("Sách mượn", 18, true, INK));
        add.addActionListener(event -> addLine());
        heading.add(add, BorderLayout.EAST);
        bookSection.add(heading, BorderLayout.NORTH);
        bookSection.add(lines, BorderLayout.CENTER);
        body.add(bookSection);
        JPanel footer = clear(new BorderLayout(0, 10));
        footer.add(message, BorderLayout.NORTH);
        JPanel controls = clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(save);
        controls.add(cancel);
        footer.add(controls, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        card.add(body);
        JPanel wrap = clear(new BorderLayout());
        wrap.add(card, BorderLayout.NORTH);
        add(scroll(wrap));
        addLine();
        save.addActionListener(event -> {
            try {
                RecordEditorPanel.Choice selected = (RecordEditorPanel.Choice) reader.getSelectedItem();
                if (selected == null || selected.id().isBlank())
                    throw new IllegalArgumentException("Vui lòng chọn độc giả.");
                LocalDate start = LocalDate.parse(borrowed.getText().trim()), end = LocalDate.parse(due.getText().trim());
                if (end.isBefore(start)) throw new IllegalArgumentException("Hạn trả phải từ ngày mượn trở đi.");
                Map<String, Integer> requested = new LinkedHashMap<>();
                for (BookLine line : entries) {
                    RecordEditorPanel.Choice book = (RecordEditorPanel.Choice) line.book.getSelectedItem();
                    if (book == null || book.id().isBlank())
                        throw new IllegalArgumentException("Vui lòng chọn sách trên mỗi dòng.");
                    line.quantity.commitEdit();
                    if (requested.putIfAbsent(book.id(), ((Number) line.quantity.getValue()).intValue()) != null)
                        throw new IllegalArgumentException("Mỗi sách chỉ được chọn một dòng. Hãy tăng số lượng tại dòng đã chọn.");
                    if (requested.values().stream().mapToLong(Integer::longValue).sum() > 5)
                        throw new IllegalArgumentException("Mỗi phiếu chỉ được mượn tối đa 5 cuốn sách (tổng số lượng tất cả các dòng).");
                }
                if (requested.isEmpty()) throw new IllegalArgumentException("Thêm ít nhất một sách để lập phiếu.");
                for (Object[] row : books.rows())
                    if (requested.getOrDefault(row[0].toString(), 0) > ((Number) row[6]).intValue())
                        throw new IllegalArgumentException("Số lượng mượn vượt tồn kho của sách: " + row[1]);
                message.setText(" ");
                onSave.accept(new Request(selected.id(), start, end, requested));
            } catch (java.time.format.DateTimeParseException ex) {
                message.setText("Ngày phải theo định dạng YYYY-MM-DD.");
            } catch (Exception ex) {
                message.setText(ex.getMessage());
            }
        });
        cancel.addActionListener(event -> onCancel.run());
    }

    private JPanel group(String name, JComponent input) {
        JPanel group = clear(new BorderLayout(0, 8));
        group.add(label(name, 13, true, INK), BorderLayout.NORTH);
        group.add(input);
        return group;
    }

    private void addLine() {
        if (busy || totalQuantity() >= 5) {
            message.setText("Mỗi phiếu chỉ được mượn tối đa 5 cuốn sách.");
            return;
        }
        entries.add(new BookLine());
        refreshChoices();
        rebuild();
    }

    private int totalQuantity() {
        return entries.stream().mapToInt(line -> ((Number) line.quantity.getValue()).intValue()).sum();
    }

    private void refreshChoices() {
        if (refreshing) return;
        refreshing = true;
        try {
            Set<String> selectedIds = new HashSet<>();
            for (BookLine line : entries) {
                RecordEditorPanel.Choice choice = (RecordEditorPanel.Choice) line.book.getSelectedItem();
                if (choice != null && !choice.id().isBlank()) selectedIds.add(choice.id());
            }
            for (BookLine line : entries) {
                RecordEditorPanel.Choice selected = (RecordEditorPanel.Choice) line.book.getSelectedItem();
                String id = selected == null ? "" : selected.id();
                DefaultComboBoxModel<RecordEditorPanel.Choice> choices = new DefaultComboBoxModel<>();
                choices.addElement(new RecordEditorPanel.Choice("", "Chọn sách"));
                int stock = 5;
                for (Object[] row : books.rows()) {
                    String bookId = row[0].toString();
                    int available = ((Number) row[6]).intValue();
                    if (bookId.equals(id)) stock = available;
                    if ((available > 0 && !selectedIds.contains(bookId)) || bookId.equals(id)) {
                        RecordEditorPanel.Choice choice = new RecordEditorPanel.Choice(bookId,
                                row[1] + " · " + bookId + " · Tồn " + row[6]);
                        choices.addElement(choice);
                        if (bookId.equals(id)) choices.setSelectedItem(choice);
                    }
                }
                line.book.setModel(choices);
                int current = ((Number) line.quantity.getValue()).intValue();
                int maximum = Math.max(1, Math.min(stock, 5 - (totalQuantity() - current)));
                SpinnerNumberModel model = (SpinnerNumberModel) line.quantity.getModel();
                if (current > maximum) model.setValue(maximum);
                model.setMaximum(maximum);
            }
            boolean unused = books.rows().stream().anyMatch(row ->
                    ((Number) row[6]).intValue() > 0 && !selectedIds.contains(row[0].toString()));
            add.setEnabled(!busy && totalQuantity() < 5 && unused);
            add.setToolTipText(totalQuantity() >= 5 ? "Đã đủ giới hạn 5 cuốn sách." : "Thêm đầu sách khác vào phiếu.");
        } finally {
            refreshing = false;
        }
    }

    private void rebuild() {
        lines.removeAll();
        int row = 0;
        for (BookLine line : entries) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 10, 0);
            lines.add(line.panel, c);
        }
        lines.revalidate();
        lines.repaint();
    }

    private final class BookLine {
        final JComboBox<RecordEditorPanel.Choice> book = new JComboBox<>();
        final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        final JPanel panel = clear(new BorderLayout(12, 0));

        BookLine() {
            book.addActionListener(event -> refreshChoices());
            quantity.addChangeListener(event -> refreshChoices());
            JSpinner.NumberEditor editor = (JSpinner.NumberEditor) quantity.getEditor();
            ((javax.swing.text.DefaultFormatter) editor.getTextField().getFormatter()).setAllowsInvalid(true);
            editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
            field(book);
            field(quantity);
            panel.add(book);
            JPanel right = clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            right.add(label("Số lượng", 13, false, MUTED));
            quantity.setPreferredSize(new Dimension(95, 40));
            right.add(quantity);
            JButton remove = button("Bỏ dòng", BEIGE, INK);
            remove.addActionListener(event -> {
                entries.remove(this);
                refreshChoices();
                rebuild();
            });
            right.add(remove);
            panel.add(right, BorderLayout.EAST);
        }
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        refreshChoices();
        save.setEnabled(!busy);
        cancel.setEnabled(!busy);
        save.setText(busy ? "Đang lưu..." : "Lập phiếu mượn");
    }

    public void showError(String text) {
        message.setText(text);
        message.setToolTipText(text);
        setBusy(false);
    }
}
