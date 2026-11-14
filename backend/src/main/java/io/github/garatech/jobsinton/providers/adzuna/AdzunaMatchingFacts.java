package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.matching.MatchingInput.Opportunity;
import io.github.garatech.jobsinton.matching.MatchingInput.Role;
import io.github.garatech.jobsinton.matching.MatchingInput.WorkMode;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

/** Recognises a small set of complete declarations in a single listing, never the search query. */
final class AdzunaMatchingFacts {
    private AdzunaMatchingFacts() {}

    private static final Pattern SPANISH_DAYS = Pattern.compile(
            "(?:se requieren? |asistencia obligatoria: )?(0|1|2|3|4|5|cero|un|uno|dos|tres|cuatro|cinco) "
            + "(?:dia presencial|dias presenciales|dias? en (?:la )?oficina) (?:por semana|a la semana)");
    private static final Pattern ENGLISH_DAYS = Pattern.compile(
            "(?:required attendance: )?(0|1|2|3|4|5|zero|one|two|three|four|five) "
            + "(?:office days?|days? (?:in the office|on site)) (?:per week|a week)(?: required)?");

    // An unrecognised schedule can qualify an otherwise clear statement elsewhere in the excerpt.
    private static final Pattern SCHEDULE_WORD = Pattern.compile(
            "\\b(?:presencial\\w*|oficinas?|teletrabaj\\w*|remot\\w*|hibrid\\w*|modalidad|flexib\\w*|"
            + "asistencia|opcional\\w*|excepto|temporalmente|optional|except|temporarily|only|solo|solamente|durante|during|seman\\w*|office|hybrid|on[ -]?site|attendance|weekly|weeks?)\\b");

    static Opportunity from(String title, String excerpt) {
        String normalizedTitle = normalize(title);
        Role role = role(normalizedTitle);
        var modes = EnumSet.noneOf(WorkMode.class);
        var counts = new HashSet<Integer>();
        boolean unclear = SCHEDULE_WORD.matcher(normalizedTitle).find();
        // Split only at a full stop, not a comma, an ellipsis or a line wrap inside a condition.
        for (String sentence : normalize(excerpt).split("(?<!\\.)\\.(?!\\.)(?:\\s+|$)")) {
            WorkMode mode = mode(sentence);
            Integer days = weeklyDays(sentence);
            if (mode != null) modes.add(mode);
            else if (days != null) counts.add(days);
            else if (SCHEDULE_WORD.matcher(sentence).find()) unclear = true;
        }
        boolean inconsistent = (modes.contains(WorkMode.REMOTE) && counts.stream().anyMatch(n -> n > 0))
                || (modes.contains(WorkMode.ONSITE) && counts.contains(0));
        if (unclear || modes.size() > 1 || counts.size() > 1 || inconsistent) {
            return new Opportunity(role, null, null, null, null, null, null);
        }
        return new Opportunity(role, null, null, modes.isEmpty() ? null : modes.iterator().next(),
                counts.isEmpty() ? null : counts.iterator().next(), null, null);
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
                .replace('í', 'i').replace('ó', 'o').replaceAll("(?U)\\s+", " ").strip();
    }

    private static Role role(String title) {
        return switch (title) {
            case "backend developer", "backend engineer", "desarrollador backend", "desarrolladora backend" -> Role.BACKEND;
            case "frontend developer", "frontend engineer", "desarrollador frontend", "desarrolladora frontend" -> Role.FRONTEND;
            case "full stack developer", "full-stack developer", "desarrollador full stack", "desarrolladora full stack" -> Role.FULL_STACK;
            case "sap developer", "desarrollador sap", "desarrolladora sap" -> Role.SAP;
            case "data engineer", "ingeniero de datos", "ingeniera de datos" -> Role.DATA;
            case "platform engineer", "ingeniero de plataforma", "ingeniera de plataforma" -> Role.PLATFORM;
            default -> null;
        };
    }

    private static WorkMode mode(String sentence) {
        return switch (sentence) {
            case "modalidad: 100% remota", "trabajo 100% remoto", "work mode: fully remote" -> WorkMode.REMOTE;
            case "modalidad: hibrida", "trabajo hibrido", "work mode: hybrid" -> WorkMode.HYBRID;
            case "modalidad: presencial", "trabajo presencial", "work mode: on-site" -> WorkMode.ONSITE;
            default -> null;
        };
    }

    private static Integer weeklyDays(String sentence) {
        var match = SPANISH_DAYS.matcher(sentence);
        if (!match.matches()) {
            match = ENGLISH_DAYS.matcher(sentence);
            if (!match.matches()) return null;
        }
        return switch (match.group(1)) {
            case "0", "cero", "zero" -> 0;
            case "1", "un", "uno", "one" -> 1;
            case "2", "dos", "two" -> 2;
            case "3", "tres", "three" -> 3;
            case "4", "cuatro", "four" -> 4;
            case "5", "cinco", "five" -> 5;
            default -> throw new IllegalStateException("Unexpected weekly day count");
        };
    }
}
