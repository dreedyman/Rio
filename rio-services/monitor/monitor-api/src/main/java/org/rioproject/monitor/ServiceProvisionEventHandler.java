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
package org.rioproject.monitor;

import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.opstring.ServiceElement;

import java.rmi.server.ExportException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to handle ServiceProvisionEvent notifications
 *
 * @author Dennis Reedy
 */
public class ServiceProvisionEventHandler implements ServiceProvisionListener,
                                                     ServerProxyTrust {
    private Exporter exporter;
    private ServiceProvisionListener provisionListener;
    private final AtomicInteger provisionedSuccessfully = new AtomicInteger();
    private final AtomicInteger provisionFailures = new AtomicInteger();
    private CountDownLatch serviceCounter;

    /**
     * Create the ServiceProvisionEventHandler utility
     *
     * @throws java.rmi.server.ExportException If the exporter cannot be
     * created
     */
    public ServiceProvisionEventHandler() throws ExportException {
        exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                         new BasicILFactory(),
                                         false,
                                         true);
        provisionListener = (ServiceProvisionListener) exporter.export(this);
    }

    /**
     * Unexport this utility
     */
    public void unexport() {
        exporter.unexport(true);
    }

    /**
     * Get the ServiceProvisionListener proxy
     *
     * @return The ServiceProvisionListener proxy
     */
    public ServiceProvisionListener getServiceProvisionListener() {
        return (provisionListener);
    }

    /**
     * Set the number of notifications
     *
     * @param serviceCount the number of services
     */
    public void setServiceCount(int serviceCount) {
        provisionedSuccessfully.set(0);
        provisionFailures.set(0);
        serviceCounter = new CountDownLatch(serviceCount);
    }

    /**
     * For for the service notifications timeout
     *
     * @param maxTimeout The maximum amount of time (in millis) to wait
     * @return The number of services that have been succesfully provisioned
     */
    public int await(long maxTimeout) {
        try {
            serviceCounter.await(maxTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return provisionedSuccessfully.get();
    }

    private void receivedNotify() {
        serviceCounter.countDown();
    }

    public void succeeded(ServiceBeanInstance jsbInstance) {
        provisionedSuccessfully.incrementAndGet();
        receivedNotify();
    }

    public void failed(ServiceElement sElem, boolean resubmitted) {
        provisionFailures.incrementAndGet();
        receivedNotify();
    }

    public TrustVerifier getProxyVerifier() {
        return new BasicProxyTrustVerifier(provisionListener);
    }
}

