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
package org.rioproject.impl.exec;

import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.impl.fdh.FaultDetectionListener;

import java.util.concurrent.TimeUnit;

/**
 * Test {@code JVMProcessMonitor} interactions
 *
 * @author Dennis Reedy
 */
public class JVMProcessMonitorTest {

    @Test
    public void testJVMProcessMonitorConstruction() throws InterruptedException {
        Client client1 = new Client();
        Client client2 = new Client();
        Client client3 = new Client();
        new Thread(client1).start();
        new Thread(client2).start();
        new Thread(client3).start();
        while(client1.jvmProcessMonitor==null ||
              client2.jvmProcessMonitor==null ||
              client3.jvmProcessMonitor==null) {
            Thread.sleep(10);
        }
        Assert.assertEquals(client1.jvmProcessMonitor, client2.jvmProcessMonitor);
        Assert.assertEquals(client2.jvmProcessMonitor, client3.jvmProcessMonitor);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullArgs1() {
        JVMProcessMonitor.getInstance().monitor(null, new Listener());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullArgs2() {
        JVMProcessMonitor.getInstance().monitor("1", null);
    }

    @Test
    public void testNotification() throws InterruptedException {
        Listener l = new Listener();
        JVMProcessMonitor.getInstance().monitor("-1", l);
        while(l.serviceID==null) {
            Thread.sleep(100);
        }
        Assert.assertNotNull(l.serviceID);
        Assert.assertEquals("-1", l.serviceID);
        while(JVMProcessMonitor.getInstance().getMonitorReaper()!=null){
            Thread.sleep(TimeUnit.SECONDS.toMillis(JVMProcessMonitor.getInstance().getMonitorReaper().getReapInterval()));
        }
        Assert.assertNull(JVMProcessMonitor.getInstance().getMonitorReaper());
    }

    @Test
    public void testNotification2() throws InterruptedException {
        Listener l1 = new Listener();
        JVMProcessMonitor.getInstance().monitor(VirtualMachineHelper.getID(), l1);
        Listener l2 = new Listener();
        JVMProcessMonitor.getInstance().monitor("-1", l2);
        while(l2.serviceID==null) {
            Thread.sleep(100);
        }
        Assert.assertNotNull(l2.serviceID);
        Assert.assertEquals("-1", l2.serviceID);
        Assert.assertNotNull(JVMProcessMonitor.getInstance().getMonitorReaper());
        JVMProcessMonitor.getInstance().clear();
        while(JVMProcessMonitor.getInstance().getMonitorReaper()!=null) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(JVMProcessMonitor.getInstance().getMonitorReaper().getReapInterval()));
        }
        Assert.assertNull(JVMProcessMonitor.getInstance().getMonitorReaper());
    }

    @Test
    public void testReapInterval() throws InterruptedException {
        JVMProcessMonitor monitor = JVMProcessMonitor.getInstance();
        monitor.clear();
        System.setProperty(JVMProcessMonitor.REAP_INTERVAL, "3");
        monitor.monitor(VirtualMachineHelper.getID(), new Listener());
        monitor.clear();
        int interval = getReapInterval(monitor);
        Assert.assertEquals(3, interval);
        /* Wait for the created MonitorReaper to be null before returning*/
        while(JVMProcessMonitor.getInstance().getMonitorReaper()!=null) {
            Thread.sleep(500);
        }
    }

    int getReapInterval(JVMProcessMonitor JVMProcessMonitor) {
        return JVMProcessMonitor.getMonitorReaper()==null?-1: JVMProcessMonitor.getMonitorReaper().getReapInterval();
    }

    class Listener implements FaultDetectionListener<String> {
        String serviceID;

        @Override
        public void serviceFailure(Object service, String serviceID) {
            this.serviceID = serviceID;
        }
    }

    class Client implements Runnable {
        JVMProcessMonitor jvmProcessMonitor;

        @Override
        public void run() {
            jvmProcessMonitor = JVMProcessMonitor.getInstance();
        }
    }
}
