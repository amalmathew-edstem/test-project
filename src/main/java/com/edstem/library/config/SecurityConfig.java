package com.edstem.library.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

    private static final String LIBRARIAN = "LIBRARIAN";
    private static final String MEMBER = "MEMBER";

    private final HandlerExceptionResolver handlerExceptionResolver;

    public SecurityConfig(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint()))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(
                        requests -> requests.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info")
                                .permitAll()
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
                                .permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/*")
                                .hasAnyRole(LIBRARIAN, MEMBER)
                                .requestMatchers(HttpMethod.POST, "/api/books/*/borrow", "/api/books/*/return")
                                .hasAnyRole(LIBRARIAN, MEMBER)
                                .requestMatchers(HttpMethod.POST, "/api/books")
                                .hasRole(LIBRARIAN)
                                .requestMatchers(HttpMethod.PUT, "/api/books/*")
                                .hasRole(LIBRARIAN)
                                .requestMatchers(HttpMethod.DELETE, "/api/books/*")
                                .hasRole(LIBRARIAN)
                                .anyRequest()
                                .denyAll())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(LibraryUsersProperties users, PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                user(users.librarian(), LIBRARIAN, passwordEncoder), user(users.member(), MEMBER, passwordEncoder));
    }

    private static UserDetails user(
            LibraryUsersProperties.Credentials credentials, String role, PasswordEncoder passwordEncoder) {
        return User.withUsername(credentials.username())
                .password(passwordEncoder.encode(credentials.password()))
                .roles(role)
                .build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"library\"");
            handlerExceptionResolver.resolveException(request, response, null, exception);
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) ->
                handlerExceptionResolver.resolveException(request, response, null, exception);
    }
}
