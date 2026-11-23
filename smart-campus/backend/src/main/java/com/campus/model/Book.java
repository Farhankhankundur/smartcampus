package com.campus.model;

public class Book extends BaseEntity {
    private String title;
    private String author;
    private String category;
    private int copies;      // total copies owned by the library
    private int available;   // computed: copies minus copies currently on loan

    public Book() { }
    public Book(int id, String title, String author, String category, int copies, int available) {
        this.id = id; this.title = title; this.author = author; this.category = category;
        this.copies = copies; this.available = available;
    }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getCopies() { return copies; }
    public int getAvailable() { return available; }

    @Override public void validate() {
        if (title == null || title.isBlank() || author == null || author.isBlank())
            throw new IllegalArgumentException("Book title and author are required.");
        if (copies < 1) throw new IllegalArgumentException("A book needs at least 1 copy.");
    }
}
