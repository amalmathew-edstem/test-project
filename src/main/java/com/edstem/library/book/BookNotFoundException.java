package com.edstem.library.book;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Book " + id + " was not found");
    }
}
