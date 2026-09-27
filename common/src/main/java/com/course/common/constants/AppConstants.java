package com.course.common.constants;

import java.util.Map;

/**
 * Global application constants.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    public static final int DEFAULT_PORT = 9999;
    public static final String DEFAULT_HOST = "127.0.0.1";

    public static final int MAX_PERIOD = 10;
    public static final int MIN_PERIOD = 1;

    public static final int MIN_DAY = 2; // Monday
    public static final int MAX_DAY = 8; // Sunday

    // Period timings as per university schedule requirements
    public static final String[] PERIOD_TIMINGS = {
        "07:30 - 08:20", // Period 1
        "08:30 - 09:20", // Period 2
        "09:30 - 10:20", // Period 3
        "10:30 - 11:20", // Period 4
        "11:30 - 12:20", // Period 5
        "13:00 - 13:50", // Period 6
        "14:00 - 14:50", // Period 7
        "15:00 - 15:50", // Period 8
        "16:00 - 16:50", // Period 9
        "17:00 - 17:50"  // Period 10
    };

    public static final Map<Integer, String> DAY_NAMES = Map.of(
        2, "Thứ 2 (Mon)",
        3, "Thứ 3 (Tue)",
        4, "Thứ 4 (Wed)",
        5, "Thứ 5 (Thu)",
        6, "Thứ 6 (Fri)",
        7, "Thứ 7 (Sat)",
        8, "Chủ Nhật (Sun)"
    );

    public static String getDayName(int dayOfWeek) {
        return DAY_NAMES.getOrDefault(dayOfWeek, "Thứ " + dayOfWeek);
    }

    public static String getPeriodTiming(int period) {
        if (period >= 1 && period <= PERIOD_TIMINGS.length) {
            return PERIOD_TIMINGS[period - 1];
        }
        return "N/A";
    }
}
