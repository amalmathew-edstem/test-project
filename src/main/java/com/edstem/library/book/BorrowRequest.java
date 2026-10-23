package com.edstem.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BorrowRequest(@NotBlank @Size(max = 64) String memberId) {}
