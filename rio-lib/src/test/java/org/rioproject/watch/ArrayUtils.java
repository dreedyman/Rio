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
 * The class provides a set of array utility methods.
 */
public class ArrayUtils {

    /**
     * Converts an array of <code>int</code>s to an array of
     * <code>Integer</code>s.
     *
     * @param ints the array of <code>int</code>s
     * @return the resulting array of <code>Integer</code>s
     */
    public static Integer[] asObjects(int[] ints) {
        Integer[] res = new Integer[ints.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = ints[i];
        }
        return res;
    }

    /**
     * Converts an array of <code>boolean</code>s to an array of
     * <code>Boolean</code>s.
     *
     * @param bools the array of <code>boolean</code>s
     * @return the resulting array of <code>Boolean</code>s
     */
    public static Boolean[] asObjects(boolean[] bools) {
        Boolean[] res = new Boolean[bools.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = bools[i];
        }
        return res;
    }

    /**
     * Builds a list of all possible combinations for a given set
     * of axes. An axis is an array of objects. A combination is an
     * array of objects each of which is taken from a different axis.
     * The number of objects in a combination is equal to the number
     * of axis. Objects appear in a combination in the order of
     * their respective axes.  
     *
     * @param axes the array of axes
     * @return the array of all possible combinations
     */
    public static Object[][] combinations(Object[][] axes) {
        int size = 1;
        if (axes.length == 0) {
            size = 0;
        }
        for (Object[] axe : axes) {
            size = size * axe.length;
        }
        Object[][] res = new Object[size][axes.length];
        int[] indices = new int[axes.length];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < axes.length; j++) {
                res[i][j] = axes[j][indices[j]];
            }
            // Next index set
            int j = indices.length - 1;
            while(true) {
                indices[j]++;
                if (indices[j] == axes[j].length) {
                    indices[j] = 0;
                    j--;
                    if (j == -1) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return res;
    }


    /**
     * Calculates the sum of array elements.
     *
     * @param array the array of elements
     * @return the sum of array elements
     */
    public static int sum(int[] array) {
        int sum = 0;
        for (int anArray : array) {
            sum += anArray;
        }
        return sum;
    }
}
