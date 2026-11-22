package controller;

import dao.DaoTaiKhoan;
import dao.DaoSach;
import dao.DaoDanhMuc;
import dao.DaoNhanVien;
import dao.DaoPhieuMuon;
import dao.DaoDocGia;
import dao.DaoHoSo;
import dao.DaoThongKe;
import model.LoaiDanhMuc;
import model.NguoiDungThuVien;
import model.LoaiHoSo;
import model.DuLieuBang;
import view.ManHinhDangNhap;
import view.MainView;
import view.HopThoaiXacNhan;
import view.FormHoSo;
import view.FormPhieuMuon;
import view.FormDanhMuc;
import view.HopThoaiChinhSua;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.Map;

public final class AppController {
    private final DaoTaiKhoan accounts = new DaoTaiKhoan();
    private final DaoSach books = new DaoSach();
    private final DaoDocGia readers = new DaoDocGia();
    private final DaoNhanVien employees = new DaoNhanVien();
    private final DaoDanhMuc catalogs = new DaoDanhMuc();
    private final DaoPhieuMuon loans = new DaoPhieuMuon();
    private final DaoThongKe statistics = new DaoThongKe();
    private ManHinhDangNhap loginFrame;
    private MainView mainFrame;
    private NguoiDungThuVien currentUser;

    private DaoHoSo recordDAO(LoaiHoSo type) {
        return switch (type) {
            case BOOKS -> books;
            case READERS -> readers;
            case EMPLOYEES -> employees;
        };
    }

    public void start() {
        loginFrame = new ManHinhDangNhap();
        loginFrame.setSubmitAction(this::login);
        loginFrame.showLogin();
    }

