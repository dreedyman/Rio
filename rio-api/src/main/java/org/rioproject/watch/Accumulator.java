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

import java.util.Vector;
import java.rmi.RemoteException;

/**
 * The Accumulator represents a collection of Calculable objects and performs
 * statistical analysis across the collection of Calculable objects, providing
 * mean, median and standard deviation
 *
 * @author Dennis Reedy
 */
public class Accumulator {
    private Calculable[] calcs;
    /** Holds reference to the statistical formulae implementor */
    private Statistics statistics = new Statistics();
    /** Holds value of property source. */
    private WatchDataSource source;

    /**
     * Creates new Accumulator
     *
     * @param source The WatchDataSource the Accumulator will use
     */
    public Accumulator(WatchDataSource source) {
        this.source = source;
    }

    /*
     * Returns a Calculable[]
     *
     * @return An array of Calculable objects. If there are no Calculable
     * objects, return a zero-length array
     */
    public Calculable[] getCalcs() {
        if(calcs == null)
            return (new Calculable[0]);
        Calculable[] c = new Calculable[calcs.length];
        System.arraycopy(calcs, 0, c, 0, calcs.length);
        return (c);
    }

    /*
     * Returns a Vector of Double for the values that will be used by the
     * calculator
     */
    private Vector<Double> getValues(Calculable[] calcs) {
        Vector<Double> vals = new Vector<Double>(calcs.length);
        for (Calculable calc : calcs) {
            vals.add(calc.getValue());
        }
        return (vals);
    }

    /*
     * Returns a Calculable[] for the requested offset and length
     */
    private Calculable[] getCalculables() throws RemoteException {
        calcs = source.getCalculable();
        return (calcs);
    }

    /**
     * Clears the current set of values
     */
    public void reset() {
        statistics.clearAll();
    }

    /**
     * Initialize the range of values for calculating statistics
     *
     * @throws RemoteException If communication errors happen interfacing with
     * the WatchDataSource
     */
    public void init() throws RemoteException {
        reset();
        statistics.setValues(getValues(getCalculables()));
    }

    /**
     * Get the count of the current set of values
     * 
     * @return int the total number of values
     */
    public int count() {
        return (statistics.count());
    }


    /**
     * Get the max value of the current set of values
     * 
     * @return double the max of values
     */
    public double max() {
        return (statistics.max());
    }


    /**
     * Get the mean of the current set of values
     * 
     * @return double the mean of the values
     */
    public double mean() {
        return (statistics.mean());
    }

    /**
     * Get the median of the current set of values
     * 
     * @return double the median of the values
     */
    public double median() {
        return (statistics.median());
    }

    /**
     * Get the min of the current set of values
     * 
     * @return double the min of the values
     */
    public double min() {
        return (statistics.min());
    }    

    /**
     * Get the mode of the current set of values
     * 
     * @return double the mode of the values
     */
    public double mode() {
        return (statistics.mode());
    }

    /**
     * Get the number of occurrences of the mode in the current set of values
     * 
     * @return int the mode count
     */
    public int modeCount() {
        return (statistics.modeOccurrenceCount());
    }

    /**
     * Get the range of the current set of values
     * 
     * @return double the range of the values
     */
    public double range() {
        return (statistics.range());
    }

    /**
     * Get the standard deviation of the current set of values
     * 
     * @return double the standard deviation of the values
     */
    public double standardDeviation() {
        return (statistics.standardDeviation());
    }

    /**
     * Get the sum of the current set of values
     * 
     * @return double the sum of the values
     */
    public double sum() {
        return (statistics.sum());
    }

    /**
     * Getter for property source.
     * 
     * @return Value of property source.
     */
    public WatchDataSource getSource() {
        return (source);
    }

    /**
     * Setter for property source.
     * 
     * @param source New value of property source.
     */
    public void setSource(WatchDataSource source) {
        this.source = source;
    }
}
