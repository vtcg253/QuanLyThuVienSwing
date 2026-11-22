package dao;

import database.KetNoiOracle;
import static dao.HoTroDao.*;
import model.DuLieuBang;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

public final class DaoPhieuMuon {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{1,10}");

    public DuLieuBang listLoans(String search, String status) throws SQLException {
        String sql = """
                SELECT pm.MAPM, dg.HOTEN, nv.HOTEN, pm.NGAYMUON, pm.NGAYHANTRA,
                       CASE WHEN s.SO_TAP IS NULL THEN s.TENSACH
                            ELSE s.TENSACH || N' · Tập ' || TO_CHAR(s.SO_TAP) END,
                       ct.MASACH, ct.SOLUONG, ct.SOLUONGMAT, ct.NGAYTRA,
                       ct.TRANGTHAI, pm.MADG, pm.MANV,
                       dg.NGAYSINH, dg.SDT, dg.DIACHI, nv.NGAYSINH, nv.SDT, nv.DIACHI, ct.TIENPHAT, s.GIABAN
                FROM PHIEUMUON pm
                JOIN DOCGIA dg ON dg.MADG = pm.MADG
                JOIN NHANVIEN nv ON nv.MANV = pm.MANV
                JOIN CT_PHIEUMUON ct ON ct.MAPM = pm.MAPM
                JOIN SACH s ON s.MASACH = ct.MASACH
                WHERE EXISTS (
                    SELECT 1 FROM CT_PHIEUMUON matched
                    JOIN SACH matched_book ON matched_book.MASACH = matched.MASACH
                    WHERE matched.MAPM = pm.MAPM
                      AND (UPPER(pm.MAPM) LIKE ? OR UPPER(dg.HOTEN) LIKE ?
                           OR UPPER(matched_book.TENSACH) LIKE ? OR UPPER(nv.HOTEN) LIKE ?
                           OR TO_CHAR(matched_book.SO_TAP) LIKE ?)
                      AND (? = 'ALL' OR matched.TRANGTHAI = ?
                           OR (? = 'OVERDUE' AND matched.TRANGTHAI = 'BORROWED'
                               AND pm.NGAYHANTRA < TRUNC(SYSDATE))
                           OR (? = 'LOST' AND matched.SOLUONGMAT > 0))
                )
                ORDER BY pm.NGAYMUON DESC, pm.MAPM, s.TENSACH, s.SO_TAP
                """;
        String term = like(search);
        return query(sql, new String[]{"Mã phiếu", "Độc giả", "Nhân viên lập", "Ngày mượn",
                        "Hạn trả", "Sách", "Mã sách", "Số lượng", "Số lượng mất", "Ngày trả",
                        "Trạng thái", "Mã độc giả", "Mã nhân viên", "Ngày sinh độc giả", "SĐT độc giả",
                        "Địa chỉ độc giả", "Ngày sinh nhân viên", "SĐT nhân viên", "Địa chỉ nhân viên",
                "Tiền phạt (VNĐ)", "Giá sách (VNĐ)"},
                term, term, term, term, term, status, status, status, status);
    }

    public DuLieuBang listPenalties(String search) throws SQLException {
        String term = like(search);
        return query("""
                SELECT pp.MAPP, pp.NGAYLAPPHAT, pm.MAPM, dg.HOTEN, nv.HOTEN,
                       s.MASACH,
                       CASE WHEN s.SO_TAP IS NULL THEN s.TENSACH
                            ELSE s.TENSACH || N' · Tập ' || TO_CHAR(s.SO_TAP) END,
                       pp.SOLUONGMAT, pp.DONGIAPHAT,
                       pp.TONGTIEN, pp.TRANGTHAI, pp.NGAYTHANHTOAN
                FROM PHIEUPHAT pp
                JOIN PHIEUMUON pm ON pm.MAPM = pp.MAPM
                JOIN DOCGIA dg ON dg.MADG = pm.MADG
                JOIN NHANVIEN nv ON nv.MANV = pp.MANV
                JOIN SACH s ON s.MASACH = pp.MASACH
                WHERE UPPER(pp.MAPP) LIKE ? OR UPPER(pm.MAPM) LIKE ?
                   OR UPPER(dg.HOTEN) LIKE ? OR UPPER(s.MASACH) LIKE ?
                   OR UPPER(s.TENSACH) LIKE ? OR TO_CHAR(s.SO_TAP) LIKE ?
                ORDER BY pp.NGAYLAPPHAT DESC, pp.MAPP DESC
                """, new String[]{"Mã phạt", "Ngày lập", "Phiếu mượn", "Độc giả", "Nhân viên xử lý",
                        "Mã sách", "Tên sách", "Số lượng mất", "Đơn giá (VNĐ)", "Thành tiền (VNĐ)",
                        "Trạng thái", "Ngày thanh toán"}, term, term, term, term, term, term);
    }

