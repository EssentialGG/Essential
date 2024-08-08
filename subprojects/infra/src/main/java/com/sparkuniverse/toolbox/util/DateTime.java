/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package com.sparkuniverse.toolbox.util;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom java.util.Date extension to support add and format.
 */
public class DateTime extends Date {

    private static final Pattern TIME_STRING_REGEX = Pattern.compile("(\\d+)([A-Za-z]+)");
    public static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyy-MM-dd @ HH:mm:ss");

    /**
     * Initializes a new DateTime instance with the current timestamp.
     */
    public DateTime() {
        this(Calendar.getInstance().getTimeInMillis());
    }

    /**
     * Initializes a new DateTime instance with a given unix timestamp in milliseconds.
     */
    public DateTime(final long millis) {
        super(millis);
    }

    /**
     * Initializes a new DateTime instance with a time given as a string, parsed with the default date format.
     * @param dateString the date string to start calculations from.
     * @throws ParseException if dateString can not be parsed to java.util.Date.
     */
    public DateTime(String dateString) throws ParseException {
        this(dateString, DEFAULT_FORMAT);
    }

    /**
     * Initializes a new DateTime instance with a time given as a string, parsed with the given date format.
     * @param dateString the date string to start calculations from.
     * @param format the date format dateString is formatted in.
     * @throws ParseException if dateString can not be parsed to java.util.Date.
     */
    public DateTime(@NotNull final String dateString, @NotNull final SimpleDateFormat format) throws ParseException {
        super(format.parse(dateString).getTime());
    }

    // Main API

    /**
     * Adds the given amount of time.
     * @param timeToAdd the unit and amount of time to add, e.g. '10s', '5min', '3w'.
     *                  Available keys:
     *                  y = years (1y == 356d), m = months (1m == 28d), w = weeks (1w == 7d),
     *                  d = days, h = hours, min(s) = minutes, s = seconds
     * @return this
     * @throws ParseException if the time string cannot be parsed into a unit and amount.
     */
    public DateTime add(@NotNull final String timeToAdd) throws ParseException {
        Validate.notNull(timeToAdd, () -> "timeToAdd must not be null");

        if(timeToAdd.length() < 2) {
            throw new ParseException("Invalid input: too short", 0);
        }

        if(timeToAdd.startsWith("-")) {
            throw new ParseException("Invalid input: cannot be negative", 0);
        }

        Matcher matcher = TIME_STRING_REGEX.matcher(timeToAdd);

        if(!matcher.matches()) {
            throw new ParseException("Invalid input: cannot parse amount and unit", 0);
        }

        String amountString = matcher.group(1);
        String unit = matcher.group(2);

        // This is safe as the regex matches only numbers in group 1
        int amount = Integer.parseInt(amountString);

        switch(unit) {
            case "y":
                return this.add(TimeUnit.DAYS, amount * 356);
            case "m":
                return this.add(TimeUnit.DAYS, amount * 28);
            case "w":
                return this.add(TimeUnit.DAYS, amount * 7);
            case "d":
                return this.add(TimeUnit.DAYS, amount);
            case "h":
                return this.add(TimeUnit.HOURS, amount);
            case "mins":
            case "min":
                return this.add(TimeUnit.MINUTES, amount);
            case "s":
                return this.add(TimeUnit.SECONDS, amount);
            default:
                throw new ParseException("Invalid input: unknown unit " + unit, 0);
        }
    }

    /**
     * Adds the given amount of time.
     * @param unit the unit of time to add.
     * @param amount the amount of time to add.
     * @return this
     */
    public DateTime add(@NotNull final TimeUnit unit, final int amount) {
        return this.add(unit.toMillis(amount));
    }

    /**
     * Adds the given amount of time.
     * @param duration the amount of time to add.
     * @return this
     */
    public DateTime add(@NotNull final Duration duration) {
        return this.add(duration.toMillis());
    }

    /**
     * Adds the given amount of time.
     * @param milliseconds the amount of time in milliseconds.
     * @return this
     */
    public DateTime add(final long milliseconds) {
        this.setTime(this.getTime() + milliseconds);
        return this;
    }

    /**
     * Format the current date given in the default format.
     * @return the formatted date.
     */
    public String format() {
        return this.format(DEFAULT_FORMAT);
    }

    /**
     * Format the current date in the given format.
     * @param format the format to use.
     * @return the formatted date.
     */
    public String format(@NotNull final SimpleDateFormat format) {
        return format.format(this);
    }

}
