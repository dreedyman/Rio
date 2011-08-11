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
package org.rioproject.tools.cli;

import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.deploy.ServiceProvisionListener;
import org.rioproject.opstring.OperationalString;
import org.rioproject.deploy.ServiceBeanInstance;
import org.rioproject.opstring.ServiceElement;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;

/**
 * Class to handle ServiceProvisionEvent notifications
 *
 * @author Dennis Reedy
 */
 public class ServiceProvisionNotification implements ServiceProvisionListener,
                                                      ServerProxyTrust {
     Exporter exporter;
     ServiceProvisionListener provisionListener;
     int provisionedSuccessfully;
     int provisionFailures;
     int notificationCounter;
     int serviceCounter;
     boolean interactive = false;
     final Object mutex = new Object();

    /**
     * Create the ServiceProvisionNotification utility
     *
     * @param config The configuration
     *
     * @throws ConfigurationException If the configuration is no good
     * @throws ExportException If the exporter cannot be created
     */
     public ServiceProvisionNotification(Configuration config) 
     throws ConfigurationException, ExportException {
         Exporter defaultExporter = 
             new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                   new BasicILFactory(), 
                                   false, 
                                   true);
         exporter = (Exporter)config.getEntry("org.rioproject.tools.cli",
                                              "provisionListenerExporter", 
                                              Exporter.class,
                                              defaultExporter);
         provisionListener = (ServiceProvisionListener)exporter.export(this);
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
         return(provisionListener);
     }
     
     /**
      * Set the number of notifications and the timeout
      *
      * @param serviceCount the number of services
      * @param maxTimeout The maximum amount of time (in millis) to wait
      */
     public void notify(int serviceCount, final long maxTimeout) {
         interactive = true;
         notificationCounter = 0;
         serviceCounter = serviceCount;
         synchronized(mutex) {
             try {
                 mutex.wait(maxTimeout);
                 interactive = false;
                 if(notificationCounter<serviceCounter) {
                     System.out.println("\t- There are "+
                                        "["+(serviceCounter-notificationCounter)+"] "+
                                        "service provisioning requests outstanding");
                 }
             } catch(InterruptedException e) {
                 e.printStackTrace();
             }
         }
     }

     private void receivedNotify() {
         if(interactive) {
             notificationCounter++;
             if(notificationCounter==serviceCounter) {
                 synchronized(mutex) {
                     mutex.notifyAll();
                 }
             }
         }
     }
     public void succeeded(ServiceBeanInstance jsbInstance) throws RemoteException {
         provisionedSuccessfully++;         
         if(interactive)
             System.out.println("\t["+(notificationCounter+1)+"] "+
                                jsbInstance.getServiceBeanConfig().getName()+" "+
                                "provisioned to\t"+jsbInstance.getHostAddress());
         receivedNotify();
     }
     
     public void failed(ServiceElement sElem, boolean resubmitted)
     throws RemoteException {
         provisionFailures++;
         receivedNotify();
         if(interactive)
             System.out.println("\t"+sElem.getName()+" provision failure");
     }
     
     public TrustVerifier getProxyVerifier() {
         return new BasicProxyTrustVerifier(provisionListener);
     }

    /**
     * Sum up all services that need to be deployed
     *
     * @param deployment The deployed OperationalString
     *
     * @return The number of services indicated by summing up all planned
     *         values
     */
    public static int sumUpServices(OperationalString deployment) {
        int summation = 0;
        ServiceElement[] elems = deployment.getServices();
        for (ServiceElement elem : elems) {
            summation += elem.getPlanned();
        }
        OperationalString[] nested = deployment.getNestedOperationalStrings();
        for (OperationalString aNested : nested) {
            summation += sumUpServices(aNested);
        }
        return (summation);
    }

    /**
     * Get the name(s) of the deployments for an OperationalString. If the
     * deployment has nested components, return a comma-separated list of
     * deployment names
     *
     * @param deployment The OperationalString
     *
     * @return The name(s) of the deployments for an OperationalString.
     */
    public static String getDeploymentNames(OperationalString deployment) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(deployment.getName());
        OperationalString[] nested = deployment.getNestedOperationalStrings();
        for (OperationalString aNested : nested) {
            buffer.append(", ").append(getDeploymentNames(aNested));
        }
        return (buffer.toString());
    }
}