    public String createLoan(String readerId, LocalDate borrowDate, LocalDate dueDate,
                             Map<String, Integer> requestedBooks, String employeeId) throws SQLException {
        if (readerId == null || readerId.isBlank() || borrowDate == null || dueDate == null
                || dueDate.isBefore(borrowDate) || requestedBooks == null || requestedBooks.isEmpty()) {
            throw new IllegalArgumentException("Hãy nhập độc giả, ngày hợp lệ và ít nhất một sách.");
        }
        readerId = model.KiemTraDuLieu.code(readerId, "Mã độc giả");
        employeeId = model.KiemTraDuLieu.code(employeeId, "Mã nhân viên");
        if (borrowDate.getYear() < 1 || dueDate.getYear() > 9999)
            throw new IllegalArgumentException("Ngày mượn và hạn trả phải thuộc năm 1–9999.");
        long totalQuantity = 0;
        for (Map.Entry<String, Integer> item : requestedBooks.entrySet()) {
            String bookId = model.KiemTraDuLieu.code(item.getKey(), "Mã sách");
            if (!bookId.equals(item.getKey())) throw new IllegalArgumentException("Mã sách không được chứa khoảng trắng ở hai đầu.");
            if (item.getValue() == null || item.getValue() <= 0)
                throw new IllegalArgumentException("Số lượng mượn phải là số nguyên dương.");
            totalQuantity += item.getValue();
            if (totalQuantity > 5)
                throw new IllegalArgumentException("Mỗi phiếu chỉ được mượn tối đa 5 cuốn sách (tổng số lượng tất cả các dòng).");
        }
        try (Connection connection = KetNoiOracle.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Tuần tự hóa việc lập phiếu của cùng độc giả trước khi kiểm tra phiếu chưa trả.
                if (!exists(connection, "SELECT 1 FROM DOCGIA WHERE MADG = ? FOR UPDATE", readerId)) {
                    throw new SQLException("Không tìm thấy độc giả " + readerId + ".");
                }
                if (exists(connection, """
                        SELECT 1 FROM PHIEUMUON p
                        JOIN CT_PHIEUMUON c ON c.MAPM = p.MAPM
                        WHERE p.MADG = ? AND c.TRANGTHAI = 'BORROWED'
                        """, readerId)) {
                    throw new SQLException("Độc giả còn sách chưa trả. Vui lòng trả hết sách trước khi mượn tiếp.");
                }
                if (exists(connection, """
                        SELECT 1 FROM PHIEUPHAT pp
                        JOIN PHIEUMUON p ON p.MAPM = pp.MAPM
                        WHERE p.MADG = ? AND pp.TRANGTHAI = 'CHUA_THANH_TOAN'
                        """, readerId)) {
                    throw new SQLException("Độc giả còn phiếu phạt chưa thanh toán. Vui lòng hoàn tất thanh toán trước khi mượn tiếp.");
                }
                if (!exists(connection, "SELECT 1 FROM NHANVIEN WHERE MANV = ?", employeeId)) {
                    throw new SQLException("Tài khoản chưa được liên kết với hồ sơ nhân viên.");
                }
                String loanId = nextCode(connection, "PHIEUMUON", "MAPM", "PM");
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO PHIEUMUON (MAPM, MADG, MANV, NGAYMUON, NGAYHANTRA) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    statement.setString(1, loanId);
                    statement.setString(2, readerId);
                    statement.setString(3, employeeId);
                    statement.setDate(4, Date.valueOf(borrowDate));
                    statement.setDate(5, Date.valueOf(dueDate));
                    statement.executeUpdate();
                }
                for (Map.Entry<String, Integer> item : requestedBooks.entrySet()) {
                    String bookId = item.getKey().trim();
                    int quantity = item.getValue();
                    if (!CODE.matcher(bookId).matches() || quantity <= 0) {
                        throw new IllegalArgumentException("Mã sách hoặc số lượng mượn không hợp lệ.");
                    }
                    int stock;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT SOLUONG FROM SACH WHERE MASACH = ? FOR UPDATE")) {
                        statement.setString(1, bookId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException("Không tìm thấy sách " + bookId + ".");
                            }
                            stock = result.getInt(1);
                        }
                    }
                    if (stock < quantity) {
                        throw new SQLException("Sách " + bookId + " chỉ còn " + stock + " cuốn.");
                    }
                    execute(connection, """
                                    INSERT INTO CT_PHIEUMUON
                                        (MAPM, MASACH, SOLUONG, NGAYTRA, SOLUONGMAT, TRANGTHAI)
                                    VALUES (?, ?, ?, NULL, 0, 'BORROWED')
                                    """,
                            new Object[]{loanId, bookId, quantity});
                }
                connection.commit();
                return loanId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void returnBook(String loanId, String bookId) throws SQLException {
        loanId = model.KiemTraDuLieu.code(loanId, "Mã phiếu mượn");
        bookId = model.KiemTraDuLieu.code(bookId, "Mã sách");
        try (Connection connection = KetNoiOracle.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE CT_PHIEUMUON SET NGAYTRA = ?, TRANGTHAI = 'RETURNED'
                        WHERE MAPM = ? AND MASACH = ? AND TRANGTHAI = 'BORROWED'
                        """)) {
                    statement.setDate(1, Date.valueOf(LocalDate.now()));
                    statement.setString(2, loanId);
                    statement.setString(3, bookId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Dòng mượn không tồn tại hoặc đã được xử lý.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void reportLost(String loanId, String bookId, int quantityLost, String employeeId) throws SQLException {
        loanId = model.KiemTraDuLieu.code(loanId, "Mã phiếu mượn");
        bookId = model.KiemTraDuLieu.code(bookId, "Mã sách");
        employeeId = model.KiemTraDuLieu.code(employeeId, "Mã nhân viên");
        if (quantityLost <= 0) {
            throw new IllegalArgumentException("Số lượng mất phải lớn hơn 0.");
        }
        try (Connection connection = KetNoiOracle.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!exists(connection, "SELECT 1 FROM NHANVIEN WHERE MANV = ?", employeeId)) {
                    throw new SQLException("Không tìm thấy nhân viên xử lý phiếu phạt.");
                }
                int borrowed;
                int lost;
                long price;
                long fine;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT ct.SOLUONG, ct.SOLUONGMAT, s.GIABAN, ct.TIENPHAT
                        FROM CT_PHIEUMUON ct JOIN SACH s ON s.MASACH = ct.MASACH
                        WHERE ct.MAPM = ? AND ct.MASACH = ? AND ct.TRANGTHAI = 'BORROWED'
                        FOR UPDATE OF ct.SOLUONGMAT
                        """)) {
                    statement.setString(1, loanId);
                    statement.setString(2, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Dòng mượn không tồn tại hoặc đã được xử lý.");
                        }
                        borrowed = result.getInt(1);
                        lost = result.getInt(2);
                        price = result.getLong(3);
                        fine = result.getLong(4);
                    }
                }
                if (quantityLost > borrowed - lost) {
                    throw new SQLException("Số lượng báo mất vượt quá số sách còn đang mượn.");
                }
                long newFine = Math.addExact(fine, Math.multiplyExact(price, quantityLost));
                int newLost = lost + quantityLost;
                if (newLost == borrowed) {
                    execute(connection, """
                                    UPDATE CT_PHIEUMUON SET SOLUONGMAT = ?, TIENPHAT = ?, TRANGTHAI = 'LOST'
                                    WHERE MAPM = ? AND MASACH = ?
                                    """,
                            new Object[]{newLost, newFine, loanId, bookId});
                } else {
                    execute(connection, "UPDATE CT_PHIEUMUON SET SOLUONGMAT = ?, TIENPHAT = ? WHERE MAPM = ? AND MASACH = ?",
                            new Object[]{newLost, newFine, loanId, bookId});
                }
                String penaltyId = nextCode(connection, "PHIEUPHAT", "MAPP", "PP");
                execute(connection, """
                                INSERT INTO PHIEUPHAT
                                    (MAPP, MAPM, MASACH, MANV, NGAYLAPPHAT, SOLUONGMAT, DONGIAPHAT, TONGTIEN, TRANGTHAI)
                                VALUES (?, ?, ?, ?, TRUNC(SYSDATE), ?, ?, ?, 'CHUA_THANH_TOAN')
                                """,
                        new Object[]{penaltyId, loanId, bookId, employeeId, quantityLost, price,
                                Math.multiplyExact(price, quantityLost)});
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void markPenaltyPaid(String penaltyId) throws SQLException {
        penaltyId = model.KiemTraDuLieu.code(penaltyId, "Mã phiếu phạt");
        try (Connection connection = KetNoiOracle.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE PHIEUPHAT
                        SET TRANGTHAI = 'DA_THANH_TOAN', NGAYTHANHTOAN = TRUNC(SYSDATE)
                        WHERE MAPP = ? AND TRANGTHAI = 'CHUA_THANH_TOAN'
                        """)) {
                    statement.setString(1, penaltyId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Phiếu phạt không tồn tại hoặc đã được thanh toán.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
