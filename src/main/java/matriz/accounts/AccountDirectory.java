package matriz.accounts;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public interface of Accounts (04-solution-architecture.md, section 3.4): <em>Find user by email</em> and
 * <em>Get users</em>.
 */
public interface AccountDirectory {

    record Account(UUID id, String name, String email) {
    }

    /** The account with this email, compared ignoring case and surrounding spaces (D01, D03). */
    Optional<UUID> findUserIdByEmail(String email);

    /** The accounts with these identifiers; identifiers without an account are left out. */
    Map<UUID, Account> findByIds(Collection<UUID> ids);
}
