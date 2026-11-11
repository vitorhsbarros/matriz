package matriz.accounts.internal;

import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Checks an email and password against the accounts (US02, NFR04).
 *
 * <ul>
 *   <li>The email is compared ignoring case and leading or trailing spaces (D01, D03, US02 AC07);
 *       the password exactly as typed (US02 AC08).
 *   <li>An unknown email and a wrong password fail in the same way, and the password is checked against a
 *       dummy hash when the email is unknown, so neither the answer nor its timing reveals which emails
 *       have an account (SEC04, US02 AC09).
 *   <li>The authenticated user's name in Spring Security is the account's identifier, so the session
 *       keeps no email or password (PRI01).
 * </ul>
 */
@Configuration
class AuthenticationConfig {

    @Bean
    UserDetailsService accountLookup(JdbcClient jdbc) {
        return email -> jdbc.sql("select id, password_hash from users where email = ?")
                .param(email.trim().toLowerCase(Locale.ROOT))
                .query((row, number) -> User.withUsername(row.getObject("id", UUID.class).toString())
                        .password(row.getString("password_hash"))
                        .build())
                .optional()
                // Never shown to the user: Spring Security turns it into the same failure as a wrong password.
                .orElseThrow(() -> new UsernameNotFoundException("No account"));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService accountLookup, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(accountLookup);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
