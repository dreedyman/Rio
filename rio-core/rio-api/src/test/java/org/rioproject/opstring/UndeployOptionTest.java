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

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Test basic interactions with the {@code UndeployOption}
 *
 * @author Dennis Reedy
 */
public class UndeployOptionTest {

    @Test
    public void testIdle() {
        UndeployOption undeployOption = new UndeployOption(5l, UndeployOption.Type.WHEN_IDLE);
        Assert.assertTrue(undeployOption.getType().equals(UndeployOption.Type.WHEN_IDLE));
        Assert.assertTrue(undeployOption.getWhen()==5);
    }


    @Test
    public void testWhen() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2013, Calendar.OCTOBER, 31);
        UndeployOption undeployOption = new UndeployOption(cal1.getTime().getTime(), UndeployOption.Type.ON_DATE);
        Assert.assertTrue(undeployOption.getType().equals(UndeployOption.Type.ON_DATE));
        Date date = new Date(undeployOption.getWhen());
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);
        Assert.assertTrue(cal1.equals(cal2));
    }
}
