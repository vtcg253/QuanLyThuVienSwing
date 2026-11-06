package dao;

import model.TableData;
import java.sql.Connection;
import java.sql.SQLException;
import static dao.DaoSupport.*;

public final class ReaderDAO extends RecordDAO {
    public ReaderDAO() {
        super("DOCGIA", "MADG", "DG", "HOTEN", "NGAYSINH", "SDT", "DIACHI");
    }

    @Override
    public TableData listRecords(String search) throws SQLException {
        return query("""
                SELECT MADG, HOTEN, NGAYSINH, SDT, DIACHI FROM DOCGIA
                WHERE UPPER(MADG) LIKE ? OR UPPER(HOTEN) LIKE ? ORDER BY MADG
                """, new String[]{"Mã độc giả", "Họ tên", "Ngày sinh", "Điện thoại", "Địa chỉ"},
                like(search), like(search));
    }

    @Override
    protected String formSql() {
        return "SELECT HOTEN, NGAYSINH, SDT, DIACHI FROM DOCGIA WHERE MADG = ?";
    }

    @Override
    protected Object[] convertValues(String[] values) {
        return personValues(values);
    }

    @Override
    protected void checkDelete(Connection connection, String id, String currentEmployeeId) throws SQLException {
        if (exists(connection, "SELECT 1 FROM PHIEUMUON WHERE MADG = ?", id))
            throw new SQLException("Không thể xóa độc giả đã có lịch sử mượn.");
    }
}
