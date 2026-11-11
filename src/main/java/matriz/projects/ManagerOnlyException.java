package matriz.projects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** {@code 403}: a member who isn't the manager tries a manager-only action (NFR03). */
public class ManagerOnlyException extends ErrorResponseException {

    public ManagerOnlyException(ManagerOnlyAction action) {
        super(HttpStatus.FORBIDDEN, ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, action.refusal()), null);
    }
}
