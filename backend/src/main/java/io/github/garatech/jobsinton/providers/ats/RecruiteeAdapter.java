package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Recruitee / Tellent public JSON Offers API adapter.
 * Endpoint: GET https://{slug}.recruitee.com/api/offers/
 */
public final class RecruiteeAdapter implements AtsProviderAdapter {
    private final AtsHttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public RecruiteeAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.RECRUITEE;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        URI uri = resolveEndpoint(board);
        var reply = http.get(uri);
        if (reply.status() != 200) {
            throw new AtsFetchException("HTTP " + reply.status() + " from Recruitee");
        }
        try {
            JsonNode root = json.readTree(reply.body());
            JsonNode offers = root == null ? null : root.path("offers");
            if (offers == null || !offers.isArray()) {
                throw new AtsFetchException("Recruitee payload missing offers array");
            }
            List<RawAtsJob> jobs = new ArrayList<>(offers.size());
            for (JsonNode offer : offers) {
                RawAtsJob parsed = mapOffer(board, offer);
                if (parsed != null) {
                    jobs.add(parsed);
                }
            }
            return jobs;
        } catch (JacksonException e) {
            throw new AtsFetchException("Unreadable JSON from Recruitee", e);
        }
    }

    private static URI resolveEndpoint(CompanyBoard board) {
        if (board.apiEndpoint() != null && AtsSupport.validHttpsUrl(board.apiEndpoint()) != null) {
            return URI.create(board.apiEndpoint());
        }
        return URI.create("https://" + AtsSupport.encodePathSegment(board.slug()) + ".recruitee.com/api/offers/");
    }

    private static RawAtsJob mapOffer(CompanyBoard board, JsonNode offer) {
        if (offer == null || !offer.isObject()) {
            return null;
        }
        String id = AtsSupport.idText(offer.path("id"));
        if (id == null) {
            id = AtsSupport.text(offer.path("slug"));
        }
        String title = AtsSupport.text(offer.path("title"));
        String applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(offer.path("careers_apply_url")));
        if (applyUrl == null) {
            applyUrl = AtsSupport.validHttpsUrl(AtsSupport.text(offer.path("careers_url")));
        }
        if (applyUrl == null && id != null) {
            applyUrl = AtsSupport.validHttpsUrl("https://" + board.slug() + ".recruitee.com/o/" + AtsSupport.encodePathSegment(id));
        }
        if (id == null || title == null || applyUrl == null) {
            return null;
        }

        Instant publishedAt = AtsSupport.parseTimestamp(offer.path("published_at"));
        if (publishedAt == null) {
            publishedAt = AtsSupport.parseTimestamp(offer.path("created_at"));
        }

        String location = buildLocation(offer);
        String workplaceType;
        if (AtsSupport.boolFlag(offer.path("remote"))) {
            workplaceType = "remote";
        } else if (AtsSupport.boolFlag(offer.path("hybrid"))) {
            workplaceType = "hybrid";
        } else if (AtsSupport.boolFlag(offer.path("on_site"))) {
            workplaceType = "onsite";
        } else {
            workplaceType = AtsSupport.inferWorkplaceFromLocation(location);
        }

        String description = buildDescription(offer);
        String department = AtsSupport.text(offer.path("department"));
        String compensation = extractSalary(offer.path("salary"));
        if (compensation != null && !description.contains(compensation)) {
            description = description.isEmpty() ? "Compensation: " + compensation : description + "\n\nCompensation: " + compensation;
        }

        return new RawAtsJob(
                id,
                AtsProviderType.RECRUITEE,
                board.slug(),
                board.name(),
                title,
                location,
                workplaceType,
                publishedAt,
                description,
                applyUrl,
                department,
                compensation,
                !description.isEmpty()
        );
    }

    private static String buildLocation(JsonNode offer) {
        List<String> parts = new ArrayList<>();
        String loc = AtsSupport.text(offer.path("location"));
        String city = AtsSupport.text(offer.path("city"));
        String country = AtsSupport.countryDisplayName(
                AtsSupport.text(offer.path("country")) != null
                        ? AtsSupport.text(offer.path("country"))
                        : AtsSupport.text(offer.path("country_code"))
        );
        if (loc != null) parts.add(loc);
        if (city != null) parts.add(city);
        if (country != null) parts.add(country);

        JsonNode locations = offer.path("locations");
        if (locations.isArray()) {
            for (JsonNode item : locations) {
                String itemCity = AtsSupport.text(item.path("city"));
                String itemCountry = AtsSupport.countryDisplayName(
                        AtsSupport.text(item.path("country")) != null
                                ? AtsSupport.text(item.path("country"))
                                : AtsSupport.text(item.path("country_code"))
                );
                if (itemCity != null) parts.add(itemCity);
                if (itemCountry != null) parts.add(itemCountry);
            }
        }
        return AtsSupport.joinUniqueParts(parts, ", ");
    }

    private static String buildDescription(JsonNode offer) {
        List<String> sections = new ArrayList<>();
        String desc = HtmlTextCleaner.toPlainText(AtsSupport.text(offer.path("description")));
        if (!desc.isEmpty()) {
            sections.add(desc);
        }
        String reqs = HtmlTextCleaner.toPlainText(AtsSupport.text(offer.path("requirements")));
        if (!reqs.isEmpty()) {
            sections.add(reqs);
        }
        return String.join("\n\n", sections);
    }

    private static String extractSalary(JsonNode salary) {
        if (salary == null || !salary.isObject()) {
            return null;
        }
        String min = AtsSupport.idText(salary.path("min"));
        String max = AtsSupport.idText(salary.path("max"));
        String currency = AtsSupport.text(salary.path("currency"));
        String period = AtsSupport.text(salary.path("period"));
        if (min == null && max == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (min != null && max != null) {
            sb.append(min).append(" - ").append(max);
        } else if (min != null) {
            sb.append("From ").append(min);
        } else {
            sb.append("Up to ").append(max);
        }
        if (currency != null) {
            sb.append(" ").append(currency);
        }
        if (period != null) {
            sb.append(" / ").append(period);
        }
        return sb.toString();
    }
}
