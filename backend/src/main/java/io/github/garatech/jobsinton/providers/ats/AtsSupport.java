package io.github.garatech.jobsinton.providers.ats;

import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared parsing, URL validation, date normalization, and workplace inference helpers for ATS adapters. */
final class AtsSupport {
    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("es", "Spain"),
            Map.entry("de", "Germany"),
            Map.entry("fr", "France"),
            Map.entry("gb", "United Kingdom"),
            Map.entry("uk", "United Kingdom"),
            Map.entry("pt", "Portugal"),
            Map.entry("it", "Italy"),
            Map.entry("nl", "Netherlands"),
            Map.entry("ie", "Ireland"),
            Map.entry("pl", "Poland"),
            Map.entry("ch", "Switzerland"),
            Map.entry("be", "Belgium"),
            Map.entry("at", "Austria"),
            Map.entry("se", "Sweden"),
            Map.entry("dk", "Denmark"),
            Map.entry("fi", "Finland"),
            Map.entry("no", "Norway"),
            Map.entry("cz", "Czechia"),
            Map.entry("ro", "Romania"),
            Map.entry("hu", "Hungary"),
            Map.entry("gr", "Greece"),
            Map.entry("bg", "Bulgaria"),
            Map.entry("hr", "Croatia"),
            Map.entry("ee", "Estonia"),
            Map.entry("lt", "Lithuania"),
            Map.entry("lv", "Latvia"),
            Map.entry("si", "Slovenia"),
            Map.entry("sk", "Slovakia"),
            Map.entry("lu", "Luxembourg"),
            Map.entry("mt", "Malta"),
            Map.entry("cy", "Cyprus"),
            Map.entry("is", "Iceland"),
            Map.entry("rs", "Serbia"),
            Map.entry("us", "United States"),
            Map.entry("ca", "Canada"),
            Map.entry("br", "Brazil"),
            Map.entry("mx", "Mexico"),
            Map.entry("ar", "Argentina"),
            Map.entry("co", "Colombia"),
            Map.entry("cl", "Chile"),
            Map.entry("pe", "Peru"),
            Map.entry("in", "India"),
            Map.entry("sg", "Singapore"),
            Map.entry("jp", "Japan"),
            Map.entry("kr", "South Korea"),
            Map.entry("cn", "China"),
            Map.entry("au", "Australia"),
            Map.entry("nz", "New Zealand"),
            Map.entry("il", "Israel"),
            Map.entry("ae", "United Arab Emirates"),
            Map.entry("za", "South Africa")
    );

    private static final java.util.regex.Pattern EXPLICIT_NON_EU_RESIDENCY_LOCK_PATTERN = java.util.regex.Pattern.compile(
            "\\b(?:u\\.?s\\.?\\s+only|u\\.?s\\.?a\\.?\\s+only|united states only|canada only|north america only"
                    + "|u\\.?k\\.?\\s+only|united kingdom only|latam only|apac only|india only|australia only"
                    + "|remote\\s*[-–(/,:|]+\\s*(?:u\\.?s\\.?a?\\.?|united states|canada|ca|latam|apac|india|brazil|mexico|australia)\\b"
                    + "|must reside in (?:the )?(?:u\\.?s\\.?|united states|canada)"
                    + "|authorized to work in (?:the )?(?:u\\.?s\\.?|united states|canada))\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    private static final java.util.regex.Pattern US_CA_STATE_CODE_AFTER_COMMA_PATTERN = java.util.regex.Pattern.compile(
            ",\\s*(?:AL|AK|AZ|AR|CA|CO|CT|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|WY|DC|ON|BC|QC|AB|MB|SK|NS|NB)(?:\\b|,|\\s|-)"
    );

    private static final java.util.regex.Pattern NON_EUROPE_LOCATION_PATTERN = java.util.regex.Pattern.compile(
            "\\b(?:united states|u\\.?s\\.?a\\.?|north america|canada|toronto|vancouver|montreal|montréal|ottawa|calgary|edmonton|waterloo|winnipeg|quebec|québec|halifax|ontario|british columbia|alberta"
                    + "|san francisco|new york|nyc|seattle|austin|boston|chicago|los angeles|denver|atlanta|miami|dallas|houston|palo alto|mountain view|menlo park|sunnyvale|santa clara|san jose|san mateo|redwood city|cupertino|san diego|irvine|phoenix|scottsdale|salt lake city|portland|minneapolis|detroit|philadelphia|pittsburgh|washington,?\\s*d\\.?c\\.?|arlington|reston|mclean|tysons|raleigh|durham|charlotte|nashville|tampa|orlando|columbus|cincinnati|cleveland|indianapolis|st\\.?\\s*louis|kansas city|milwaukee|baltimore|newark|jersey city|hoboken|stamford|boulder|las vegas|sacramento|oakland|berkeley|bellevue|redmond|kirkland|silicon valley"
                    + "|latam|latin america|brazil|brasil|s[aã]o paulo|rio de janeiro|belo horizonte|curitiba|mexico|m[eé]xico|ciudad de m[eé]xico|cdmx|monterrey|argentina|buenos aires|colombia|bogot[aá]|medell[ií]n|chile|peru|per[uú]|lima|costa rica|uruguay|montevideo"
                    + "|apac|asia pacific|india|bangalore|bengaluru|hyderabad|pune|mumbai|gurugram|gurgaon|noida|chennai|new delhi|delhi|kolkata|ahmedabad|kochi|singapore|japan|tokyo|osaka|south korea|seoul|china|beijing|shanghai|shenzhen|hong kong|taiwan|taipei|malaysia|kuala lumpur|thailand|bangkok|vietnam|ho chi minh|hanoi|philippines|manila|indonesia|jakarta"
                    + "|australia|sydney|melbourne|brisbane|perth|new zealand|auckland"
                    + "|israel|tel aviv|united arab emirates|dubai|abu dhabi|saudi arabia|riyadh|south africa|cape town|johannesburg|egypt|cairo"
                    + "|pst|pdt|est|edt|cst|cdt|mst|mdt)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    private static final java.util.regex.Pattern EUROPE_OR_SPAIN_SIGNAL_PATTERN = java.util.regex.Pattern.compile(
            "\\b(?:spain|espa[nñ]a|iberia|madrid|barcelona|valencia|m[aá]laga|sevilla|seville|bilbao|zaragoza|pozuelo|las rozas|alcobendas|tres cantos|getafe|boadilla|alicante|coru[nñ]a|vigo|granada|oviedo|valladolid|san sebasti[aá]n|murcia|castell[oó]n|galicia|asturias|catalonia|catalunya|andalusia|andaluc[ií]a|basque|euskadi|mallorca|palma|tenerife|canarias|pamplona|santander|logro[nñ]o|burgos|salamanca|c[oó]rdoba|c[aá]diz|gij[oó]n|le[oó]n|lleida|girona|tarragona|elche|santiago de compostela"
                    + "|europe|european union|eu|emea|cet|cest|dach|nordics|benelux|worldwide|anywhere"
                    + "|germany|deutschland|berlin|munich|m[üu]nchen|hamburg|frankfurt|stuttgart|cologne|k[öo]ln|d[üu]sseldorf|leipzig|dresden|karlsruhe|nuremberg|n[üu]rnberg|walldorf|bonn|hannover"
                    + "|france|paris|lyon|toulouse|nantes|bordeaux|lille|marseille|nice|sophia antipolis|grenoble|rennes|strasbourg|montpellier"
                    + "|portugal|lisbon|lisboa|porto|braga|coimbra|aveiro|faro"
                    + "|italy|italia|milan|milano|rome|roma|turin|torino|bologna|florence|firenze|naples|napoli|padua|padova|genoa|verona|bari"
                    + "|netherlands|holland|amsterdam|rotterdam|utrecht|eindhoven|the hague|den haag|delft|groningen"
                    + "|ireland|dublin|cork|galway|limerick"
                    + "|poland|polska|warsaw|warszawa|krakow|krak[óo]w|cracow|wroclaw|wroc[łl]aw|gdansk|gda[ńn]sk|poznan|pozna[ńn]|katowice|lodz"
                    + "|sweden|sverige|stockholm|gothenburg|g[öo]teborg|malm[öo]|uppsala|lund"
                    + "|austria|[öo]sterreich|vienna|wien|graz|linz|salzburg|innsbruck"
                    + "|belgium|brussels|bruxelles|antwerp|ghent|leuven"
                    + "|switzerland|schweiz|suisse|zurich|z[üu]rich|geneva|gen[èe]ve|basel|lausanne|bern|zug"
                    + "|denmark|copenhagen|k[øo]benhavn|aarhus|odense"
                    + "|finland|helsinki|espoo|tampere|oulu|turku"
                    + "|norway|oslo|bergen|trondheim|stavanger"
                    + "|czech|czechia|prague|praha|brno"
                    + "|romania|bucharest|bucure[șs]ti|cluj|timisoara|iasi"
                    + "|hungary|budapest|debrecen"
                    + "|greece|athens|thessaloniki"
                    + "|bulgaria|sofia|plovdiv|varna"
                    + "|croatia|zagreb|split"
                    + "|estonia|tallinn|tartu"
                    + "|lithuania|vilnius|kaunas"
                    + "|latvia|riga"
                    + "|slovenia|ljubljana"
                    + "|slovakia|bratislava|kosice"
                    + "|luxembourg|malta|cyprus|nicosia|limassol|iceland|reykjavik|serbia|belgrade"
                    + "|united kingdom|great britain|england|scotland|uk|london|manchester|edinburgh|bristol|cambridge|oxford|birmingham|leeds|glasgow|belfast)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE
    );

    private AtsSupport() {}

    static String text(JsonNode node) {
        if (node == null || !node.isString()) {
            return null;
        }
        String value = node.asString().strip();
        return value.isEmpty() ? null : value;
    }

    static String idText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isString()) {
            return text(node);
        }
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        return null;
    }

    static boolean boolFlag(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isString()) {
            return "true".equalsIgnoreCase(node.asString().strip());
        }
        return false;
    }

    static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String validHttpsUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(raw.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getRawUserInfo() != null) {
                return null;
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static Instant parseTimestamp(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            long value = node.asLong();
            if (value <= 0) {
                return null;
            }
            // Distinguish seconds vs milliseconds cleanly.
            return value > 100_000_000_000L ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
        }
        return parseTimestampString(text(node));
    }

    static Instant parseTimestampString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.strip();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Fall through to offset/local formats.
        }
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // Fall through.
        }
        try {
            return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // Fall through.
        }
        try {
            return LocalDate.parse(trimmed).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
            // Fall through to RFC-1123 (used by Teamtailor RSS <pubDate>).
        }
        try {
            return java.time.ZonedDateTime.parse(trimmed, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    static String normalizeWorkplaceType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String lower = raw.strip().toLowerCase(Locale.ROOT);
        if ("fully".equals(lower) || "temporary".equals(lower)
                || lower.contains("remote") || lower.contains("telecommut") || lower.contains("remoto")
                || lower.contains("teletrabajo") || lower.contains("mobil")) {
            return lower.contains("hybrid") || lower.contains("híbrid") || lower.contains("hibrid") ? "hybrid" : "remote";
        }
        if (lower.contains("hybrid") || lower.contains("híbrid") || lower.contains("hibrid")) {
            return "hybrid";
        }
        if ("none".equals(lower) || lower.contains("onsite") || lower.contains("on-site") || lower.contains("on_site")
                || lower.contains("in-office") || lower.contains("office") || lower.contains("presencial")) {
            return "onsite";
        }
        return null;
    }

    static String inferWorkplaceFromLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String lower = location.toLowerCase(Locale.ROOT);
        if (lower.contains("remote") || lower.contains("remoto") || lower.contains("teletrabajo")) {
            return lower.contains("hybrid") || lower.contains("híbrid") ? "hybrid" : "remote";
        }
        if (lower.contains("hybrid") || lower.contains("híbrid") || lower.contains("hibrid")) {
            return "hybrid";
        }
        return null;
    }

    static boolean isSpainOrEuLocation(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("cartagena") && !lower.contains("colombia")) {
            return true;
        }
        return EUROPE_OR_SPAIN_SIGNAL_PATTERN.matcher(text).find();
    }

    static boolean isNonEuFakeRemote(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return EXPLICIT_NON_EU_RESIDENCY_LOCK_PATTERN.matcher(text).find()
                || US_CA_STATE_CODE_AFTER_COMMA_PATTERN.matcher(text).find()
                || NON_EUROPE_LOCATION_PATTERN.matcher(text).find();
    }

    /**
     * Determines whether an ATS posting is eligible for a developer based in Spain or Europe.
     *
     * <ul>
     *   <li>Postings explicitly located in Spain or any European country/city, or tagged {@code Europe}, {@code EU},
     *       {@code EMEA}, {@code CET}, {@code CEST}, {@code Worldwide}, or {@code Anywhere} (without a US/CA-only lock)
     *       are always kept.</li>
     *   <li>Postings explicitly restricted to North America (US/Canada), LATAM, APAC, India, Australia, or other
     *       non-European regions with no European location option are discarded at ingestion.</li>
     *   <li>Postings with neutral/unlabeled locations or general remote tags without non-EU residency locks are kept.</li>
     * </ul>
     */
    static boolean isEuropeEligibleJob(RawAtsJob job) {
        if (job == null) {
            return false;
        }
        String loc = job.location() == null ? "" : job.location();
        String title = job.title() == null ? "" : job.title();
        String locAndTitle = (loc + " " + title).strip();
        String descHead = job.description() == null
                ? ""
                : job.description().substring(0, Math.min(600, job.description().length()));

        boolean hasExplicitNonEuLock = EXPLICIT_NON_EU_RESIDENCY_LOCK_PATTERN.matcher(locAndTitle).find()
                || EXPLICIT_NON_EU_RESIDENCY_LOCK_PATTERN.matcher(descHead).find();

        boolean hasEuropeOrSpainSignal = isSpainOrEuLocation(locAndTitle);
        if (hasEuropeOrSpainSignal) {
            // If a role is in London/UK and explicitly states "UK only", or says "US only", reject it;
            // otherwise multi-hub roles that include a European hub (e.g. "Madrid / New York" or "Lisbon / Austin") are eligible.
            if (hasExplicitNonEuLock && !isSpainLocationOnly(locAndTitle)) {
                return false;
            }
            return true;
        }

        if (hasExplicitNonEuLock) {
            return false;
        }
        if (US_CA_STATE_CODE_AFTER_COMMA_PATTERN.matcher(locAndTitle).find()) {
            return false;
        }
        if (NON_EUROPE_LOCATION_PATTERN.matcher(locAndTitle).find()) {
            return false;
        }
        return true;
    }

    private static boolean isSpainLocationOnly(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("spain") || lower.contains("españa") || lower.contains("espana")
                || lower.contains("madrid") || lower.contains("barcelona") || lower.contains("valencia")
                || lower.contains("málaga") || lower.contains("malaga") || lower.contains("sevilla")
                || lower.contains("bilbao") || lower.contains("zaragoza");
    }

    static String countryDisplayName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.strip();
        if (trimmed.length() == 2) {
            return COUNTRY_NAMES.getOrDefault(trimmed.toLowerCase(Locale.ROOT), trimmed.toUpperCase(Locale.ROOT));
        }
        return trimmed;
    }

    static String joinUniqueParts(List<String> parts, String delimiter) {
        var unique = new LinkedHashSet<String>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                unique.add(part.strip());
            }
        }
        return unique.isEmpty() ? null : String.join(delimiter, unique);
    }
}
