package matriz.tasks.internal;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The refusals of this module's operations, as Problem Details with the exact message of the stories (DoD04). Each
 * module has its own copy, because modules don't share internal code (ADR-0001) and the web entry maps Spring's
 * {@code ErrorResponseException} for all of them.
 */
final class Problems {

    private Problems() {
    }

    static ErrorResponseException problem(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }

    /**
     * The identifier written in the URL; text that isn't an identifier can't name anything, so it gets the same
     * "not found" refusal as an identifier that doesn't exist (api-contract.md, AS19, AS22).
     */
    static UUID idOr(String value, Supplier<? extends RuntimeException> notFound) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAnIdentifier) {
            throw notFound.get();
        }
    }

    /**
     * Whether the database refused a row because a row it refers to is gone: a foreign-key violation (PostgreSQL's
     * SQL state {@code 23503}). Only then does a deleted project, task or team explain the error; any other refusal
     * is a different problem and must not be reported as "not found" (docs/quality-security-review.md, F2).
     */
    static boolean isMissingReference(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && FOREIGN_KEY_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static final String FOREIGN_KEY_VIOLATION = "23503";
}
