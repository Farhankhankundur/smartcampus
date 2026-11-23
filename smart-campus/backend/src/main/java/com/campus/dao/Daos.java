package com.campus.dao;

import com.campus.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** One small DAO per entity (you may move each into its own file). */
public class Daos {

    @Repository
    public static class StudentDao extends BaseDao<Student> {
        public StudentDao(JdbcTemplate j) { super(j); }
        protected String table() { return "students"; }
        protected RowMapper<Student> mapper() {
            return (rs, i) -> new Student(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                    rs.getString("department"), rs.getInt("semester"));
        }
        protected String insertSql() { return "INSERT INTO students(name,email,department,semester) VALUES (?,?,?,?)"; }
        protected String updateSql() { return "UPDATE students SET name=?,email=?,department=?,semester=? WHERE id=?"; }
        protected Object[] values(Student s) { return new Object[]{s.getName(), s.getEmail(), s.getDepartment(), s.getSemester()}; }
    }

    @Repository
    public static class FacultyDao extends BaseDao<Faculty> {
        public FacultyDao(JdbcTemplate j) { super(j); }
        protected String table() { return "faculty"; }
        protected RowMapper<Faculty> mapper() {
            return (rs, i) -> new Faculty(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("department"));
        }
        protected String insertSql() { return "INSERT INTO faculty(name,email,department) VALUES (?,?,?)"; }
        protected String updateSql() { return "UPDATE faculty SET name=?,email=?,department=? WHERE id=?"; }
        protected Object[] values(Faculty f) { return new Object[]{f.getName(), f.getEmail(), f.getDepartment()}; }
    }

    @Repository
    public static class CourseDao extends BaseDao<Course> {
        public CourseDao(JdbcTemplate j) { super(j); }
        protected String table() { return "courses"; }
        protected RowMapper<Course> mapper() {
            return (rs, i) -> new Course(rs.getInt("id"), rs.getString("code"), rs.getString("title"),
                    rs.getInt("credits"), rs.getInt("faculty_id"));
        }
        protected String insertSql() { return "INSERT INTO courses(code,title,credits,faculty_id) VALUES (?,?,?,?)"; }
        protected String updateSql() { return "UPDATE courses SET code=?,title=?,credits=?,faculty_id=? WHERE id=?"; }
        protected Object[] values(Course c) { return new Object[]{c.getCode(), c.getTitle(), c.getCredits(), c.getFacultyId()}; }
    }

    @Repository
    public static class ResourceDao extends BaseDao<Resource> {
        public ResourceDao(JdbcTemplate j) { super(j); }
        protected String table() { return "resources"; }
        protected RowMapper<Resource> mapper() {
            return (rs, i) -> Resource.of(rs.getInt("id"), rs.getString("name"), rs.getString("res_type"), rs.getInt("capacity"));
        }
        protected String insertSql() { return "INSERT INTO resources(name,res_type,capacity) VALUES (?,?,?)"; }
        protected String updateSql() { return "UPDATE resources SET name=?,res_type=?,capacity=? WHERE id=?"; }
        protected Object[] values(Resource r) { return new Object[]{r.getName(), r.getType(), r.getCapacity()}; }
    }

    @Repository
    public static class EventDao extends BaseDao<CampusEvent> {
        public EventDao(JdbcTemplate j) { super(j); }
        protected String table() { return "events"; }
        protected RowMapper<CampusEvent> mapper() {
            return (rs, i) -> new CampusEvent(rs.getInt("id"), rs.getString("title"), rs.getString("event_date"), rs.getString("venue"));
        }
        protected String insertSql() { return "INSERT INTO events(title,event_date,venue) VALUES (?,?,?)"; }
        protected String updateSql() { return "UPDATE events SET title=?,event_date=?,venue=? WHERE id=?"; }
        protected Object[] values(CampusEvent e) { return new Object[]{e.getTitle(), e.getEventDate(), e.getVenue()}; }
    }

    @Repository
    public static class BookDao extends BaseDao<Book> {
        public BookDao(JdbcTemplate j) { super(j); }
        protected String table() { return "books"; }
        @Override protected String selectSql() {
            return "SELECT b.*, (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id AND l.returned = FALSE) AS on_loan FROM books b";
        }
        protected RowMapper<Book> mapper() {
            return (rs, i) -> new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author"), rs.getString("category"),
                    rs.getInt("copies"), rs.getInt("copies") - rs.getInt("on_loan"));
        }
        protected String insertSql() { return "INSERT INTO books(title,author,category,copies) VALUES (?,?,?,?)"; }
        protected String updateSql() { return "UPDATE books SET title=?,author=?,category=?,copies=? WHERE id=?"; }
        protected Object[] values(Book b) { return new Object[]{b.getTitle(), b.getAuthor(), b.getCategory(), b.getCopies()}; }
    }
}
