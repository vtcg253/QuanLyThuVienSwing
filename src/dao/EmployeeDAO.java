package dao;

import database.DBConnection;
import model.TableData;
import java.sql.Connection;
import java.sql.SQLException;
import static dao.DaoSupport.*;

public final class EmployeeDAO extends RecordDAO {
    private final AccountDAO accounts = new AccountDAO();

    public EmployeeDAO() {
        super("NHANVIEN", "MANV", "NV", "HOTEN", "NGAYSINH", "SDT", "DIACHI");
    }

    @Override
    public TableData listRecords(String search) throws SQLException {
        return query("""
                SELECT n.MANV, n.HOTEN, n.NGAYSINH, n.SDT, n.DIACHI,
                       t.TENDANGNHAP, t.VAITRO, t.KICHHOAT
                FROM NHANVIEN n LEFT JOIN TAIKHOAN t ON t.MANV = n.MANV
                WHERE UPPER(n.MANV) LIKE ? OR UPPER(n.HOTEN) LIKE ? ORDER BY n.MANV
                """, new String[]{"Mã nhân viên", "Họ tên", "Ngày sinh", "Điện thoại",
                "Địa chỉ", "Tên đăng nhập", "Vai trò", "Kích hoạt"}, like(search), like(search));
    }

    @Override
    protected String formSql() {
        return """
                SELECT n.HOTEN, n.NGAYSINH, n.SDT, n.DIACHI, t.TENDANGNHAP, t.VAITRO, t.KICHHOAT
                FROM NHANVIEN n LEFT JOIN TAIKHOAN t ON t.MANV = n.MANV WHERE n.MANV = ?
                """;
    }

    @Override
    protected Object[] convertValues(String[] values) {
        return personValues(values);
    }

    public void saveWithAccount(String id, String[] values, String username, String password,
                                String role, boolean enabled) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String employeeId = saveValues(connection, id, values);
                accounts.saveAccount(connection, employeeId, username, password, role, enabled);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    @Override
    protected void checkDelete(Connection connection, String id, String currentEmployeeId) throws SQLException {
        if (id.equals(currentEmployeeId))
            throw new SQLException("Không thể xóa nhân viên đang đăng nhập.");
        if (exists(connection, "SELECT 1 FROM PHIEUMUON WHERE MANV = ?", id))
            throw new SQLException("Không thể xóa nhân viên đã lập phiếu mượn.");
        execute(connection, "DELETE FROM TAIKHOAN WHERE MANV = ?", new Object[]{id});
    }
}
