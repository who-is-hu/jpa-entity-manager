package persistence.entity;

import jakarta.persistence.Id;
import persistence.sql.ddl.exception.IdAnnotationMissingException;
import persistence.sql.dml.*;
import persistence.sql.mapping.ColumnData;
import persistence.sql.mapping.Columns;
import jdbc.JdbcTemplate;
import persistence.sql.mapping.TableData;

import java.lang.reflect.Field;
import java.util.Arrays;

import static persistence.sql.dml.BooleanExpression.eq;

public class EntityPersister {
    private final GeneratedIdObtainStrategy generatedIdObtainStrategy;
    private final JdbcTemplate jdbcTemplate;

    public EntityPersister(GeneratedIdObtainStrategy generatedIdObtainStrategy, JdbcTemplate jdbcTemplate) {
        this.generatedIdObtainStrategy = generatedIdObtainStrategy;
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean update(Object entity) {
        TableData table = TableData.from(entity.getClass());
        Columns columns = Columns.createColumnsWithValue(entity.getClass(), entity);
        ColumnData keyColumn = columns.getKeyColumn();

        if(keyColumn.getValue() == null) {
            return false;
        }

        UpdateQueryBuilder updateQueryBuilder = new UpdateQueryBuilder(table, columns);
        WhereBuilder whereBuilder = new WhereBuilder();
        whereBuilder.and(eq(keyColumn.getName(), keyColumn.getValue()));

        jdbcTemplate.execute(updateQueryBuilder.toQuery(entity, whereBuilder));

        return true;
    }

    public void insert(Object entity) {
        Class<?> clazz = entity.getClass();
        InsertQueryBuilder insertQueryBuilder = new InsertQueryBuilder(clazz);

        jdbcTemplate.execute(insertQueryBuilder.toQuery(entity));

        setIdToEntity(entity, clazz);
    }

    private void setIdToEntity(Object entity, Class<?> clazz) {
        Long id = generatedIdObtainStrategy.getGeneratedId(jdbcTemplate);

        Field idField = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow();
        idField.setAccessible(true);

        try {
            idField.set(entity, id);
        } catch (IllegalAccessException e) {
            throw new IdAnnotationMissingException();
        }
    }

    public void delete(Object entity) {
        Class<?> clazz = entity.getClass();
        ColumnData idColumn = Columns.createColumnsWithValue(clazz, entity).getKeyColumn();

        DeleteQueryBuilder deleteQueryBuilder = new DeleteQueryBuilder(clazz);
        WhereBuilder builder = new WhereBuilder();
        builder.and(eq(idColumn.getName(), idColumn.getValue()));

        jdbcTemplate.execute(deleteQueryBuilder.toQuery(builder));
    }
}
