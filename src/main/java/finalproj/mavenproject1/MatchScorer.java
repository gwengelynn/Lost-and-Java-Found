package finalproj.mavenproject1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ranks inventory candidates against a lost report with a confidence score + reasons.
 * Suggested matches use a 70%–100% confidence window only.
 */
final class MatchScorer {

    /** Minimum confidence for automatic / suggested potential matches. */
    static final int MIN_SUGGEST_SCORE = 70;
    /** Maximum confidence (scores are capped here). */
    static final int MAX_SUGGEST_SCORE = 100;

    record ScoredMatch(Inventory item, int score, List<String> reasons) {
        String scoreLabel() {
            return score + "%";
        }

        String confidenceWord() {
            if (score >= 90) return "Strong";
            if (score >= 80) return "Good";
            if (score >= MIN_SUGGEST_SCORE) return "Possible";
            return "Weak";
        }

        /** True when this score is in the auto-suggest window (70–100). */
        boolean isSuggestedMatch() {
            return score >= MIN_SUGGEST_SCORE && score <= MAX_SUGGEST_SCORE;
        }

        String reasonLine() {
            if (reasons == null || reasons.isEmpty()) return "Overlapping details";
            return String.join(" · ", reasons);
        }
    }

    private MatchScorer() {}

    /** Score a report against its existing candidate list (from findPossibleMatches). */
    static List<ScoredMatch> rank(Report report, List<Inventory> candidates) {
        if (report == null || candidates == null || candidates.isEmpty()) return List.of();
        List<ScoredMatch> scored = new ArrayList<>();
        for (Inventory item : candidates) {
            ScoredMatch m = score(report, item);
            if (m.isSuggestedMatch()) scored.add(m);
        }
        scored.sort(Comparator.comparingInt(ScoredMatch::score).reversed()
                .thenComparing(m -> m.item().getItemName(), String.CASE_INSENSITIVE_ORDER));
        return scored;
    }

    static ScoredMatch score(Report report, Inventory item) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (eq(report.getCategory(), item.getCategory())) {
            score += 25;
            reasons.add("Same category");
        }
        if (eq(report.getSubCategory(), item.getSubCategory())) {
            score += 15;
            reasons.add("Same subcategory");
        }
        if (eq(report.getColor(), item.getColor())) {
            score += 20;
            reasons.add("Same color");
        } else if (containsEither(report.getColor(), item.getColor())) {
            score += 10;
            reasons.add("Similar color");
        }
        if (eq(report.getBrand(), item.getBrand())) {
            score += 20;
            reasons.add("Same brand");
        } else if (containsEither(report.getBrand(), item.getBrand())) {
            score += 10;
            reasons.add("Similar brand");
        }
        if (nameOverlap(report.getItemName(), item.getItemName())) {
            score += 20;
            reasons.add("Name overlap");
        }
        if (containsEither(report.getLostLocation(), item.getStorageLocation())
                || containsEither(report.getLostLocation(), item.getDescription())) {
            score += 8;
            reasons.add("Location hint");
        }

        // Description / private-details overlap with Tagalog↔English synonym help.
        int meaningHits = KeywordSynonyms.sharedMeaningCount(
                joinText(report.getDescription(), report.getItemName(), report.getBrand(), report.getColor()),
                joinText(item.getDescription(), item.getPrivateDetails(), item.getItemName(),
                        item.getBrand(), item.getColor()));
        if (meaningHits >= 3) {
            score += 18;
            reasons.add("Description keywords match");
        } else if (meaningHits == 2) {
            score += 12;
            reasons.add("Related description keywords");
        } else if (meaningHits == 1) {
            score += 6;
            reasons.add("Shared keyword hint");
        }

        if (score > MAX_SUGGEST_SCORE) score = MAX_SUGGEST_SCORE;
        if (reasons.isEmpty()) reasons.add("Partial overlap");
        return new ScoredMatch(item, score, reasons);
    }

    private static String joinText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.trim());
        }
        return sb.toString();
    }

    static List<ScoredMatch> rankForReport(Report report) {
        if (report == null) return List.of();
        return rank(report, report.findPossibleMatches());
    }

    /**
     * After a Found item is logged, find open Lost reports that look like the same item
     * so admin can verify Lost ↔ Found before notifying the owner.
     */
    static List<LostHit> rankLostReportsForItem(Inventory foundItem) {
        if (foundItem == null) return List.of();
        List<LostHit> hits = new ArrayList<>();
        for (Report lost : Report.loadOpenLostReports()) {
            ScoredMatch scored = score(lost, foundItem);
            if (scored.isSuggestedMatch()) {
                hits.add(new LostHit(lost, scored.score(), scored.reasons()));
            }
        }
        hits.sort(Comparator.comparingInt(LostHit::score).reversed());
        return hits;
    }

    record LostHit(Report report, int score, List<String> reasons) {
        String scoreLabel() { return score + "%"; }
        String confidenceWord() {
            if (score >= 90) return "Strong";
            if (score >= 80) return "Good";
            if (score >= MIN_SUGGEST_SCORE) return "Possible";
            return "Weak";
        }
        String reasonLine() {
            return reasons == null || reasons.isEmpty() ? "Overlapping details" : String.join(" · ", reasons);
        }
    }

    static String topSummary(Report report) {
        List<ScoredMatch> ranked = rankForReport(report);
        if (ranked.isEmpty()) return "No scored candidates";
        ScoredMatch top = ranked.get(0);
        return top.scoreLabel() + " " + top.confidenceWord().toLowerCase(Locale.ENGLISH)
                + " · " + top.item().getTrackingNumber()
                + " · " + top.reasonLine();
    }

    static String formatList(List<ScoredMatch> ranked) {
        if (ranked == null || ranked.isEmpty()) return "No inventory matches found.";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (ScoredMatch m : ranked) {
            Inventory item = m.item();
            sb.append(i++).append(". ").append(m.scoreLabel())
                    .append(" — ").append(m.confidenceWord())
                    .append("\n   ").append(item.getTrackingNumber())
                    .append(" · ").append(item.getItemName())
                    .append(" (").append(nullSafe(item.getColor()))
                    .append(" / ").append(nullSafe(item.getBrand())).append(")")
                    .append("\n   Why: ").append(m.reasonLine())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

    private static boolean eq(String a, String b) {
        if (blank(a) || blank(b)) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean containsEither(String a, String b) {
        if (blank(a) || blank(b)) return false;
        String x = a.trim().toLowerCase(Locale.ENGLISH);
        String y = b.trim().toLowerCase(Locale.ENGLISH);
        return x.contains(y) || y.contains(x);
    }

    private static boolean nameOverlap(String a, String b) {
        if (blank(a) || blank(b)) return false;
        String x = a.trim().toLowerCase(Locale.ENGLISH);
        String y = b.trim().toLowerCase(Locale.ENGLISH);
        if (x.equals(y) || x.contains(y) || y.contains(x)) return true;
        for (String token : x.split("[^a-z0-9]+")) {
            if (token.length() < 3) continue;
            if (y.contains(token)) return true;
        }
        // Synonym-aware: "cellphone" ↔ "phone", "payong" ↔ "umbrella", etc.
        return KeywordSynonyms.sharesMeaning(a, b);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullSafe(String s) {
        return blank(s) ? "—" : s;
    }
}
