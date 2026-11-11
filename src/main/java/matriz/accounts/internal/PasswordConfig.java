package matriz.accounts.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password protection (NFR04, PD06: BCrypt). The delegating encoder stores the method with each
 * hash ("{bcrypt}..."), so it can be changed later without breaking existing passwords.
 */
@Configuration
class PasswordConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
