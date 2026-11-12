package model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;

/** Input rules shared by forms and DAOs. Optional schema fields remain optional. */
public final class InputValidation {
    private InputValidation() { }

    public static String text(String value, String field, int max, boolean required) {
        String clean = value == null ? "" : value.trim();
        if (required && clean.isBlank()) throw new IllegalArgumentException(field + " không được để trống.");
        if (clean.length() > max) throw new IllegalArgumentException(field + " không được vượt quá " + max + " ký tự.");
        return clean;
    }

    public static String code(String value, String field) {
        String clean = text(value, field, 10, true);
        if (!clean.matches("[A-Za-z0-9_-]+"))
            throw new IllegalArgumentException(field + " chỉ được gồm chữ không dấu, số, dấu gạch ngang hoặc gạch dưới.");
        return clean;
    }

    public static void phone(String value) {
        String clean = text(value, "Số điện thoại", 15, false);
        if (!clean.isEmpty() && !clean.matches("\\+?[0-9]{7,15}"))
            throw new IllegalArgumentException("Số điện thoại phải gồm 7–15 chữ số, có thể bắt đầu bằng + (tổng tối đa 15 ký tự).");
    }

    public static int integer(String value, String field, int min, int max) {
        try {
            int number = Integer.parseInt(value == null ? "" : value.trim());
            if (number >= min && number <= max) return number;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(field + " phải là số nguyên từ " + min + " đến " + max + ".");
    }

    public static void record(RecordType type, String[] values) {
        int count = type == RecordType.BOOKS ? 6 : 4;
        if (values == null || values.length < count) throw new IllegalArgumentException("Dữ liệu biểu mẫu không hợp lệ.");
        text(values[0], type == RecordType.BOOKS ? "Tên sách" : "Họ tên", type == RecordType.BOOKS ? 200 : 100, true);
        if (type == RecordType.BOOKS) {
            String[] labels = {"thể loại", "tác giả", "nhà xuất bản"};
            for (int i = 1; i <= 3; i++) {
                if (values[i] == null || values[i].isBlank())
                    throw new IllegalArgumentException("Vui lòng chọn " + labels[i - 1] + ".");
                code(values[i], "Mã " + labels[i - 1]);
            }
            if (values[4] != null && !values[4].isBlank()) integer(values[4], "Năm xuất bản", 1, LocalDate.now().getYear());
            integer(values[5], "Tồn kho", 0, Integer.MAX_VALUE);
        } else {
            String birth = text(values[1], "Ngày sinh", 10, false);
            if (!birth.isEmpty()) {
                LocalDate date;
                try { date = LocalDate.parse(birth); }
                catch (RuntimeException e) { throw new IllegalArgumentException("Ngày sinh không hợp lệ."); }
                if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
                if (date.getYear() < 1) throw new IllegalArgumentException("Năm sinh phải lớn hơn 0.");
            }
            phone(values[2]);
            text(values[3], "Địa chỉ", 200, false);
        }
    }

    public static void catalog(CatalogType type, String[] values) {
        if (type == null || values == null || values.length != type.formColumns().length)
            throw new IllegalArgumentException("Dữ liệu danh mục không hợp lệ.");
        text(values[0], type.formLabels()[0], 100, true);
        if (values.length > 1) text(values[1], "Địa chỉ", 200, false);
        if (type == CatalogType.NHA_XUAT_BAN) phone(values[2]);
    }

    public static void account(String username, String password, String role, boolean newAccount) {
        // VARCHAR2(50) uses the database's byte semantics by default.
        String clean = text(username, "Tên đăng nhập", 50, true);
        if (clean.getBytes(StandardCharsets.UTF_8).length > 50)
            throw new IllegalArgumentException("Tên đăng nhập vượt quá 50 byte UTF-8.");
        if (!Arrays.asList("ADMIN", "THU_THU").contains(role))
            throw new IllegalArgumentException("Vai trò tài khoản không hợp lệ.");
        if (newAccount || (password != null && !password.isEmpty())) {
            if (password == null || password.isBlank() || password.length() < 6)
                throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự và không được để trống.");
            if (password.getBytes(StandardCharsets.UTF_8).length > 72)
                throw new IllegalArgumentException("Mật khẩu không được vượt quá 72 byte UTF-8.");
        }
    }
}
