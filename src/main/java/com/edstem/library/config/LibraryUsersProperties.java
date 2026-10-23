package com.edstem.library.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.users")
public record LibraryUsersProperties(
        @Valid @NotNull Credentials librarian,
        @Valid @NotNull Credentials member) {

    public record Credentials(
            @NotBlank String username, @NotBlank String password) {}
}
