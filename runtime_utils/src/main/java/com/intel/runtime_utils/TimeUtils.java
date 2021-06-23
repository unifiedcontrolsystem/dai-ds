// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.runtime_utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public final class TimeUtils {
    private TimeUtils() {} // Do not allow instantiation...

    /**
     * Get the current time stamp as a nanosecond long.
     *
     * @return The current time stamp as a nanosecond long.
     */
    public static long getNsTimestamp() {
        Instant now = Instant.now();
        return now.getEpochSecond() * NANO_FACTOR + now.getNano();
    }

    /**
     * Convert a ISO 8601 formatted string to a nanosecond long. The 'T' may also be a ' ' in the string.
     *
     * @param isoFormat The ISO 8601 string to convert to nanosecond long.
     * @return The nanosecond long of the input string.
     * @throws ParseException When the string cannot be converted to a long.
     */
    public static long nSFromIso8601(String isoFormat) throws ParseException {
        SimpleDateFormat[] df = new SimpleDateFormat[] {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        };
        String[] parts = isoFormat.split("\\.");
        // Fraction without TZ
        String fraction = parts.length == 1 ? "0" : parts[1].replaceFirst("([A-Z]+)|([-+][0-9:]+)","");
        // timestamp without fraction but with TZ (" " or "T" separator agnostic)
        String tsToSecond = isoFormat.replaceFirst("\\.[0-9]+", "").replace(" ", "T");
        Instant ts = null;
        // Do appropriate parsing...
        for(SimpleDateFormat fmt: df) {
            try {
                ts = fmt.parse(tsToSecond).toInstant();
                break;
            } catch(ParseException | NullPointerException e) { /* Not used */ }
        }
        if(ts == null)
            throw new ParseException(tsToSecond, 0); // Parsing timestamp failed.
        if(fraction.length() > 9)
            throw new ParseException("Fraction of seconds is malformed, must be 1-9 digits", isoFormat.indexOf('.'));
        return (ts.getEpochSecond() * NANO_FACTOR) +
                (Long.parseLong(fraction) * (long)Math.pow(10, 9-fraction.length()));
    }

    public static String stringToIso8601(String isoFormat) throws ParseException {
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        return format.parse(isoFormat).toInstant().toString();
    }

    /**
     * Convert long nanoseconds to a ISO8601 string.
     *
     * @param nsTimestamp The nanosecond timestamp to convert.
     * @return The ISO 8601 date-time string.
     */
    public static String nsToIso8601(long nsTimestamp) {
        return ISO_INSTANT.format(Instant.ofEpochSecond(nsTimestamp / NANO_FACTOR, nsTimestamp % NANO_FACTOR)).replace("GMT", "Z");
    }

    /**
     * Convert long nanoseconds to microseconds.
     *
     * @param ns The nanoseconds to convert to microseconds.
     * @return The microseconds from the nanoseconds.
     */
    public static long nanosecondsToMicroseconds(long ns) {
        return ns / MILLI_FACTOR;
    }

    /**
     * Convert long nanoseconds to seconds.
     *
     * @param ns The nanoseconds to convert to seconds.
     * @return The seconds from the nanoseconds.
     */
    public static long nanosecondsToSeconds(long ns) {
        return ns /NANO_FACTOR;
    }

    /**
     * Convert long nanoseconds to milliseconds.
     *
     * @param ns The nanoseconds to convert to milliseconds.
     * @return The milliseconds from the nanoseconds.
     */
    public static long nanosecondsToMilliseconds(long ns) {
        return ns / MICRO_FACTOR;
    }

    /**
     * Convert long microseconds to nanoseconds.
     *
     * @param ns The microseconds to convert to nanoseconds.
     * @return The nanoseconds from the microseconds.
     */
    public static long microsecondsToNanoseconds(long ns) {
        return ns * MILLI_FACTOR;
    }

    /**
     * Convert long milliseconds to nanoseconds.
     *
     * @param ns The milliseconds to convert to nanoseconds.
     * @return The nanoseconds from the milliseconds.
     */
    public static long millisecondsToNanoseconds(long ns) {
        return ns * MICRO_FACTOR;
    }

    /**
     * Convert long seconds to nanoseconds.
     *
     * @param ns The seconds to convert to nanoseconds.
     * @return The nanoseconds from the seconds.
     */
    public static long secondsToNanoseconds(long ns) {
        return ns * NANO_FACTOR;
    }

    private static final long NANO_FACTOR = 1_000_000_000L;
    private static final long MICRO_FACTOR = 1_000_000L;
    private static final long MILLI_FACTOR = 1_000L;
}
