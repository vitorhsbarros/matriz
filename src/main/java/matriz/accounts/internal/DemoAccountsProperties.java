package matriz.accounts.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo accounts (FR21, US19, PD12). The passwords come only from the environment (SEC10); they
 * are never written in the repository.
 *
 * @param seed whether to create the demo data at start-up ({@code MATRIZ_DEMO_SEED})
 * @param passwordAna password of ana@example.com ({@code MATRIZ_DEMO_PASSWORD_ANA})
 * @param passwordRui password of rui@example.com ({@code MATRIZ_DEMO_PASSWORD_RUI})
 * @param passwordSara password of sara@example.com ({@code MATRIZ_DEMO_PASSWORD_SARA})
 */
@ConfigurationProperties("matriz.demo")
record DemoAccountsProperties(boolean seed, String passwordAna, String passwordRui, String passwordSara) {
}
