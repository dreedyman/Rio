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
package org.rioproject.costmodel;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the GenericCostModel
 */
public class GenericCostModelTest {
    long twoSeconds = 1000 * 2;
    long sixMinutes = 1000 * 60 * 6;
    long twoHours = 1000 * 60 * 60 * 2;
    long sixHours = 1000 * 60 * 60 * 6;
    long twoDays = 1000 * 60 * 60 * 24 * 2;
    long tenDays = 1000 * 60 * 60 * 24 * 10;

    @Test
    public void testGenericCostModel() {
        GenericCostModel gcm = new GenericCostModel(0.01);
        gcm.addTimeBoundary(new ResourceCostModel.TimeBoundary(5, 10, ResourceCostModel.TimeBoundary.SECONDS));
        gcm.addTimeBoundary(new ResourceCostModel.TimeBoundary(5, 100, ResourceCostModel.TimeBoundary.MINUTES));
        System.out.println("Testing " + gcm.getClass().getName() + "\n");
        System.out.println(gcm.getDescription());
        Assert.assertTrue("Cost per unit for 2 seconds should be 0.01", gcm.getCostPerUnit(twoSeconds)==0.01);
        Assert.assertTrue("Cost per unit for 6 minutes should be 1.0", gcm.getCostPerUnit(sixMinutes) == 1.0);
        Assert.assertTrue("Cost per unit for 2 hours should be 1.0", gcm.getCostPerUnit(twoHours) == 1.0);
    }

    @Test
    public void testGenericCostModel2() {
        ResourceCostModel.TimeBoundary[] timeBoundaries =
            new ResourceCostModel.TimeBoundary[]{new ResourceCostModel.TimeBoundary(5, 1000, ResourceCostModel.TimeBoundary.HOURS),
                                                 new ResourceCostModel.TimeBoundary(5, 10000, ResourceCostModel.TimeBoundary.DAYS)};
        GenericCostModel gcm = new GenericCostModel(0.01, timeBoundaries);
        System.out.println("Testing " + gcm.getClass().getName() + "\n");
        System.out.println(gcm.getDescription());
        Assert.assertTrue("Cost per unit for 2 seconds should be 0.01", gcm.getCostPerUnit(twoSeconds)==0.01);
        Assert.assertTrue("Cost per unit for 6 minutes should be 1.0", gcm.getCostPerUnit(sixMinutes)==0.01);
        Assert.assertTrue("Cost per unit for 2 hours should be 1.0", gcm.getCostPerUnit(twoHours)==0.01);
        Assert.assertTrue("Cost per unit for 6 hours should be 10", gcm.getCostPerUnit(sixHours)==10);
        Assert.assertTrue("Cost per unit for 2 days should be 10", gcm.getCostPerUnit(twoDays)==10);
        Assert.assertTrue("Cost per unit for 10 days should be 100", gcm.getCostPerUnit(tenDays)==100);
    }
}
