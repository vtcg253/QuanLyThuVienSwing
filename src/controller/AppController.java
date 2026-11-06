package controller;

import dao.AccountDAO;
import dao.BookDAO;
import dao.CatalogDAO;
import dao.EmployeeDAO;
import dao.LoanDAO;
import dao.ReaderDAO;
import dao.RecordDAO;
import dao.StatisticsDAO;
import model.CatalogType;
import model.LibraryUser;
import model.RecordType;
import model.TableData;
import view.DangNhapView;
import view.MainView;
import view.RecordFormDialog;
import view.RecordEditorPanel;
import view.LoanEditorPanel;
import view.CatalogEditorPanel;
import view.EditorPopup;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.Map;

public final class AppController {
    private final AccountDAO accounts = new AccountDAO();
    private final BookDAO books = new BookDAO();
    private final ReaderDAO readers = new ReaderDAO();
    private final EmployeeDAO employees = new EmployeeDAO();
    private final CatalogDAO catalogs = new CatalogDAO();
    private final LoanDAO loans = new LoanDAO();
    private final StatisticsDAO statistics = new StatisticsDAO();
    private DangNhapView loginFrame;
    private MainView mainFrame;
    private LibraryUser currentUser;

    private RecordDAO recordDAO(RecordType type) {
        return switch (type) {
            case BOOKS -> books;
            case READERS -> readers;
            case EMPLOYEES -> employees;
        };
    }

    public void start() {
        loginFrame = new DangNhapView();
        loginFrame.setSubmitAction(this::login);
        loginFrame.showLogin();
    }

    private void login(String username, char[] password) {
        loginFrame.setBusy(true);
        loginFrame.showMessage("Đang xác thực...");
        new SwingWorker<LibraryUser, Void>() {
            @Override
            protected LibraryUser doInBackground() throws Exception {
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
            public void logout() {
                currentUser = null;
                loginFrame = new DangNhapView();
                loginFrame.setSubmitAction(AppController.this::login);
                loginFrame.showLogin();
            }
        });
        mainFrame.setVisible(true);
        loadDashboard();
    }

