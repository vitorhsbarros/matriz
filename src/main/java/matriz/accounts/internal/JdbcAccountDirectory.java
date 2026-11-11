package matriz.accounts.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    @Override
    public Map<UUID, Account> findByIds(Collection<UUID> ids) {
        Map<UUID, Account> accounts = new HashMap<>();
        if (ids.isEmpty()) {
            return accounts;
        }
        jdbc.sql("select id, name, email from users where id in (:ids)")
                .param("ids", ids)
                .query((row, number) -> new Account(row.getObject("id", UUID.class), row.getString("name"),
                        row.getString("email")))
                .list()
                .forEach(account -> accounts.put(account.id(), account));
        return accounts;
    }
}
