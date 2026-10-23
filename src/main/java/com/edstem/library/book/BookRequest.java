package com.edstem.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @NotBlank @Size(max = 20) String isbn,
        @NotInFutureYear Integer publishedYear) {}
