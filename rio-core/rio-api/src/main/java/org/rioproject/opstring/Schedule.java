/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.opstring;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * The Schedule provides a way to define the scheduling for an activity,
 * allowing the capability to specify a time in the future when a specific activity
 * when take place, and how long that activity should remain active.
 *
 * @author Dennis Reedy
 * @deprecated This class is no longer used in Rio, has been kept here for compatibility
 */
@Deprecated
public class Schedule implements Serializable {
    static final long serialVersionUID=2L;
    /** The month to schedule an activity */
    private int month;
    /** The day of the week (or month) to schedule an activity */
    private int day;
    /** The hour of the day to schedule an activity */
    private int hour;
    /** The minute of the hour to schedule an activity */
    private int minute;    
    /** Constant indicating the activity has an indefinite duration or 
     * repeat count */        
    public static final long INDEFINITE=-1;
    /** Constant indicating the minimum repeatInterval, 5 seconds */
    public static final long MINIMUM_REPEAT_INTERVAL = 5000;
    /** How long the activity shall remain active */
    private long duration = INDEFINITE;
    /** The number of times the activity should repeat */
    private long repeatCount;
    /** The time in millis between schedule activity executions */
    private long repeatInterval;
    /** Indicates that the day should be considered a day of the month instead
     * of a day of the week. */
    private boolean useDayOfMonth=false;
    /** Compute the next hour or minute using the current year, month day */
    private boolean useToday = false;
    /** Compute the next minute using the current hour */
    private boolean useCurrentHour = false;
    
    /**
     * Create a Schedule with no configured attributes
     */
    public Schedule() {
    }
    
    /**
     * Create a Schedule to start immediately  
     * 
     * @param duration How long (in milliseconds) the activity being scheduled
     * will remain active. If the intent is to keep the scheduled activity active
     * for an indeterminate time, use INDEFINITE
     * @param repeatCount The number of times the activity should repeat. Use 
     * INDEFINTE if it should repeat forever. If this value 
     * is >0, the duration value must not be INDEFINITE
     * @param repeatInterval The time (in milliseconds) between  
     * executions. Only meaningful if repeatCount is >0 
     */
    public Schedule(long duration,
                    long repeatCount,
                    long repeatInterval) {
        useCurrentHour = true;
        init(0, 0, 0, duration, repeatCount, repeatInterval);
    }
    
    /**
     * Create a Schedule to start at a specific minute in the future 
     * 
     * @param minute The minute of the hour to schedule an activity. If 
     * the minute has already passed, then the Calendar will
     * be rolled forward to the next hour
     * @param duration How long (in milliseconds) the planned activity shall
     * be active. If the keep the scheduled activity active
     * for an indeterminate time, use INDEFINITE
     * @param repeatCount The number of times the activity should repeat. Use 
     * INDEFINTE if it should repeat forever. If this value 
     * is >0, the duration value must not be INDEFINITE
     * @param repeatInterval The time (in milliseconds) between  
     * executions. Only meaningful if repeatCount is >0 
     */
    public Schedule(int minute,
                    long duration,
                    long repeatCount,
                    long repeatInterval) {
        useToday = true;
        useCurrentHour = true;
        init(0, 0, minute, duration, repeatCount, repeatInterval);
    }
    
    /**
     * Create a Schedule to start on a specific hour and minute in the future
     * 
     * @param hour The hour of the day, in 24 hour format to schedule an 
     * activity. If the hour has already passed, then the Calendar will
     * be rolled forward to the next day
     * @param minute The minute of the hour to schedule an activity. If 
     * the minute has already passed, then the Calendar will
     * be rolled forward to the next hour
     * @param duration How long (in milliseconds) the planned activity shall
     * be active. If the keep the scheduled activity active
     * for an indeterminate time, use INDEFINITE
     * @param repeatCount The number of times the activity should repeat. Use 
     * INDEFINTE if it should repeat forever. If this value 
     * is >0, the duration value must not be INDEFINITE
     * @param repeatInterval The time (in milliseconds) between  
     * executions. Only meaningful if repeatCount is >0  
     */
    public Schedule(int hour,
                    int minute,
                    long duration,
                    long repeatCount,
                    long repeatInterval) {     
        useToday = true;
        init(0, hour, minute, duration, repeatCount, repeatInterval);
    }
    
