package finalproj.mavenproject1;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory login rate limit (per username/email key on this PC).
 * After too many failures, further attempts must wait.
 */
final class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final int LOCK_SECONDS = 60;

    private record Bucket(int failures, long lockedUntilMs) {}

    private static final Map<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

    private LoginThrottle() {}

    static String normalizeKey(String loginKey, String role) {
        String key = loginKey == null ? "" : loginKey.trim().toLowerCase();
        String r = role == null ? "" : role.trim().toUpperCase();
        return r + "|" + key;
    }

    /** Seconds remaining in lock, or 0 if allowed. */
    static int secondsRemaining(String loginKey, String role) {
        purgeExpired();
        Bucket b = BUCKETS.get(normalizeKey(loginKey, role));
        if (b == null) return 0;
        long left = b.lockedUntilMs() - System.currentTimeMillis();
        if (left <= 0) return 0;
        return (int) Math.ceil(left / 1000.0);
    }

    static boolean isLocked(String loginKey, String role) {
        return secondsRemaining(loginKey, role) > 0;
    }

    static void recordFailure(String loginKey, String role) {
        String key = normalizeKey(loginKey, role);
        BUCKETS.compute(key, (k, old) -> {
            int fails = (old == null ? 0 : old.failures()) + 1;
            long lockUntil = 0L;
            if (fails >= MAX_FAILURES) {
                lockUntil = System.currentTimeMillis() + LOCK_SECONDS * 1000L;
                fails = 0; // reset after lock starts
            }
            return new Bucket(fails, Math.max(lockUntil, old == null ? 0L : old.lockedUntilMs()));
        });
    }

    static void recordSuccess(String loginKey, String role) {
        BUCKETS.remove(normalizeKey(loginKey, role));
    }

    private static void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Bucket>> it = BUCKETS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bucket> e = it.next();
            Bucket b = e.getValue();
            if (b.failures() == 0 && b.lockedUntilMs() > 0 && b.lockedUntilMs() <= now) {
                it.remove();
            }
        }
    }
}
