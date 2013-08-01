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

import junit.framework.Assert;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Test to make sure that a {@code StopWatch} can be used concurrently.
 *
 * @author Dennis Reedy
 */
public class ConcurrentStopWatchTest {
    @Test
    public void testConcurrency() throws RemoteException, InterruptedException {
        StopWatch stopWatch = new StopWatch("Test");
        CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread(new StopWatchRunner(stopWatch, 100, countDownLatch)).start();
        new Thread(new StopWatchRunner(stopWatch, 50, countDownLatch)).start();
        countDownLatch.await();
        List<Double> runner1 = new ArrayList<Double>();
        List<Double> runner2 = new ArrayList<Double>();
        for(Calculable c : stopWatch.getWatchDataSource().getCalculable()) {
            if(c.getValue()<100) {
                runner1.add(c.getValue());
                /* Allow for some variance in the recorded time */
                Assert.assertTrue("Expected value to be "+c.getValue()+" < 100", c.getValue()<100);
            } else {
                runner2.add(c.getValue());
                Assert.assertTrue("Expected value to be "+c.getValue()+" < 200", c.getValue()<200);
            }
        }
        Statistics stats1 = new Statistics(runner1);
        Statistics stats2 = new Statistics(runner2);
        System.out.println("Runner 1 stats: mean="+stats1.mean()+",  low="+stats1.min()+",  high="+stats1.max());
        System.out.println("Runner 2 stats: mean="+stats2.mean()+", low="+stats2.min()+", high="+stats2.max());
    }

    private class StopWatchRunner implements Runnable {
        StopWatch stopWatch;
        long delay;
        CountDownLatch countDownLatch;

        StopWatchRunner(StopWatch stopWatch, long delay, CountDownLatch countDownLatch) {
            this.stopWatch = stopWatch;
            this.delay = delay;
            this.countDownLatch = countDownLatch;
        }

        public void run() {
            for(int i=0; i<100; i++) {
                stopWatch.startTiming();
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopWatch.stopTiming();
            }
            System.out.println("StopWatchRunner "+Thread.currentThread().getId()+" completed");
            countDownLatch.countDown();
        }
    }
}
