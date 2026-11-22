import model.*;
import java.time.LocalDate;

/** Boundary checks for shared form/DAO input rules; no database connection. */
public class KiemTraDuLieuNhap {
    private static int checks;
    private static void accepted(Runnable action) { action.run(); checks++; }
    private static void rejected(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) {
            if (expected.getMessage() == null || expected.getMessage().isBlank())
                throw new AssertionError("Missing user-facing message");
            checks++; return;
        }
        throw new AssertionError("Invalid input accepted");
    }
    public static void main(String[] args) {
        accepted(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, new String[]{"Nguyễn Văn A", "2000-02-29", "+84912345678", "Hà Nội"}));
        accepted(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, new String[]{"A", "", "", ""}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, new String[]{" ", "", "", ""}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, new String[]{"A", "2023-02-29", "", ""}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, new String[]{"A", LocalDate.now().plusDays(1).toString(), "", ""}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.EMPLOYEES, new String[]{"A", "", "abc1234567", ""}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, new String[]{"A".repeat(101), "", "", ""}));
        accepted(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", "2020", "0", "25000"}));
        accepted(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "2", "TL01", "TG01", "NXB01", "2020", "0", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "0", "TL01", "TG01", "NXB01", "2020", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "1.5", "TL01", "TG01", "NXB01", "2020", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", "2020", "-1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", "2020", "1.5", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", "abc", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", String.valueOf(LocalDate.now().getYear()+1), "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "", "TG01", "NXB01", "2020", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "", "NXB01", "2020", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", null, "2020", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.THE_LOAI, new String[]{"A".repeat(101)}));
        rejected(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.NHA_XUAT_BAN, new String[]{"NXB", "Hà Nội", "123"}));
        rejected(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.NHA_XUAT_BAN, new String[]{"NXB", " ", "0241234567"}));
        rejected(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.NHA_XUAT_BAN, new String[]{"NXB", "Hà Nội", " "}));
        rejected(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.NHA_XUAT_BAN, new String[]{"NXB", "A".repeat(201), "0241234567"}));
        rejected(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.NHA_XUAT_BAN, new String[]{"NXB", "Hà Nội", "abc1234567"}));
        accepted(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.NHA_XUAT_BAN, new String[]{"NXB", "Hà Nội", "0241234567"}));
        accepted(() -> KiemTraDuLieu.catalog(LoaiDanhMuc.TAC_GIA, new String[]{"Tên tác giả", null}));
        accepted(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", "2020", "1", "25000"}));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.BOOKS, new String[]{"Sách", "", "TL01", "TG01", "NXB01", "2020", "1", "-1"}));
        accepted(() -> KiemTraDuLieu.account("tester", "123456", "THU_THU", true));
        accepted(() -> KiemTraDuLieu.account("tester", "", "ADMIN", false));
        rejected(() -> KiemTraDuLieu.account("tester", "", "THU_THU", true));
        rejected(() -> KiemTraDuLieu.account("tester", "      ", "THU_THU", true));
        rejected(() -> KiemTraDuLieu.account("tester", "12345", "THU_THU", true));
        rejected(() -> KiemTraDuLieu.account("tester", "a".repeat(73), "THU_THU", true));
        rejected(() -> KiemTraDuLieu.account("tester", "ế".repeat(25), "THU_THU", true));
        rejected(() -> KiemTraDuLieu.account("tester", "123456", "STAFF", true));
        rejected(() -> KiemTraDuLieu.account("a".repeat(51), "123456", "ADMIN", true));
        rejected(() -> KiemTraDuLieu.code("S01'", "Mã sách"));
        rejected(() -> KiemTraDuLieu.record(LoaiHoSo.READERS, null));
        System.out.println("PASS: " + checks + " input boundary cases (no database writes).");
    }
}
