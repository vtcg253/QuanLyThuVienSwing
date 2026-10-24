package model;

import java.util.List;

public record TableData(String[] columns, List<Object[]> rows) {
}
