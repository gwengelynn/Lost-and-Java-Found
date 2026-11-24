package finalproj.mavenproject1;

import java.util.Locale;

/**
 * Campus location text helpers — collapse whitespace / case so
 * "Library" and "library" count as one hotspot.
 */
final class LocationNormalizer {

    private LocationNormalizer() {}

    /** Key used to merge duplicates (lowercase, single spaces). */
    static String key(String location) {
        if (location == null) return "";
        return location.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
    }

    /**
     * Title-case display form for storage and charts.
     * Blank / null → null. "(unspecified)" left alone when passed as that token.
     */
    static String display(String location) {
        if (location == null) return null;
        String trimmed = location.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) return null;
        if ("(unspecified)".equalsIgnoreCase(trimmed)) return "(unspecified)";

        String[] parts = trimmed.toLowerCase(Locale.ENGLISH).split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.toString();
    }
}