    /**
     * Create a Schedule on a specific day, hour and minute in the future
     * 
     * @param dayOfWeek  The day of the week to schedule an activity. The 
     * value must be either Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, 
     * Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY or Calendar.SATURDAY 
     * then the day of the week will be set to the current day. If the day has 
     * already past, then the Calendar will be rolled forward one week
     * @param hour The hour of the day, in 24 hour format to schedule an 
     * activity. If the hour has already passed, then the Calendar will
     * be rolled forward to the next day
     * @param minute The minute of the hour to schedule an activity. If 
     * the minute has already passed, then the Calendar will
     * be rolled forward to the next hour
     * @param duration How long (in milliseconds) the planned activity shall
     * be active. If the keep the scheduled activity active
     * for an indeterminate time, use INDEFINITE
     * @param repeatCount The number of times the activity should repeat. Use 
     * INDEFINTE if it should repeat forever. If this value 
     * is >0, the duration value must not be INDEFINITE
     * @param repeatInterval The time (in milliseconds) between  
     * executions. Only meaningful if repeatCount is >0   
     */
    public Schedule(int dayOfWeek,
                    int hour,
                    int minute,
                    long duration,
                    long repeatCount,
                    long repeatInterval) {
        
        Calendar cal = new GregorianCalendar();
        int min = cal.getMinimum(Calendar.DAY_OF_WEEK);
        int max = cal.getMaximum(Calendar.DAY_OF_WEEK);
        if(dayOfWeek<min || dayOfWeek>max)
            throw new IllegalArgumentException("Bad dayOfWeek : "+dayOfWeek);
        this.month = cal.get(Calendar.MONTH);
        init(dayOfWeek, hour, minute, duration, repeatCount, repeatInterval);
    }
    
