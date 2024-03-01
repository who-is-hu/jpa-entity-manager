package persistence.sql.dml;

import persistence.sql.mapping.Columns;
import persistence.sql.mapping.TableData;

import java.util.stream.Collectors;

public class UpdateQueryBuilder {
    private final TableData table;

    public UpdateQueryBuilder(Class<?> clazz) {
        this.table = TableData.from(clazz);
    }

    public String toQuery(Object entity, WhereBuilder whereBuilder) {
        Columns columns = Columns.createColumnsWithValue(entity.getClass(), entity);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("update ");
        stringBuilder.append(table.getName());
        stringBuilder.append(" set ");
        stringBuilder.append(valueClause(columns));

        if (whereBuilder.isEmpty()) {
            return stringBuilder.toString();
        }

        stringBuilder.append(" where ");
        stringBuilder.append(whereBuilder.toClause());

        return stringBuilder.toString();
    }

    private String valueClause(Columns columns) {
        return columns.getValuesMap()
                .entrySet()
                .stream()
                .map(entry -> String.format("%s = %s", entry.getKey(), ValueUtil.getValueString(entry.getValue())))
                .collect(Collectors.joining(", "));
    }
}