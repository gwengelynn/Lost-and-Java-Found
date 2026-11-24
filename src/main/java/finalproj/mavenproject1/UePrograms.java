package finalproj.mavenproject1;

/**
 * UE Caloocan college / course / year catalogs — shared by signup and edit profile.
 */
final class UePrograms {

    static final String[] COLLEGES = {
            "College of Arts and Sciences – Caloocan (CAS)",
            "College of Business Administration – Caloocan (CBA)",
            "College of Engineering – Caloocan (CEng'g)",
            "College of Fine Arts, Architecture and Design (CFAAD)"
    };

    static final String[] YEAR_LEVELS = {
            "1st Year",
            "2nd Year",
            "3rd Year",
            "4th Year",
            "5th Year",
            "6th Year",
            "Nth Year"
    };

    private UePrograms() {}

    /** Undergraduate programs for a college. */
    static String[] coursesForCollege(String college) {
        if (college == null) {
            return new String[0];
        }
        return switch (college) {
            case "College of Arts and Sciences – Caloocan (CAS)" -> new String[]{
                    "Bachelor of Arts in Communication (BA Comm)",
                    "Bachelor of Science in Hospitality Management (BSHM)",
                    "Bachelor of Science in Tourism Management (BSTM)",
                    "Bachelor of Science in Psychology (BS Psych)",
                    "Bachelor of Science in Criminology (Cybersecurity)"
            };
            case "College of Business Administration – Caloocan (CBA)" -> new String[]{
                    "Bachelor of Science in Accountancy (BSA)",
                    "Bachelor of Science in Management Accounting (BSMA)",
                    "Bachelor of Science in Business Administration (BSBA)"
            };
            case "College of Engineering – Caloocan (CEng'g)" -> new String[]{
                    "Bachelor of Science in Civil Engineering (BSCE)",
                    "Bachelor of Science in Computer Engineering (BSCpE)",
                    "Bachelor of Science in Electrical Engineering (BSEE)",
                    "Bachelor of Science in Electronics Engineering (BSECE)",
                    "Bachelor of Science in Mechanical Engineering (BSME)",
                    "Bachelor of Science in Computer Science (BSCS)",
                    "Bachelor of Science in Information Technology (BSIT)",
                    "Bachelor of Science in Data Science (BSDS)"
            };
            case "College of Fine Arts, Architecture and Design (CFAAD)" -> new String[]{
                    "Bachelor of Science in Architecture (BS Archi)",
                    "Bachelor of Science in Interior Design (BSID)",
                    "Bachelor of Fine Arts (BFA)",
                    "Bachelor of Multimedia Arts (BMMA)"
            };
            default -> new String[0];
        };
    }

    /** Finds which college owns a course name, or {@code null} if unknown. */
    static String collegeForCourse(String course) {
        if (course == null || course.isBlank()) {
            return null;
        }
        String needle = course.trim();
        for (String college : COLLEGES) {
            for (String c : coursesForCollege(college)) {
                if (c.equalsIgnoreCase(needle) || c.equalsIgnoreCase(normalizeLegacyCourse(needle))) {
                    return college;
                }
                // Match shortened / legacy labels like "BSCPE" or truncated display text.
                if (c.toLowerCase().contains(needle.toLowerCase())
                        || needle.toLowerCase().contains(shortCode(c).toLowerCase())) {
                    return college;
                }
            }
        }
        return null;
    }

    /** Best matching catalog course for a stored value, or {@code null}. */
    static String matchCourse(String course) {
        if (course == null || course.isBlank()) {
            return null;
        }
        String needle = course.trim();
        String college = collegeForCourse(needle);
        if (college == null) {
            return null;
        }
        for (String c : coursesForCollege(college)) {
            if (c.equalsIgnoreCase(needle) || c.equalsIgnoreCase(normalizeLegacyCourse(needle))) {
                return c;
            }
        }
        for (String c : coursesForCollege(college)) {
            if (c.toLowerCase().contains(needle.toLowerCase())
                    || needle.toLowerCase().contains(shortCode(c).toLowerCase())) {
                return c;
            }
        }
        return null;
    }

    static String matchYearLevel(String year) {
        if (year == null || year.isBlank()) {
            return null;
        }
        String needle = year.trim();
        for (String y : YEAR_LEVELS) {
            if (y.equalsIgnoreCase(needle)) {
                return y;
            }
        }
        // Accept bare digits from older signup ("1".."6").
        if (needle.matches("[1-6]")) {
            return YEAR_LEVELS[Integer.parseInt(needle) - 1];
        }
        if (needle.equalsIgnoreCase("N") || needle.equalsIgnoreCase("Nth")
                || needle.equalsIgnoreCase("N Year")) {
            return "Nth Year";
        }
        return null;
    }

    private static String normalizeLegacyCourse(String course) {
        return switch (course.trim().toUpperCase()) {
            case "BSCPE", "BS CPE", "BSCOMPENG" ->
                    "Bachelor of Science in Computer Engineering (BSCpE)";
            case "BSCS" -> "Bachelor of Science in Computer Science (BSCS)";
            case "BSIT" -> "Bachelor of Science in Information Technology (BSIT)";
            case "BSCE" -> "Bachelor of Science in Civil Engineering (BSCE)";
            case "BSEE" -> "Bachelor of Science in Electrical Engineering (BSEE)";
            case "BSECE" -> "Bachelor of Science in Electronics Engineering (BSECE)";
            case "BSME" -> "Bachelor of Science in Mechanical Engineering (BSME)";
            case "BSA" -> "Bachelor of Science in Accountancy (BSA)";
            default -> course.trim();
        };
    }

    private static String shortCode(String course) {
        int open = course.lastIndexOf('(');
        int close = course.lastIndexOf(')');
        if (open >= 0 && close > open) {
            return course.substring(open + 1, close);
        }
        return course;
    }
}
