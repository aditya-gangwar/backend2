package com.mytest.utilities;

/**
 * Created by adgangwa on 20-04-2016.
 */
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Utility class for date manipulation.
 * This class gives a simple interface for common Date, Calendar and Timezone
 * operations.
 * It is possible to apply subsequent transformations to an initial date, and
 * retrieve the changed Date object at any point.
 *
 */
public class DateUtil {

    //-------------------------------------------------------------- Attributes
    private Calendar cal;

    //------------------------------------------------------------ Constructors

    /** Inizialize a new instance with the current date */
    public DateUtil() {
        this(new Date());
    }

    /** Inizialize a new instance with the given date */
    public DateUtil(Date d) {
        cal = Calendar.getInstance();
        cal.setTime(d);
    }

    //---------------------------------------------------------- Public methods

    /** Set a new time */
    public void setTime(Date d) {
        cal.setTime(d);
    }

    /** Get the current time */
    public Date getTime() {
        return cal.getTime();
    }

    /** Get the current TimeZone */
    public String getTZ() {
        return cal.getTimeZone().getID();
    }

    /**
     * Convert the time to the midnight of the currently set date.
     * The internal date is changed after this call.
     *
     * @return a reference to this DateUtil, for concatenation.
     */
    public DateUtil toMidnight() {

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND,0);

        return this;
    }

    /**
     * Make the date go back of the specified amount of days
     * The internal date is changed after this call.
     *
     * @return a reference to this DateUtil, for concatenation.
     */
    public DateUtil removeDays(int days) {

        int x = days*-1;
        cal.add(Calendar.DAY_OF_YEAR, x);

        /*
        Date d = cal.getTime();
        long time = d.getTime();
        long milliSec = days * 24 * 3600 * 1000;
        time = time - milliSec;
        //time -= days * 24 * 3600 * 1000;
        d.setTime(time);
        cal.setTime(d);*/

        return this;
    }

    public DateUtil addDays(int days) {
        cal.add( Calendar.DAY_OF_YEAR, days);
        return this;
    }

    /**
     * Make the date go forward of the specified amount of minutes
     * The internal date is changed after this call.
     *
     * @return a reference to this DateUtil, for concatenation.
     */
    public DateUtil addMinutes(int minutes) {
        Date d = cal.getTime();
        long time = d.getTime();
        time += minutes * 60 * 1000;
        d.setTime(time);
        cal.setTime(d);

        return this;
    }

    /**
     * Convert the date to GMT. The internal date is changed
     *
     * @return a reference to this DateUtil, for concatenation.
     */
    public DateUtil toGMT() {
        return toTZ("GMT");
    }

    /**
     * Convert the date to the given timezone. The internal date is changed.
     *
     * @param tz The name of the timezone to set
     *
     * @return a reference to this DateUtil, for concatenation.
     */
    public DateUtil toTZ(String tz) {
        cal.setTimeZone(TimeZone.getTimeZone(tz));

        return this;
    }

    /**
     * Get the days passed from the specified date up to the date provided
     * in the constructor
     *
     * @param date The starting date
     *
     * @return number of days within date used in constructor and the provided
     * date
     */
    public int getDaysSince(Date date) {
        long millisecs = date.getTime();
        Date d = cal.getTime();
        long time = d.getTime();
        long daysMillisecs = time - millisecs;
        int days = (int)((((daysMillisecs / 1000)/60)/60)/24);
        return days;
    }

    /**
     * Utility method wrapping Calendar.after method
     * Compares the date field parameter with the date provided with the constructor
     * answering the question: date from constructor is after the given param date ?
     *
     * @param date The date to be used for comparison
     *
     * @return true if date from constructor is after given param date
     */
    public boolean isAfter(Date date) {
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);
        return cal.after(cal2);
    }

    public boolean isHourBetween(Date from, Date to) {
        long now = cal.getTimeInMillis();
        if(now >= from.getTime() && now <= to.getTime()) {
            return true;
        }
        return false;
    }
}