package com.campus.service;

import com.campus.dao.Daos.BookDao;
import com.campus.exception.Errors;
import com.campus.model.Book;
import com.campus.model.Loan;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class LibraryService {
    private final JdbcTemplate jdbc;
    private final BookDao books;

    public LibraryService(JdbcTemplate jdbc, BookDao books) {
        this.jdbc = jdbc;
        this.books = books;
    }

    private static final RowMapper<Loan> LOAN = (rs, i) -> new Loan(rs.getInt("id"), rs.getInt("book_id"), rs.getString("title"),
            rs.getString("member"), rs.getString("issue_date"), rs.getString("due_date"), rs.getBoolean("returned"));

    /** synchronized: two people can never take the last copy at the same moment. */
    public synchronized Loan issue(int bookId, String member, int days) {
        Book b = books.findById(bookId);
        if (b.getAvailable() <= 0)
            throw new Errors.Unavailable("No copies of \"" + b.getTitle() + "\" are available right now.");
        Integer dup = jdbc.queryForObject(
                "SELECT COUNT(*) FROM loans WHERE book_id=? AND member=? AND returned=FALSE", Integer.class, bookId, member);
        if (dup != null && dup > 0)
            throw new Errors.Unavailable(member + " already has this book on loan.");
        String issued = LocalDate.now().toString();
        String due = LocalDate.now().plusDays(days).toString();
        jdbc.update("INSERT INTO loans(book_id,member,issue_date,due_date,returned) VALUES (?,?,?,?,FALSE)",
                bookId, member, issued, due);
        return new Loan(0, bookId, b.getTitle(), member, issued, due, false);
    }

    /** onlyMember = null means "any member" (administrator). */
    public synchronized void giveBack(int loanId, String onlyMember) {
        String sql = "UPDATE loans SET returned=TRUE WHERE id=? AND returned=FALSE" + (onlyMember == null ? "" : " AND member=?");
        int changed = onlyMember == null ? jdbc.update(sql, loanId) : jdbc.update(sql, loanId, onlyMember);
        if (changed == 0) throw new Errors.NotFound("Loan not found, or it was already returned.");
    }

    public List<Loan> all(String onlyMember) {
        String sql = "SELECT l.*, b.title FROM loans l JOIN books b ON b.id = l.book_id"
                + (onlyMember == null ? "" : " WHERE l.member = ?") + " ORDER BY l.returned, l.due_date";
        return onlyMember == null ? jdbc.query(sql, LOAN) : jdbc.query(sql, LOAN, onlyMember);
    }

    public int activeCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM loans WHERE returned=FALSE", Integer.class);
        return n == null ? 0 : n;
    }
}
