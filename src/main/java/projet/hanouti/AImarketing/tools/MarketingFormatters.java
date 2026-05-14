package projet.hanouti.AImarketing.tools;

import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MarketingFormatters {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private MarketingFormatters() {}

    public static String money(double value) {
        return String.format(Locale.US, "%.0f TND", value);
    }

    public static String shortMoney(double tnd) {
        return String.format(Locale.US, "%.0f TND  -  %.0f EUR  -  %.0f USD", tnd, tnd * 0.296, tnd * 0.347);
    }

    public static String percent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    public static String dateRange(Date start, Date end) {
        String from = start == null ? "N/A" : start.toLocalDate().format(DATE_FORMAT);
        String to = end == null ? "N/A" : end.toLocalDate().format(DATE_FORMAT);
        return from + " - " + to;
    }

    public static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
