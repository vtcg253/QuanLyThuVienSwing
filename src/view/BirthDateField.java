package view;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Optional birth date: numeric input with fixed day/month/year separators. */
final class BirthDateField extends JFormattedTextField {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    BirthDateField(String initial) {
        super(mask());
        LibraryStyle.field(this);
        String hint=LocalDate.now().format(DISPLAY);
        setUI(new javax.swing.plaf.basic.BasicFormattedTextFieldUI() {
            @Override protected void paintSafely(Graphics graphics) {
                if (!isEmptyDate()) { super.paintSafely(graphics);return; }
                paintBackground(graphics);
                graphics.setFont(getFont());
                FontMetrics metrics=graphics.getFontMetrics();
                int x=getInsets().left;
                int y=(getHeight()-metrics.getHeight())/2+metrics.getAscent();
                for(char character:hint.toCharArray()) {
                    graphics.setColor(character=='/'?LibraryStyle.INK:new Color(166,170,166));
                    graphics.drawString(String.valueOf(character),x,y);
                    x+=metrics.charWidth(character);
                }
                if(hasFocus()&&getCaret()!=null) getCaret().paint(graphics);
            }
        });
        setFocusLostBehavior(PERSIST);
        setToolTipText("Ngày/tháng/năm · chỉ cần nhập số; có thể để trống");
        getAccessibleContext().setAccessibleName("Ngày sinh, ngày tháng năm");
        if (initial != null && !initial.isBlank()) setText(display(initial));
        else setValue(null);
    }

    private static MaskFormatter mask() {
        try {
            MaskFormatter formatter = new MaskFormatter("##/##/####");
            formatter.setValidCharacters("0123456789");
            formatter.setPlaceholderCharacter(' ');
            return formatter;
        } catch (ParseException exception) { throw new IllegalStateException(exception); }
    }

    @Override public void replaceSelection(String content) {
        if (content != null && content.matches("[0-9]{8}")
                && (getText().replace("/", "").isBlank() || getSelectionEnd()-getSelectionStart()==getText().length())) {
            setText(content.substring(0,2)+"/"+content.substring(2,4)+"/"+content.substring(4));
            setCaretPosition(getText().length());
        } else super.replaceSelection(content);
    }

    String isoValue() {
        String text=getText();
        if (isEmptyDate()) return "";
        if (!text.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) {
            requestFocusInWindow();
            throw new IllegalArgumentException("Vui lòng nhập đủ ngày sinh theo dạng dd/MM/yyyy.");
        }
        try { return LocalDate.parse(text,DISPLAY).toString(); }
        catch (DateTimeParseException exception) {
            requestFocusInWindow();
            throw new IllegalArgumentException("Ngày sinh không hợp lệ. Vui lòng kiểm tra ngày, tháng và năm.");
        }
    }

    private boolean isEmptyDate() { return getText().replace("/", "").isBlank(); }

    static String display(Object value) {
        if (value==null || value.toString().isBlank()) return "—";
        if (value instanceof LocalDate date) return date.format(DISPLAY);
        if (value instanceof java.sql.Date date) return date.toLocalDate().format(DISPLAY);
        if (value instanceof java.sql.Timestamp date) return date.toLocalDateTime().toLocalDate().format(DISPLAY);
        String text=value.toString();
        try { return LocalDate.parse(text.length()>10?text.substring(0,10):text).format(DISPLAY); }
        catch (DateTimeParseException exception) { return text; }
    }
}
