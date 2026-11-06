package dao;

import database.DBConnection;
import model.TableData;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class DaoSupport {
    private DaoSupport() {}

    static String nextCode(Connection connection, String table, String column, String prefix)
            throws SQLException {
        String sql = "SELECT NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(" + column
                + ", '[0-9]+$'))), 0) + 1 FROM " + table
                + " WHERE REGEXP_LIKE(" + column + ", '^" + prefix + "[0-9]+$')";
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return prefix + String.format("%02d", result.getInt(1));
        }
    }

    static boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    static void deleteById(String table, String idColumn, String id) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            execute(connection, "DELETE FROM " + table + " WHERE " + idColumn + " = ?", new Object[]{id});
        }
    }

    static TableData query(String sql, String[] headers, Object... parameters) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Object[] row = new Object[headers.length];
                    for (int i = 0; i < headers.length; i++) {
                        Object value = result.getObject(i + 1);
                        row[i] = value instanceof Timestamp timestamp
                                ? timestamp.toLocalDateTime().toLocalDate()
                                : value instanceof Date date ? date.toLocalDate() : value;
                    }
                    rows.add(row);
                }
            }
        }
        return new TableData(headers, rows);
    }

    static void execute(Connection connection, String sql, Object[] parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            statement.executeUpdate();
        }
    }

    static void bind(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    static String assignments(String[] columns) {
        return String.join(", ", java.util.Arrays.stream(columns).map(column -> column + " = ?").toList());
    }

    static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

    static Object[] append(Object[] values, Object last) {
        Object[] result = java.util.Arrays.copyOf(values, values.length + 1);
        result[values.length] = last;
        return result;
    }

    static Object[] prepend(Object first, Object[] values) {
        Object[] result = new Object[values.length + 1];
        result[0] = first;
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    static String like(String text) {
        return "%" + (text == null ? "" : text.trim().toUpperCase()) + "%";
    }

    static int parsePositiveInt(String value, String field) {
        try {
            int number = Integer.parseInt(value);
            if (number <= 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " phải là số nguyên dương.");
        }
    }

    static int parseNonNegativeInt(String value, String field) {
        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(field + " phải là số nguyên không âm.");
        }
    }

    static String nullable(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static String displayValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        return value.toString();
    }
    static Object[] personValues(String[] values) {
        String[] clean = java.util.Arrays.stream(values).map(String::trim).toArray(String[]::new);
        Date birthDate = null;
        if (!clean[1].isBlank()) {
            try {
                birthDate = Date.valueOf(LocalDate.parse(clean[1]));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Ngày sinh phải theo định dạng YYYY-MM-DD.");
            }
        }
        return new Object[]{clean[0], birthDate, nullable(clean[2]), nullable(clean[3])};
    }

}
