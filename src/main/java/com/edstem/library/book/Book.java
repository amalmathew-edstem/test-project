package com.edstem.library.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "borrowed_by")
    private String borrowedBy;

    @Column(name = "borrowed_at")
    private Instant borrowedAt;

    @Version
    private long version;

    public Book(String title, String author, String isbn, Integer publishedYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
    }

    public void updateDetails(String title, String author, String isbn, Integer publishedYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
    }

    public boolean isBorrowed() {
        return borrowedBy != null;
    }

    public void lendTo(String memberId, Instant at) {
        this.borrowedBy = memberId;
        this.borrowedAt = at;
    }

    public void markReturned() {
        this.borrowedBy = null;
        this.borrowedAt = null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Book book)) {
            return false;
        }
        return id != null && Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Book.class.hashCode();
    }
}
