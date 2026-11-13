package io.github.garatech.jobsinton.providers.adzuna;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("adzuna-local")
class AdzunaLocalConfiguration {
    @Bean AdzunaTransport adzunaTransport() { return new JdkAdzunaTransport(Duration.ofSeconds(10)); }

    @Bean AdzunaSearch adzunaSearch(Environment environment, AdzunaTransport transport) {
        return new AdzunaSearch(environment.getProperty("ADZUNA_APP_ID", ""),
                environment.getProperty("ADZUNA_APP_KEY", ""), transport);
    }
}
