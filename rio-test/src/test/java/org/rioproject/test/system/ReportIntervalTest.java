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
package org.rioproject.test.system;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.test.RioTestRunner;
import org.rioproject.test.Utils;

import java.util.Observable;
import java.util.Observer;

/**
 * Test report interval notification
 */
@RunWith(RioTestRunner.class)
public class ReportIntervalTest {

    @Test
    public void atOneSecondIntervals() {
        CRO cro = new CRO();
        Throwable thrown = null;
        try {
            ComputeResource cr = new ComputeResource();
            cr.setReportInterval(1000);
            cr.boot();
            cr.addObserver(cro);
            Assert.assertEquals("Report interval should be 1000 millis",
                                1000, cr.getReportInterval());
            for(MeasurableCapability mCap : cr.getMeasurableCapabilities()) {
                mCap.setPeriod(1000);
            }
            Utils.sleep(5000);
            Assert.assertEquals("Should have been notified 5 times", 5, cro.count);
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNull("Should not have thrown an exception creating " +
                          "a ComputeResource", thrown);
    }

    @Test
    public void justFirstUpdate() {
        CRO cro = new CRO();
        Throwable thrown = null;
        try {
            ComputeResource cr = new ComputeResource();
            cr.boot();
            cr.addObserver(cro);
            cr.setReportInterval(1000);
            Assert.assertEquals("Report interval should be 1000 millis",
                                1000, cr.getReportInterval());
            for(MeasurableCapability mCap : cr.getMeasurableCapabilities()) {
                mCap.setPeriod(10000);
            }
            Utils.sleep(5000);
            Assert.assertEquals("Should have been notified just 1 time", 1, cro.count);
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNull("Should not have thrown an exception creating " +
                          "a ComputeResource", thrown);
    }

    @Test
    public void checkMeasurableCapabilityReportRate() {
        Throwable thrown = null;
        try {
            ComputeResource cr = new ComputeResource();
            cr.boot();
            for(MeasurableCapability mCap : cr.getMeasurableCapabilities()) {
                mCap.setSampleSize(1);
                mCap.setPeriod(1000);                
            }

            for(MeasurableCapability mCap : cr.getMeasurableCapabilities()) {
                Assert.assertEquals("MeasurableCapability ["+mCap.getId()+"] " +
                                    "period should be 1000",
                                    1000, mCap.getPeriod());
            }
            
        } catch (Exception e) {
            thrown = e;
        }
        Assert.assertNull("Should not have thrown an exception creating " +
                          "a ComputeResource", thrown);
    }

    class CRO implements Observer {
        int count;
        public void update(Observable o, Object arg) {
            count++;
            /*
            ResourceCapability rCap = (ResourceCapability) arg;
            for (MeasuredResource mRes : rCap.getMeasuredResources()) {
                if(mRes.getIdentifier().equalsIgnoreCase("memory")) {
                    System.out.println(mRes);
                    System.out.println("===> value="+mRes.getValue());
                }
            }
            */
        }
    }

}