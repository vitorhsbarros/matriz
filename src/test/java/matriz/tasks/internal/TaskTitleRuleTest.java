package matriz.tasks.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.ErrorResponseException;

/**
 * A task's title is required and has between 3 and 150 characters, counted after leading and trailing spaces are
 * removed.
 */
class TaskTitleRuleTest {

    @ParameterizedTest(name = "a title of {0} characters is accepted")
    @ValueSource(ints = {3, 4, 75, 149, 150})
    void aTitleWithinTheLimitsIsAccepted(int length) {
        String title = "a".repeat(length);

        assertThat(TaskFields.title(title)).isEqualTo(title);
    }

    @Test
    void aTitleIsKeptWithoutItsLeadingAndTrailingSpaces() {
        assertThat(TaskFields.title("  Review copy  ")).isEqualTo("Review copy");
    }

    @Test
    void spacesInsideTheTitleAreKept() {
        assertThat(TaskFields.title("Design  home page")).isEqualTo("Design  home page");
    }

    @ParameterizedTest(name = "the title \"{0}\" is refused as missing")
    @ValueSource(strings = {"", " ", "   "})
    void anEmptyTitleOrOneWithOnlySpacesIsRequired(String title) {
        assertRefusedWith(title, "Task title is required.");
    }

    @ParameterizedTest(name = "a title of {0} characters is too short")
    @ValueSource(ints = {1, 2})
    void aTitleShorterThanThreeCharactersIsRefused(int length) {
        assertRefusedWith("a".repeat(length), "Task title must be at least 3 characters.");
    }

    @ParameterizedTest(name = "a title of {0} characters is too long")
    @ValueSource(ints = {151, 300})
    void aTitleLongerThan150CharactersIsRefused(int length) {
        assertRefusedWith("a".repeat(length), "Task title must be at most 150 characters.");
    }

    @Test
    void theMinimumLengthIsCountedAfterRemovingTheSpacesAround() {
        assertRefusedWith("  ab  ", "Task title must be at least 3 characters.");
    }

    @Test
    void theMaximumLengthIsCountedAfterRemovingTheSpacesAround() {
        String title = "a".repeat(150);

        assertThat(TaskFields.title("  " + title + "  ")).isEqualTo(title);
    }

    private static void assertRefusedWith(String title, String message) {
        ErrorResponseException refusal = catchThrowableOfType(ErrorResponseException.class,
                () -> TaskFields.title(title));

        assertThat(refusal).as("the title \"%s\" should be refused", title).isNotNull();
        assertThat(refusal.getBody().getDetail()).isEqualTo(message);
    }
}
