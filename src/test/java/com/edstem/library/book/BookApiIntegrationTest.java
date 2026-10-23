package com.edstem.library.book;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class BookApiIntegrationTest {

    private static final String LIBRARIAN_PASSWORD = UUID.randomUUID().toString();
    private static final String MEMBER_PASSWORD = UUID.randomUUID().toString();

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("library.users.librarian.username", () -> "librarian");
        registry.add("library.users.librarian.password", () -> LIBRARIAN_PASSWORD);
        registry.add("library.users.member.username", () -> "member");
        registry.add("library.users.member.password", () -> MEMBER_PASSWORD);
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BookRepository bookRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void borrowingAnUnavailableBookIsRejectedWithAConflictProblem() throws Exception {
        long id = createBook("9780441013593");

        mockMvc.perform(post("/api/books/{id}/borrow", id)
                        .with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.borrowedBy").value("M-1"));

        mockMvc.perform(post("/api/books/{id}/borrow", id)
                        .with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"M-2\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("Book " + id + " is already borrowed and is not available until it is returned"));
    }

    @Test
    void aReturnedBookCanBeBorrowedAgain() throws Exception {
        long id = createBook("9780441013593");
        borrow(id, "M-1");

        mockMvc.perform(post("/api/books/{id}/return", id).with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        borrow(id, "M-2");
    }

    @Test
    void aBorrowedBookCannotBeDeleted() throws Exception {
        long id = createBook("9780441013593");
        borrow(id, "M-1");

        mockMvc.perform(delete("/api/books/{id}", id).with(librarian()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/books/{id}/return", id).with(member())).andExpect(status().isOk());
        mockMvc.perform(delete("/api/books/{id}", id).with(librarian())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/books/{id}", id).with(member()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void anInvalidBookIsRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(librarian())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"isbn\":\"123\",\"publishedYear\":9999}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.author").exists())
                .andExpect(jsonPath("$.errors.publishedYear").value("must not be in the future"));
    }

    @Test
    void aDuplicateIsbnIsRejected() throws Exception {
        createBook("9780441013593");

        mockMvc.perform(post("/api/books")
                        .with(librarian())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Another", "Someone", "9780441013593")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A book with ISBN 9780441013593 already exists"));
    }

    @Test
    void booksCanBeSearchedByTitleOrAuthor() throws Exception {
        createBook("Dune", "Frank Herbert", "9780441013593");
        createBook("Emma", "Jane Austen", "9780141439587");

        mockMvc.perform(get("/api/books").param("title", "dun").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Dune"));

        mockMvc.perform(get("/api/books").param("author", "austen").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].author").value("Jane Austen"));
    }

    @Test
    void securityErrorsUseTheSameProblemFormat() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/books")
                        .with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Dune", "Frank Herbert", "9780441013593")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    private long createBook(String isbn) throws Exception {
        return createBook("Dune", "Frank Herbert", isbn);
    }

    private long createBook(String title, String author, String isbn) throws Exception {
        mockMvc.perform(post("/api/books")
                        .with(librarian())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(title, author, isbn)))
                .andExpect(status().isCreated());
        return bookRepository.findAll().stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void borrow(long id, String memberId) throws Exception {
        mockMvc.perform(post("/api/books/{id}/borrow", id)
                        .with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + memberId + "\"}"))
                .andExpect(status().isOk());
    }

    private static String bookJson(String title, String author, String isbn) {
        return "{\"title\":\"" + title + "\",\"author\":\"" + author + "\",\"isbn\":\"" + isbn
                + "\",\"publishedYear\":1965}";
    }

    private static RequestPostProcessor librarian() {
        return httpBasic("librarian", LIBRARIAN_PASSWORD);
    }

    private static RequestPostProcessor member() {
        return httpBasic("member", MEMBER_PASSWORD);
    }
}
