package matriz.web.internal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errors of every module as Problem Details (RFC 9457). Modules refuse a request by throwing
 * Spring's {@code ErrorResponseException} with the exact message in {@code detail} (DoD04), so they don't
 * depend on the web entry.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    static final String MALFORMED_BODY = "The request body is not valid.";

    /** DoD07: the body isn't valid JSON, has unknown fields, or has a field of the wrong type. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, MALFORMED_BODY);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * DoD07 too: a body sent as something other than JSON (e.g. {@code text/plain}) can't be read, so it gets the
     * same {@code 400} and message, instead of a {@code 415} with a technical message the contract doesn't define.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, MALFORMED_BODY);
        return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}
