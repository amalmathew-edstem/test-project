package com.edstem.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void borrowLendsAnAvailableBookToTheMember() {
        Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookResponse response = bookService.borrow(1L, new BorrowRequest("M-1"));

        assertThat(response.available()).isFalse();
        assertThat(response.borrowedBy()).isEqualTo("M-1");
        assertThat(response.borrowedAt()).isEqualTo(NOW);
    }

    @Test
    void borrowRejectsABookThatIsAlreadyBorrowed() {
        Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
        book.lendTo("M-1", NOW);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.borrow(1L, new BorrowRequest("M-2")))
                .isInstanceOf(BookConflictException.class)
                .hasMessageContaining("already borrowed");
        assertThat(book.getBorrowedBy()).isEqualTo("M-1");
    }

    @Test
    void giveBackMakesTheBookAvailableAgain() {
        Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
        book.lendTo("M-1", NOW);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookResponse response = bookService.giveBack(1L);

        assertThat(response.available()).isTrue();
        assertThat(response.borrowedBy()).isNull();
    }

    @Test
    void giveBackRejectsABookThatIsNotBorrowed() {
        Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.giveBack(1L)).isInstanceOf(BookConflictException.class);
    }

    @Test
    void deleteRejectsABorrowedBook() {
        Book book = new Book("Dune", "Frank Herbert", "9780441013593", 1965);
        book.lendTo("M-1", NOW);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOf(BookConflictException.class)
                .hasMessageContaining("cannot be deleted");
        verify(bookRepository, never()).delete(book);
    }

    @Test
    void createRejectsADuplicateIsbn() {
        when(bookRepository.existsByIsbn("9780441013593")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(new BookRequest("Dune", "Frank Herbert", "9780441013593", 1965)))
                .isInstanceOf(BookConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getReportsAMissingBook() {
        when(bookRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.get(9L)).isInstanceOf(BookNotFoundException.class);
    }
}
