package matriz.accounts.internal;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import matriz.accounts.AccountDirectory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcAccountDirectory implements AccountDirectory {

    private final JdbcClient jdbc;

    JdbcAccountDirectory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return jdbc.sql("select id from users where email = ?")
                .param(email.trim().toLowerCase(Locale.ROOT))
                .query(UUID.class)
                .optional();
    }
}
