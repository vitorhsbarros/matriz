package matriz.web.internal;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Web entry access rules (04-solution-architecture.md, section 3.3; ADR-0002).
 *
 * <ul>
 *   <li>Public: the browser application's files, the health check and log-in ({@code POST /api/session}).
 *   <li>Everything else needs a valid session (NFR02, DoD02): otherwise {@code 401} "Please log in to continue.".
 *   <li>Every request that changes data needs the session's CSRF token in the {@code X-CSRF-Token} header
 *       (SEC07, DoD06). Log-in is exempt, because no session exists yet.
 * </ul>
 *
 * <p>Sessions are kept by Spring Session in the database, never in this instance's memory (ADR-0001, ADR-0002).
 * Access to a project is checked by the Projects module ({@code matriz.projects.ProjectAccess}).
 */
@Configuration
class SecurityConfig {

    static final String CSRF_HEADER = "X-CSRF-Token";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokens,
            SecurityContextRepository securityContexts, ProblemResponses problems) throws Exception {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        http.authorizeHttpRequests(requests -> requests
                        // An error raised while handling a request keeps that request's own status.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.svg", "/actuator/health")
                        .permitAll()
                        .requestMatchers(paths.matcher(HttpMethod.POST, "/api/session")).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                        // The token travels in a JSON body and a header, not in an HTML page, so it isn't masked.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(paths.matcher(HttpMethod.POST, "/api/session")))
                // Log-in and log-out are API operations of the Accounts module, not Spring Security pages.
                .securityContext(context -> context.securityContextRepository(securityContexts))
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(problems::notLoggedIn)
                        .accessDeniedHandler(problems::accessDenied))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable());
        return http.build();
    }

    /** The CSRF token is kept in the session, so it ends with it (ADR-0002). */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName(CSRF_HEADER);
        return repository;
    }

    /** The logged-in user is kept in the session, which Spring Session stores in the database. */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
