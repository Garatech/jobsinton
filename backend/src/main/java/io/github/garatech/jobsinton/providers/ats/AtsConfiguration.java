package io.github.garatech.jobsinton.providers.ats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/** Zero-config Spring wiring for the Multi-ATS Direct Provider Engine (no API keys required). */
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
    AtsFeedService atsFeedService(
            CompanyRegistry registry,
            GreenhouseAdapter greenhouse,
            LeverAdapter lever,
            SmartRecruitersAdapter smartRecruiters,
            WorkableAdapter workable,
            AshbyAdapter ashby,
            PersonioAdapter personio,
            RecruiteeAdapter recruitee,
            Clock clock,
            FirstSeenRegistry firstSeenRegistry,
            @Value("${jobsinton.ats.cache-ttl-seconds:300}") long cacheTtlSeconds
    ) {
        return new AtsFeedService(
                registry,
                List.of(greenhouse, lever, smartRecruiters, workable, ashby, personio, recruitee),
                smartRecruiters,
                clock,
                Duration.ofSeconds(Math.max(0L, cacheTtlSeconds)),
                firstSeenRegistry
        );
    }
}
