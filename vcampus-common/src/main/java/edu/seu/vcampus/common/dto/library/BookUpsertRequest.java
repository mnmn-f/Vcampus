package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;

/** 图书管理员新增或维护图书的请求。id 为 0 表示新增。 */
public final class BookUpsertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String isbn;
    private final String title;
    private final String author;
    private final String publisher;
    private final String category;
    private final Integer totalCopies;
    private final Integer availableCopies;
    private final String location;
    private final String description;
    private final String status;

    public BookUpsertRequest(long id, String isbn, String title, String author,
                             String publisher, String category, Integer totalCopies,
                             Integer availableCopies, String location,
                             String description, String status) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.category = category;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.location = location;
        this.description = description;
        this.status = status;
    }

    public BookUpsertRequest(String isbn, String title, String author,
                             String publisher, String category, int totalCopies,
                             int availableCopies, String location,
                             String description) {
        this(0L, isbn, title, author, publisher, category, Integer.valueOf(totalCopies),
                Integer.valueOf(availableCopies), location, description, "ON_SHELF");
    }

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getCategory() { return category; }
    public Integer getTotalCopies() { return totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
}
