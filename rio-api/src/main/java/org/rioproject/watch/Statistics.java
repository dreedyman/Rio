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
package org.rioproject.watch;

import java.util.*;

/**
 * Class Statistics implements basic statistical formulae.
 */
public class Statistics {
    /** the collection of values */
    private final Vector<Double> v = new Vector<Double>();

    /** no arg constructor */
    public Statistics() {
        super();
    }

    /**
     * Construct a new Statistics
     *
     * @param values initial values for new instance
     */
    public Statistics(Iterable<Double> values) {
        if(values==null)
            throw new IllegalArgumentException("values is null");
        for(Double d : values) {
            v.add(d);
        }
    }

    /**
     * Removes all of the values in the internal collection.
     */
    public void clearAll() {
        v.clear();
    }

    /**
     * Number of values in collection of values
     * 
     * @return int number of values
     */
    public int count() {
        return v.size();
    }

    /**
     * Get largest value in collection of values
     * 
     * @return double largest value
     */
    public double max() {
        if(v.isEmpty())
            return(0.0D / 0.0D); //NaN
        return Collections.max(v);
    }

    /**
     * Get smallest value in collection of values
     * 
     * @return double smallest value
     */
    public double min() {
        if(v.isEmpty())
            return(0.0D / 0.0D); //NaN
        return Collections.min(v);
    }

    /**
     * Get the average value in the collection of values
     * 
     * @return double the average value in collection
     */
    public double mean() {
        if(v.isEmpty())
            return(0.0D / 0.0D); //NaN
        double sum = sum();
        return sum / (double) v.size();
    }

    /**
     * Get the median in the collection of values
     * 
     * @return double the middle value in the collection of values
     */
    public double median() {
        if(v.isEmpty())
            return(0.0D / 0.0D); //NaN
        double result;
        Vector<Double> sortedV = sorted();
        Double[] dArray = new Double[v.size()];
        dArray = sortedV.toArray(dArray);

        if(dArray.length % 2 != 0 && dArray.length > 0) {
            // length is odd
            int k = (dArray.length -1) / 2; // middle index
            result = dArray[k];
        } else { // length is even, take avg of two mid vals
            int k = (dArray.length-1) / 2; // low middle index
            /* average of low mid val and low mid idx+1 val */
            result = (dArray[k] + dArray[k + 1]) / 2D;
        }
        return result;
    }

    /**
     * Get the mode
     * 
     * @return double the value with the highest number of occurences
     */     
    public double mode() {
        if(v.isEmpty()) return(0.0D / 0.0D); //NaN
        return (Double) mode0(true);
    }

    /**
     * Get the occurrences of the mode
     * 
     * @return int count of the value with the highest number of occurences
     */     
    public int modeOccurrenceCount() {
        if(v.isEmpty()) return(0);
        return (Integer) mode0(false);
    }

    /**
     * Get the mode or occurrences of the mode
     * 
     * @return Object the value with the highest number of occurences
     * @param returnMode boolean true to return the mode, false occurrences
     */    
    private Object mode0(boolean returnMode) {
        Map<Object, Integer> hashtable = new Hashtable<Object, Integer>(v.size(),
                                                                        1.0f);
        Object obj;
        Integer occurrences = 0;
        int modeCnt;
        Double mode = 0.0D;

        for (Double aV : v) {
            obj = aV;
            if (hashtable.containsKey(obj)) {
                modeCnt = (hashtable.get(obj)) + 1;
            } else {
                modeCnt = 1;
            }
            hashtable.put(obj, modeCnt);
        }
        /*
        it = v.iterator();
        while(it.hasNext()) {
            obj = it.next();
            cnt = (Integer)hashtable.get(obj);
            if((cnt.intValue()) > modeCnt) {
                mode = (Double)obj;
                occurrences = cnt;
            }
        }
        */
        if(returnMode) {
            return mode;
        } else {
            return occurrences;
        }
    }

    /**
     * Get the range
     * 
     * @return double the difference between the min and max
     */    
    public double range() {
        if(v.isEmpty()) return(0.0D / 0.0D); //NaN
        else
            return max() - min();
    }

    /**
     * Get the standard deviation
     * 
     * @return double the dispersion from the mean
     */
    public double standardDeviation() {
        if(v.isEmpty()) {
            return(0.0D / 0.0D); //NaN
        }
        if(v.size() == 1) {
            // Need to return 0 explicitly because otherwise there would
            // be division by zero
            return 0;
        }
        double sum = sum();
        double sumOfSquares = 0.0D;

        for (Double aV : v) {
            sumOfSquares += Math.pow((aV), 2D);
        }
        return Math.sqrt((sumOfSquares - Math.pow(sum, 2d) / 
                         (double) v.size()) / (double)(v.size() - 1));    

    }

    public Vector getValues() {
        return (Vector)v.clone();
    }

    public void addValue(Double double1) {
        if(double1==null)
            throw new IllegalArgumentException("value is null");
        v.addElement(double1);
    }

    public void addValue(double d) {
        v.addElement(d);
    }

    public void setValues(Iterable<Double> vals) {
        if(vals==null)
            throw new IllegalArgumentException("vector is null");
        v.clear();
        for(Double d : vals)
            v.add(d);
    }

    public void setValues(Calculable[] calcs) {
        if(calcs==null)
            throw new IllegalArgumentException("calcs is null");
        v.clear();
        for(Calculable calc : calcs)
            v.add(calc.getValue());
    }

    /**
     * Remove value
     * 
     * @param d the double to remove
     * @param removeAll false remove first occurrence; true remove all
     */    
    public void removeValues(double d, boolean removeAll) {
        removeValues((Double) d, removeAll);
    }

    /**
     * Remove value
     * 
     * @param double1 the Double to remove
     * @param removeAll false remove first occurrence; true remove all
     */     
    public void removeValues(Double double1, boolean removeAll) {
        if(double1==null)
            throw new IllegalArgumentException("value is null");
        if(removeAll) {
            Vector<Double> toRemove = new Vector<Double>(1);
            toRemove.add(double1);
            v.removeAll(toRemove);
        } else {
            v.removeElement(double1);
        }
    }

    public void removeValue(int location) {
        if(location < v.size() && location >= 0)
            v.removeElementAt(location);
    }

    /**
     * Remove values with a value between double1 and double2
     * 
     * @param low the lowest value to remove
     * @param high the highest value to remove
     */     
    public void removeValues(Double low, Double high) {
        if(low==null)
            throw new IllegalArgumentException("low is null");
        if(high==null)
            throw new IllegalArgumentException("high is null");
        removeValues(low.doubleValue(), high.doubleValue());
    }

    /**
     * Remove values with a value between double1 and double2
     * 
     * @param low the lowest value to remove
     * @param high the highest value to remove
     */ 
    public void removeValues(double low, double high) {
        for(int i = 0; i < v.size(); i++) {        
            if(v.elementAt(i) >= low
               && v.elementAt(i) <= high) {
                v.removeElementAt(i);
                i--;
            }
        }
    }

    /**
     * Get the sum of all values
     * 
     * @return double the sum of all values
     */
    public double sum() {
        if(v.isEmpty()) return(0.0D / 0.0D);

        double tot = 0.0D;
        for (Double aV : v) {
            tot += aV;
        }
        return tot;
    }    

    private Vector<Double> sorted() {
        Vector<Double> cpy = new Vector<Double>();
        cpy.addAll(v);
        Collections.sort(cpy);
        return cpy;
    }
}
