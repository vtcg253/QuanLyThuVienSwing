package dao;

import database.DBConnection;
import model.TableData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static dao.DaoSupport.*;

/** Shared CRUD mechanics; concrete DAOs provide their own SQL and validation. */
public abstract class RecordDAO {
    private final String table;
    private final String idColumn;
    private final String prefix;
    private final String[] columns;

    protected RecordDAO(String table, String idColumn, String prefix, String... columns) {
        this.table = table;
        this.idColumn = idColumn;
        this.prefix = prefix;
        this.columns = columns.clone();
    }

    public abstract TableData listRecords(String search) throws SQLException;

    protected abstract String formSql();

    protected abstract Object[] convertValues(String[] values);

    protected abstract void checkDelete(Connection connection, String id, String currentEmployeeId)
            throws SQLException;

    public final String[] recordFormValues(String id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(formSql())) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Không tìm thấy dữ liệu có mã " + id + ".");
                String[] values = new String[result.getMetaData().getColumnCount()];
                for (int i = 0; i < values.length; i++) values[i] = displayValue(result.getObject(i + 1));
                return values;
            }
        }
    }

    protected final String saveValues(Connection connection, String id, String[] values) throws SQLException {
        if (values.length != columns.length) throw new IllegalArgumentException("Dữ liệu biểu mẫu không hợp lệ.");
        if (values[0].isBlank()) throw new IllegalArgumentException(
                "SACH".equals(table) ? "Tên sách không được để trống." : "Họ tên không được để trống.");
        Object[] parameters = convertValues(values);
        if (id == null || id.isBlank()) {
            String newId = nextCode(connection, table, idColumn, prefix);
            execute(connection, "INSERT INTO " + table + " (" + idColumn + ", "
                    + String.join(", ", columns) + ") VALUES (" + placeholders(columns.length + 1) + ")",
                    prepend(newId, parameters));
            return newId;
        }
        execute(connection, "UPDATE " + table + " SET " + assignments(columns) + " WHERE " + idColumn + " = ?",
                append(parameters, id));
        return id;
    }

    public final void saveRecord(String id, String[] values) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                saveValues(connection, id, values);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public final void deleteRecord(String id, String currentEmployeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                checkDelete(connection, id, currentEmployeeId);
                execute(connection, "DELETE FROM " + table + " WHERE " + idColumn + " = ?", new Object[]{id});
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
