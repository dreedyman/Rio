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

import org.junit.Assert;
import org.junit.Test;

import java.io.*;

/**
 * Tests loading older version of the {@code ThresholdEvent}. Both older and newer versions have the same serialVersionUID,
 * this test verifies that the custom de-serialization works
 *
 * @author Dennis Reedy
 */
public class ThresholdEventLoadUTest {

    @Test
    public void loadVersion1() throws IOException, ClassNotFoundException {
        File v1Cleared = new File("src/test/resources/ThresholdEventCleared.ser");
        File v1Breached = new File("src/test/resources/ThresholdEventBreached-Old.ser");
        ThresholdEvent cleared = loadThresholdEvent(v1Cleared);
        Assert.assertEquals(ThresholdType.CLEARED, cleared.getThresholdType());
        ThresholdEvent breached = loadThresholdEvent(v1Breached);
        Assert.assertEquals(ThresholdType.BREACHED, breached.getThresholdType());
        printEvent(cleared, breached);
    }

    @Test
    public void writeAndLoadVersion2() throws IOException, ClassNotFoundException {
        Calculable calculable = new Calculable("foo", 1);
        ThresholdValues values = new ThresholdValues(0, 1);
        ThresholdEvent event1 = new ThresholdEvent(new Dummy(), calculable, values, ThresholdType.BREACHED);
        ThresholdEvent event2 = new ThresholdEvent(new Dummy(), calculable, values, ThresholdType.CLEARED);

        File v2Cleared = new File("target/ThresholdEventClearedv2.ser");
        File v2Breached = new File("target/ThresholdEventBreachedv2.ser");

        writeThresholdEvent(v2Breached, event1);
        writeThresholdEvent(v2Cleared, event2);

        ThresholdEvent cleared = loadThresholdEvent(v2Cleared);
        verify(cleared, ThresholdType.CLEARED, calculable, values);
        ThresholdEvent breached = loadThresholdEvent(v2Breached);
        verify(breached, ThresholdType.BREACHED, calculable, values);

        printEvent(cleared, breached);
    }

    private void verify(ThresholdEvent event, ThresholdType type, Calculable calculable, ThresholdValues values) {
        Assert.assertEquals(type, event.getThresholdType());
        Assert.assertEquals(calculable, event.getCalculable());
        Assert.assertEquals(values.getLowThreshold(), event.getThresholdValues().getLowThreshold(), 0);
        Assert.assertEquals(values.getHighThreshold(), event.getThresholdValues().getHighThreshold(), 0);

    }

    private void printEvent(ThresholdEvent... events) {
        StringBuilder builder = new StringBuilder();
        for (ThresholdEvent event : events) {
            builder.append(event.getClass().getName()).append("\n");
            builder.append("\tCalculable:      ").append(event.getCalculable()).append("\n");
            builder.append("\tDate:            ").append(event.getDate()).append("\n");
            builder.append("\tSequence Number: ").append(event.getSequenceNumber()).append("\n");
            builder.append("\tThresholdType:   ").append(event.getThresholdType()).append("\n");
        }
        System.out.println(builder.toString());
    }

    private ThresholdEvent loadThresholdEvent(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream input = new ObjectInputStream(new FileInputStream(file));
        return (ThresholdEvent) input.readObject();
    }

    private void writeThresholdEvent(File file, ThresholdEvent event) throws IOException {
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
        output.writeObject(event);
    }
}
