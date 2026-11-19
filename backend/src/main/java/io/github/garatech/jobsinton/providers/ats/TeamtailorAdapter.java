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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Teamtailor public RSS/XML job feed adapter supporting both `*.teamtailor.com` slugs
 * and custom career domains (`empleo.civitatis.com`, `careers.pccomponentes.com`, etc.).
 * Endpoint: GET https://{domain_or_slug}/jobs.rss
 */
public final class TeamtailorAdapter implements AtsProviderAdapter {
    private static final Pattern JOB_ID_IN_LINK = Pattern.compile("/jobs/(\\d+)");

    private final AtsHttpClient http;

    public TeamtailorAdapter(AtsHttpClient http) {
        this.http = http;
    }

    @Override
    public AtsProviderType type() {
        return AtsProviderType.TEAMTAILOR;
    }

    @Override
    public List<RawAtsJob> fetch(CompanyBoard board) {
        List<String> urls = buildCandidateUrls(board);
        var reply = http.get(URI.create(urls.get(0)));
        if (reply.status() != 200) {
            boolean recovered = false;
            for (int i = 1; i < urls.size(); i++) {
                var fallbackReply = http.get(URI.create(urls.get(i)));
                if (fallbackReply.status() == 200) {
                    reply = fallbackReply;
                    recovered = true;
                    break;
                }
            }
            if (!recovered) {
                throw new AtsFetchException("HTTP " + reply.status() + " from Teamtailor");
            }
        }
        return parseRss(board, reply.body());
    }

    private static List<String> buildCandidateUrls(CompanyBoard board) {
        Set<String> urls = new LinkedHashSet<>();
        if (board.apiEndpoint() != null && AtsSupport.validHttpsUrl(board.apiEndpoint()) != null) {
            urls.add(board.apiEndpoint());
        }
        if (board.fallbackEndpoint() != null && AtsSupport.validHttpsUrl(board.fallbackEndpoint()) != null) {
            urls.add(board.fallbackEndpoint());
        }
        if (board.host() != null && !board.host().isBlank()) {
            urls.add("https://" + board.host().strip() + "/jobs.rss");
        }
        String slug = board.slug();
        if (slug.contains(".")) {
            urls.add("https://" + slug + "/jobs.rss");
        } else {
            urls.add("https://" + AtsSupport.encodePathSegment(slug) + ".teamtailor.com/jobs.rss");
        }
        return List.copyOf(urls);
    }

    private static List<RawAtsJob> parseRss(CompanyBoard board, byte[] rssBytes) {
        if (rssBytes == null || rssBytes.length == 0) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(rssBytes));
            NodeList items = doc.getElementsByTagName("item");
            List<RawAtsJob> jobs = new ArrayList<>(items.getLength());
            for (int i = 0; i < items.getLength(); i++) {
                if (items.item(i) instanceof Element item) {
                    RawAtsJob job = mapItem(board, item);
                    if (job != null) {
                        jobs.add(job);
                    }
                }
            }
            return jobs;
        } catch (Exception e) {
            throw new AtsFetchException("Unreadable RSS XML from Teamtailor", e);
        }
    }

    private static RawAtsJob mapItem(CompanyBoard board, Element item) {
        String title = childByLocalName(item, "title");
        String link = AtsSupport.validHttpsUrl(childByLocalName(item, "link"));
        String guid = childByLocalName(item, "guid");
        if (title == null || link == null) {
            return null;
        }

        String id = null;
        Matcher matcher = JOB_ID_IN_LINK.matcher(link);
        if (matcher.find()) {
            id = matcher.group(1);
        }
        if (id == null) {
            id = guid != null ? guid : link;
        }

        Instant publishedAt = AtsSupport.parseTimestampString(childByLocalName(item, "pubDate"));
        String remoteStatus = childByLocalName(item, "remoteStatus");
        String workplaceType = AtsSupport.normalizeWorkplaceType(remoteStatus);
        String location = extractLocations(item);
        if (workplaceType == null) {
            workplaceType = AtsSupport.inferWorkplaceFromLocation(location);
        }

        String department = childByLocalName(item, "department");
        String role = childByLocalName(item, "role");
        if (department == null) {
            department = role;
        } else if (role != null && !role.equalsIgnoreCase(department)) {
            department = department + " · " + role;
        }

        String rawDesc = childByLocalName(item, "description");
        String description = HtmlTextCleaner.toPlainText(rawDesc);
        if (department != null && !description.contains(department)) {
            description = description.isEmpty() ? department : description + "\n\nDepartment: " + department;
        }

        return new RawAtsJob(
                id,
                AtsProviderType.TEAMTAILOR,
                board.slug(),
                board.name(),
                title,
                location,
                workplaceType,
                publishedAt,
                description,
                link,
                department,
                null,
                !description.isEmpty()
        );
    }

    private static String extractLocations(Element item) {
        List<String> parts = new ArrayList<>();
        NodeList children = item.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }
            String local = localName(el);
            if ("locations".equals(local)) {
                NodeList locNodes = el.getChildNodes();
                for (int j = 0; j < locNodes.getLength(); j++) {
                    if (locNodes.item(j) instanceof Element locEl && "location".equals(localName(locEl))) {
                        String formatted = formatLocationElement(locEl);
                        if (formatted != null) {
                            parts.add(formatted);
                        }
                    }
                }
            } else if ("location".equals(local)) {
                String formatted = formatLocationElement(el);
                if (formatted != null) {
                    parts.add(formatted);
                }
            }
        }
        return AtsSupport.joinUniqueParts(parts, " · ");
    }

    private static String formatLocationElement(Element locEl) {
        String name = childByLocalName(locEl, "name");
        String city = childByLocalName(locEl, "city");
        String country = AtsSupport.countryDisplayName(childByLocalName(locEl, "country"));
        List<String> sub = new ArrayList<>();
        if (city != null) {
            sub.add(city);
        } else if (name != null) {
            sub.add(name);
        }
        if (country != null) {
            sub.add(country);
        }
        if (!sub.isEmpty()) {
            return AtsSupport.joinUniqueParts(sub, ", ");
        }
        return cleanText(locEl.getTextContent());
    }

    private static String childByLocalName(Element parent, String targetLocalName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element el && targetLocalName.equals(localName(el))) {
                return cleanText(el.getTextContent());
            }
        }
        return null;
    }

    private static String localName(Element el) {
        String local = el.getLocalName();
        if (local != null) {
            return local;
        }
        String tag = el.getTagName();
        int colon = tag.indexOf(':');
        return colon >= 0 ? tag.substring(colon + 1) : tag;
    }

    private static String cleanText(String raw) {
        if (raw == null) {
            return null;
        }
        String stripped = raw.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
