package io.github.garatech.jobsinton.providers.ats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** Zero-config Spring wiring for all 9 public ATS platforms (no API keys required). */
@Configuration
public class AtsConfiguration {

    @Bean
    Clock atsClock() {
        return Clock.systemUTC();
    }

    @Bean
    AtsHttpClient atsHttpClient(@Value("${jobsinton.ats.request-timeout-ms:8000}") long timeoutMs) {
        return new JdkAtsHttpClient(Duration.ofMillis(Math.max(500L, timeoutMs)));
    }

    @Bean
    CompanyRegistry companyRegistry() {
        return CompanyRegistry.loadDefault();
    }

    @Bean
    FirstSeenRegistry firstSeenRegistry(
            @Value("${jobsinton.ats.seen-jobs-file:data/ats-seen-jobs.json}") String seenFile
    ) {
        Path path = seenFile == null || seenFile.isBlank() ? null : Path.of(seenFile.strip());
        return new FirstSeenRegistry(path);
    }

    @Bean
    GreenhouseAdapter greenhouseAdapter(AtsHttpClient http) {
        return new GreenhouseAdapter(http);
    }

    @Bean
    LeverAdapter leverAdapter(AtsHttpClient http) {
        return new LeverAdapter(http);
    }

    @Bean
    SmartRecruitersAdapter smartRecruitersAdapter(
            AtsHttpClient http,
            @Value("${jobsinton.ats.smartrecruiters-detail-cap:6}") int detailCap
    ) {
        return new SmartRecruitersAdapter(http, detailCap);
    }

    @Bean
    WorkableAdapter workableAdapter(AtsHttpClient http) {
        return new WorkableAdapter(http);
    }

    @Bean
    AshbyAdapter ashbyAdapter(AtsHttpClient http) {
        return new AshbyAdapter(http);
    }

    @Bean
    PersonioAdapter personioAdapter(AtsHttpClient http) {
        return new PersonioAdapter(http);
    }

    @Bean
    RecruiteeAdapter recruiteeAdapter(AtsHttpClient http) {
        return new RecruiteeAdapter(http);
    }

    @Bean
    TeamtailorAdapter teamtailorAdapter(AtsHttpClient http) {
        return new TeamtailorAdapter(http);
    }

    @Bean
    FactorialAdapter factorialAdapter(
            AtsHttpClient http,
            @Value("${jobsinton.ats.factorial-detail-cap:6}") int detailCap
    ) {
        return new FactorialAdapter(http, detailCap);
    }

    @Bean
    AtsFeedService atsFeedService(
            CompanyRegistry registry,
            GreenhouseAdapter greenhouse,
            LeverAdapter lever,
            SmartRecruitersAdapter smartRecruiters,
            WorkableAdapter workable,
            AshbyAdapter ashby,
            PersonioAdapter personio,
            RecruiteeAdapter recruitee,
            TeamtailorAdapter teamtailor,
            FactorialAdapter factorial,
            Clock clock,
            FirstSeenRegistry firstSeenRegistry,
            @Value("${jobsinton.ats.cache-ttl-seconds:600}") long cacheTtlSeconds,
            @Value("${jobsinton.ats.max-concurrency:18}") int maxConcurrency
    ) {
        return new AtsFeedService(
                registry,
                List.of(greenhouse, lever, smartRecruiters, workable, ashby, personio, recruitee, teamtailor, factorial),
                smartRecruiters,
                clock,
                Duration.ofSeconds(Math.max(0L, cacheTtlSeconds)),
                firstSeenRegistry,
                maxConcurrency
        );
    }
}
