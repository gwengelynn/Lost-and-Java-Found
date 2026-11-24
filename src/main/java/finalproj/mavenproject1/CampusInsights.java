package finalproj.mavenproject1;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only campus ops insights (aging, monthly totals, hotspots).
 * Does not change report / claim / inventory workflow.
 */
final class CampusInsights {

    record AgingItem(String kind, String id, String title, long daysOpen, String severity) {}

    record MonthSummary(
            String monthLabel,
            long lostFiled,
            long foundRegistered,
            long lostMatched,
            long returned,
            double avgDaysToReturn
    ) {}

    record LocationCount(String location, long count) {}

    private CampusInsights() {}

    /** Open Lost reports and Pending claims older than 3 days (7+ = critical). */
    static List<AgingItem> agingAlerts() {
        LocalDate today = LocalDate.now();
        List<AgingItem> list = new ArrayList<>();

        for (Report r : Report.loadOpenLostReports()) {
            LocalDate when = r.getDateSubmitted() != null ? r.getDateSubmitted() : r.getLostDate();
            if (when == null) continue;
            long days = ChronoUnit.DAYS.between(when, today);
            if (days < 3) continue;
            list.add(new AgingItem(
                    r.isOnHoldAwaitingInventory() ? "Lost (on hold)" : "Lost",
                    r.getDisplayId(),
                    r.isOnHoldAwaitingInventory()
                            ? r.getItemName() + " — waiting for inventory"
                            : r.getItemName(),
                    days,
                    days >= 7 ? "Critical" : "Watch"
            ));
        }
        // Matched Lost still waiting for the owner to claim (skip if a claim is already active).
        Report probe = new Report();
        probe.setUserId(0);
        for (Report r : probe.filterReports()) {
            if (!r.isLostReport()) continue;
            String s = r.getReportStatus() == null ? "" : r.getReportStatus().toUpperCase(Locale.ENGLISH);
            if (!"MATCHED".equals(s)) continue;
            if (Claim.hasActiveClaimForReport(r.getReportId())) continue;
            LocalDate when = r.getDateSubmitted() != null ? r.getDateSubmitted() : r.getLostDate();
            if (when == null) continue;
            long days = ChronoUnit.DAYS.between(when, today);
            if (days < 3) continue;
            boolean already = list.stream().anyMatch(a -> a.id().equals(r.getDisplayId()));
            if (already) continue;
            list.add(new AgingItem(
                    "Lost (matched)",
                    r.getDisplayId(),
                    r.getItemName() + " — awaiting claim",
                    days,
                    days >= 7 ? "Critical" : "Watch"
            ));
        }

        for (Claim c : Claim.viewAllClaims()) {
            String cs = c.getClaimStatus() == null ? "" : c.getClaimStatus().toUpperCase(Locale.ENGLISH);
            if (!"PENDING".equals(cs) && !"ON_HOLD".equals(cs)) continue;
            if (c.getClaimDate() == null) continue;
            long days = ChronoUnit.DAYS.between(c.getClaimDate(), today);
            if (days < 3) continue;
            list.add(new AgingItem(
                    "ON_HOLD".equals(cs) ? "Claim (hold)" : "Claim",
                    c.getDisplayId(),
                    c.getItemName(),
                    days,
                    days >= 7 ? "Critical" : "Watch"
            ));
        }

        list.sort(Comparator.comparingLong(AgingItem::daysOpen).reversed());
        return list;
    }

    static MonthSummary monthSummary(LocalDate anyDayInMonth) {
        LocalDate start = anyDayInMonth.withDayOfMonth(1);
        LocalDate endExclusive = start.plusMonths(1);
        String label = start.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                + " " + start.getYear();
        return summarize(label, start, endExclusive);
    }

    /**
     * Inclusive custom range for analytics date filters.
     * Null from/to = unbounded on that side (all time when both null).
     */
    static MonthSummary rangeSummary(LocalDate fromInclusive, LocalDate toInclusive) {
        String label = DateRanges.describe(DateRanges.Preset.CUSTOM, fromInclusive, toInclusive);
        LocalDate start = fromInclusive == null ? LocalDate.of(2000, 1, 1) : fromInclusive;
        LocalDate endExclusive = toInclusive == null
                ? LocalDate.of(2100, 1, 1)
                : toInclusive.plusDays(1);
        return summarize(label, start, endExclusive);
    }

