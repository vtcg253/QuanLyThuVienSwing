package dao;

import model.TableData;
import java.sql.Connection;
import java.sql.SQLException;
import static dao.DaoSupport.*;

public final class BookDAO extends RecordDAO {
    public BookDAO() {
        super("SACH", "MASACH", "S", "TENSACH", "MATL", "MATG", "MANXB", "NAMXUATBAN", "SOLUONG");
    }

    @Override
    public TableData listRecords(String search) throws SQLException {
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

    @Override
    protected String formSql() {
        return "SELECT TENSACH, MATL, MATG, MANXB, NAMXUATBAN, SOLUONG FROM SACH WHERE MASACH = ?";
    }

    @Override
    protected Object[] convertValues(String[] values) {
        String[] clean = java.util.Arrays.stream(values).map(String::trim).toArray(String[]::new);
        Integer year = clean[4].isBlank() ? null : parsePositiveInt(clean[4], "Năm xuất bản");
        int quantity = parseNonNegativeInt(clean[5], "Tồn kho");
        return new Object[]{clean[0], nullable(clean[1]), nullable(clean[2]), nullable(clean[3]), year, quantity};
    }

    @Override
    protected void checkDelete(Connection connection, String id, String currentEmployeeId) throws SQLException {
        if (exists(connection, "SELECT 1 FROM CT_PHIEUMUON WHERE MASACH = ?", id))
            throw new SQLException("Không thể xóa sách đã có lịch sử mượn trả.");
    }
}
