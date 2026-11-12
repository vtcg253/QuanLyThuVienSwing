package dao;

import database.DBConnection;
import static dao.DaoSupport.*;
import model.CatalogType;
import model.TableData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CatalogDAO {
    public TableData listCatalog(CatalogType type, String search) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(type.idColumn()).append(", ")
                .append(type.nameColumn());
        for (String column : type.formColumns()) {
            if (!column.equals(type.nameColumn())) {
                sql.append(", ").append(column);
            }
        }
        sql.append(" FROM ").append(type.table()).append(" WHERE UPPER(")
                .append(type.idColumn()).append(") LIKE ? OR UPPER(")
                .append(type.nameColumn()).append(") LIKE ? ORDER BY ")
                .append(type.idColumn());
        List<String> headers = new ArrayList<>(List.of("Mã", type.formLabels()[0]));
        for (int i = 1; i < type.formLabels().length; i++) {
            headers.add(type.formLabels()[i]);
        }
        return query(sql.toString(), headers.toArray(String[]::new), like(search), like(search));
    }

    public void saveCatalog(CatalogType type, String id, String[] values) throws SQLException {
        model.InputValidation.catalog(type, values);
        String[] columns = type.formColumns();
        if (values.length != columns.length) {
            throw new IllegalArgumentException("Dữ liệu danh mục không hợp lệ.");
        }
        String[] cleanValues = java.util.Arrays.stream(values).map(value -> value == null ? "" : value.trim()).toArray(String[]::new);
        if (cleanValues[0].isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            if (id != null && !id.isBlank()) {
                model.InputValidation.code(id, "Mã danh mục");
                if (!exists(connection, "SELECT 1 FROM " + type.table() + " WHERE " + type.idColumn() + " = ?", id))
                    throw new SQLException("Danh mục không còn tồn tại. Vui lòng tải lại danh sách.");
            }
            ensureUnique(connection, type.table(), type.nameColumn(), cleanValues[0], type.idColumn(), id);
            if (id == null || id.isBlank()) {
                String newId = nextCode(connection, type.table(), type.idColumn(), type.prefix());
                String sql = "INSERT INTO " + type.table() + " (" + type.idColumn() + ", "
                        + String.join(", ", columns) + ") VALUES ("
                        + placeholders(columns.length + 1) + ")";
                execute(connection, sql, prepend(newId, cleanValues));
            } else {
                String sql = "UPDATE " + type.table() + " SET " + assignments(columns)
                        + " WHERE " + type.idColumn() + " = ?";
                execute(connection, sql, append(cleanValues, id));
            }
        }
    }

    public void deleteCatalog(CatalogType type, String id) throws SQLException {
        if (type == null) throw new IllegalArgumentException("Danh mục không hợp lệ.");
        model.InputValidation.code(id, "Mã danh mục");
        try (Connection connection = DBConnection.getConnection()) {
            if (!exists(connection, "SELECT 1 FROM " + type.table() + " WHERE " + type.idColumn() + " = ?", id))
                throw new SQLException("Danh mục không còn tồn tại. Vui lòng tải lại danh sách.");
            if (exists(connection, "SELECT 1 FROM SACH WHERE " + type.idColumn() + " = ?", id))
                throw new SQLException("Không thể xóa " + type.title().toLowerCase(java.util.Locale.ROOT) + " đang được sử dụng trong sách.");
            execute(connection, "DELETE FROM " + type.table() + " WHERE " + type.idColumn() + " = ?", new Object[]{id});
        }
    }

    private void ensureUnique(Connection connection, String table, String column, String value,
                              String idColumn, String id) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE UPPER(" + column + ") = UPPER(?)";
        if (id != null && !id.isBlank()) {
            sql += " AND " + idColumn + " <> ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value.trim());
            if (id != null && !id.isBlank()) {
                statement.setString(2, id);
            }
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new SQLException("Tên danh mục đã tồn tại.");
                }
            }
        }
    }
}
