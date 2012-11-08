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

import net.jini.config.ConfigurationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.system.ComputeResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.system.ResourceCapabilityChangeListener;
import org.rioproject.system.measurable.MeasurableCapability;
import org.rioproject.test.Utils;

import java.net.UnknownHostException;

/**                                                                            ,
 * Test report interval notification
 */
public class ReportIntervalTest {
    private ComputeResource computeResource;

    @Before
    public void create() throws UnknownHostException, ConfigurationException {
        computeResource = new ComputeResource();
    }

    @After
    public void shutdown() {
        computeResource.shutdown();
    }

    @Test
    public void atOneSecondIntervals() {
        CRO cro = new CRO();
        computeResource.setReportInterval(1000);
        computeResource.boot();
        Assert.assertEquals("Report interval should be 1000 millis",
                            1000, computeResource.getReportInterval());
        computeResource.addListener(cro);
        Utils.sleep(5000);
        Assert.assertEquals("Should have been notified 5 times", 5, cro.count);
    }

    @Test
    public void justOneUpdate() {
        CRO cro = new CRO();
        computeResource.boot();
        computeResource.addListener(cro);
        computeResource.setReportInterval(5000);
        Assert.assertEquals("Report interval should be 5000 millis",
                            5000, computeResource.getReportInterval());
        for (MeasurableCapability mCap : computeResource.getMeasurableCapabilities()) {
            mCap.setPeriod(5000);
        }
        Utils.sleep(6000);
        Assert.assertEquals("Should have been notified once", 1, cro.count);
    }

    @Test
    public void checkMeasurableCapabilityReportRate() {
        computeResource.boot();
        for(MeasurableCapability mCap : computeResource.getMeasurableCapabilities()) {
            mCap.setSampleSize(1);
            mCap.setPeriod(1000);
        }

        for(MeasurableCapability mCap : computeResource.getMeasurableCapabilities()) {
            Assert.assertEquals("MeasurableCapability ["+mCap.getId()+"] " +
                                "period should be 1000",
                                1000, mCap.getPeriod());
        }
    }

    class CRO implements ResourceCapabilityChangeListener {
        int count;

        public void update(ResourceCapability updatedCapability) {
            count++;
        }
    }

}
