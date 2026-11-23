package com.campus.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One borrowed book. The fine is calculated, not stored. */
public class Loan extends BaseEntity {
    public static final int FINE_PER_DAY = 2;   // rupees

    private int bookId;
    private String bookTitle;
    private String member;
    private String issueDate;
    private String dueDate;
    private boolean returned;

    public Loan() { }
    public Loan(int id, int bookId, String bookTitle, String member, String issueDate, String dueDate, boolean returned) {
        this.id = id; this.bookId = bookId; this.bookTitle = bookTitle; this.member = member;
        this.issueDate = issueDate; this.dueDate = dueDate; this.returned = returned;
    }
    public int getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getMember() { return member; }
    public String getIssueDate() { return issueDate; }
    public String getDueDate() { return dueDate; }
    public boolean isReturned() { return returned; }

    /** Polymorphic-style business rule: overdue days x fine per day (0 if returned or on time). */
    public int getFine() {
        if (returned) return 0;
        long late = ChronoUnit.DAYS.between(LocalDate.parse(dueDate), LocalDate.now());
        return late > 0 ? (int) late * FINE_PER_DAY : 0;
    }
}
