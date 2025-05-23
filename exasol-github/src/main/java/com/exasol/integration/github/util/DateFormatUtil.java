package com.exasol.integration.github.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateFormatUtil {

    public static String formatIsoUtcToYyyyMmDd(String isoUtcDateTime) {
        if (isoUtcDateTime == null || isoUtcDateTime.isEmpty()) {
            return "";
        }
        Instant instant = Instant.parse(isoUtcDateTime);  // parses ISO-8601 UTC
        LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);  // "yyyy-MM-dd"
    }
}
