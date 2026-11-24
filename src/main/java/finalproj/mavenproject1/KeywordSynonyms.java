package finalproj.mavenproject1;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Small Tagalog ↔ English keyword map for campus Lost & Found descriptions.
 * Not a full translator — expands shared meaning so MatchScorer can still
 * connect phrases like "sticker sa likod" and "sticker at the back".
 */
final class KeywordSynonyms {

    private static final Map<String, String> TO_CANONICAL = new HashMap<>();

    static {
        // Position / place
        group("back", "likod", "behind", "rear", "dorsal");
        group("front", "harap", "unahan", "forward");
        group("inside", "loob", "looban", "interior");
        group("outside", "labas", "exterior");
        group("left", "kaliwa");
        group("right", "kanan");
        group("top", "taas", "itaas");
        group("bottom", "baba", "ibaba");
        group("side", "gilid", "tabi");
        group("corner", "kanto", "sulok");

        // Colors (common student wording)
        group("red", "pula", "reddish");
        group("black", "itim", "dark");
        group("white", "puti");
        group("blue", "asul", "blu");
        group("yellow", "dilaw");
        group("green", "berde", "greenish");
        group("pink", "rosas", "pinkish");
        group("orange", "kahel");
        group("brown", "kayumanggi", "brownish");
        group("gray", "grey", "abo", "abuhin");
        group("gold", "ginto", "golden");
        group("silver", "pilak");

        // Condition / marks
        group("sticker", "stiker", "stickers", "decal");
        group("crack", "cracked", "basag", "basagbasag", "sira");
        group("scratch", "scratched", "gasgas", "gasgasgas");
        group("dent", "dented", "bakat", "pilas");
        group("broken", "sira", "sirang", "bust");
        group("new", "bago");
        group("old", "luma", "used");
        group("dirty", "marumi", "madumi");
        group("clean", "malinis");

        // Common items
        group("phone", "cellphone", "cell", "cp", "smartphone", "mobile");
        group("earphone", "earphones", "earbud", "earbuds", "headset", "headphone", "headphones");
        group("charger", "charging", "cable", "wire", "cord", "adapter");
        group("wallet", "pitaka", "dompet");
        group("bag", "bags", "backpack", "bayong");
        group("key", "keys", "susi", "keychain");
        group("id", "schoolid", "idcard", "identification");
        group("umbrella", "payong");
        group("tumbler", "bottle", "botelya", "waterbottle");
        group("laptop", "notebook", "macbook");
        group("tablet", "ipad");
        group("powerbank", "powerbanks", "baterya");
        group("fan", "electricfan");
        group("glasses", "eyeglasses", "salamin", "specs");
        group("watch", "orasan", "wristwatch");

        // Brands / campus cues students often mention
        group("jollibee", "jollybee", "jolibee");
        group("ue", "uecaloocan", "universityoftheeast");
    }

    private KeywordSynonyms() {}

    private static void group(String canonical, String... aliases) {
        String canon = canonical.toLowerCase(Locale.ENGLISH);
        TO_CANONICAL.put(canon, canon);
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) continue;
            TO_CANONICAL.put(alias.toLowerCase(Locale.ENGLISH), canon);
        }
    }

    /** Raw tokens (letters/digits, length ≥ 2) plus their canonical synonym forms. */
    static Set<String> expand(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Set<String> out = new HashSet<>();
        for (String raw : text.toLowerCase(Locale.ENGLISH).split("[^a-z0-9]+")) {
            if (raw.isEmpty()) continue;
            if (raw.length() < 2) continue;
            out.add(raw);
            String canon = TO_CANONICAL.get(raw);
            if (canon != null) out.add(canon);
        }
        return Collections.unmodifiableSet(out);
    }

    /** Count of shared meaning tokens after synonym expansion. */
    static int sharedMeaningCount(String a, String b) {
        Set<String> left = expand(a);
        Set<String> right = expand(b);
        if (left.isEmpty() || right.isEmpty()) return 0;
        int hits = 0;
        for (String t : left) {
            if (right.contains(t)) hits++;
        }
        return hits;
    }

    /** True when texts share a meaningful synonym-aware keyword. */
    static boolean sharesMeaning(String a, String b) {
        return sharedMeaningCount(a, b) > 0;
    }
}
