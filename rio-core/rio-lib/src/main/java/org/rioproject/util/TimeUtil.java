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
package org.rioproject.util;

/**
 * Format time into a readable string.
 *
 * @author Dennis Reedy
 */
public class TimeUtil {
    
    /**
     * Format a time into a readable string
     *
     * @param duration The time duration to format
     *
     * @return The formatted time
     */
    public static String format(long duration) {
        long value = duration;
        value = value/1000;
        long seconds = value % 60;
        value = value/60;
        long minutes = value % 60;
        value = value/60;
        long hours = value % 24;
        long days = value/24;

        String result;
        if(days>0)
            result = days+(days>1?" days":" day")+
                     (hours>0?", "+hours+getHoursText(hours):"")+                        
                     (minutes>0?", "+minutes+getMinutesText(minutes):"")+
                     (seconds>0?", "+seconds+getSecondsText(seconds):"");
        else if(hours>0)
            result = hours+getHoursText(hours)+
            (minutes>0?", "+minutes+getMinutesText(minutes):"")+
            (seconds>0?", "+seconds+getSecondsText(seconds):"");
        else if(minutes>0)
            result = minutes+getMinutesText(minutes)+
            (seconds>0?", "+seconds+getSecondsText(seconds):"");
        else if(seconds>0)
            result = seconds+getSecondsText(seconds);
        else
            result = "0";
        return(result);
    }
    
    private static String getHoursText(long hours) {
        if(hours==0)
            return(" hours");
        return((hours>1?" hours":" hour"));
    }

    private static String getMinutesText(long minutes) {
        if(minutes==0)
            return(" minutes");
        return((minutes>1?" minutes":" minute"));
    }

    private static String getSecondsText(long seconds) {
        if(seconds==0)
            return(" seconds");
        return((seconds>1?" seconds":" second"));
    }
    
    /**
     * Compute the delay to wait for lease renewal based on lease duration
     * 
     * @param leaseDuration How long the lease has been granted for
     * 
     * @return How long to wait to attempt to renew the lease
     */
    public static long computeLeaseRenewalTime(long leaseDuration) {        
        /** Assumed round trip time for any lease renewal, 5 seconds */
        long RENEW_RTT = 1000*5;
        long now = System.currentTimeMillis();        
        long endTime = now + leaseDuration;
        long delta = endTime - now;
        /* If the leaseDuration is less then the round trip time, adjust the
         * leaseTime to be half of itself */         
        if (delta <= RENEW_RTT) {
            delta /= 2;
        } else if (delta <= RENEW_RTT * 2) {
            delta = RENEW_RTT;
        } else if (delta <= RENEW_RTT * 8) {
            delta /= 2;
        } else if (delta <= 1000 * 60 * 60 * 24 * 7) {
            delta /= 8;
        } else if (delta <= 1000 * 60 * 60 * 24 * 14) {
            delta = 1000 * 60 * 60 * 24;
        } else {
            delta = 1000 * 60 * 60 * 24 * 3;
        }        
        return((endTime - delta)-now);
    }
}
