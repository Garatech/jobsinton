package io.github.garatech.jobsinton.providers.ats;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Personio public XML Job Feed adapter with dual-language (es/en) description merge and .com/.de fallback.
 * Primary:  GET https://{slug}.jobs.personio.com/xml?language=es (or configured apiEndpoint/host)
 * Fallback: GET https://{slug}.jobs.personio.de/xml?language=en
 */
public final class PersonioAdapter implements AtsProviderAdapter {
    private final AtsHttpClient http;

    public PersonioAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.PERSONIO;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        List<String> candidateUrls = buildCandidateUrls(board);
        String primaryUrl = candidateUrls.get(0);
        var reply = http.get(URI.create(primaryUrl));

        int usedIndex = 0;
        if (reply.status() != 200) {
            boolean recovered = false;
            for (int i = 1; i < candidateUrls.size(); i++) {
                var nextReply = http.get(URI.create(candidateUrls.get(i)));
                if (nextReply.status() == 200) {
                    reply = nextReply;
                    usedIndex = i;
                    recovered = true;
                    break;
                }
            }
            if (!recovered) {
                throw new AtsFetchException("HTTP " + reply.status() + " from Personio");
            }
        }

        List<ParsedPosition> positions = parseXmlPositions(board, reply.body(), candidateUrls.get(usedIndex));
        if (positions.isEmpty()) {
            for (int i = usedIndex + 1; i < candidateUrls.size(); i++) {
                try {
                    var nextReply = http.get(URI.create(candidateUrls.get(i)));
                    if (nextReply.status() == 200) {
                        List<ParsedPosition> fallbackPositions = parseXmlPositions(board, nextReply.body(), candidateUrls.get(i));
                        if (!fallbackPositions.isEmpty()) {
                            positions = fallbackPositions;
                            usedIndex = i;
                            break;
                        }
                    }
                } catch (AtsFetchException ignored) {
                    // Continue checking remaining candidate URLs.
                }
            }
        }

        // Dual-Language XML Rule: if any position in the primary language feed has an empty description,
        // fetch the alternate language feed and merge descriptions by position id.
        boolean anyMissingDescription = positions.stream().anyMatch(p -> p.description.isEmpty());
        if (anyMissingDescription) {
            for (int i = 0; i < candidateUrls.size(); i++) {
                if (i == usedIndex) {
                    continue;
                }
                try {
                    var altReply = http.get(URI.create(candidateUrls.get(i)));
                    if (altReply.status() == 200) {
                        Map<String, ParsedPosition> altById = new LinkedHashMap<>();
                        for (ParsedPosition alt : parseXmlPositions(board, altReply.body(), candidateUrls.get(i))) {
                            if (!alt.description.isEmpty()) {
                                altById.put(alt.id, alt);
                            }
                        }
                        if (!altById.isEmpty()) {
                            List<ParsedPosition> merged = new ArrayList<>(positions.size());
                            for (ParsedPosition pos : positions) {
                                if (pos.description.isEmpty() && altById.containsKey(pos.id)) {
                                    ParsedPosition donor = altById.get(pos.id);
                                    merged.add(pos.withDescription(donor.description, donor.metadataSuffix));
                                } else {
                                    merged.add(pos);
                                }
                            }
                            positions = merged;
                            if (positions.stream().noneMatch(p -> p.description.isEmpty())) {
                                break;
                            }
                        }
                    }
                } catch (AtsFetchException ignored) {
                    // Best-effort language fallback.
                }
            }
        }