    private void login(String username, char[] password) {
        loginFrame.setBusy(true);
        loginFrame.showMessage("Đang xác thực...");
        new SwingWorker<NguoiDungThuVien, Void>() {
            @Override
            protected NguoiDungThuVien doInBackground() throws Exception {
                return accounts.login(username, password);
            }

            @Override
            protected void done() {
                java.util.Arrays.fill(password, '\0');
                loginFrame.setBusy(false);
                try {
                    currentUser = get();
                    if (currentUser == null) {
                        loginFrame.showMessage("Tên đăng nhập hoặc mật khẩu không đúng.");
                        return;
                    }
                    loginFrame.close();
                    openMainFrame();
                } catch (Exception exception) {
                    loginFrame.showMessage("Không thể kết nối/xác thực Oracle: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void openMainFrame() {
        mainFrame = new MainView(currentUser);
        mainFrame.setActions(new MainView.Actions() {
            @Override
            public void navigate(String page) {
                if ("dashboard".equals(page)) {
                    loadDashboard();
                } else {
                    if ("books".equals(page)) mainFrame.resetBookSeries();
                    reload(page, "", "ALL");
                }
            }

            @Override
            public void reload(String page, String search, String status) {
                AppController.this.reload(page, search, status);
            }

            @Override
            public void add(String page) {
                addRecord(page);
            }

            @Override
            public void addBookVolume(String seriesTitle) {
                AppController.this.addBookVolume(seriesTitle);
            }

            @Override
            public void edit(String page, Object[] row) {
                editRecord(page, row);
            }

            @Override
            public void delete(String page, Object[] row) {
                deleteRecord(page, row);
            }

            @Override
            public void createLoan() {
                AppController.this.createLoan();
            }

            @Override
            public void returnBook(Object[] row) {
                AppController.this.returnBook(row);
            }

            @Override
            public void reportLost(Object[] row) {
                AppController.this.reportLost(row);
            }

            @Override
            public void markPenaltyPaid(Object[] row) {
                AppController.this.markPenaltyPaid(row);
            }

            @Override
            public void logout() {
                currentUser = null;
                loginFrame = new ManHinhDangNhap();
                loginFrame.setSubmitAction(AppController.this::login);
                loginFrame.showLogin();
            }
        });
        mainFrame.setVisible(true);
        loadDashboard();
    }

    private void reload(String page, String search, String status) {
        new SwingWorker<DuLieuBang, Void>() {
            private Map<String, Long> loanStatistics;
            @Override
            protected DuLieuBang doInBackground() throws Exception {
                if ("loans".equals(page)) loanStatistics = statistics.dashboardStatistics();
                return switch (page) {
                    case "books" -> books.listRecords(search);
                    case "readers" -> readers.listRecords(search);
                    case "employees" -> {
                        requireAdmin();
                        yield employees.listRecords(search);
                    }
                    case "loans" -> loans.listLoans(search, status);
                    case "penalties" -> loans.listPenalties(search);
                    case "genres" -> catalogs.listCatalog(LoaiDanhMuc.THE_LOAI, search);
                    case "authors" -> catalogs.listCatalog(LoaiDanhMuc.TAC_GIA, search);
                    case "publishers" -> catalogs.listCatalog(LoaiDanhMuc.NHA_XUAT_BAN, search);
                    default -> throw new IllegalArgumentException("Màn hình không hợp lệ.");
                };
            }

            @Override
            protected void done() {
                try {
                    mainFrame.display(page, get());
                    if (loanStatistics != null) mainFrame.displayLoanStatistics(loanStatistics);
                } catch (Exception exception) {
                    showError("Không thể tải dữ liệu: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void loadDashboard() {
        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                return new DashboardData(statistics.dashboardStatistics(), statistics.dashboardBooks());
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();
                    mainFrame.displayDashboard(data.statistics(), data.books());
                } catch (Exception exception) {
                    showError("Không thể tải tổng quan: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private record DashboardData(Map<String, Long> statistics, DuLieuBang books) { }

    private void addRecord(String page) {
        LoaiDanhMuc catalog = catalogType(page);
        if (catalog != null) {
            openCatalogEditor(page, catalog, null, null);
            return;
        }
        openRecordEditor(page, null);
    }

    private void addBookVolume(String seriesTitle) {
        new SwingWorker<String[], Void>() {
            @Override protected String[] doInBackground() throws Exception {
                DuLieuBang series = books.listRecords(seriesTitle);
                Object[] template = null;
                int highestVolume = 0;
                for (Object[] row : series.rows()) {
                    if (!seriesTitle.equals(String.valueOf(row[1]))) continue;
                    int volume = volumeNumber(row[2]);
                    if (template == null || volume > highestVolume) {
                        template = row;
                        highestVolume = volume;
                    }
                }
                if (template == null) throw new IllegalStateException("Không tìm thấy sách trong bộ này.");

                String[] previous = books.recordFormValues(String.valueOf(template[0]));
                return new String[]{seriesTitle, Integer.toString(highestVolume + 1), previous[2], previous[3],
                        previous[4], previous[5], "0", previous[7]};
            }

            @Override protected void done() {
                try {
                    openRecordEditor("books", null, get());
                } catch (Exception exception) {
                    showError("Không thể lấy thông tin bộ sách: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private static int volumeNumber(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try { return Integer.parseInt(value.toString().trim()); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private void editRecord(String page, Object[] row) {
        LoaiDanhMuc catalog = catalogType(page);
        if (catalog != null) {
            String[] initial = new String[row.length - 1];
            for (int i = 1; i < row.length; i++) initial[i - 1] = row[i] == null ? "" : row[i].toString();
            openCatalogEditor(page, catalog, row[0].toString(), initial);
            return;
        }
        openRecordEditor(page, row[0].toString());
    }

    private static int catalogPopupHeight(LoaiDanhMuc catalog) {
        return switch (catalog) {
            case THE_LOAI -> 320;
            case TAC_GIA -> 410;
            case NHA_XUAT_BAN -> 500;
        };
    }

    private void openCatalogEditor(String page, LoaiDanhMuc catalog, String id, String[] initial) {
        HopThoaiChinhSua popup = id == null ? new HopThoaiChinhSua(mainFrame,
                "Thêm " + catalog.title().toLowerCase(java.util.Locale.ROOT), 560, catalogPopupHeight(catalog)) : null;
        FormDanhMuc[] editor = new FormDanhMuc[1];
        editor[0] = new FormDanhMuc(catalog, id != null, initial, values -> {
            editor[0].setBusy(true);
            if (popup != null) popup.setBusy(true);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    catalogs.saveCatalog(catalog, id, values); return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        if (popup != null) popup.dispose();
                        showSaveSuccess(id);
                        reload(page, "", "ALL");
                    } catch (Exception exception) {
                        if (popup != null) popup.setBusy(false);
                        editor[0].showError("Không thể lưu: " + rootMessage(exception));
                    }
                }
            }.execute();
        }, () -> { if (popup != null) popup.closeIfIdle();else reload(page, "", "ALL"); });
        if (popup != null) popup.showEditor(editor[0]);
        else mainFrame.displayForm("Chỉnh sửa " + catalog.title().toLowerCase(java.util.Locale.ROOT), page, editor[0]);
    }

    private record EditorData(String[] stored, Map<LoaiDanhMuc, DuLieuBang> catalogs) { }

    private void openRecordEditor(String page, String id) {
        openRecordEditor(page, id, null);
    }

    private void openRecordEditor(String page, String id, String[] initialValues) {
        LoaiHoSo type = recordType(page);
        if (type == LoaiHoSo.EMPLOYEES) requireAdmin();
        new SwingWorker<EditorData, Void>() {
            @Override protected EditorData doInBackground() throws Exception {
                Map<LoaiDanhMuc, DuLieuBang> catalogData = new java.util.EnumMap<>(LoaiDanhMuc.class);
                if (type == LoaiHoSo.BOOKS) for (LoaiDanhMuc catalog : LoaiDanhMuc.values())
                    catalogData.put(catalog, catalogs.listCatalog(catalog, ""));
                return new EditorData(id == null ? null : recordDAO(type).recordFormValues(id), catalogData);
            }
            @Override protected void done() {
                try {
                    EditorData data = get();
                    String[] stored = data.stored();
                    String[] fields = initialValues != null ? initialValues.clone()
                            : stored == null ? null : java.util.Arrays.copyOf(stored, type == LoaiHoSo.BOOKS ? 8 : 4);
                    String[] account = stored == null || type != LoaiHoSo.EMPLOYEES ? null : new String[]{stored[4],
                            stored[5] == null || stored[5].isBlank() ? "THU_THU" : stored[5], stored[6]};
                    HopThoaiChinhSua popup = id == null && type == LoaiHoSo.READERS
                            ? new HopThoaiChinhSua(mainFrame, "Thêm độc giả", 820, 480) : null;
                    FormHoSo[] editor = new FormHoSo[1];
                    editor[0] = new FormHoSo(type, id, fields, account, data.catalogs(),
                            AppController.this::quickAddCatalog,
                            values -> {
                                editor[0].setBusy(true);
                                if (popup != null) popup.setBusy(true);
                                saveRecordFromEditor(type, id, values, page, editor[0], popup);
                            },
                            () -> { if (popup != null) popup.closeIfIdle();else reload(page, "", "ALL"); });
                    if (popup != null) popup.showEditor(editor[0]);
                    else mainFrame.displayForm((id == null ? "Thêm " : "Chỉnh sửa ") + type.title().toLowerCase(java.util.Locale.ROOT),
                            id == null && type == LoaiHoSo.BOOKS ? "add-books" : page, editor[0]);
                } catch (Exception exception) { showError("Không thể mở biểu mẫu: " + rootMessage(exception)); }
            }
        }.execute();
    }

    private void quickAddCatalog(LoaiDanhMuc catalog, java.util.function.Consumer<FormHoSo.Choice> select) {
        HopThoaiChinhSua popup = new HopThoaiChinhSua(mainFrame,"Thêm " + catalog.title().toLowerCase(java.util.Locale.ROOT),560,catalogPopupHeight(catalog));
        FormDanhMuc[] editor = new FormDanhMuc[1];
        editor[0] = new FormDanhMuc(catalog,false,null,values -> {
            popup.setBusy(true);editor[0].setBusy(true);
            new SwingWorker<FormHoSo.Choice, Void>() {
                @Override protected FormHoSo.Choice doInBackground() throws Exception {
                    catalogs.saveCatalog(catalog, null, values);
                    DuLieuBang entries = catalogs.listCatalog(catalog, values[0].trim());
                    for (Object[] row : entries.rows()) if (row[1].toString().equalsIgnoreCase(values[0].trim()))
                        return new FormHoSo.Choice(row[0].toString(), row[1].toString());
                    throw new IllegalStateException("Không tìm thấy danh mục vừa tạo.");
                }
                @Override protected void done() {
                    try { FormHoSo.Choice choice=get();select.accept(choice);popup.dispose();showSaveSuccess(null); }
                    catch (Exception exception) { popup.setBusy(false);editor[0].showError("Không thể lưu: " + rootMessage(exception)); }
                }
            }.execute();
        },popup::closeIfIdle);
        popup.showEditor(editor[0]);
    }

    private void saveRecordFromEditor(LoaiHoSo type, String id, String[] values, String page, FormHoSo editor, HopThoaiChinhSua popup) {
        int length = type == LoaiHoSo.BOOKS ? 8 : 4;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String[] fields = java.util.Arrays.copyOf(values, length);
                if (type == LoaiHoSo.EMPLOYEES) {
                    employees.saveWithAccount(id, fields, values[length], values[length + 1],
                            id != null ? values[length + 2] : "THU_THU",
                            Boolean.parseBoolean(values[length + 3]));
                } else {
                    recordDAO(type).saveRecord(id, fields);
                }
                return null;
            }
            @Override protected void done() {
                try { get(); if (popup != null) popup.dispose();showSaveSuccess(id);reload(page, "", "ALL"); }
                catch (Exception exception) { if (popup != null) popup.setBusy(false);editor.showError("Không thể lưu: " + rootMessage(exception)); }
            }
        }.execute();
    }

    private void deleteRecord(String page, Object[] row) {
        if (currentUser == null || !currentUser.isAdmin()) {
            showError("Chỉ ADMIN được phép xóa dữ liệu.");
            return;
        }
        String id = row[0].toString();
        if (!HopThoaiXacNhan.confirm(mainFrame, "Xác nhận xóa",
                new JLabel("Bạn có chắc muốn xóa mã " + id + "?"), "Xóa")) {
            return;
        }
        LoaiDanhMuc catalog = catalogType(page);
        if (catalog != null) {
            runAction(() -> catalogs.deleteCatalog(catalog, id), page);
        } else {
            LoaiHoSo type = recordType(page);
            if (type == LoaiHoSo.EMPLOYEES && !currentUser.isAdmin()) {
                showError("Chỉ ADMIN được quản lý nhân viên.");
                return;
            }
            runAction(() -> recordDAO(type).deleteRecord(id, currentUser.employeeId()), page);
        }
    }

    private void createLoan() {
        new SwingWorker<DuLieuBang[], Void>() {
            @Override protected DuLieuBang[] doInBackground() throws Exception {
                return new DuLieuBang[]{readers.listRecords(""), books.listRecords("")};
            }
            @Override protected void done() {
                try {
                    DuLieuBang[] data = get();
                    FormPhieuMuon[] editor = new FormPhieuMuon[1];
                    editor[0] = new FormPhieuMuon(data[0], data[1], request -> {
                        editor[0].setBusy(true);
                        new SwingWorker<String, Void>() {
                            @Override protected String doInBackground() throws Exception {
                                return loans.createLoan(request.readerId(), request.borrowed(), request.due(), request.books(), currentUser.employeeId());
                            }
                            @Override protected void done() {
                                try {
                                    String loanId = get();
                                    HopThoaiXacNhan.showContent(mainFrame, "Hoàn tất", new JLabel("Đã lập phiếu mượn thành công."));
                                    mainFrame.showLoan(loanId);
                                }
                                catch (Exception exception) { editor[0].showError("Không thể lập phiếu: " + rootMessage(exception)); }
                            }
                        }.execute();
                    }, () -> reload("loans", "", "ALL"));
                    mainFrame.displayForm("Lập phiếu mượn", "loans", editor[0]);
                } catch (Exception exception) { showError(rootMessage(exception)); }
            }
        }.execute();
    }

    private void returnBook(Object[] row) {
        if (!confirmLoanAction("Đánh dấu đã trả sách này?")) return;
        String loanId = row[0].toString();
        String bookId = row[6].toString();
        runLoanAction(() -> loans.returnBook(loanId, bookId), loanId);
    }

    private void reportLost(Object[] row) {
        try {
            String loanId = row[0].toString();
            String bookId = row[6].toString();
            int alreadyLost = Integer.parseInt(row[8].toString());
            int borrowed = Integer.parseInt(row[7].toString());
            if (row.length <= 20 || !(row[20] instanceof Number price)) {
                showError("Không đọc được giá sách. Hãy tải lại danh sách phiếu.");
                return;
            }
            long unitPrice = price.longValue();
            if (unitPrice <= 0) {
                showError("Giá sách đang bằng 0. Hãy cập nhật giá trong mục Danh sách sách trước khi lập phiếu phạt.");
                return;
            }
            JTextField quantity = new JTextField(Integer.toString(Math.max(1, borrowed - alreadyLost)));
            javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 8));
            form.add(new JLabel("Số lượng báo mất (còn mượn: " + (borrowed - alreadyLost) + ")"));
            form.add(quantity);
            form.add(new JLabel("Giá phạt mỗi cuốn: " + money(unitPrice)));
            if (!HopThoaiXacNhan.confirm(mainFrame.loanActionParent(), "Lập phiếu phạt", form, "Lập phiếu phạt")) return;
            int value = Integer.parseInt(quantity.getText().trim());
            if (value < 1 || value > borrowed - alreadyLost) {
                showError("Số lượng báo mất phải từ 1 đến " + (borrowed - alreadyLost) + ".");
                return;
            }
            long amount = Math.multiplyExact(unitPrice, value);
            String receipt = "Phiếu phạt mất sách · " + loanId + "\nSách: " + row[5]
                    + "\nSố lượng: " + value + "\nĐơn giá: " + money(unitPrice)
                    + "\nTổng tiền: " + money(amount);
            runLoanAction(() -> loans.reportLost(loanId, bookId, value, currentUser.employeeId()), loanId, receipt);
        } catch (NumberFormatException exception) {
            showError("Số lượng mất phải là số nguyên.");
        } catch (ArithmeticException exception) {
            showError("Số tiền phạt vượt giới hạn.");
        } catch (RuntimeException exception) {
            showError("Không thể mở phiếu phạt: " + rootMessage(exception));
        }
    }

    private boolean confirmLoanAction(String message) {
        return HopThoaiXacNhan.confirm(mainFrame.loanActionParent(), "Xác nhận trả sách", new JLabel(message), "Xác nhận");
    }

    private void markPenaltyPaid(Object[] row) {
        String penaltyId = row[0].toString();
        if (!"CHUA_THANH_TOAN".equals(row[10])) {
            showError("Phiếu phạt này đã được thanh toán.");
            return;
        }
        if (!HopThoaiXacNhan.confirm(mainFrame, "Xác nhận thanh toán",
                new JLabel("Xác nhận đã thu tiền cho phiếu phạt " + penaltyId + "?"), "Đã thu tiền")) {
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                loans.markPenaltyPaid(penaltyId);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    HopThoaiXacNhan.showContent(mainFrame, "Hoàn tất", new JLabel("Đã xác nhận thanh toán phiếu phạt."));
                    reload("penalties", mainFrame.penaltySearch(), "ALL");
                } catch (Exception exception) {
                    showError("Không thể xác nhận thanh toán: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private String money(long amount) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(amount) + " VNĐ";
    }

    private record LoanRefresh(DuLieuBang list, DuLieuBang details, Map<String, Long> statistics) { }

    private void runLoanAction(SqlAction action, String loanId) {
        runLoanAction(action, loanId, null);
    }

    private void runLoanAction(SqlAction action, String loanId, String receipt) {
        String search = mainFrame.loanSearch();
        String status = mainFrame.loanStatus();
        mainFrame.setLoanBusy(true);
        new SwingWorker<LoanRefresh, Void>() {
            private boolean saved;
            @Override protected LoanRefresh doInBackground() throws Exception {
                action.run();
                saved = true;
                return new LoanRefresh(loans.listLoans(search,status), loans.listLoans(loanId,"ALL"),
                        statistics.dashboardStatistics());
            }
            @Override protected void done() {
                try {
                    LoanRefresh refreshed = get();
                    if (mainFrame.isDisplayable()) {
                        mainFrame.display("loans",refreshed.list());
                        mainFrame.displayLoanStatistics(refreshed.statistics());
                        mainFrame.refreshLoanDetails(loanId,refreshed.details());
                        if (receipt != null) HopThoaiXacNhan.showContent(mainFrame, "Phiếu phạt", new JLabel("<html>" + receipt.replace("\n", "<br>") + "</html>"));
                    }
                } catch (Exception exception) {
                    showError((saved ? "Đã ghi nhận thao tác nhưng chưa tải lại được dữ liệu. Hãy đóng và mở lại phiếu: "
                            : "Thao tác thất bại: ") + rootMessage(exception));
                } finally { mainFrame.setLoanBusy(false); }
            }
        }.execute();
    }

    private void runAction(SqlAction action, String page) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                action.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (mainFrame.isDisplayable()) {
                        HopThoaiXacNhan.showContent(mainFrame, "Hoàn tất", new JLabel("Đã xóa thành công."));
                        reload(page, "", "ALL");
                    }
                } catch (Exception exception) {
                    showError("Thao tác thất bại: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void showSaveSuccess(String id) {
        HopThoaiXacNhan.showContent(mainFrame, "Hoàn tất",
                new JLabel(id == null ? "Đã thêm thành công." : "Đã cập nhật thành công."));
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new SecurityException("Chỉ ADMIN được quản lý nhân viên.");
        }
    }

    private LoaiHoSo recordType(String page) {
        return switch (page) {
            case "books" -> LoaiHoSo.BOOKS;
            case "readers" -> LoaiHoSo.READERS;
            case "employees" -> LoaiHoSo.EMPLOYEES;
            default -> throw new IllegalArgumentException("Loại hồ sơ không hợp lệ.");
        };
    }

    private LoaiDanhMuc catalogType(String page) {
        return switch (page) {
            case "genres" -> LoaiDanhMuc.THE_LOAI;
            case "authors" -> LoaiDanhMuc.TAC_GIA;
            case "publishers" -> LoaiDanhMuc.NHA_XUAT_BAN;
            default -> null;
        };
    }

    private void showError(String message) {
        Component parent = mainFrame != null ? mainFrame.loanActionParent() : loginFrame;
        HopThoaiXacNhan.showContent(parent, "Không thể hoàn tất", new JLabel(message));
    }

    private String rootMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (current instanceof java.sql.SQLException sql) {
            switch (sql.getErrorCode()) {
                case 2292: return "Không thể xóa dữ liệu đang được sử dụng hoặc đã có lịch sử giao dịch.";
                case 2291: return "Dữ liệu liên quan không còn tồn tại. Vui lòng tải lại và chọn lại.";
                case 1: return "Mã hoặc thông tin duy nhất đã tồn tại. Vui lòng kiểm tra và thử lại.";
                case 12899: return "Nội dung nhập vượt quá độ dài cho phép. Vui lòng rút ngắn thông tin.";
                case 1400: return "Vui lòng nhập đầy đủ các thông tin bắt buộc.";
                case 2290: return "Dữ liệu không hợp lệ theo quy tắc hệ thống. Vui lòng kiểm tra lại.";
                default: break;
            }
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws Exception;
    }
}
