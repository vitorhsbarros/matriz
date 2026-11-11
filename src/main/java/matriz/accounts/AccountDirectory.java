package matriz.accounts;

import java.util.Optional;
import java.util.UUID;

/** Public interface of Accounts: look up a user by email (D03: compared ignoring case). */
public interface AccountDirectory {

    Optional<UUID> findUserIdByEmail(String email);
}
