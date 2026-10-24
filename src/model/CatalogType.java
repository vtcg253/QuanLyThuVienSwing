package model;

public enum CatalogType {
    THE_LOAI("Thể loại", "THELOAI", "MATL", "TENTHELOAI", "TL",
            new String[]{"Tên thể loại"}, new String[]{"TENTHELOAI"}),
    TAC_GIA("Tác giả", "TACGIA", "MATG", "TENTACGIA", "TG",
            new String[]{"Tên tác giả", "Địa chỉ"}, new String[]{"TENTACGIA", "DIACHI"}),
    NHA_XUAT_BAN("Nhà xuất bản", "NHAXUATBAN", "MANXB", "TENNXB", "NXB",
            new String[]{"Tên nhà xuất bản", "Địa chỉ", "Điện thoại"},
            new String[]{"TENNXB", "DIACHI", "SDT"});

    private final String title;
    private final String table;
    private final String idColumn;
    private final String nameColumn;
    private final String prefix;
    private final String[] formLabels;
    private final String[] formColumns;

    CatalogType(String title, String table, String idColumn, String nameColumn, String prefix,
                String[] formLabels, String[] formColumns) {
        this.title = title;
        this.table = table;
        this.idColumn = idColumn;
        this.nameColumn = nameColumn;
        this.prefix = prefix;
        this.formLabels = formLabels;
        this.formColumns = formColumns;
    }

    public String title() { return title; }
    public String table() { return table; }
    public String idColumn() { return idColumn; }
    public String nameColumn() { return nameColumn; }
    public String prefix() { return prefix; }
    public String[] formLabels() { return formLabels.clone(); }
    public String[] formColumns() { return formColumns.clone(); }

    @Override
    public String toString() {
        return title;
    }
}
