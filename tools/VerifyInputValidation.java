import model.*;
import java.time.LocalDate;

/** Boundary checks for shared form/DAO input rules; no database connection. */
public class VerifyInputValidation {
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
        accepted(() -> InputValidation.record(RecordType.READERS, new String[]{"Nguyễn Văn A", "2000-02-29", "+84912345678", "Hà Nội"}));
        accepted(() -> InputValidation.record(RecordType.READERS, new String[]{"A", "", "", ""}));
        rejected(() -> InputValidation.record(RecordType.READERS, new String[]{" ", "", "", ""}));
        rejected(() -> InputValidation.record(RecordType.READERS, new String[]{"A", "2023-02-29", "", ""}));
        rejected(() -> InputValidation.record(RecordType.READERS, new String[]{"A", LocalDate.now().plusDays(1).toString(), "", ""}));
        rejected(() -> InputValidation.record(RecordType.EMPLOYEES, new String[]{"A", "", "abc1234567", ""}));
        rejected(() -> InputValidation.record(RecordType.READERS, new String[]{"A".repeat(101), "", "", ""}));
        accepted(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "TG01", "NXB01", "2020", "0"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "TG01", "NXB01", "2020", "-1"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "TG01", "NXB01", "2020", "1.5"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "TG01", "NXB01", "abc", "1"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "TG01", "NXB01", String.valueOf(LocalDate.now().getYear()+1), "1"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "", "TG01", "NXB01", "2020", "1"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "", "NXB01", "2020", "1"}));
        rejected(() -> InputValidation.record(RecordType.BOOKS, new String[]{"Sách", "TL01", "TG01", null, "2020", "1"}));
        rejected(() -> InputValidation.catalog(CatalogType.THE_LOAI, new String[]{"A".repeat(101)}));
        rejected(() -> InputValidation.catalog(CatalogType.NHA_XUAT_BAN, new String[]{"NXB", "", "123"}));
        accepted(() -> InputValidation.catalog(CatalogType.TAC_GIA, new String[]{"Tên tác giả", null}));
        accepted(() -> InputValidation.account("tester", "123456", "THU_THU", true));
        accepted(() -> InputValidation.account("tester", "", "ADMIN", false));
        rejected(() -> InputValidation.account("tester", "", "THU_THU", true));
        rejected(() -> InputValidation.account("tester", "      ", "THU_THU", true));
        rejected(() -> InputValidation.account("tester", "12345", "THU_THU", true));
        rejected(() -> InputValidation.account("tester", "a".repeat(73), "THU_THU", true));
        rejected(() -> InputValidation.account("tester", "ế".repeat(25), "THU_THU", true));
        rejected(() -> InputValidation.account("tester", "123456", "STAFF", true));
        rejected(() -> InputValidation.account("a".repeat(51), "123456", "ADMIN", true));
        rejected(() -> InputValidation.code("S01'", "Mã sách"));
        rejected(() -> InputValidation.record(RecordType.READERS, null));
        System.out.println("PASS: " + checks + " input boundary cases (no database writes).");
    }
}
