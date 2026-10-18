package io.github.garatech.jobsinton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the JobSinton HTTP API. Domain modules will be added beneath this package. */
@SpringBootApplication
public class JobSintonApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobSintonApplication.class, args);
    }
}
