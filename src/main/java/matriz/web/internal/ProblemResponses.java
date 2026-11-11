package matriz.web.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * The two refusals made before a request reaches a module, as Problem Details (RFC 9457) with the exact
 * messages of the acceptance criteria (DoD04).
 */
@Component
class ProblemResponses {

    static final String NOT_LOGGED_IN = "Please log in to continue.";
    static final String NOT_VERIFIED = "The request could not be verified. Please reload and try again.";

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    /** DoD02, US02 AC02 and AC06: no session, or a session that is no longer valid. */
    void notLoggedIn(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", NOT_LOGGED_IN);
    }

    /**
     * DoD06: a change request without the session's CSRF token, or with a wrong one.
     *
     * <p>Without a valid session the answer is {@code 401} instead: the token ends with its session, so an
     * expired or logged-out session would otherwise be told to reload instead of to log in again.
     */
    void accessDenied(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!trustResolver.isAuthenticated(authentication)) {
            notLoggedIn(request, response, null);
        } else if (exception instanceof CsrfException) {
            write(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", NOT_VERIFIED);
        } else {
            write(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", null);
        }
    }

    private static void write(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // The messages are fixed texts without quotes or backslashes, so they need no escaping.
        String body = "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + (detail == null ? "" : ",\"detail\":\"" + detail + "\"") + "}";
        response.getWriter().write(body);
    }
}
