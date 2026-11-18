package io.github.garatech.jobsinton.providers.ats;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts raw or HTML-escaped ATS job descriptions into clean plain text for matching and preview. */
public final class HtmlTextCleaner {
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile(
            "(?is)<(script|style)\\b[^>]*>.*?</\\1>");
    private static final Pattern HTML_COMMENT = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern LIST_ITEM_OPEN = Pattern.compile("(?i)<li\\b[^>]*>");
    private static final Pattern BLOCK_BREAK = Pattern.compile(
            "(?i)(?:<br\\s*/?>|</(?:p|div|li|tr|h[1-6]|ul|ol|section|article|blockquote)>)");
    private static final Pattern HTML_TAG = Pattern.compile(
            "</?[a-zA-Z][a-zA-Z0-9:-]*(?:\\s[^<>]*?)?/?>");
    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z]{2,12});");
    private static final Pattern ENCODED_TAG_HINT = Pattern.compile("(?i)&lt;/?(?:p|div|ul|ol|li|br|span|h[1-6]|strong|em|a)\\b");

    private static final Map<String, String> NAMED_ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"),
            Map.entry("lt", "<"),
            Map.entry("gt", ">"),
            Map.entry("quot", "\""),
            Map.entry("apos", "'"),
            Map.entry("nbsp", " "),
            Map.entry("ndash", "\u2013"),
            Map.entry("mdash", "\u2014"),
            Map.entry("lsquo", "\u2018"),
            Map.entry("rsquo", "\u2019"),
            Map.entry("ldquo", "\u201C"),
            Map.entry("rdquo", "\u201D"),
            Map.entry("bull", "\u2022"),
            Map.entry("hellip", "\u2026"),
            Map.entry("euro", "\u20AC"),
            Map.entry("middot", "\u00B7"),
            Map.entry("aacute", "\u00E1"),
            Map.entry("eacute", "\u00E9"),
            Map.entry("iacute", "\u00ED"),
            Map.entry("oacute", "\u00F3"),
            Map.entry("uacute", "\u00FA"),
            Map.entry("ntilde", "\u00F1"),
            Map.entry("uuml", "\u00FC"),
            Map.entry("Aacute", "\u00C1"),
            Map.entry("Eacute", "\u00C9"),
            Map.entry("Iacute", "\u00CD"),
            Map.entry("Oacute", "\u00D3"),
            Map.entry("Uacute", "\u00DA"),
            Map.entry("Ntilde", "\u00D1"),
            Map.entry("iexcl", "\u00A1"),
            Map.entry("iquest", "\u00BF")
    );

    private HtmlTextCleaner() {}

    public static String toPlainText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String text = input;
        // Greenhouse frequently wraps HTML tags as entity-encoded strings (&lt;p&gt;...&lt;/p&gt;).
        if (text.contains("&")) {
            text = decodeEntities(text);
            if (ENCODED_TAG_HINT.matcher(text).find()) {
                text = decodeEntities(text);
            }
        }
        text = SCRIPT_OR_STYLE.matcher(text).replaceAll(" ");
        text = HTML_COMMENT.matcher(text).replaceAll(" ");
        text = LIST_ITEM_OPEN.matcher(text).replaceAll("\n\u2022 ");
        text = BLOCK_BREAK.matcher(text).replaceAll("\n");
        text = HTML_TAG.matcher(text).replaceAll(" ");
        if (text.contains("&")) {
            text = decodeEntities(text);
        }
        text = text.replace('\u00A0', ' ').replace("\r\n", "\n").replace('\r', '\n');
        // Collapse horizontal whitespace while keeping paragraph structure readable.
        String[] rawLines = text.split("\n", -1);
        StringBuilder out = new StringBuilder(text.length());
        int blankRun = 0;
        for (String rawLine : rawLines) {
            String line = rawLine.replaceAll("[ \\t\\f\\x0B]+", " ").strip();
            if (line.isEmpty()) {
                if (out.length() > 0 && blankRun == 0) {
                    out.append("\n\n");
                }
                blankRun++;
            } else {
                if (out.length() > 0 && blankRun == 0) {
                    out.append('\n');
                }
                out.append(line);
                blankRun = 0;
            }
        }
        return out.toString().strip();
    }

    public static String snippet(String plainText, int maxChars) {
        if (plainText == null || plainText.isBlank()) {
            return "";
        }
        String singleLine = plainText.replaceAll("\\s+", " ").strip();
        if (singleLine.length() <= maxChars) {
            return singleLine;
        }
        int cut = singleLine.lastIndexOf(' ', maxChars);
        if (cut < maxChars / 2) {
            cut = maxChars;
        }
        return singleLine.substring(0, cut).stripTrailing() + "\u2026";
    }

    static String decodeEntities(String value) {
        Matcher matcher = ENTITY.matcher(value);
        StringBuilder sb = new StringBuilder(value.length());
        while (matcher.find()) {
            String token = matcher.group(1);
            String replacement = resolveEntity(token);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String resolveEntity(String token) {
        if (token.startsWith("#x") || token.startsWith("#X")) {
            try {
                int codePoint = Integer.parseInt(token.substring(2), 16);
                return Character.isValidCodePoint(codePoint) ? new String(Character.toChars(codePoint)) : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (token.startsWith("#")) {
            try {
                int codePoint = Integer.parseInt(token.substring(1), 10);
                return Character.isValidCodePoint(codePoint) ? new String(Character.toChars(codePoint)) : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return NAMED_ENTITIES.get(token);
    }
}
