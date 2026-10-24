package model;

public record LibraryUser(String employeeId, String fullName, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
