package matriz.accounts.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Log-in, the current session and log-out: {@code logIn}, {@code getSession} and {@code logOut} in
 * openapi.yaml (US02, US19; ADR-0002). The session lifecycle is described in api-contract.md, section 4.
 */
@RestController
@RequestMapping("/api/session")
class SessionController {

    static final String REQUIRED = "Email and password are required.";
    static final String INVALID = "Invalid email or password.";

    record LogInRequest(String email, String password) {
    }

    record UserView(UUID id, String name, String email) {
    }

    record SessionView(UserView user, String csrfToken) {
    }

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContexts;
    private final CsrfTokenRepository csrfTokens;
    private final JdbcClient jdbc;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    SessionController(AuthenticationManager authenticationManager, SecurityContextRepository securityContexts,
            CsrfTokenRepository csrfTokens, JdbcClient jdbc) {
        this.authenticationManager = authenticationManager;
        this.securityContexts = securityContexts;
        this.csrfTokens = csrfTokens;
        this.jdbc = jdbc;
    }

    /** US02 AC01, AC03–AC05, AC07–AC09; US19 AC01. */
    @PostMapping
    SessionView logIn(@RequestBody LogInRequest body, HttpServletRequest request, HttpServletResponse response) {
        // A field with only spaces counts as empty (D01). The password itself is never trimmed (US02 AC08).
        if (isBlank(body.email()) || isBlank(body.password())) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, REQUIRED);
        }
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.email(), body.password()));
        } catch (BadCredentialsException wrongEmailOrPassword) {
            throw problem(HttpStatus.UNAUTHORIZED, INVALID);
        }

        // A new session identifier at every log-in, so an identifier known before log-in is useless after it
        // (session fixation).
        request.getSession();
        request.changeSessionId();

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);

        // A new CSRF token for the new session (ADR-0002).
        csrfTokens.saveToken(null, request, response);
        CsrfToken token = csrfTokens.generateToken(request);
        csrfTokens.saveToken(token, request, response);

        return new SessionView(user(authentication), token.getToken());
    }

    /** US02 AC10: who is logged in, also after a page reload. */
    @GetMapping
    SessionView current(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokens.loadToken(request);
        if (token == null) {
            token = csrfTokens.generateToken(request);
            csrfTokens.saveToken(token, request, response);
        }
        return new SessionView(user(authentication), token.getToken());
    }

    /** US02 AC02: the session is deleted on the server, so the cookie stops working at once. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logOut(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    private UserView user(Authentication authentication) {
        UUID id = UUID.fromString(authentication.getName());
        return jdbc.sql("select id, name, email from users where id = ?")
                .param(id)
                .query((row, number) -> new UserView(
                        row.getObject("id", UUID.class), row.getString("name"), row.getString("email")))
                .single();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ErrorResponseException problem(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
