package com.edstem.library.book;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final Clock clock;

    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw BookConflictException.duplicateIsbn(request.isbn());
        }
        Book book = new Book(request.title(), request.author(), request.isbn(), request.publishedYear());
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional(readOnly = true)
    public BookPage search(String title, String author, Pageable pageable) {
        return BookPage.from(bookRepository
                .findByTitleContainingIgnoreCaseAndAuthorContainingIgnoreCase(
                        title == null ? "" : title, author == null ? "" : author, pageable)
                .map(BookResponse::from));
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return BookResponse.from(findBook(id));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = findBook(id);
        if (bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
            throw BookConflictException.duplicateIsbn(request.isbn());
        }
        book.updateDetails(request.title(), request.author(), request.isbn(), request.publishedYear());
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findBook(id);
        if (book.isBorrowed()) {
            throw BookConflictException.deleteWhileBorrowed(id);
        }
        bookRepository.delete(book);
    }

    @Transactional
    public BookResponse borrow(Long id, BorrowRequest request) {
        Book book = findBook(id);
        if (book.isBorrowed()) {
            throw BookConflictException.alreadyBorrowed(id);
        }
        book.lendTo(request.memberId(), Instant.now(clock));
        return BookResponse.from(book);
    }

    @Transactional
    public BookResponse giveBack(Long id) {
        Book book = findBook(id);
        if (!book.isBorrowed()) {
            throw BookConflictException.notBorrowed(id);
        }
        book.markReturned();
        return BookResponse.from(book);
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }
}
