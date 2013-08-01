/*
 * Copyright 2008 the original author or authors.
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

/**
 * Provides a standard MBean to use when administering a CounterWatch using JMX
 */
public interface CounterWatchMBean extends ThresholdWatchMBean {
    /**
     * Getter for property counter
     *
     * @return Value of property counter.
     */
    long getCounter();

    /**
     * Setter for property counter
     *
     * @param counter New value of property counter.
     */
    void setCounter(long counter);

    /**
     * Increment the count by one
     */
    void increment();

    /**
     * Increment the count
     *
     * @param value the amount to increment the counter
     */
    void increment(long value);

    /**
     * Decrement the count by one
     */
    void decrement();

    /**
     * Decrement the count
     *
     * @param value the amount to decrement the counter
     */
    void decrement(long value);
}
