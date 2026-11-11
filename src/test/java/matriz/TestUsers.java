package matriz;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Made-up users created by a test for itself (PRI05), so lists like the home dashboard depend only on that
 * test's own data. The password is fictitious and exists only in the tests.
 */
final class TestUsers {

    static final String PASSWORD = "test-password-x";

    record TestUser(UUID id, String name, String email) {
    }

    private TestUsers() {
    }

    static TestUser create(JdbcClient jdbc, PasswordEncoder encoder, String name) {
        UUID id = UUID.randomUUID();
        String email = "test-" + id + "@example.com";
        jdbc.sql("insert into users (id, name, email, password_hash) values (?, ?, ?, ?)")
                .params(id, name, email, encoder.encode(PASSWORD))
                .update();
        return new TestUser(id, name, email);
    }
}
