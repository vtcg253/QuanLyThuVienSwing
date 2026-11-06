package dao;

import database.DBConnection;
import static dao.DaoSupport.*;
import model.LibraryUser;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AccountDAO {
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

    void saveAccount(Connection connection, String employeeId, String username, String password,
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
}