    private void reload(String page, String search, String status) {
        new SwingWorker<TableData, Void>() {
            private Map<String, Long> loanStatistics;
            @Override
            protected TableData doInBackground() throws Exception {
                if ("loans".equals(page)) loanStatistics = statistics.dashboardStatistics();
                return switch (page) {
                    case "books" -> books.listRecords(search);
                    case "readers" -> readers.listRecords(search);
                    case "employees" -> {
                        requireAdmin();
                        yield employees.listRecords(search);
                    }
                    case "loans" -> loans.listLoans(search, status);
                    case "genres" -> catalogs.listCatalog(CatalogType.THE_LOAI, search);
                    case "authors" -> catalogs.listCatalog(CatalogType.TAC_GIA, search);
                    case "publishers" -> catalogs.listCatalog(CatalogType.NHA_XUAT_BAN, search);
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

    private record DashboardData(Map<String, Long> statistics, TableData books) { }

    private void addRecord(String page) {
        CatalogType catalog = catalogType(page);
        if (catalog != null) {
            openCatalogEditor(page, catalog, null, null);
            return;
        }
        openRecordEditor(page, null);
    }

    private void editRecord(String page, Object[] row) {
        CatalogType catalog = catalogType(page);
        if (catalog != null) {
            String[] initial = new String[row.length - 1];
            for (int i = 1; i < row.length; i++) initial[i - 1] = row[i] == null ? "" : row[i].toString();
            openCatalogEditor(page, catalog, row[0].toString(), initial);
            return;
        }
        openRecordEditor(page, row[0].toString());
    }

    private static int catalogPopupHeight(CatalogType catalog) {
        return switch (catalog) {
            case THE_LOAI -> 320;
            case TAC_GIA -> 410;
            case NHA_XUAT_BAN -> 500;
        };
    }

    private void openCatalogEditor(String page, CatalogType catalog, String id, String[] initial) {
        EditorPopup popup = id == null ? new EditorPopup(mainFrame,
                "Thêm " + catalog.title().toLowerCase(java.util.Locale.ROOT), 560, catalogPopupHeight(catalog)) : null;
        CatalogEditorPanel[] editor = new CatalogEditorPanel[1];
        editor[0] = new CatalogEditorPanel(catalog, id != null, initial, values -> {
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

    private record EditorData(String[] stored, Map<CatalogType, TableData> catalogs) { }

    private void openRecordEditor(String page, String id) {
        RecordType type = recordType(page);
        if (type == RecordType.EMPLOYEES) requireAdmin();
        new SwingWorker<EditorData, Void>() {
            @Override protected EditorData doInBackground() throws Exception {
                Map<CatalogType, TableData> catalogData = new java.util.EnumMap<>(CatalogType.class);
                if (type == RecordType.BOOKS) for (CatalogType catalog : CatalogType.values())
                    catalogData.put(catalog, catalogs.listCatalog(catalog, ""));
                return new EditorData(id == null ? null : recordDAO(type).recordFormValues(id), catalogData);
            }
            @Override protected void done() {
                try {
                    EditorData data = get();
                    String[] stored = data.stored();
                    String[] fields = stored == null ? null : java.util.Arrays.copyOf(stored, type == RecordType.BOOKS ? 6 : 4);
                    String[] account = stored == null || type != RecordType.EMPLOYEES ? null : new String[]{stored[4],
                            stored[5] == null || stored[5].isBlank() ? "THU_THU" : stored[5], stored[6]};
                    EditorPopup popup = id == null && type == RecordType.READERS
                            ? new EditorPopup(mainFrame, "Thêm độc giả", 820, 480) : null;
                    RecordEditorPanel[] editor = new RecordEditorPanel[1];
                    editor[0] = new RecordEditorPanel(type, id, fields, account, data.catalogs(),
                            AppController.this::quickAddCatalog,
                            values -> {
                                editor[0].setBusy(true);
                                if (popup != null) popup.setBusy(true);
                                saveRecordFromEditor(type, id, values, page, editor[0], popup);
                            },
                            () -> { if (popup != null) popup.closeIfIdle();else reload(page, "", "ALL"); });
                    if (popup != null) popup.showEditor(editor[0]);
                    else mainFrame.displayForm((id == null ? "Thêm " : "Chỉnh sửa ") + type.title().toLowerCase(java.util.Locale.ROOT),
                            id == null && type == RecordType.BOOKS ? "add-books" : page, editor[0]);
                } catch (Exception exception) { showError("Không thể mở biểu mẫu: " + rootMessage(exception)); }
            }
        }.execute();
    }

    private void quickAddCatalog(CatalogType catalog, java.util.function.Consumer<RecordEditorPanel.Choice> select) {
        EditorPopup popup = new EditorPopup(mainFrame,"Thêm " + catalog.title().toLowerCase(java.util.Locale.ROOT),560,catalogPopupHeight(catalog));
        CatalogEditorPanel[] editor = new CatalogEditorPanel[1];
        editor[0] = new CatalogEditorPanel(catalog,false,null,values -> {
            popup.setBusy(true);editor[0].setBusy(true);
            new SwingWorker<RecordEditorPanel.Choice, Void>() {
                @Override protected RecordEditorPanel.Choice doInBackground() throws Exception {
                    catalogs.saveCatalog(catalog, null, values);
                    TableData entries = catalogs.listCatalog(catalog, values[0].trim());
                    for (Object[] row : entries.rows()) if (row[1].toString().equalsIgnoreCase(values[0].trim()))
                        return new RecordEditorPanel.Choice(row[0].toString(), row[1].toString());
                    throw new IllegalStateException("Không tìm thấy danh mục vừa tạo.");
                }
                @Override protected void done() {
                    try { RecordEditorPanel.Choice choice=get();select.accept(choice);popup.dispose(); }
                    catch (Exception exception) { popup.setBusy(false);editor[0].showError("Không thể lưu: " + rootMessage(exception)); }
                }
            }.execute();
        },popup::closeIfIdle);
        popup.showEditor(editor[0]);
    }

    private void saveRecordFromEditor(RecordType type, String id, String[] values, String page, RecordEditorPanel editor, EditorPopup popup) {
        int length = type == RecordType.BOOKS ? 6 : 4;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String[] fields = java.util.Arrays.copyOf(values, length);
                if (type == RecordType.EMPLOYEES) {
                    employees.saveWithAccount(id, fields, values[length], values[length + 1],
                            id != null ? values[length + 2] : "THU_THU",
                            Boolean.parseBoolean(values[length + 3]));
                } else {
                    recordDAO(type).saveRecord(id, fields);
                }
                return null;
            }
            @Override protected void done() {
                try { get(); if (popup != null) popup.dispose();reload(page, "", "ALL"); }
                catch (Exception exception) { if (popup != null) popup.setBusy(false);editor.showError("Không thể lưu: " + rootMessage(exception)); }
            }
        }.execute();
    }

    private void deleteRecord(String page, Object[] row) {
        String id = row[0].toString();
        if (!RecordFormDialog.confirm(mainFrame, "Xác nhận xóa",
                new JLabel("Bạn có chắc muốn xóa mã " + id + "?"), "Xóa")) {
            return;
        }
        CatalogType catalog = catalogType(page);
        if (catalog != null) {
            runAction(() -> catalogs.deleteCatalog(catalog, id), page);
        } else {
            RecordType type = recordType(page);
            if (type == RecordType.EMPLOYEES && !currentUser.isAdmin()) {
                showError("Chỉ ADMIN được quản lý nhân viên.");
                return;
            }
            runAction(() -> recordDAO(type).deleteRecord(id, currentUser.employeeId()), page);
        }
    }

    private void createLoan() {
        new SwingWorker<TableData[], Void>() {
            @Override protected TableData[] doInBackground() throws Exception {
                return new TableData[]{readers.listRecords(""), books.listRecords("")};
            }
            @Override protected void done() {
                try {
                    TableData[] data = get();
                    LoanEditorPanel[] editor = new LoanEditorPanel[1];
                    editor[0] = new LoanEditorPanel(data[0], data[1], request -> {
                        editor[0].setBusy(true);
                        new SwingWorker<String, Void>() {
                            @Override protected String doInBackground() throws Exception {
                                return loans.createLoan(request.readerId(), request.borrowed(), request.due(), request.books(), currentUser.employeeId());
                            }
                            @Override protected void done() {
                                try { get(); reload("loans", "", "ALL"); }
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
        String loanId = row[0].toString();
        String bookId = row[6].toString();
        int alreadyLost = Integer.parseInt(row[8].toString());
        int borrowed = Integer.parseInt(row[7].toString());
        JTextField quantity = new JTextField(Integer.toString(Math.max(1, borrowed - alreadyLost)));
        if (!RecordFormDialog.confirm(mainFrame.loanActionParent(), "Số lượng báo mất (còn mượn: " + (borrowed - alreadyLost) + ")", quantity)) return;
        try {
            int value = Integer.parseInt(quantity.getText().trim());
            if (value < 1 || value > borrowed - alreadyLost) {
                showError("Số lượng báo mất phải từ 1 đến " + (borrowed - alreadyLost) + ".");
                return;
            }
            runLoanAction(() -> loans.reportLost(loanId, bookId, value), loanId);
        } catch (NumberFormatException exception) {
            showError("Số lượng mất phải là số nguyên.");
        }
    }

    private boolean confirmLoanAction(String message) {
        return RecordFormDialog.confirm(mainFrame.loanActionParent(), "Xác nhận trả sách", new JLabel(message), "Xác nhận");
    }

    private record LoanRefresh(TableData list, TableData details, Map<String, Long> statistics) { }

    private void runLoanAction(SqlAction action, String loanId) {
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
                        RecordFormDialog.showContent(mainFrame, "Hoàn tất", new JLabel("Đã lưu thành công."));
                        reload(page, "", "ALL");
                    }
                } catch (Exception exception) {
                    showError("Thao tác thất bại: " + rootMessage(exception));
                }
            }
        }.execute();
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new SecurityException("Chỉ ADMIN được quản lý nhân viên.");
        }
    }

    private RecordType recordType(String page) {
        return switch (page) {
            case "books" -> RecordType.BOOKS;
            case "readers" -> RecordType.READERS;
            case "employees" -> RecordType.EMPLOYEES;
            default -> throw new IllegalArgumentException("Loại hồ sơ không hợp lệ.");
        };
    }

    private CatalogType catalogType(String page) {
        return switch (page) {
            case "genres" -> CatalogType.THE_LOAI;
            case "authors" -> CatalogType.TAC_GIA;
            case "publishers" -> CatalogType.NHA_XUAT_BAN;
            default -> null;
        };
    }

    private void showError(String message) {
        Component parent = mainFrame != null ? mainFrame.loanActionParent() : loginFrame;
        RecordFormDialog.showContent(parent, "Không thể hoàn tất", new JLabel(message));
    }

    private String rootMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws Exception;
    }
}
