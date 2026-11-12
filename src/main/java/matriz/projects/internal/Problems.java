package matriz.projects.internal;

import java.util.UUID;
import java.util.function.Supplier;
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
}
