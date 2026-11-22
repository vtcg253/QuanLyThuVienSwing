package model;

public enum LoaiHoSo {
    BOOKS("Sách"),
    READERS("Độc giả"),
    EMPLOYEES("Nhân viên");

    private final String title;

    LoaiHoSo(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