    private static MonthSummary summarize(String label, LocalDate start, LocalDate endExclusive) {
        Report probe = new Report();
        probe.setUserId(0);
        List<Report> reports = probe.filterReports();

        long lostFiled = reports.stream()
                .filter(Report::isLostReport)
                .filter(r -> inRange(r.getDateSubmitted(), start, endExclusive))
                .count();

        long lostMatched = reports.stream()
                .filter(Report::isLostReport)
                .filter(r -> {
                    String s = r.getReportStatus() == null ? "" : r.getReportStatus().toUpperCase(Locale.ENGLISH);
                    // Lost lifecycle only — Found REGISTERED/RESOLVED is not a "lost match".
                    return "MATCHED".equals(s) || "CLAIMED".equals(s);
                })
                .filter(r -> inRange(r.getDateSubmitted(), start, endExclusive))
                .count();

        Inventory inv = new Inventory();
        long foundRegistered = inv.filterItems().stream()
                .filter(i -> inRange(i.getFoundDate(), start, endExclusive))
                .count();

        List<Claim> claims = Claim.viewAllClaims();
        List<Long> returnDays = new ArrayList<>();
        long returned = 0;
        for (Claim c : claims) {
            if (c.getReturnDate() == null || !inRange(c.getReturnDate(), start, endExclusive)) continue;
            // Only completed pickups (approved claims with an item_returns row).
            String status = c.getClaimStatus() == null ? "" : c.getClaimStatus().toUpperCase(Locale.ENGLISH);
            if (!"APPROVED".equals(status)) continue;
            returned++;
            // Strict approved→pickup: require decision_date (set on Approve).
            // Do NOT fall back to claim_date — that inflated averages with Pending wait
            // (e.g. filed in April, picked up in Nov → ~165 days).
            LocalDate approvedOn = c.getDecisionDate();
            if (approvedOn == null) continue;
            long days = ChronoUnit.DAYS.between(approvedOn, c.getReturnDate());
            if (days < 0) continue; // skip corrupt date pairs
            returnDays.add(days);
        }
        double avg = returnDays.isEmpty() ? Double.NaN
                : returnDays.stream().mapToLong(Long::longValue).average().orElse(Double.NaN);

        return new MonthSummary(label, lostFiled, foundRegistered, lostMatched, returned, avg);
    }

    /** Top Lost report locations for the month (heatmap-style bars). */
    static List<LocationCount> topLostLocations(LocalDate anyDayInMonth, int limit) {
        LocalDate start = anyDayInMonth.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return topLostLocations(start, end.minusDays(1), limit);
    }

    /** Inclusive from/to hotspot list for analytics date filters. */
    static List<LocationCount> topLostLocations(LocalDate fromInclusive, LocalDate toInclusive, int limit) {
        LocalDate start = fromInclusive == null ? LocalDate.of(2000, 1, 1) : fromInclusive;
        LocalDate endExclusive = toInclusive == null
                ? LocalDate.of(2100, 1, 1)
                : toInclusive.plusDays(1);
        // Key = lowercase merge key; value = display label + count (Library + library → one bar).
        Map<String, LocationCount> byKey = new LinkedHashMap<>();

        Report probe = new Report();
        probe.setUserId(0);
        for (Report r : probe.filterReports()) {
            if (!r.isLostReport()) continue;
            if (!inRange(r.getDateSubmitted(), start, endExclusive)) continue;
            String raw = r.getLostLocation();
            String label = (raw == null || raw.isBlank())
                    ? "(unspecified)"
                    : LocationNormalizer.display(raw);
            if (label == null || label.isBlank()) label = "(unspecified)";
            String mergeKey = "(unspecified)".equals(label) ? "(unspecified)" : LocationNormalizer.key(label);
            byKey.merge(mergeKey, new LocationCount(label, 1L),
                    (a, b) -> new LocationCount(a.location(), a.count() + b.count()));
        }

        return byKey.values().stream()
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .limit(Math.max(1, limit))
                .toList();
    }

    private static boolean inRange(LocalDate d, LocalDate startInclusive, LocalDate endExclusive) {
        return d != null && !d.isBefore(startInclusive) && d.isBefore(endExclusive);
    }
}
