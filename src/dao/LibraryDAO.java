package dao;

import database.DBConnection;
import model.CatalogType;
import model.LibraryUser;
import model.RecordType;
import model.TableData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LibraryDAO {
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{1,10}");

    public LibraryUser login(String username, char[] password) throws SQLException {
        String sql = """
                SELECT n.MANV, n.HOTEN, t.MATKHAU, t.VAITRO
                FROM TAIKHOAN t JOIN NHANVIEN n ON n.MANV = t.MANV
                WHERE t.TENDANGNHAP = ? AND t.KICHHOAT = 'Y'
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                String hash = result.getString("MATKHAU");
                if (hash == null || !BCrypt.checkpw(new String(password), hash)) {
                    return null;
                }
                return new LibraryUser(result.getString("MANV"), result.getString("HOTEN"),
                        result.getString("VAITRO"));
            }
        }
    }

    public TableData listBooks(String search) throws SQLException {
        String sql = """
                SELECT s.MASACH, s.TENSACH, tl.TENTHELOAI, tg.TENTACGIA, nxb.TENNXB,
                       s.NAMXUATBAN, s.SOLUONG
                FROM SACH s
                LEFT JOIN THELOAI tl ON tl.MATL = s.MATL
                LEFT JOIN TACGIA tg ON tg.MATG = s.MATG
                LEFT JOIN NHAXUATBAN nxb ON nxb.MANXB = s.MANXB
                WHERE UPPER(s.MASACH) LIKE ? OR UPPER(s.TENSACH) LIKE ?
                   OR UPPER(tl.TENTHELOAI) LIKE ? OR UPPER(tg.TENTACGIA) LIKE ?
                   OR UPPER(nxb.TENNXB) LIKE ?
                ORDER BY s.MASACH
                """;
        String term = like(search);
        return query(sql, new String[]{"Mã sách", "Tên sách", "Thể loại", "Tác giả",
                "Nhà xuất bản", "Năm xuất bản", "Tồn kho"}, term, term, term, term, term);
    }

    public TableData listRecords(RecordType type, String search) throws SQLException {
        return switch (type) {
            case BOOKS -> listBooks(search);
            case READERS -> query("""
                            SELECT MADG, HOTEN, NGAYSINH, SDT, DIACHI FROM DOCGIA
                            WHERE UPPER(MADG) LIKE ? OR UPPER(HOTEN) LIKE ? ORDER BY MADG
                            """,
                    new String[]{"Mã độc giả", "Họ tên", "Ngày sinh", "Điện thoại", "Địa chỉ"},
                    like(search), like(search));
            case EMPLOYEES -> query("""
                            SELECT n.MANV, n.HOTEN, n.NGAYSINH, n.SDT, n.DIACHI,
                                   t.TENDANGNHAP, t.VAITRO, t.KICHHOAT
                            FROM NHANVIEN n LEFT JOIN TAIKHOAN t ON t.MANV = n.MANV
                            WHERE UPPER(n.MANV) LIKE ? OR UPPER(n.HOTEN) LIKE ?
                            ORDER BY n.MANV
                            """,
                    new String[]{"Mã nhân viên", "Họ tên", "Ngày sinh", "Điện thoại",
                            "Địa chỉ", "Tên đăng nhập", "Vai trò", "Kích hoạt"},
                    like(search), like(search));
        };
    }

    public TableData listCatalog(CatalogType type, String search) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(type.idColumn()).append(", ")
                .append(type.nameColumn());
        for (String column : type.formColumns()) {
            if (!column.equals(type.nameColumn())) {
                sql.append(", ").append(column);
            }
        }
        sql.append(" FROM ").append(type.table()).append(" WHERE UPPER(")
                .append(type.idColumn()).append(") LIKE ? OR UPPER(")
                .append(type.nameColumn()).append(") LIKE ? ORDER BY ")
                .append(type.idColumn());
        List<String> headers = new ArrayList<>(List.of("Mã", type.formLabels()[0]));
        for (int i = 1; i < type.formLabels().length; i++) {
            headers.add(type.formLabels()[i]);
        }
        return query(sql.toString(), headers.toArray(String[]::new), like(search), like(search));
    }

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

    public Map<String, Long> dashboardStatistics() throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM SACH),
                       (SELECT COALESCE(SUM(SOLUONG), 0) FROM SACH),
                       (SELECT COUNT(*) FROM PHIEUMUON),
                       (SELECT COUNT(DISTINCT MAPM) FROM CT_PHIEUMUON WHERE TRANGTHAI = 'BORROWED'),
                       (SELECT COUNT(*) FROM PHIEUMUON p WHERE EXISTS
                           (SELECT 1 FROM CT_PHIEUMUON c WHERE c.MAPM = p.MAPM)
                           AND NOT EXISTS (SELECT 1 FROM CT_PHIEUMUON c
                               WHERE c.MAPM = p.MAPM AND c.TRANGTHAI <> 'RETURNED')),
                       (SELECT COUNT(DISTINCT p.MAPM) FROM PHIEUMUON p
                           JOIN CT_PHIEUMUON c ON c.MAPM = p.MAPM
                           WHERE c.TRANGTHAI = 'BORROWED' AND p.NGAYHANTRA < TRUNC(SYSDATE))
                FROM DUAL
                """;
        Map<String, Long> values = new java.util.LinkedHashMap<>();
        String[] labels = {"Đầu sách", "Tồn kho", "Phiếu mượn", "Đang mượn", "Đã trả", "Quá hạn"};
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            for (int i = 0; i < labels.length; i++) values.put(labels[i], result.getLong(i + 1));
        }
        return values;
    }

    public TableData dashboardBooks() throws SQLException {
        return query("""
                SELECT s.MASACH, s.TENSACH, s.SOLUONG, COALESCE(SUM(c.SOLUONG), 0)
                FROM SACH s LEFT JOIN CT_PHIEUMUON c ON c.MASACH = s.MASACH
                GROUP BY s.MASACH, s.TENSACH, s.SOLUONG
                ORDER BY s.SOLUONG DESC, s.MASACH
                """, new String[]{"Mã sách", "Tên sách", "Tồn kho", "Đã mượn"});
    }

    public String[] recordFormValues(RecordType type, String id) throws SQLException {
        String sql = switch (type) {
            case BOOKS -> "SELECT TENSACH, MATL, MATG, MANXB, NAMXUATBAN, SOLUONG FROM SACH WHERE MASACH = ?";
            case READERS -> "SELECT HOTEN, NGAYSINH, SDT, DIACHI FROM DOCGIA WHERE MADG = ?";
            case EMPLOYEES -> """
                    SELECT n.HOTEN, n.NGAYSINH, n.SDT, n.DIACHI, t.TENDANGNHAP, t.VAITRO, t.KICHHOAT
                    FROM NHANVIEN n LEFT JOIN TAIKHOAN t ON t.MANV = n.MANV WHERE n.MANV = ?
                    """;
        };
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Không tìm thấy dữ liệu có mã " + id + ".");
                }
                int columns = result.getMetaData().getColumnCount();
                String[] values = new String[columns];
                for (int i = 0; i < columns; i++) {
                    Object value = result.getObject(i + 1);
                    values[i] = displayValue(value);
                }
                return values;
            }
        }
    }

    public void saveCatalog(CatalogType type, String id, String[] values) throws SQLException {
        String[] columns = type.formColumns();
        if (values.length != columns.length) {
            throw new IllegalArgumentException("Dữ liệu danh mục không hợp lệ.");
        }
        String[] cleanValues = java.util.Arrays.stream(values).map(String::trim).toArray(String[]::new);
        if (cleanValues[0].isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            ensureUnique(connection, type.table(), type.nameColumn(), cleanValues[0], type.idColumn(), id);
            if (id == null || id.isBlank()) {
                String newId = nextCode(connection, type.table(), type.idColumn(), type.prefix());
                String sql = "INSERT INTO " + type.table() + " (" + type.idColumn() + ", "
                        + String.join(", ", columns) + ") VALUES ("
                        + placeholders(columns.length + 1) + ")";
                execute(connection, sql, prepend(newId, cleanValues));
            } else {
                String sql = "UPDATE " + type.table() + " SET " + assignments(columns)
                        + " WHERE " + type.idColumn() + " = ?";
                execute(connection, sql, append(cleanValues, id));
            }
        }
    }

    public void deleteCatalog(CatalogType type, String id) throws SQLException {
        deleteById(type.table(), type.idColumn(), id);
    }

    public void saveRecord(RecordType type, String id, String[] values, String username,
                           String password, String role, boolean enabled) throws SQLException {
        String table = switch (type) {
            case BOOKS -> "SACH";
            case READERS -> "DOCGIA";
            case EMPLOYEES -> "NHANVIEN";
        };
        String idColumn = switch (type) {
            case BOOKS -> "MASACH";
            case READERS -> "MADG";
            case EMPLOYEES -> "MANV";
        };
        String prefix = switch (type) {
            case BOOKS -> "S";
            case READERS -> "DG";
            case EMPLOYEES -> "NV";
        };
        String[] columns = switch (type) {
            case BOOKS -> new String[]{"TENSACH", "MATL", "MATG", "MANXB", "NAMXUATBAN", "SOLUONG"};
            case READERS, EMPLOYEES -> new String[]{"HOTEN", "NGAYSINH", "SDT", "DIACHI"};
        };
        if (values.length != columns.length) {
            throw new IllegalArgumentException("Dữ liệu biểu mẫu không hợp lệ.");
        }
        if (values[0].isBlank()) {
            throw new IllegalArgumentException(type == RecordType.BOOKS
                    ? "Tên sách không được để trống." : "Họ tên không được để trống.");
        }
        Object[] parameters = convertValues(type, values);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String effectiveId = id;
                if (effectiveId == null || effectiveId.isBlank()) {
                    effectiveId = nextCode(connection, table, idColumn, prefix);
                    String sql = "INSERT INTO " + table + " (" + idColumn + ", "
                            + String.join(", ", columns) + ") VALUES ("
                            + placeholders(columns.length + 1) + ")";
                    execute(connection, sql, prepend(effectiveId, parameters));
                } else {
                    String sql = "UPDATE " + table + " SET " + assignments(columns)
                            + " WHERE " + idColumn + " = ?";
                    execute(connection, sql, append(parameters, effectiveId));
                }
                if (type == RecordType.EMPLOYEES) {
                    saveAccount(connection, effectiveId, username, password, role, enabled);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void deleteRecord(RecordType type, String id, String currentEmployeeId) throws SQLException {
        String table = switch (type) {
            case BOOKS -> "SACH";
            case READERS -> "DOCGIA";
            case EMPLOYEES -> "NHANVIEN";
        };
        String idColumn = switch (type) {
            case BOOKS -> "MASACH";
            case READERS -> "MADG";
            case EMPLOYEES -> "MANV";
        };
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (type == RecordType.READERS && exists(connection,
                        "SELECT 1 FROM PHIEUMUON WHERE MADG = ?", id)) {
                    throw new SQLException("Không thể xóa độc giả đã có lịch sử mượn.");
                }
                if (type == RecordType.BOOKS && exists(connection,
                        "SELECT 1 FROM CT_PHIEUMUON WHERE MASACH = ? AND TRANGTHAI = 'BORROWED'", id)) {
                    throw new SQLException("Không thể xóa sách đang được mượn.");
                }
                if (type == RecordType.EMPLOYEES) {
                    if (id.equals(currentEmployeeId)) {
                        throw new SQLException("Không thể xóa nhân viên đang đăng nhập.");
                    }
                    if (exists(connection, "SELECT 1 FROM PHIEUMUON WHERE MANV = ?", id)) {
                        throw new SQLException("Không thể xóa nhân viên đã lập phiếu mượn.");
                    }
                    execute(connection, "DELETE FROM TAIKHOAN WHERE MANV = ?", new Object[]{id});
                }
                execute(connection, "DELETE FROM " + table + " WHERE " + idColumn + " = ?",
                        new Object[]{id});
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
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

    private Object[] convertValues(RecordType type, String[] values) {
        String[] clean = values.clone();
        for (int i = 0; i < clean.length; i++) {
            clean[i] = clean[i].trim();
        }
        if (type == RecordType.BOOKS) {
            Integer year = clean[4].isBlank() ? null : parsePositiveInt(clean[4], "Năm xuất bản");
            int quantity = parseNonNegativeInt(clean[5], "Tồn kho");
            return new Object[]{clean[0], nullable(clean[1]), nullable(clean[2]), nullable(clean[3]), year, quantity};
        }
        Date birthDate = null;
        if (!clean[1].isBlank()) {
            try {
                birthDate = Date.valueOf(LocalDate.parse(clean[1]));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Ngày sinh phải theo định dạng YYYY-MM-DD.");
            }
        }
        return new Object[]{clean[0], birthDate, nullable(clean[2]), nullable(clean[3])};
    }

    private void saveAccount(Connection connection, String employeeId, String username, String password,
                             String role, boolean enabled) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập nhân viên không được để trống.");
        }
        boolean exists = exists(connection, "SELECT 1 FROM TAIKHOAN WHERE MANV = ?", employeeId);
        if (!exists && (password == null || password.length() < 6)) {
            throw new IllegalArgumentException("Mật khẩu tài khoản mới phải có ít nhất 6 ký tự.");
        }
        if (password != null && !password.isBlank() && password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        String hashedPassword = password == null || password.isBlank() ? null : BCrypt.hashpw(password, BCrypt.gensalt(10));
        String active = enabled ? "Y" : "N";
        if (exists) {
            String sql = hashedPassword == null
                    ? "UPDATE TAIKHOAN SET TENDANGNHAP = ?, VAITRO = ?, KICHHOAT = ? WHERE MANV = ?"
                    : "UPDATE TAIKHOAN SET TENDANGNHAP = ?, MATKHAU = ?, VAITRO = ?, KICHHOAT = ? WHERE MANV = ?";
            Object[] parameters = hashedPassword == null
                    ? new Object[]{username.trim(), role, active, employeeId}
                    : new Object[]{username.trim(), hashedPassword, role, active, employeeId};
            execute(connection, sql, parameters);
        } else {
            execute(connection, """
                            INSERT INTO TAIKHOAN (MANV, TENDANGNHAP, MATKHAU, VAITRO, KICHHOAT)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    new Object[]{employeeId, username.trim(), hashedPassword, role, active});
        }
    }

    private void ensureUnique(Connection connection, String table, String column, String value,
                              String idColumn, String id) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE UPPER(" + column + ") = UPPER(?)";
        if (id != null && !id.isBlank()) {
            sql += " AND " + idColumn + " <> ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value.trim());
            if (id != null && !id.isBlank()) {
                statement.setString(2, id);
            }
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new SQLException("Tên danh mục đã tồn tại.");
                }
            }
        }
    }

    private String nextCode(Connection connection, String table, String column, String prefix)
            throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(" + column
                + ", '[0-9]+$'))), 0) + 1 FROM " + table
                + " WHERE REGEXP_LIKE(" + column + ", '^" + prefix + "[0-9]+$')";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return prefix + String.format("%02d", result.getInt(1));
        }
    }

    private boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void deleteById(String table, String idColumn, String id) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            execute(connection, "DELETE FROM " + table + " WHERE " + idColumn + " = ?", new Object[]{id});
        }
    }

    private TableData query(String sql, String[] headers, Object... parameters) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Object[] row = new Object[headers.length];
                    for (int i = 0; i < headers.length; i++) {
                        Object value = result.getObject(i + 1);
                        row[i] = value instanceof Timestamp timestamp
                                ? timestamp.toLocalDateTime().toLocalDate()
                                : value instanceof Date date ? date.toLocalDate() : value;
                    }
                    rows.add(row);
                }
            }
        }
        return new TableData(headers, rows);
    }

    private void execute(Connection connection, String sql, Object[] parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    private String assignments(String[] columns) {
        return String.join(", ", java.util.Arrays.stream(columns).map(column -> column + " = ?").toList());
    }

    private String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    private Object[] append(Object[] values, Object last) {
        Object[] result = java.util.Arrays.copyOf(values, values.length + 1);
        result[values.length] = last;
        return result;
    }

    private Object[] prepend(Object first, Object[] values) {
        Object[] result = new Object[values.length + 1];
        result[0] = first;
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private String like(String text) {
        return "%" + (text == null ? "" : text.trim().toUpperCase()) + "%";
    }

    private int parsePositiveInt(String value, String field) {
        try {
            int number = Integer.parseInt(value);
            if (number <= 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " phải là số nguyên dương.");
        }
    }

    private int parseNonNegativeInt(String value, String field) {
        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " phải là số nguyên không âm.");
        }
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String displayValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        return value.toString();
    }
}