        List<RawAtsJob> jobs = new ArrayList<>(positions.size());
        for (ParsedPosition pos : positions) {
            jobs.add(pos.toRawJob(board));
        }
        return jobs;
    }

    private static List<String> buildCandidateUrls(CompanyBoard board) {
        Set<String> urls = new LinkedHashSet<>();
        String slug = AtsSupport.encodePathSegment(board.slug());
        if (board.apiEndpoint() != null && AtsSupport.validHttpsUrl(board.apiEndpoint()) != null) {
            urls.add(board.apiEndpoint());
        }
        if (board.fallbackEndpoint() != null && AtsSupport.validHttpsUrl(board.fallbackEndpoint()) != null) {
            urls.add(board.fallbackEndpoint());
        }
        String host = board.host() != null && board.host().contains("personio.")
                ? (board.host().startsWith(board.slug() + ".") ? board.host() : board.slug() + "." + board.host())
                : null;
        if (host != null) {
            urls.add("https://" + host + "/xml?language=es");
            urls.add("https://" + host + "/xml?language=en");
        }
        urls.add("https://" + slug + ".jobs.personio.com/xml?language=es");
        urls.add("https://" + slug + ".jobs.personio.de/xml?language=en");
        urls.add("https://" + slug + ".jobs.personio.de/xml?language=es");
        urls.add("https://" + slug + ".jobs.personio.com/xml?language=en");
        return List.copyOf(urls);
    }

    private record ParsedPosition(
            String id,
            String title,
            String location,
            String workplaceType,
            Instant publishedAt,
            String description,
            String metadataSuffix,
            String department,
            String applyUrl
    ) {
        ParsedPosition withDescription(String newDescription, String donorSuffix) {
            String updatedWorkplace = inferPersonioWorkplace(location, newDescription);
            return new ParsedPosition(
                    id,
                    title,
                    location,
                    updatedWorkplace,
                    publishedAt,
                    newDescription,
                    metadataSuffix.isEmpty() ? donorSuffix : metadataSuffix,
                    department,
                    applyUrl
            );
        }

        RawAtsJob toRawJob(CompanyBoard board) {
            String fullDesc = description;
            if (!metadataSuffix.isEmpty()) {
                fullDesc = fullDesc.isEmpty() ? metadataSuffix : fullDesc + "\n\n" + metadataSuffix;
            }
            String resolvedWorkplace = workplaceType;
            if (resolvedWorkplace == null) {
                resolvedWorkplace = inferPersonioWorkplace(location, fullDesc);
            }
            return new RawAtsJob(
                    id,
                    AtsProviderType.PERSONIO,
                    board.slug(),
                    board.name(),
                    title,
                    location,
                    resolvedWorkplace,
                    publishedAt,
                    fullDesc,
                    applyUrl,
                    department,
                    null,
                    !fullDesc.isEmpty()
            );
        }
    }

    private static List<ParsedPosition> parseXmlPositions(CompanyBoard board, byte[] xmlBytes, String sourceUrl) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
            NodeList positionNodes = doc.getElementsByTagName("position");
            List<ParsedPosition> positions = new ArrayList<>(positionNodes.getLength());
            String baseDomain = resolveApplyHost(board, sourceUrl);

            for (int i = 0; i < positionNodes.getLength(); i++) {
                Node node = positionNodes.item(i);
                if (node instanceof Element position) {
                    ParsedPosition parsed = mapPosition(position, baseDomain);
                    if (parsed != null) {
                        positions.add(parsed);
                    }
                }
            }
            return positions;
        } catch (Exception e) {
            throw new AtsFetchException("Unreadable XML from Personio", e);
        }
    }

    private static String resolveApplyHost(CompanyBoard board, String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to board configuration.
        }
        if (board.host() != null && board.host().contains("personio.")) {
            return board.host().startsWith(board.slug() + ".")
                    ? board.host()
                    : board.slug() + "." + board.host();
        }
        return board.slug() + ".jobs.personio.com";
    }

    private static ParsedPosition mapPosition(Element position, String applyHost) {
        String id = directChildText(position, "id");
        String title = directChildText(position, "name");
        if (id == null || title == null) {
            return null;
        }
        String applyUrl = AtsSupport.validHttpsUrl("https://" + applyHost + "/job/" + AtsSupport.encodePathSegment(id));
        if (applyUrl == null) {
            return null;
        }

        List<String> offices = new ArrayList<>();
        String primaryOffice = directChildText(position, "office");
        if (primaryOffice != null) {
            offices.add(primaryOffice);
        }
        NodeList additionalNodes = position.getElementsByTagName("additionalOffices");
        for (int i = 0; i < additionalNodes.getLength(); i++) {
            if (additionalNodes.item(i) instanceof Element additional) {
                NodeList subOffices = additional.getElementsByTagName("office");
                for (int j = 0; j < subOffices.getLength(); j++) {
                    String extra = cleanText(subOffices.item(j).getTextContent());
                    if (extra != null) {
                        offices.add(extra);
                    }
                }
            }
        }
        String location = AtsSupport.joinUniqueParts(offices, " · ");
        Instant publishedAt = AtsSupport.parseTimestampString(directChildText(position, "createdAt"));
        String department = directChildText(position, "department");
        String schedule = directChildText(position, "schedule");
        String employmentType = directChildText(position, "employmentType");
        String seniority = directChildText(position, "seniority");
        String yearsOfExperience = directChildText(position, "yearsOfExperience");
        String keywords = directChildText(position, "keywords");

        List<String> descBlocks = new ArrayList<>();
        NodeList jdList = position.getElementsByTagName("jobDescription");
        for (int i = 0; i < jdList.getLength(); i++) {
            if (jdList.item(i) instanceof Element jd) {
                String sectionName = directChildText(jd, "name");
                String rawValue = directChildText(jd, "value");
                String cleanValue = HtmlTextCleaner.toPlainText(rawValue);
                if (!cleanValue.isEmpty()) {
                    if (sectionName != null && !sectionName.isBlank()) {
                        descBlocks.add(sectionName + "\n" + cleanValue);
                    } else {
                        descBlocks.add(cleanValue);
                    }
                }
            }
        }
        String description = String.join("\n\n", descBlocks);

        List<String> metaParts = new ArrayList<>();
        if (department != null) metaParts.add("Department: " + department);
        if (seniority != null) metaParts.add("Seniority: " + seniority);
        if (yearsOfExperience != null) metaParts.add("Experience: " + yearsOfExperience);
        if (employmentType != null) metaParts.add("Employment: " + employmentType);
        if (schedule != null) metaParts.add("Schedule: " + schedule);
        if (keywords != null) metaParts.add("Keywords: " + keywords);
        String metadataSuffix = String.join(" · ", metaParts);

        String workplaceType = inferPersonioWorkplace(
                (location == null ? "" : location) + " " + (schedule == null ? "" : schedule),
                description
        );

        return new ParsedPosition(
                id,
                title,
                location,
                workplaceType,
                publishedAt,
                description,
                metadataSuffix,
                department,
                applyUrl
        );
    }

    private static String inferPersonioWorkplace(String locationAndSchedule, String description) {
        String combined = ((locationAndSchedule == null ? "" : locationAndSchedule) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        if (combined.contains("hybrid") || combined.contains("híbrid") || combined.contains("hibrid")) {
            return "hybrid";
        }
        if (combined.contains("remote") || combined.contains("remoto") || combined.contains("teletrabajo")
                || combined.contains("mobil") || combined.contains("home office")) {
            return "remote";
        }
        return "onsite";
    }

    private static String directChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element el && tagName.equals(el.getTagName())) {
                return cleanText(el.getTextContent());
            }
        }
        return null;
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return null;
        }
        String stripped = raw.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
