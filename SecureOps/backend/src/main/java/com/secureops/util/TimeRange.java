package com.secureops.util;

import com.secureops.exception.ApiException;
import java.time.Duration;

public final class TimeRange {
    private TimeRange() {}

    public static Duration parse(String range) {
        String r = range == null || range.isBlank() ? "24h" : range;
        return switch (r) {
            case "6h" -> Duration.ofHours(6);
            case "24h" -> Duration.ofHours(24);
            case "7d" -> Duration.ofDays(7);
            case "30d" -> Duration.ofDays(30);
            default -> throw ApiException.badRequest("Unsupported range '" + r + "'. Use 6h, 24h, 7d or 30d.");
        };
    }
}
