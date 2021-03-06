package org.dynamide.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GregorianCalendarDateTimeUtils {

    private static final Logger logger = LoggerFactory.getLogger(GregorianCalendarDateTimeUtils.class);

    /**
     * Returns a String representing the current date and time instance.
     * in the UTC time zone, formatted as an ISO 8601 timestamp.
     *
     * @return A String representing the current date and time instance.
     */
     public static String timestampUTC() {
         return formatAsISO8601Timestamp(currentDateAndTime(DateUtils.UTCTimeZone()));
     }
     
    /**
     * Returns a String representing the current date and time instance.
     * in the UTC time zone, formatted as an ISO 8601 date.
     *
     * @return A String representing the current date and time instance.
     */
     public static String currentDateUTC() {
         return formatAsISO8601Date(currentDateAndTime(DateUtils.UTCTimeZone()));
     }

    public static String dateUTCToString(Object millis){
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTimeZone(DateUtils.UTCTimeZone());
        Date now = new Date(Long.parseLong(millis.toString()));
        gcal.setTime(now);
        return formatAsISO8601Date(gcal);
    }
    
   /**
    * Returns a calendar date, representing the current date and time instance
    * in the UTC time zone.
    *
    * @return The current date and time instance in the UTC time zone.
    */
    public static GregorianCalendar currentDateAndTimeUTC() {
        return currentDateAndTime(DateUtils.UTCTimeZone());
    }

   /**
    * Returns a calendar date, representing the current date and time instance
    * in the specified time zone.
    *
    * @return The current date and time instance in the specified time zone.
    *         If the time zone is null, will return the current time and
    *         date in the time zone intrinsic to a new Calendar instance.
    */
    public static GregorianCalendar currentDateAndTime(TimeZone tz) {
        GregorianCalendar gcal = new GregorianCalendar();
        if (tz != null) {
            gcal.setTimeZone(tz);
        }
        Date now = new Date();
        gcal.setTime(now);
        return gcal;
    }

    
    /**
     * Returns a representation of a calendar date and time instance,
     * as an ISO 8601-formatted timestamp in the UTC time zone.
     *
     * @param cal a calendar date and time instance.
     *
     * @return    a representation of that calendar date and time instance,
     *            as an ISO 8601-formatted timestamp in the UTC time zone.
     */
    public static String formatAsISO8601Timestamp(GregorianCalendar cal) {
        return formatGregorianCalendarDate(cal, DateUtils.UTCTimeZone(),
        		DateUtils.getDateFormatter(DateUtils.ISO_8601_UTC_TIMESTAMP_PATTERN));
    }
    
    /**
     * Returns a representation of a calendar date and time instance,
     * as an ISO 8601-formatted date.
     *
     * @param cal a calendar date and time instance.
     *
     * @return    a representation of that calendar date and time instance,
     *            as an ISO 8601-formatted date.
     */
    public static String formatAsISO8601Date(GregorianCalendar cal) {
        return formatGregorianCalendarDate(cal, DateUtils.UTCTimeZone(),
        		DateUtils.getDateFormatter(DateUtils.ISO_8601_DATE_PATTERN));
    }

    /**
     * Formats a provided calendar date using a supplied date formatter,
     * in the default system time zone.
     *
     * @param gcal  A GregorianCalendar date to format.
     * @param df    A date formatter to apply.
     *
     * @return      A formatted date string, or the empty string
     *              if one or more of the parameter values were invalid.
     */
    public static String formatGregorianCalendarDate(GregorianCalendar gcal, DateFormat df) {
        return formatGregorianCalendarDate(gcal, TimeZone.getDefault(), df);
    }

    /**
     * Formats a provided calendar date using a provided date formatter,
     * in a provided time zone.
     *
     * @param gcal  A GregorianCalendar date to format.
     * @param tz    The time zone qualifier for the calendar date to format.
     * @param df    A date formatter to apply.
     *
     * @return      A formatted date string, or the empty string
     *              if one or more of the parameter values were invalid.
     */
    public static String formatGregorianCalendarDate(GregorianCalendar gcal, TimeZone tz, DateFormat df) {
        String formattedDate = "";
        if (gcal == null) {
            logger.warn("Null calendar date was provided when a non-null calendar date was required.");
            return formattedDate;
        }
        if (tz == null) {
            logger.warn("Null time zone was provided when a non-null time zone was required.");
            return formattedDate;
        }
        if (df == null) {
            logger.warn("Null date formatter was provided when a non-null date formatter was required.");
            return formattedDate;
        }
        gcal.setTimeZone(tz);
        Date date = gcal.getTime();
        df.setTimeZone(tz);
        formattedDate = df.format(date);
        return formattedDate;
    }


}
