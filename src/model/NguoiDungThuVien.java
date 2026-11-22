package model;

public record NguoiDungThuVien(String employeeId, String fullName, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
