package matriz.projects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** {@code 404} "Project not found.": the project doesn't exist, or the user isn't a member (DoD03, SEC03). */
public class ProjectNotFoundException extends ErrorResponseException {

    public static final String MESSAGE = "Project not found.";

    public ProjectNotFoundException() {
        super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, MESSAGE), null);
    }
}
