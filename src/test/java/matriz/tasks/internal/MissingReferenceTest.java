package matriz.tasks.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Only a refusal because a referenced row is gone (PostgreSQL's foreign-key violation, SQL state 23503) may be
 * reported as "not found"; any other database refusal is a different problem.
 */
class MissingReferenceTest {

    @Test
    void aForeignKeyViolationMeansTheReferencedRowIsGone() {
        assertThat(Problems.isMissingReference(refusedWith("23503"))).isTrue();
    }

    @Test
    void aForeignKeyViolationFurtherDownTheCausesIsFound() {
        DataIntegrityViolationException wrapped = new DataIntegrityViolationException("insert failed",
                new RuntimeException("driver", new SQLException("violates foreign key constraint", "23503")));

        assertThat(Problems.isMissingReference(wrapped)).isTrue();
    }

    @Test
    void anyOtherRefusalIsNotAMissingReference() {
        for (String state : new String[] {"22021", "23505", "23514", "22001"}) {
            assertThat(Problems.isMissingReference(refusedWith(state))).as("SQL state %s", state).isFalse();
        }
        assertThat(Problems.isMissingReference(new DataIntegrityViolationException("no cause"))).isFalse();
    }

    private static DataIntegrityViolationException refusedWith(String sqlState) {
        return new DataIntegrityViolationException("refused", new SQLException("refused", sqlState));
    }
}
