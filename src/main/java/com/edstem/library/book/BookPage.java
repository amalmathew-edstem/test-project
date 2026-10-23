package com.edstem.library.book;

import java.util.List;
import org.springframework.data.domain.Page;

public record BookPage(List<BookResponse> content, int page, int size, long totalElements, int totalPages) {

    static BookPage from(Page<BookResponse> page) {
        return new BookPage(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
