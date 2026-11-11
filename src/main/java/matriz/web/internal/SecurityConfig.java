package matriz.web.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Access rules for increment I0: the browser application's files and the health check are public;
 * everything else is refused until log-in is implemented (I1, US02).
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/actuator/health")
                .permitAll()
                .anyRequest()
                .denyAll());
        return http.build();
    }

    /**
     * No users are known to Spring Security yet. Declaring an empty store also stops Spring Boot
     * from generating a default user and printing its password in the log (SEC10).
     */
    @Bean
    UserDetailsService noUsersYet() {
        return new InMemoryUserDetailsManager();
    }
}
