package edu.seu.vcampus.common.dto.library;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/** 图书详情和库存快照。 */
public final class BookDetail implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Integer publicationYear;
    private final byte[] coverImage;
    private final long id;
    private final String isbn;
    private final String title;
    private final String author;
    private final String publisher;
    private final String category;
    private final int totalCopies;
    private final int availableCopies;
    private final String location;
    private final String description;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BookDetail(long id, String isbn, String title, String author,
                      String publisher, String category, int totalCopies,
                      int availableCopies, String location, String description,
                      String status, LocalDateTime createdAt,
                      LocalDateTime updatedAt) {
        this(id, isbn, title, author, publisher, category, totalCopies,
                availableCopies, location, description, status, createdAt,
                updatedAt, null, null);
    }

    public BookDetail(long id, String isbn, String title, String author,
                      String publisher, String category, int totalCopies,
                      int availableCopies, String location, String description,
                      String status, LocalDateTime createdAt,
                      LocalDateTime updatedAt, Integer publicationYear,
                      byte[] coverImage) {
        this.publicationYear = publicationYear;
        this.coverImage = coverImage == null ? null : coverImage.clone();
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getPublicationYear() { return publicationYear; }
    public byte[] getCoverImage() { return coverImage == null ? null : coverImage.clone(); }
    public BookDetail withoutCover() {
        return new BookDetail(id, isbn, title, author, publisher, category,
                totalCopies, availableCopies, location, description, status,
                createdAt, updatedAt, publicationYear, null);
    }

    public long getId() { return id; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getCategory() { return category; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
