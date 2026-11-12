package matriz.projects.internal;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/**
 * The project's fields as US03 and US14 define them. Leading and trailing spaces are removed first, and the
 * limits apply to what is left (D01, D02). Fields are checked in the order of the schema, and only the first
 * failure is reported (api-contract.md, AS08).
 */
final class ProjectFields {

    static final String NAME_REQUIRED = "Project name is required.";
    static final String NAME_TOO_LONG = "Project name must be at most 100 characters.";
    static final String DESCRIPTION_TOO_LONG = "Description must be at most 2000 characters.";

    private ProjectFields() {
    }

    /** The trimmed name: required, 1–100 characters (US03 AC03, AC04; US14 AC03, AC06). */
    static String name(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) {
            throw invalid(NAME_REQUIRED);
        }
        if (name.codePointCount(0, name.length()) > 100) {
            throw invalid(NAME_TOO_LONG);
        }
        return name;
    }

    /** The trimmed description, or {@code null} when it is missing or empty (US03 AC02, AC05; US14 AC02). */
    static String description(String value) {
        String description = value == null ? "" : value.strip();
        if (description.codePointCount(0, description.length()) > 2000) {
            throw invalid(DESCRIPTION_TOO_LONG);
        }
        return description.isEmpty() ? null : description;
    }

    private static ErrorResponseException invalid(String detail) {
        return Problems.problem(HttpStatus.UNPROCESSABLE_CONTENT, detail);
    }
}
