package com.campus.dao;

import com.campus.exception.Errors;
import com.campus.model.BaseEntity;
import java.util.Arrays;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** Generic CRUD over JDBC. Each subclass only supplies table name, SQL and row mapping. */
public abstract class BaseDao<T extends BaseEntity> {
    protected final JdbcTemplate jdbc;
    protected BaseDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    protected abstract String table();
    protected abstract RowMapper<T> mapper();
    protected abstract String insertSql();
    protected abstract String updateSql();      // must end with WHERE id=?
    protected abstract Object[] values(T t);    // same order as the ? placeholders (without id)

    /** Override when a table needs a custom SELECT (for example a computed column). */
    protected String selectSql() { return "SELECT * FROM " + table(); }

    public List<T> findAll() {
        return jdbc.query(selectSql() + " ORDER BY id", mapper());
    }

    public T findById(int id) {
        List<T> rows = jdbc.query(selectSql() + " WHERE id=?", mapper(), id);
        if (rows.isEmpty()) throw new Errors.NotFound(table() + " record " + id + " not found.");
        return rows.get(0);
    }

    public T create(T t) {
        KeyHolder keys = new GeneratedKeyHolder();
        Object[] v = values(t);
        jdbc.update(con -> {
            var ps = con.prepareStatement(insertSql(), new String[]{"id"});
            for (int i = 0; i < v.length; i++) ps.setObject(i + 1, v[i]);
            return ps;
        }, keys);
        int newId = ((Number) keys.getKeyList().get(0).values().iterator().next()).intValue();
        return findById(newId);
    }

    public T update(int id, T t) {
        findById(id);   // 404 if missing
        Object[] v = values(t);
        Object[] args = Arrays.copyOf(v, v.length + 1);
        args[v.length] = id;
        jdbc.update(updateSql(), args);
        return findById(id);
    }

    public void delete(int id) {
        findById(id);
        jdbc.update("DELETE FROM " + table() + " WHERE id=?", id);
    }
}
