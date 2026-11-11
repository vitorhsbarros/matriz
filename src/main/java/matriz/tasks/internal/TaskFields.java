package matriz.tasks.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * A task's fields as US05, US07 and US08 define them (limits decided on 2026-11-11). Leading and trailing
 * spaces are removed first, and the limits apply to what is left (D01, D02).
 *
 * <p>Status, priority and due date arrive as text and are checked here, so a wrong value gets the story's
 * message with {@code 422} instead of a generic {@code 400} (api-contract.md, AS07).
 */
final class TaskFields {

    static final String TITLE_REQUIRED = "Task title is required.";
    static final String TITLE_TOO_SHORT = "Task title must be at least 3 characters.";
    static final String TITLE_TOO_LONG = "Task title must be at most 150 characters.";
    static final String DESCRIPTION_TOO_LONG = "Description must be at most 2000 characters.";
    static final String INVALID_DATE = "Enter a valid date.";
    static final String DATE_IN_PAST = "Due date cannot be in the past.";
    static final String INVALID_STATUS = "Invalid status.";
    static final String INVALID_PRIORITY = "Invalid priority.";

    static final Set<String> STATUSES = Set.of("TODO", "IN_PROGRESS", "DONE");
    static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private TaskFields() {
    }

    /** Required, 3–150 characters (US05 AC03, AC08; US08 AC03). */
    static String title(String value) {
        String title = value == null ? "" : value.strip();
        int length = title.codePointCount(0, title.length());
        if (length == 0) {
            throw invalid(TITLE_REQUIRED);
        }
        if (length < 3) {
            throw invalid(TITLE_TOO_SHORT);
        }
        if (length > 150) {
            throw invalid(TITLE_TOO_LONG);
        }
        return title;
    }

    /** Optional, up to 2000 characters; empty means none (US05 AC08, US08 AC02). */
    static String description(String value) {
        String description = value == null ? "" : value.strip();
        if (description.codePointCount(0, description.length()) > 2000) {
            throw invalid(DESCRIPTION_TOO_LONG);
        }
        return description.isEmpty() ? null : description;
    }

    /**
     * Optional; a real calendar date in ISO format, not earlier than today (US05 AC04, AC05, AC07; D12).
     *
     * @param unchanged the task's current due date: keeping it is always allowed, even once it has passed
     *        (US08 AC07). {@code null} when creating.
     */
    static LocalDate dueDate(String value, LocalDate today, LocalDate unchanged) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDate date;
        try {
            // ISO_LOCAL_DATE resolves strictly, so 2027-02-30 is refused instead of becoming 2027-02-28.
            date = LocalDate.parse(value.strip(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException notADate) {
            throw invalid(INVALID_DATE);
        }
        if (date.isBefore(today) && !date.equals(unchanged)) {
            throw invalid(DATE_IN_PAST);
        }
        return date;
    }

    /** One of {@code TODO}, {@code IN_PROGRESS}, {@code DONE} (US07 AC04). */
    static String status(String value) {
        if (value == null || !STATUSES.contains(value)) {
            throw invalid(INVALID_STATUS);
        }
        return value;
    }

    /** One of {@code LOW}, {@code MEDIUM}, {@code HIGH} (US05 AC10, US08 AC10). */
    static String priority(String value) {
        if (value == null || !PRIORITIES.contains(value)) {
            throw invalid(INVALID_PRIORITY);
        }
        return value;
    }

    static ErrorResponseException invalid(String detail) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
