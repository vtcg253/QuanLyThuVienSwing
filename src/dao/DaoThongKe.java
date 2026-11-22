package dao;

import database.KetNoiOracle;
import static dao.HoTroDao.*;
import model.DuLieuBang;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public final class DaoThongKe {
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
        try (Connection connection = KetNoiOracle.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            for (int i = 0; i < labels.length; i++) values.put(labels[i], result.getLong(i + 1));
        }
        return values;
    }

    public DuLieuBang dashboardBooks() throws SQLException {
        return query("""
                SELECT s.MASACH,
                       CASE WHEN s.SO_TAP IS NULL THEN s.TENSACH
                            ELSE s.TENSACH || N' · Tập ' || TO_CHAR(s.SO_TAP) END,
                       s.SOLUONG, COALESCE(SUM(c.SOLUONG), 0)
                FROM SACH s LEFT JOIN CT_PHIEUMUON c ON c.MASACH = s.MASACH
                GROUP BY s.MASACH, s.TENSACH, s.SO_TAP, s.SOLUONG
                ORDER BY s.SOLUONG DESC, s.MASACH
                """, new String[]{"Mã sách", "Tên sách", "Tồn kho", "Đã mượn"});
    }
}
