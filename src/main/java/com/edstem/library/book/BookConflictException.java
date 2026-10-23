package com.edstem.library.book;

public class BookConflictException extends RuntimeException {

    private BookConflictException(String message) {
        super(message);
    }

    static BookConflictException duplicateIsbn(String isbn) {
        return new BookConflictException("A book with ISBN " + isbn + " already exists");
    }

    static BookConflictException alreadyBorrowed(Long id) {
        return new BookConflictException(
                "Book " + id + " is already borrowed and is not available until it is returned");
    }

    static BookConflictException notBorrowed(Long id) {
        return new BookConflictException("Book " + id + " is not currently borrowed");
    }

    static BookConflictException deleteWhileBorrowed(Long id) {
        return new BookConflictException("Book " + id + " is currently borrowed and cannot be deleted");
    }
}
