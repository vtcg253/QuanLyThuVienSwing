package dao;

import database.DBConnection;
import static dao.DaoSupport.*;
import model.TableData;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;

public final class LoanDAO {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{1,10}");

    public TableData listLoans(String search, String status) throws SQLException {
        String sql = """
                SELECT pm.MAPM, dg.HOTEN, nv.HOTEN, pm.NGAYMUON, pm.NGAYHANTRA,
                       s.TENSACH, ct.MASACH, ct.SOLUONG, ct.SOLUONGMAT, ct.NGAYTRA,
                       ct.TRANGTHAI, pm.MADG, pm.MANV,
                       dg.NGAYSINH, dg.SDT, dg.DIACHI, nv.NGAYSINH, nv.SDT, nv.DIACHI
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
                           OR UPPER(matched_book.TENSACH) LIKE ? OR UPPER(nv.HOTEN) LIKE ?)
                      AND (? = 'ALL' OR matched.TRANGTHAI = ?
                           OR (? = 'OVERDUE' AND matched.TRANGTHAI = 'BORROWED'
                               AND pm.NGAYHANTRA < TRUNC(SYSDATE))
                           OR (? = 'LOST' AND matched.SOLUONGMAT > 0))
                )
                ORDER BY pm.NGAYMUON DESC, pm.MAPM, s.TENSACH
                """;
        String term = like(search);
        return query(sql, new String[]{"Mã phiếu", "Độc giả", "Nhân viên lập", "Ngày mượn",
                        "Hạn trả", "Sách", "Mã sách", "Số lượng", "Số lượng mất", "Ngày trả",
                        "Trạng thái", "Mã độc giả", "Mã nhân viên", "Ngày sinh độc giả", "SĐT độc giả",
                        "Địa chỉ độc giả", "Ngày sinh nhân viên", "SĐT nhân viên", "Địa chỉ nhân viên"},
                term, term, term, term, status, status, status, status);
    }

    public String createLoan(String readerId, LocalDate borrowDate, LocalDate dueDate,
                             Map<String, Integer> requestedBooks, String employeeId) throws SQLException {
        if (readerId == null || readerId.isBlank() || borrowDate == null || dueDate == null
                || dueDate.isBefore(borrowDate) || requestedBooks.isEmpty()) {
            throw new IllegalArgumentException("Hãy nhập độc giả, ngày hợp lệ và ít nhất một sách.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!exists(connection, "SELECT 1 FROM DOCGIA WHERE MADG = ?", readerId)) {
                    throw new SQLException("Không tìm thấy độc giả " + readerId + ".");
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
        try (Connection connection = DBConnection.getConnection()) {
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

    public void reportLost(String loanId, String bookId, int quantityLost) throws SQLException {
        if (quantityLost <= 0) {
            throw new IllegalArgumentException("Số lượng mất phải lớn hơn 0.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int borrowed;
                int lost;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT SOLUONG, SOLUONGMAT FROM CT_PHIEUMUON
                        WHERE MAPM = ? AND MASACH = ? AND TRANGTHAI = 'BORROWED' FOR UPDATE
                        """)) {
                    statement.setString(1, loanId);
                    statement.setString(2, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Dòng mượn không tồn tại hoặc đã được xử lý.");
                        }
                        borrowed = result.getInt(1);
                        lost = result.getInt(2);
                    }
                }
                if (quantityLost > borrowed - lost) {
                    throw new SQLException("Số lượng báo mất vượt quá số sách còn đang mượn.");
                }
                int newLost = lost + quantityLost;
                if (newLost == borrowed) {
                    execute(connection, """
                                    UPDATE CT_PHIEUMUON SET SOLUONGMAT = ?, TRANGTHAI = 'LOST'
                                    WHERE MAPM = ? AND MASACH = ?
                                    """,
                            new Object[]{newLost, loanId, bookId});
                } else {
                    execute(connection, "UPDATE CT_PHIEUMUON SET SOLUONGMAT = ? WHERE MAPM = ? AND MASACH = ?",
                            new Object[]{newLost, loanId, bookId});
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
