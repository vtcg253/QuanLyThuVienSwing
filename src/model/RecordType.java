package model;

public enum RecordType {
    BOOKS("Sách"),
    READERS("Độc giả"),
    EMPLOYEES("Nhân viên");

    private final String title;

    RecordType(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
