package dao;

import model.DuLieuBang;
import java.sql.Connection;
import java.sql.SQLException;
import static dao.HoTroDao.*;

public final class DaoSach extends DaoHoSo {
    public DaoSach() {
        super("SACH", "MASACH", "S", "TENSACH", "SO_TAP", "MATL", "MATG", "MANXB", "NAMXUATBAN", "SOLUONG", "GIABAN");
    }

    @Override
    public DuLieuBang listRecords(String search) throws SQLException {
        String sql = """
                SELECT s.MASACH, s.TENSACH, s.SO_TAP, tl.TENTHELOAI, tg.TENTACGIA, nxb.TENNXB,
                       s.NAMXUATBAN, s.SOLUONG, s.GIABAN
                FROM SACH s
                LEFT JOIN THELOAI tl ON tl.MATL = s.MATL
                LEFT JOIN TACGIA tg ON tg.MATG = s.MATG
                LEFT JOIN NHAXUATBAN nxb ON nxb.MANXB = s.MANXB
                WHERE UPPER(s.MASACH) LIKE ? OR UPPER(s.TENSACH) LIKE ?
                   OR UPPER(tl.TENTHELOAI) LIKE ? OR UPPER(tg.TENTACGIA) LIKE ?
                   OR UPPER(nxb.TENNXB) LIKE ? OR TO_CHAR(s.SO_TAP) LIKE ?
                ORDER BY s.TENSACH, s.SO_TAP NULLS FIRST, s.MASACH
                """;
        String term = like(search);
        return query(sql, new String[]{"Mã sách", "Tên sách", "Tập", "Thể loại", "Tác giả",
                "Nhà xuất bản", "Năm xuất bản", "Tồn kho", "Giá (VNĐ)"}, term, term, term, term, term, term);
    }

    @Override
    protected String formSql() {
        return "SELECT TENSACH, SO_TAP, MATL, MATG, MANXB, NAMXUATBAN, SOLUONG, GIABAN FROM SACH WHERE MASACH = ?";
    }

    @Override
    protected Object[] convertValues(String[] values) {
        String[] clean = java.util.Arrays.stream(values).map(String::trim).toArray(String[]::new);
        Integer volume = clean[1].isBlank() ? null : parsePositiveInt(clean[1], "Số tập");
        Integer year = clean[5].isBlank() ? null : parsePositiveInt(clean[5], "Năm xuất bản");
        int quantity = parseNonNegativeInt(clean[6], "Tồn kho");
        long price;
        try { price = Long.parseLong(clean[7]); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Giá sách không hợp lệ."); }
        return new Object[]{clean[0], volume, nullable(clean[2]), nullable(clean[3]), nullable(clean[4]), year, quantity, price};
    }

    @Override
    protected void checkDelete(Connection connection, String id, String currentEmployeeId) throws SQLException {
        if (exists(connection, "SELECT 1 FROM CT_PHIEUMUON WHERE MASACH = ?", id))
            throw new SQLException("Không thể xóa sách đã có lịch sử mượn trả.");
    }
}
