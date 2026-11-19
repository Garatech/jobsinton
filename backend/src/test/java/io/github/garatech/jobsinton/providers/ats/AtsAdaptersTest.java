package io.github.garatech.jobsinton.providers.ats;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtsAdaptersTest {

    private static AtsHttpClient.HttpReply ok(String body) {
        return new AtsHttpClient.HttpReply(200, body.getBytes(StandardCharsets.UTF_8));
    }

    private static AtsHttpClient.HttpReply status(int code) {
        return new AtsHttpClient.HttpReply(code, new byte[0]);
    }

    @Test
    void greenhouseAdapterParsesContentTimestampLocationAndRejectsUnsafeUrls() {
        List<URI> requested = new ArrayList<>();
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                requested.add(uri);
                return ok("""
                        {
                          "jobs": [
                            {
                              "id": 451092,
                              "title": "Junior Full Stack Engineer",
                              "updated_at": "2026-11-17T08:00:00-02:00",
                              "location": { "name": "Madrid, Spain (Hybrid)" },
                              "absolute_url": "https://boards.greenhouse.io/cabify/jobs/451092",
                              "departments": [{ "name": "Engineering" }],
                              "content": "&lt;p&gt;Stack: Java, Spring Boot &amp;amp; Angular.&lt;/p&gt;"
                            },
                            {
                              "id": 999999,
                              "title": "Bad Link Role",
                              "absolute_url": "javascript:alert(1)"
                            }
                          ]
                        }
                        """);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var board = new CompanyBoard("cabify", "Cabify", "greenhouse", null, "Madrid");
        var jobs = new GreenhouseAdapter(stub).fetch(board);

        assertThat(requested).containsExactly(URI.create("https://boards-api.greenhouse.io/v1/boards/cabify/jobs?content=true"));
        assertThat(jobs).hasSize(1);
        RawAtsJob job = jobs.getFirst();
        assertThat(job.compositeId()).isEqualTo("greenhouse:cabify:451092");
        assertThat(job.title()).isEqualTo("Junior Full Stack Engineer");
        assertThat(job.publishedAt()).isEqualTo(Instant.parse("2026-11-17T10:00:00Z"));
        assertThat(job.location()).isEqualTo("Madrid, Spain (Hybrid)");
        assertThat(job.workplaceType()).isEqualTo("hybrid");
        assertThat(job.department()).isEqualTo("Engineering");
        assertThat(job.description()).contains("Java, Spring Boot & Angular.");
        assertThat(job.applyUrl()).isEqualTo("https://boards.greenhouse.io/cabify/jobs/451092");
        assertThat(job.hasFullDescription()).isTrue();
    }

    @Test
    void leverAdapterSupportsEuRegionAndAutomaticFallbackFromGlobal404() {
        List<URI> requested = new ArrayList<>();
        String payload = """
                [
                  {
                    "id": "lev-1",
                    "text": "Backend Software Engineer",
                    "createdAt": 1763370000000,
                    "workplaceType": "remote",
                    "categories": {
                      "location": "Madrid",
                      "allLocations": ["Madrid", "Remote Spain"],
                      "team": "Platform"
                    },
                    "descriptionPlain": "Join our European platform team.",
                    "lists": [
                      { "text": "Tech Stack", "content": "<li>Java 21 &amp; Spring Boot</li><li>PostgreSQL &amp; Docker</li>" }
                    ],
                    "applyUrl": "https://jobs.eu.lever.co/tomtom/lev-1/apply"
                  }
                ]
                """;
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                requested.add(uri);
                if (uri.getHost().equals("api.lever.co")) {
                    return status(404);
                }
                return ok(payload);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var adapter = new LeverAdapter(stub);

        // Global board returns 404 -> falls back automatically to api.eu.lever.co.
        var fallbackBoard = new CompanyBoard("tomtom", "TomTom", "lever", "global", "Madrid");
        var fallbackJobs = adapter.fetch(fallbackBoard);
        assertThat(requested).containsExactly(
                URI.create("https://api.lever.co/v0/postings/tomtom?mode=json"),
                URI.create("https://api.eu.lever.co/v0/postings/tomtom?mode=json")
        );
        assertThat(fallbackJobs).hasSize(1);
        RawAtsJob job = fallbackJobs.getFirst();
        assertThat(job.publishedAt()).isEqualTo(Instant.ofEpochMilli(1763370000000L));
        assertThat(job.location()).isEqualTo("Madrid · Remote Spain");
        assertThat(job.workplaceType()).isEqualTo("remote");
        assertThat(job.department()).isEqualTo("Platform");
        assertThat(job.description())
                .contains("Join our European platform team.")
                .contains("Tech Stack")
                .contains("Java 21 & Spring Boot")
                .contains("PostgreSQL & Docker");

        // Explicit EU board calls api.eu.lever.co directly on the first request.
        requested.clear();
        var euBoard = new CompanyBoard("olx", "OLX", "lever", "eu", "Europe");
        adapter.fetch(euBoard);
        assertThat(requested).containsExactly(URI.create("https://api.eu.lever.co/v0/postings/olx?mode=json"));
    }

    @Test
    void smartRecruitersAdapterCapsInitialDetailsAndHydratesOnDemand() {
        List<URI> requested = new ArrayList<>();
        Map<String, String> responses = Map.of(
                "https://api.smartrecruiters.com/v1/companies/Celonis/postings?limit=100", """
                        {
                          "content": [
                            {
                              "id": "sr-sales",
                              "name": "Account Executive",
                              "releasedDate": "2026-11-10T09:00:00Z",
                              "location": { "city": "New York", "country": "us", "remote": false },
                              "department": { "label": "Sales" }
                            },
                            {
                              "id": "sr-sap",
                              "name": "SAP ABAP / RAP Developer",
                              "releasedDate": "2026-11-17T09:00:00Z",
                              "location": { "city": "Madrid", "country": "es", "remote": true },
                              "department": { "label": "Engineering" }
                            }
                          ]
                        }
                        """,
                "https://api.smartrecruiters.com/v1/companies/Celonis/postings/sr-sap", """
                        {
                          "applyUrl": "https://jobs.smartrecruiters.com/Celonis/sr-sap/apply",
                          "jobAd": {
                            "sections": {
                              "jobDescription": { "text": "<p>Work on SAP S/4HANA &amp; OData v4.</p>" },
                              "qualifications": { "text": "<ul><li>ABAP OO</li><li>RAP / CAP</li></ul>" }
                            }
                          }
                        }
                        """,
                "https://api.smartrecruiters.com/v1/companies/Celonis/postings/sr-sales", """
                        {
                          "jobAd": {
                            "sections": {
                              "jobDescription": { "text": "<p>Enterprise SaaS sales.</p>" }
                            }
                          }
                        }
                        """
        );
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                requested.add(uri);
                String body = responses.get(uri.toString());
                return body != null ? ok(body) : status(404);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var board = new CompanyBoard("Celonis", "Celonis", "smartrecruiters", null, "Madrid");
        var adapter = new SmartRecruitersAdapter(stub, 1); // Only enrich top 1 candidate on startup.

        var jobs = adapter.fetch(board);
        assertThat(jobs).hasSize(2);
        // Priority enrichment selects sr-sap (Madrid + Remote + SAP) over sr-sales.
        assertThat(requested).containsExactly(
                URI.create("https://api.smartrecruiters.com/v1/companies/Celonis/postings?limit=100"),
                URI.create("https://api.smartrecruiters.com/v1/companies/Celonis/postings/sr-sap")
        );

        RawAtsJob sapJob = jobs.stream().filter(j -> j.sourceId().equals("sr-sap")).findFirst().orElseThrow();
        assertThat(sapJob.location()).isEqualTo("Madrid, Spain, Remote");
        assertThat(sapJob.workplaceType()).isEqualTo("remote");
        assertThat(sapJob.hasFullDescription()).isTrue();
        assertThat(sapJob.description()).contains("SAP S/4HANA & OData v4.").contains("ABAP OO");
        assertThat(sapJob.applyUrl()).isEqualTo("https://jobs.smartrecruiters.com/Celonis/sr-sap/apply");

        RawAtsJob salesJob = jobs.stream().filter(j -> j.sourceId().equals("sr-sales")).findFirst().orElseThrow();
        assertThat(salesJob.hasFullDescription()).isFalse();
        assertThat(salesJob.applyUrl()).isEqualTo("https://jobs.smartrecruiters.com/Celonis/sr-sales");

        // On-demand detail hydration loads sr-sales and caches it.
        SmartRecruitersDetail detail = adapter.fetchDetail(board, "sr-sales");
        assertThat(detail.hasFullDescription()).isTrue();
        assertThat(detail.description()).contains("Enterprise SaaS sales.");
        int callsBeforeCacheHit = requested.size();
        adapter.fetchDetail(board, "sr-sales");
        assertThat(requested).hasSize(callsBeforeCacheHit);
    }

    @Test
    void workableAdapterReadsWidgetV1AndFallsBackToV3Post() {
        List<String> calls = new ArrayList<>();
        AtsHttpClient v1Stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                calls.add("GET " + uri);
                return ok("""
                        {
                          "jobs": [
                            {
                              "shortcode": "ABC123",
                              "title": "Frontend Angular Developer",
                              "published_on": "2026-11-16",
                              "city": "Madrid",
                              "country": "Spain",
                              "telecommuting": true,
                              "department": "Engineering",
                              "description": "<p>Angular 21 &amp; TypeScript</p>",
                              "url": "https://apply.workable.com/mediaradar/j/ABC123/"
                            }
                          ]
                        }
                        """);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("v3 fallback should not run when v1 succeeds");
            }
        };

        var board = new CompanyBoard("mediaradar", "MediaRadar", "workable", null, "Remote");
        var v1Jobs = new WorkableAdapter(v1Stub).fetch(board);
        assertThat(v1Jobs).hasSize(1);
        assertThat(v1Jobs.getFirst().sourceId()).isEqualTo("ABC123");
        assertThat(v1Jobs.getFirst().publishedAt()).isEqualTo(Instant.parse("2026-11-16T00:00:00Z"));
        assertThat(v1Jobs.getFirst().workplaceType()).isEqualTo("remote");
        assertThat(v1Jobs.getFirst().location()).isEqualTo("Madrid, Spain, Remote");
        assertThat(v1Jobs.getFirst().description()).isEqualTo("Angular 21 & TypeScript");

        // Now verify v3 POST fallback when v1 widget returns 404.
        calls.clear();
        AtsHttpClient v3Stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                calls.add("GET " + uri);
                return status(404);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                calls.add("POST " + uri);
                return ok("""
                        {
                          "results": [
                            {
                              "shortcode": "XYZ987",
                              "title": "Java Developer",
                              "published": "2026-11-17T06:00:00Z",
                              "remote": false,
                              "workplace": "hybrid",
                              "location": { "city": "Madrid", "country": "Spain" },
                              "department": ["IT"]
                            }
                          ]
                        }
                        """);
            }
        };

        var v3Jobs = new WorkableAdapter(v3Stub).fetch(board);
        assertThat(calls).containsExactly(
                "GET https://apply.workable.com/api/v1/widget/accounts/mediaradar?details=true",
                "POST https://apply.workable.com/api/v3/accounts/mediaradar/jobs"
        );
        assertThat(v3Jobs).hasSize(1);
        assertThat(v3Jobs.getFirst().sourceId()).isEqualTo("XYZ987");
        assertThat(v3Jobs.getFirst().workplaceType()).isEqualTo("hybrid");
        assertThat(v3Jobs.getFirst().applyUrl()).isEqualTo("https://apply.workable.com/mediaradar/j/XYZ987/");
    }

    @Test
    void ashbyAdapterParsesCompensationSecondaryLocationsAndSkipsUnlistedJobs() {
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                assertThat(uri).isEqualTo(URI.create("https://api.ashbyhq.com/posting-api/job-board/invopop?includeCompensation=true"));
                return ok("""
                        {
                          "jobs": [
                            {
                              "id": "ash-1",
                              "title": "Full Stack Engineer (Node.js / React)",
                              "publishedAt": "2026-11-17T07:30:00.000+00:00",
                              "location": "Madrid, Spain",
                              "secondaryLocations": [{ "location": "Remote EU" }],
                              "isRemote": true,
                              "workplaceType": "Remote",
                              "isListed": true,
                              "department": "Product Engineering",
                              "descriptionPlain": "TypeScript, React, Node.js, PostgreSQL and Docker.",
                              "applyUrl": "https://jobs.ashbyhq.com/invopop/ash-1/application",
                              "compensation": {
                                "compensationTierSummary": "€42K – €55K"
                              }
                            },
                            {
                              "id": "ash-hidden",
                              "title": "Unlisted Internal Role",
                              "isListed": false,
                              "applyUrl": "https://jobs.ashbyhq.com/invopop/ash-hidden/application"
                            }
                          ]
                        }
                        """);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var board = new CompanyBoard("invopop", "Invopop", "ashby", null, "Madrid");
        var jobs = new AshbyAdapter(stub).fetch(board);

        assertThat(jobs).hasSize(1);
        RawAtsJob job = jobs.getFirst();
        assertThat(job.compositeId()).isEqualTo("ashby:invopop:ash-1");
        assertThat(job.location()).isEqualTo("Madrid, Spain · Remote EU");
        assertThat(job.workplaceType()).isEqualTo("remote");
        assertThat(job.compensation()).isEqualTo("€42K – €55K");
        assertThat(job.description()).contains("TypeScript, React, Node.js, PostgreSQL and Docker.");
    }

    @Test
    void adaptersSurfaceHttpFailuresAsAtsFetchException() {
        AtsHttpClient fail = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                return status(503);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                return status(503);
            }
        };
        assertThatThrownBy(() -> new GreenhouseAdapter(fail).fetch(new CompanyBoard("x", "X", "greenhouse", null, null)))
                .isInstanceOf(AtsFetchException.class)
                .hasMessageContaining("503");
        assertThatThrownBy(() -> new AshbyAdapter(fail).fetch(new CompanyBoard("x", "X", "ashby", null, null)))
                .isInstanceOf(AtsFetchException.class)
                .hasMessageContaining("503");
    }

    @Test
    void greenhouseAdapterPrefersFirstPublishedAndFallsBackToEuCluster() {
        List<URI> requested = new ArrayList<>();
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                requested.add(uri);
                if (uri.getHost().equals("boards-api.greenhouse.io")) {
                    return status(404);
                }
                return ok("""
                        {
                          "jobs": [
                            {
                              "id": 777,
                              "title": "Java Backend Developer",
                              "first_published": "2026-11-10T10:00:00Z",
                              "updated_at": "2026-11-18T10:00:00Z",
                              "location": { "name": "Madrid, Spain" },
                              "absolute_url": "https://boards.eu.greenhouse.io/clarityai/jobs/777",
                              "content": "<p>Spring Boot &amp; PostgreSQL</p>"
                            }
                          ]
                        }
                        """);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var board = new CompanyBoard("clarityai", "Clarity AI", "greenhouse", "eu", "Madrid");
        var jobs = new GreenhouseAdapter(stub).fetch(board);
        assertThat(requested).containsExactly(
                URI.create("https://boards-api.greenhouse.io/v1/boards/clarityai/jobs?content=true"),
                URI.create("https://boards-api.eu.greenhouse.io/v1/boards/clarityai/jobs?content=true")
        );
        assertThat(jobs).hasSize(1);
        assertThat(jobs.getFirst().publishedAt()).isEqualTo(Instant.parse("2026-11-10T10:00:00Z"));
    }

    @Test
    void personioAdapterParsesXmlAndMergesEnglishDescriptionsWhenSpanishIsEmpty() {
        String esXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workzag-jobs>
                  <position>
                    <id>1001</id>
                    <name>Desarrollador Java Spring Boot</name>
                    <office>Madrid</office>
                    <additionalOffices><office>Remote Spain</office></additionalOffices>
                    <department>Engineering</department>
                    <seniority>junior</seniority>
                    <yearsOfExperience>1-2</yearsOfExperience>
                    <schedule>full-time</schedule>
                    <createdAt>2026-11-18T09:00:00Z</createdAt>
                    <jobDescriptions>
                      <jobDescription>
                        <name>Requisitos</name>
                        <value><![CDATA[<p>Java 21, Spring Boot, Angular y PostgreSQL en Madrid (Híbrido).</p>]]></value>
                      </jobDescription>
                    </jobDescriptions>
                  </position>
                  <position>
                    <id>1002</id>
                    <name>SAP S/4HANA ABAP Consultant</name>
                    <office>Madrid</office>
                    <createdAt>2026-11-18T11:00:00Z</createdAt>
                    <jobDescriptions></jobDescriptions>
                  </position>
                </workzag-jobs>
                """;
        String enXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workzag-jobs>
                  <position>
                    <id>1002</id>
                    <name>SAP S/4HANA ABAP Consultant</name>
                    <office>Madrid</office>
                    <createdAt>2026-11-18T11:00:00Z</createdAt>
                    <jobDescriptions>
                      <jobDescription>
                        <name>Role</name>
                        <value><![CDATA[<p>SAP S/4HANA, ABAP OO, RAP, CAP and OData v4 in Madrid.</p>]]></value>
                      </jobDescription>
                    </jobDescriptions>
                  </position>
                </workzag-jobs>
                """;

        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                if (uri.toString().contains("language=es")) {
                    return ok(esXml);
                }
                return ok(enXml);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var board = new CompanyBoard("paradigmadigital", "Paradigma Digital", "personio", "eu", "Madrid");
        var jobs = new PersonioAdapter(stub).fetch(board);
        assertThat(jobs).hasSize(2);

        RawAtsJob javaJob = jobs.get(0);
        assertThat(javaJob.compositeId()).isEqualTo("personio:paradigmadigital:1001");
        assertThat(javaJob.location()).isEqualTo("Madrid · Remote Spain");
        assertThat(javaJob.workplaceType()).isEqualTo("hybrid");
        assertThat(javaJob.description()).contains("Java 21, Spring Boot, Angular").contains("Seniority: junior");
        assertThat(javaJob.applyUrl()).isEqualTo("https://paradigmadigital.jobs.personio.com/job/1001");

        RawAtsJob sapJob = jobs.get(1);
        assertThat(sapJob.compositeId()).isEqualTo("personio:paradigmadigital:1002");
        assertThat(sapJob.hasFullDescription()).isTrue();
        assertThat(sapJob.description()).contains("SAP S/4HANA, ABAP OO, RAP, CAP and OData v4");
    }

    @Test
    void recruiteeAdapterParsesOffersFlagsAndSalary() {
        AtsHttpClient stub = new AtsHttpClient() {
            @Override
            public HttpReply get(URI uri) {
                assertThat(uri).isEqualTo(URI.create("https://makingscience.recruitee.com/api/offers/"));
                return ok("""
                        {
                          "offers": [
                            {
                              "id": 8841,
                              "title": "Junior Full Stack Developer (Node.js / React)",
                              "published_at": "2026-11-18T10:15:00Z",
                              "city": "Madrid",
                              "country_code": "ES",
                              "remote": false,
                              "hybrid": true,
                              "on_site": false,
                              "department": "Digital Engineering",
                              "description": "<p>Build products with Node.js, TypeScript and React.</p>",
                              "requirements": "<ul><li>PostgreSQL</li><li>Docker</li></ul>",
                              "salary": { "min": 32000, "max": 40000, "currency": "EUR", "period": "year" },
                              "careers_apply_url": "https://makingscience.recruitee.com/o/junior-full-stack/c/new"
                            }
                          ]
                        }
                        """);
            }

            @Override
            public HttpReply postJson(URI uri, String jsonBody) {
                throw new AssertionError("Unexpected POST");
            }
        };

        var board = new CompanyBoard("makingscience", "Making Science", "recruitee", "eu", "Madrid");
        var jobs = new RecruiteeAdapter(stub).fetch(board);
        assertThat(jobs).hasSize(1);
        RawAtsJob job = jobs.getFirst();
        assertThat(job.compositeId()).isEqualTo("recruitee:makingscience:8841");
        assertThat(job.workplaceType()).isEqualTo("hybrid");
        assertThat(job.location()).isEqualTo("Madrid, Spain");
        assertThat(job.compensation()).isEqualTo("32000 - 40000 EUR / year");
        assertThat(job.description()).contains("Node.js, TypeScript and React.").contains("PostgreSQL");
        assertThat(job.applyUrl()).isEqualTo("https://makingscience.recruitee.com/o/junior-full-stack/c/new");
    }
}