    /**
     * Create a Schedule to start on a specific month, day, hour and minute in the
     * future 
     * 
     * @param month The month to schedule an activity. The value must be 
     * either Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL,
     * Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, 
     * Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER
     * @param dayOfMonth The day of the month
     * @param hour The hour of the day, in 24 hour format to schedule an
     * activity. If the hour has already passed, then the Calendar will
     * be rolled forward to the next day
     * @param minute The minute of the hour to schedule an activity. If 
     * the minute has already passed, then the Calendar will
     * be rolled forward to the next hour
     * @param duration How long (in milliseconds) the planned activity shall
     * be active. If the keep the scheduled activity active
     * for an indeterminate time, use INDEFINITE
     * @param repeatCount The number of times the activity should repeat. Use 
     * INDEFINTE if it should repeat forever. If this value 
     * is >0, the duration value must not be INDEFINITE
     * @param repeatInterval The time (in milliseconds) between  
     * executions. Only meaningful if repeatCount is >0   
     */
    public Schedule(int month,
                    int dayOfMonth,
                    int hour,
                    int minute,
                    long duration,
                    long repeatCount,
                    long repeatInterval) {
        Calendar cal = new GregorianCalendar();        
        int monthMin = cal.getMinimum(Calendar.MONTH);
        int monthMax = cal.getMaximum(Calendar.MONTH);        
        if(month<monthMin || month>monthMax)
            throw new IllegalArgumentException("bad month : "+month);        
        this.month = month;
        int currentMonth = cal.get(Calendar.MONTH);
        if(month<currentMonth)
            cal.roll(Calendar.YEAR, true);        
        cal.setLenient(false);        
        /* Test to try and set the month & day of month */
        try {
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);            
            cal.getTime();
        } catch(IllegalArgumentException e) {
            cal.setLenient(true);
            throw new IllegalArgumentException("Invalid Date "+
                                               (month+1)+"/"+
                                               dayOfMonth+"/"+
                                               cal.get(Calendar.YEAR));
        }
        useDayOfMonth = true;
        init(dayOfMonth, hour, minute, duration, repeatCount, repeatInterval);
    }    
            
    /**
     * Get the startDate. This value is computed each time this method is invoked. 
     * The value returned is a time in the future based on a requested day of week, 
     * hour and minute using a GregorianCalendar with it's date and time set for the 
     * default time zone with the default locale
     *  
     * If the day of week has already passed, then the 
     * GregorianCalendar is rolled forward one day, having the calculated hour and 
     * minute be the hour and minute of the next day
     * 
     * @return The Date to start the activity. This value is computed each time 
     * this method is invoked. 
     */
    public Date getStartDate() {        
        Calendar cal = new GregorianCalendar();
        /* If no values are set return the current time */
        if(day==0 && hour==0 && minute==0 && !useToday) {
            return(cal.getTime());
        }
        
        boolean rollYear = false;
        boolean rollMonth = false;
        int calculatedDay = (useToday?cal.get(Calendar.DAY_OF_WEEK):day);
        int calculatedMonth = (useToday?cal.get(Calendar.MONTH):month);
        int calculatedHour = (useCurrentHour?cal.get(Calendar.HOUR_OF_DAY):hour);
        int currentMonth = cal.get(Calendar.MONTH);         
        if(calculatedMonth < currentMonth)
            rollYear = true;
        
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);               
        if(!useDayOfMonth) {
            /* Use the input day of the week (Monday, Tuesday, etc...) to compute 
             * the day of the month */
            int currentDOW = cal.get(Calendar.DAY_OF_WEEK);
            if(day==0)
                calculatedDay = currentDOW;
            if(currentDOW!=calculatedDay) {
                /* The asked for dow is later in the week */
                if(currentDOW<calculatedDay) {
                    dayOfMonth = dayOfMonth+(calculatedDay-currentDOW);
                } else {
                    /* The asked for dow already happened, schedule for next week */
                    dayOfMonth = (7-(currentDOW-calculatedDay))+dayOfMonth;
                }
            }
        } else {            
            if(calculatedDay<dayOfMonth)
                rollMonth = true;
            dayOfMonth = calculatedDay;
        }
                
        GregorianCalendar gCal = new GregorianCalendar(cal.get(Calendar.YEAR),
                                                       calculatedMonth,
                                                       dayOfMonth,
                                                       calculatedHour,
                                                       minute);
        if(rollYear) {
            gCal.roll(Calendar.YEAR, true);
        } else if(rollMonth) {
            gCal.roll(Calendar.MONTH, true);
        } else {
            /* If the calendar is still behind, roll it forward a day */
            long time = gCal.getTimeInMillis();
            if(time < System.currentTimeMillis()) {            
                gCal.set(Calendar.DAY_OF_WEEK, cal.get(Calendar.DAY_OF_WEEK));
                gCal.roll(Calendar.DATE, true);            
            }
        }        
        return(gCal.getTime());
    }
    
    /**
     * Get the duration property
     * 
     * @return How long (in milliseconds) the activity shall be active
     */
    public long getDuration() {
        return(duration);
    }
    
    /**
     * Get the repeatCount property
     * 
     * @return The number of times the activity should repeat.
     */
    public long getRepeatCount() {
        return(repeatCount);
    }
    
    /**
     * Get the repeatInterval property
     * 
     * @return The time (in milliseconds) in between executions. Only 
     * meaningful if repeatCount is >0 
     */
    public long getRepeatInterval() {
        return(repeatInterval);
    }
    
    /*
     * Override hashCode
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int hc = 17;        
        hc = 37*hc+month;
        hc = 37*hc+day;
        hc = 37*hc+hour;
        hc = 37*hc+minute;
        hc = 37*hc+(int)(duration^(duration>>>32));
        hc = 37*hc+(int)(repeatCount^(repeatCount>>>32));
        hc = 37*hc+(int)(repeatInterval^(repeatInterval>>>32));        
        return(hc);
    }
    
    /*
     * Override equals
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if(this == obj)
            return(true);
        if(!(obj instanceof Schedule))
            return(false);
        Schedule that = (Schedule)obj;
        if(this.month == that.month &&
           this.day == that.day &&
           this.hour == that.hour &&
           this.minute == that.minute &&
           this.duration == that.duration &&
           this.repeatCount == that.repeatCount &&
           this.repeatInterval == that.repeatInterval) {            
            return(true);
        }               
        return(false);
    }
    
    /*
     * Override toString
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {                                    
        return("month="+month+", "+
               "day="+day+", "+
               "hour="+hour+", "+ 
               "minute="+minute+", "+
               "duration="+duration+", "+
               "repeatCount="+repeatCount+", "+
               "repeatInterval="+repeatInterval);
    }
    
    /*
     * Initialization routine
     */
    private void init(int day,
                      int hour,
                      int minute,
                      long duration,
                      long repeatCount,
                      long repeatInterval) {
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        if(hour<0 || hour>23)
            throw new IllegalArgumentException("bad hour ["+hour+"]");
        if(minute<0 || minute>59)
            throw new IllegalArgumentException("bad minute ["+minute+"]");        
        this.duration = duration;
        this.repeatCount = repeatCount;
        if(repeatCount>0 && duration==INDEFINITE)
            throw new IllegalArgumentException("cannot repeat a duration set "+
                                               "to INDEFINITE");
        if(repeatInterval < MINIMUM_REPEAT_INTERVAL)
            throw new IllegalArgumentException("bad repeatInterval, must be "+
                                               "greater then "+
                                               MINIMUM_REPEAT_INTERVAL);
        this.repeatInterval = repeatInterval;
    }
    
    public static void main(String[] args) {
        Calendar cal = new GregorianCalendar();
        System.out.println("Today : "+cal.getTime());
        System.out.println("-----------");
        Schedule s = new Schedule(Calendar.JANUARY,       /* month */
                                  1,                      /* day of month */
                                  0,                      /* hour */
                                  0,                      /* minute */
                                  10*1000,                /* duration */
                                  2,                      /* repeats */
                                  3*1000);                /* repeat interval */
        System.out.println(s.toString());
        System.out.println("Computed Time : "+s.getStartDate());        
        
        System.out.println("-----------");
        s = new Schedule(Calendar.MONDAY,      /* day */
                         10,                   /* hour */
                         10,                   /* minute */
                         10*1000,              /* duration */
                         2,                    /* repeats */
                         3*1000);              /* repeat interval */
        System.out.println(s.toString());
        System.out.println("Computed Time : "+s.getStartDate());
        
        System.out.println("-----------");
        s = new Schedule(20,                   /* hour */
                         0,                    /* minute */
                         10*1000,              /* duration */
                         2,                    /* repeats */
                         3*1000);              /* repeat interval */
        System.out.println(s.toString());
        System.out.println("Computed Time : "+s.getStartDate());
        
        System.out.println("-----------");
        s = new Schedule(0,                    /* minute */
                         10*1000,              /* duration */
                         2,                    /* repeats */
                         3*1000);              /* repeat interval */
        System.out.println(s.toString());
        System.out.println("Computed Time : "+s.getStartDate());
        
        System.out.println("-----------");
        s = new Schedule();
        System.out.println(s.toString());
        System.out.println("Computed Time : "+s.getStartDate());
        
    }
}

