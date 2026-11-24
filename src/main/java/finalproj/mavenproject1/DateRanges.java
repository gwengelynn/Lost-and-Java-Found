package finalproj.mavenproject1;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Shared date-range helpers for list / analytics filters.
 * Additive only — does not change report/claim save logic.
 */
final class DateRanges {

    enum Preset {
        ALL,
        WEEK,
        MONTH,
        CUSTOM
    }

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private DateRanges() {}

    /** Inclusive from/to. Null bounds mean unbounded on that side. */
    static boolean includes(LocalDate date, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (date == null) return false;
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    static LocalDate[] boundsFor(Preset preset, LocalDate customFrom, LocalDate customTo) {
        LocalDate today = LocalDate.now();
        return switch (preset == null ? Preset.ALL : preset) {
            case ALL -> new LocalDate[]{null, null};
            case WEEK -> new LocalDate[]{today.minusDays(6), today};
            case MONTH -> new LocalDate[]{today.withDayOfMonth(1), today};
            case CUSTOM -> new LocalDate[]{customFrom, customTo};
        };
    }

    static String describe(Preset preset, LocalDate from, LocalDate to) {
        Preset p = preset == null ? Preset.ALL : preset;
        return switch (p) {
            case ALL -> "All dates";
            case WEEK -> "This week";
            case MONTH -> "This month";
            case CUSTOM -> {
                if (from == null && to == null) yield "Custom (all dates)";
                if (from != null && to != null) {
                    yield LABEL_FMT.format(from) + " – " + LABEL_FMT.format(to);
                }
                if (from != null) yield "From " + LABEL_FMT.format(from);
                yield "Until " + LABEL_FMT.format(to);
            }
        };
    }
}
