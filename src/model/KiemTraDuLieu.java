package model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;

/** Quy tắc kiểm tra dữ liệu dùng chung cho biểu mẫu và DAO; trường không bắt buộc vẫn được để trống. */
public final class KiemTraDuLieu {
    private KiemTraDuLieu() { }

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

    public static void record(LoaiHoSo type, String[] values) {
        int count = type == LoaiHoSo.BOOKS ? 8 : 4;
        if (values == null || values.length < count) throw new IllegalArgumentException("Dữ liệu biểu mẫu không hợp lệ.");
        text(values[0], type == LoaiHoSo.BOOKS ? "Tên sách" : "Họ tên", type == LoaiHoSo.BOOKS ? 200 : 100, true);
        if (type == LoaiHoSo.BOOKS) {
            if (values[1] != null && !values[1].isBlank()) integer(values[1], "Số tập", 1, 99999);
            String[] labels = {"thể loại", "tác giả", "nhà xuất bản"};
            for (int i = 2; i <= 4; i++) {
                if (values[i] == null || values[i].isBlank())
                    throw new IllegalArgumentException("Vui lòng chọn " + labels[i - 2] + ".");
                code(values[i], "Mã " + labels[i - 2]);
            }
            if (values[5] != null && !values[5].isBlank()) integer(values[5], "Năm xuất bản", 1, LocalDate.now().getYear());
            integer(values[6], "Tồn kho", 0, Integer.MAX_VALUE);
            String price = text(values[7], "Giá sách", 15, true);
            if (!price.matches("[0-9]+"))
                throw new IllegalArgumentException("Giá sách phải là số nguyên không âm.");
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

    public static void catalog(LoaiDanhMuc type, String[] values) {
        if (type == null || values == null || values.length != type.formColumns().length)
            throw new IllegalArgumentException("Dữ liệu danh mục không hợp lệ.");
        text(values[0], type.formLabels()[0], 100, true);
        if (values.length > 1) text(values[1], "Địa chỉ", 200, type == LoaiDanhMuc.NHA_XUAT_BAN);
        if (type == LoaiDanhMuc.NHA_XUAT_BAN) {
            text(values[2], "Số điện thoại", 15, true);
            phone(values[2]);
        }
    }

    public static void account(String username, String password, String role, boolean newAccount) {
        // VARCHAR2(50) mặc định giới hạn theo số byte trong cơ sở dữ liệu.
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
