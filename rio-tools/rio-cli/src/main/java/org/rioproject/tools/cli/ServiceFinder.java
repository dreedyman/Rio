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

import com.sun.jini.lookup.entry.LookupAttributes;
import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.*;
import net.jini.lookup.entry.Host;
import net.jini.lookup.entry.Name;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.entry.ComputeResourceInfo;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.resources.client.DiscoveryManagementPool;
import org.rioproject.tools.discovery.RecordingDiscoveryListener;
import org.rioproject.tools.discovery.ReggieStat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Find a service
 *
 * @author Dennis Reedy
 */
public class ServiceFinder {
    private ServiceDiscoveryManager sdm;
    private LookupCache allServicesCache;
    private LookupCache cybernodeCache;
    private LookupCache monitorCache;
    private ServiceTemplate allServices;
    private ServiceTemplate cybernodeServices;
    private ServiceTemplate monitorServices;
    /** Collection of service info  */
    private final List<ServiceInfo> serviceInfo = new ArrayList<ServiceInfo>();
    /** ThreadPool for fetching service information */
    private Executor serviceInfoFetchPool;
    /** Table of ServiceInfo and InfoFetchStats */
    private final Map<ServiceInfo, InfoFetchStat> serviceInfoFetchMap = new HashMap<ServiceInfo, InfoFetchStat>();
    private RecordingDiscoveryListener recordingDiscoveryListener;
    private ProxyPreparer proxyPreparer;
    private ProxyPreparer adminProxyPreparer;

    /**
     * Create a ServiceFinder
     * 
     * @param groups The groups to discover
     * @param locators The LookupLocators to use
     * @param config The Configuration to set
     * 
     * @throws IOException If there are problems initializing discovery
     * @throws ConfigurationException If problems are encountered accessing
     * the configuration
     */
    public ServiceFinder(final String[] groups, final LookupLocator[] locators, final Configuration config)
        throws IOException, ConfigurationException {

        proxyPreparer = (ProxyPreparer)config.getEntry(CLI.CONFIG_COMPONENT,
                                                       "proxyPreparer",
                                                       ProxyPreparer.class,
                                                       new BasicProxyPreparer());
        adminProxyPreparer = (ProxyPreparer)config.getEntry(CLI.CONFIG_COMPONENT,
                                                            "adminProxyPreparer",
                                                            ProxyPreparer.class,
                                                            new BasicProxyPreparer());
        int serviceInfoPoolSize = (Integer) config.getEntry(CLI.CONFIG_COMPONENT, "serviceInfoPoolSize", int.class, 5);
        serviceInfoFetchPool = Executors.newFixedThreadPool(serviceInfoPoolSize);

        DiscoveryManagementPool discoPool = DiscoveryManagementPool.getInstance();
        DiscoveryManagement discoMgr = discoPool.getDiscoveryManager("cli", groups, locators, null, config);
        //LookupDiscoveryManager discoMgr =
        //    new LookupDiscoveryManager(groups, locators, null, lookupConfig);
        recordingDiscoveryListener = new RecordingDiscoveryListener(discoMgr);
        discoMgr.addDiscoveryListener(recordingDiscoveryListener);
        sdm = new ServiceDiscoveryManager(discoMgr, new LeaseRenewalManager());

        allServices = new ServiceTemplate(null, null, null);
        cybernodeServices = new ServiceTemplate(null, new Class[] {Cybernode.class}, null);
        monitorServices = new ServiceTemplate(null, new Class[] {ProvisionMonitor.class}, null);
        if(!CLI.getInstance().commandLine) {
            allServicesCache = sdm.createLookupCache(allServices, null, new ServiceListener());
            cybernodeCache = sdm.createLookupCache(cybernodeServices, null, null);
            monitorCache = sdm.createLookupCache(monitorServices, null, null);
        }
    }

    ReggieStat[] getReggieStats(final int type) {
        return(recordingDiscoveryListener.getReggieStats(type));
    }

    /**
     * Terminate the utility
     */
    public void terminate() {
        sdm.terminate();
    }

    /**
     * Find all services
     * 
     * @param machines String array of machine names to filter
     * @param attrs An array of attributes to filter on
     * 
     * @return Array of ServiceItem instances corresponding to the number of 
     * discovered service instances. If no services have been discovered, a 
     * zero-lengh array is returned
     */
    public ServiceItem[] find(final String[] machines, final Entry[] attrs) {
        ServiceItem[] results = new ServiceItem[0];
        try {
            ServiceItemFilter filter = null;
            if((machines!=null && machines.length>0) || attrs!=null && attrs.length>0)
                filter = new ServiceFilter(machines, attrs);
            if(!CLI.getInstance().commandLine) {
                results = allServicesCache.lookup(filter, Integer.MAX_VALUE);
                if(results.length==0)
                    System.out.println("num lookups "+ getDiscoveryManagement().getRegistrars().length+
                                       ", total services "+results.length);
                else
                    System.out.println("total services "+results.length);
            } else {
                long timeOut = (Long) CLI.getInstance().settings.get(CLI.DISCOVERY_TIMEOUT);
                long t0 = System.currentTimeMillis();
                results = sdm.lookup(allServices, 1, Integer.MAX_VALUE, filter, timeOut);
                long t1 = System.currentTimeMillis();
                System.out.println("total "+results.length);
                System.out.println("discovery time "+(t1-t0)+" millis, "+
                                   ((t1-t0)/1000)+" seconds, "+
                                   "timeout used "+timeOut);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(results);
    }

    /**
     * Find all Cybernode services
     * 
     * @param machines String array of machine names to filter
     * @param attrs An array of attributes to filter on
     *
     * @return Array of ServiceItem instances corresponding to the number of
     * discovered Cybernode service instances. If no Cybernode services have
     * been discovered, a zero-lengh array is returned
     */
    public ServiceItem[] findCybernodes(final String[] machines, final Entry[] attrs) {
        ServiceItem[] results = new ServiceItem[0];
        try {
            ServiceItemFilter filter = null;
            if((machines!=null && machines.length>0) ||
               attrs!=null && attrs.length>0)
                filter = new ServiceFilter(machines, attrs);
            if(!CLI.getInstance().commandLine) {
                results = cybernodeCache.lookup(filter, Integer.MAX_VALUE);
                System.out.println("total "+results.length);
            } else {
                long timeOut =
                    (Long) CLI.getInstance().settings.get(CLI.DISCOVERY_TIMEOUT);
                long t0 = System.currentTimeMillis();
                results = sdm.lookup(cybernodeServices, 1, Integer.MAX_VALUE, filter, timeOut);
                long t1 = System.currentTimeMillis();
                System.out.println("total "+results.length);
                System.out.println("discovery time "+(t1-t0)+" millis, "+
                                   "timeout used "+timeOut);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return(results);
    }

    /**
     * Find all Monitor services
     * 
     * @param machines String array of machine names to filter
     * @param attrs An array of attributes to filter on
     *
     * @return Array of ServiceItem instances corresponding to the number of
     * discovered Monitor service instances. If no Monitor services have been
     * discovered, a zero-lengh array is returned
     */
    public ServiceItem[] findMonitors(final String[] machines, final Entry[] attrs) {
        return(findMonitors(machines, attrs, true));
    }

    /**
     * Find all Monitor services
     *
     * @param machines String array of machine names to filter
     * @param attrs An array of attributes to filter on
     * @param verbose For more output set to true
     *
     * @return Array of ServiceItem instances corresponding to the number of
     * discovered Monitor service instances. If no Monitor services have been
     * discovered, a zero-lengh array is returned
     */
    public ServiceItem[] findMonitors(final String[] machines,
                                      final Entry[] attrs,
                                      final boolean verbose) {
        ServiceItem[] results = new ServiceItem[0];
        try {
            ServiceItemFilter filter = null;
            if((machines!=null && machines.length>0) || attrs!=null && attrs.length>0)
                filter = new ServiceFilter(machines, attrs);
            if(!CLI.getInstance().commandLine) {
                results = monitorCache.lookup(filter, Integer.MAX_VALUE);
                if(verbose) {
                    System.out.println("total "+results.length);
                }
            } else {
                long timeOut = (Long) CLI.getInstance().settings.get(CLI.DISCOVERY_TIMEOUT);
                long t0 = System.currentTimeMillis();
                results = sdm.lookup(monitorServices, 1, Integer.MAX_VALUE, filter, timeOut);
                if(verbose) {
                    long t1 = System.currentTimeMillis();
                    System.out.println("total "+results.length);
                    System.out.println("discovery time "+(t1-t0)+" millis, "+
                            "timeout used "+timeOut);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(results);
    }

    /**
     * Get DiscoveryManagement
     *
     * @return The DiscoveryManagement instance being used
     */
    public DiscoveryManagement getDiscoveryManagement() {
        return(sdm.getDiscoveryManager());
    }

    /**
     * Resolve the information for a service in a poolable thread
     *
     * @param sInfo The ServiceInfo to resolve
     *
     * @return a Future
     */
    Future<ServiceInfo> resolveServiceInfo(final ServiceInfo sInfo) {
        InfoFetchStat i = new InfoFetchStat(sInfo.getServiceName());
        i.starTime = System.currentTimeMillis();
        synchronized(serviceInfoFetchMap) {
            serviceInfoFetchMap.put(sInfo, i);
        }
        FutureTask<ServiceInfo> future = new FutureTask<ServiceInfo>(new InfoFetcher(sInfo));
        serviceInfoFetchPool.execute(future);
        return future;
    }

    /**
     * Get the table ServiceInfo instances
     *
     * @return A Map of ServiceInfo to creation time instances. A new Map
     * is created each time. If there are no pending ServiceInfo instances
     * an empty Map is returned
     */
    Map<ServiceInfo, InfoFetchStat> getServiceInfoFetchMap() {
        Map<ServiceInfo, InfoFetchStat> map;
        synchronized(serviceInfoFetchMap) {
            map = new HashMap<ServiceInfo, InfoFetchStat>(serviceInfoFetchMap);
        }
        return map;
    }

    /**
     * A ServiceItemFilter that filters on host names and attributes
     */
    public class ServiceFilter implements ServiceItemFilter {
        private String[] hostNames;
        private Entry[] attrs;

        /**
         * Create an ServiceFilter
         * 
         * @param hostNames The names of hosts to match
         * @param attrs Additional attributes to filter
         */
        public ServiceFilter(final String[] hostNames, final Entry[] attrs) {
            if(hostNames==null)
                this.hostNames = new String[0];
            else
                this.hostNames = hostNames;
            if(attrs==null) {
                this.attrs = new Entry[0];
            } else {
                this.attrs = new Entry[attrs.length];
                System.arraycopy(attrs, 0, this.attrs, 0, attrs.length);
            }
        }

        /**
         * If the input ServiceItem has an 
         * {@link org.rioproject.entry.ComputeResourceInfo} and the hostname or address
         * matches an array element in the <code>hostNames</code> array, return 
         * <code>true</code>
         * 
         * @see net.jini.lookup.ServiceItemFilter#check
         */
        public boolean check(final ServiceItem item) {
            boolean matched = false;
            if(hostNames.length==0) {
                matched = true;
            } else {
                if(item.service instanceof ServiceRegistrar) {
                    try {
                        String host = ((ServiceRegistrar)item.service).getLocator().getHost();
                        for (String hostName : hostNames) {
                            if (hostName.equalsIgnoreCase(host)) {
                                matched = true;
                                break;
                            }
                        }
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    matched = hostNameMatches(item.attributeSets);
                }
            }
            if(matched)
                matched = checkAttributes(item.attributeSets);
            return(matched);
        }

        /**
         * Determine if the attribute collecton contains an ComputeResourceInfo which
         * matches an element in the hostNames array
         * 
         * @param attrs Array of Entry objects
         * 
         * @return If an ComputeResourceInfo is found in the collection and and the
         * hostname or address matches an array element in the
         * <code>hostNames</code> array, return <code>true</code>, otherwise,
         * return <code>false</code>
         */
        public boolean hostNameMatches(final Entry[] attrs) {
            Host host = getHost(attrs);
            if(host!=null)
                return(checkHosts(host));
            ComputeResourceInfo computeResourceInfo = getComputeResourceInfo(attrs);
            if(computeResourceInfo!=null)
                return(checkHosts(computeResourceInfo));
            return(false);
        }

        /**
         * Check the hostNames list
         *
         * @param aInfo The ComputeResourceInfo entry to check
         *
         * @return If the hostname in the ComputeResourceInfo is in the known
         * hostnames, return true
         */
        boolean checkHosts(final ComputeResourceInfo aInfo) {
            boolean found = false;
            for (String hostName : hostNames) {
                if (hostName.equalsIgnoreCase(aInfo.hostAddress) || hostName.equalsIgnoreCase(aInfo.hostName)) {
                    found = true;
                }
            }
            return(found);
        }

        /**
         * Check the hostNames list
         *
         * @param host The host to check
         *
         * @return If found return true
         */
        boolean checkHosts(final Host host) {
            boolean found = false;
            for (String hostName : hostNames) {
                if (hostName.equalsIgnoreCase(host.hostName)) {
                    found = true;
                }
            }
            return (found);
        }


        /**
         * Check attributes list for match
         *
         * @param attributes The array of attributes to check
         *
         * @return True if the attributes match
         */
        boolean checkAttributes(final Entry[] attributes) {
            if(attrs.length==0)
                return(true);
            boolean matched = false;
            int numMatched = 0;
            for (Entry attr : attrs) {
                for (Entry attribute : attributes) {
                    if (LookupAttributes.matches(attr, attribute)) {
                        matched = true;
                        numMatched++;
                        break;
                    }
                }
                if (!matched)
                    break;
            }
            return(numMatched==attrs.length);
        }
    }

    /**
     * A ServiceDiscoveryListener
     */
    public class ServiceListener implements ServiceDiscoveryListener {

        public void serviceAdded(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPostEventServiceItem();
            try {
                if(item.service instanceof ProvisionMonitor)
                    item.service = proxyPreparer.prepareProxy(item.service);
                ServiceInfo sb = new ServiceInfo(item);
                boolean added = false;
                synchronized(serviceInfo) {
                    if(!serviceInfo.contains(sb)) {
                        added = serviceInfo.add(sb);
                    }
                }
                if(added) {
                    resolveServiceInfo(sb);
                }
            } catch (Throwable t) {
                Throwable cause = t;
                if(t.getCause()!=null)
                    cause = t.getCause();
                System.out.println("Unable to administer service, exception " +
                                   "preparing proxy ["+
                                   cause.getClass().getName()+": "+
                                   cause.getMessage()+
                                   "], " +
                                   "check log for details");
                t.printStackTrace();  
            }
        }

        public void serviceRemoved(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPreEventServiceItem();
            ServiceInfo[] info = getServiceInfo();
            for (ServiceInfo anInfo : info) {
                if (anInfo.item.serviceID.equals(item.serviceID)) {
                    synchronized (serviceInfo) {
                        serviceInfo.remove(anInfo);
                    }
                    break;
                }
            }
        }

        public void serviceChanged(final ServiceDiscoveryEvent sdEvent) {
            ServiceItem item = sdEvent.getPostEventServiceItem();
            ServiceInfo[] info = getServiceInfo();
            for (ServiceInfo anInfo : info) {
                if (anInfo.item.serviceID.equals(item.serviceID)) {
                    anInfo.setServiceItem(item);
                    break;
                }
            }
        }
    }

    /**
     * Get all ServiceInfo
     *
     * @return An array of ServiceInfo objects
     */
    public ServiceInfo[] getServiceInfo() {
        ServiceInfo[] info;
        synchronized(serviceInfo) {
            info = serviceInfo.toArray(new ServiceInfo[serviceInfo.size()]);
        }
        return(info);
    }

    /**
     * Class to hold a ServiceItem and the groups the service is a member of.
     * This class is created to pre-fetch group information since it is an
     * expensive operation, as it uses remote invocation to acquire a
     * service's JoinAdmin
     */
    public static class ServiceInfo {
        static final String UNKNOWN="<?>";
        private String name = UNKNOWN;
        private String[] groups = new String[]{UNKNOWN};
        private ServiceItem item;
        private String host = UNKNOWN;

        ServiceInfo(final ServiceItem item) {
            setServiceItem(item);
        }

        private void setServiceItem(final ServiceItem item) {
            this.item = item;
            name = getName(item.attributeSets);
            if(name == null)
                name = item.service.getClass().getName();
        }

        /*
         * Get the service name
         */
        String getServiceName() {
            return(name);
        }

        /*
         * Set the hostname
         */
        void setHost(String h) {
            host = h;
        }

        /*
         * Get the hostname
         */
        String getHost() {
            return(host);
        }

        /*
         * Set the groups
         */
        void setGroups(final String[] g) {
            groups = new String[g.length];
            System.arraycopy(g, 0, groups, 0, g.length);
        }

        /*
         * Get the groups
         */
        String[] getGroups() {
            return(groups);
        }

        /*
         * Get the ServiceItem
         *
         * @return The ServiceItem
         */
        ServiceItem getServiceItem() {
            return(item);
        }

        @Override
        public int hashCode() {
            return(item.serviceID.hashCode());
        }

        @Override
        public boolean equals(final Object obj) {
            if(this == obj)
                return(true);
            if(!(obj instanceof ServiceInfo))
                return(false);
            ServiceInfo that = (ServiceInfo)obj;
            return(this.item.serviceID.equals(that.item.serviceID));
        }
    }

    /**
     * Used to fetch information about services
     */
    public class InfoFetcher implements Callable<ServiceInfo> {
        ServiceInfo sInfo;

        InfoFetcher(final ServiceInfo sInfo) {
            this.sInfo = sInfo;
        }

        void updateHost() {
            InfoFetchStat ifs;
            synchronized(serviceInfoFetchMap) {
                ifs = serviceInfoFetchMap.get(sInfo);
                ifs.host = sInfo.host;
                serviceInfoFetchMap.put(sInfo, ifs);
            }
        }

        @Override
        public ServiceInfo call() throws Exception {
            try {
                ServiceItem item = sInfo.getServiceItem();
                if(item.service instanceof ServiceRegistrar) {
                    try {
                        LookupLocator loc = ((ServiceRegistrar)item.service).getLocator();
                        sInfo.setHost(loc.getHost()+":"+loc.getPort());
                        updateHost();
                    } catch(RemoteException e) {
                        e.printStackTrace();
                    }
                } else {
                    Host host = getHost(item.attributeSets);
                    if(host!=null) {
                        sInfo.setHost(host.hostName);
                        updateHost();
                    } else {
                        ComputeResourceInfo computeResourceInfo = getComputeResourceInfo(item.attributeSets);
                        if(computeResourceInfo != null) {
                            /*if(computeResourceInfo.hostName.equals(computeResourceInfo.hostAddress))
                                sInfo.setHost(computeResourceInfo.hostName);
                            else*/
                                sInfo.setHost(computeResourceInfo.hostName+"@"+computeResourceInfo.hostAddress);

                            updateHost();
                        }
                    }
                }
                if(item.service instanceof Administrable) {
                    try {
                        String [] groups = new String[0];
                        Object admin = ((Administrable)item.service).getAdmin();
                        if(admin instanceof JoinAdmin) {
                            groups = ((JoinAdmin)admin).getLookupGroups();
                        }

                        /* If no groups were returned and the Administrable object
                         * additionally implements DiscoveryAdmin
                         */
                        if(groups!=null && groups.length == 0 && (admin instanceof DiscoveryAdmin))
                            groups = ((DiscoveryAdmin)admin).getMemberGroups();

                        if(groups == DiscoveryGroupManagement.ALL_GROUPS)
                            groups = new String[]{"ALL_GROUPS"};
                        sInfo.setGroups(groups);
                    } catch(RemoteException e) {
                        System.err.println("EXCEPTION GETTING GROUPS FOR ["+sInfo.getServiceName()+"]");
                        e.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                System.err.println("EXCEPTION GETTING ServiceInfo FOR ["+sInfo.getServiceName()+"]");
                t.printStackTrace();
            } finally {
                long t1 = System.currentTimeMillis();
                InfoFetchStat ifs;
                int size;
                synchronized(serviceInfoFetchMap) {
                    ifs = serviceInfoFetchMap.remove(sInfo);
                    size = serviceInfoFetchMap.size();
                }
                ifs.stopTime = t1;
                if(!CLI.getInstance().commandLine)
                    System.err.println("XXXX ServiceInfo resolve time for " +
                                       "["+sInfo.getServiceName()+"] = "+
                                       (t1-ifs.starTime)+", pool size = "+size);
            }
            return sInfo;
        }
    }

    /**
     * Simple data structure holding the service name, the time the
     * fetch request began, and the time it ended
     */
    public static class InfoFetchStat {
        String name;
        String host=ServiceInfo.UNKNOWN;
        long starTime;
        long stopTime;

        InfoFetchStat(final String name) {
            this.name = name;
        }
    }

    /**
     * Get the name of the service from either the Name attribute or the ServiceInfo
     * attribute
     *
     * @param attrs Array of Entry attributes
     *
     * @return The name of the service from either the Name attribute or the
     * ServiceInfo attribute
     */
    public static String getName(final Entry[] attrs) {
        String name = null;
        for (Entry attr : attrs) {
            if (attr instanceof Name) {
                name = ((Name) attr).name;
                break;
            }
        }
        if(name==null) {
            for (Entry attr : attrs) {
                if (attr instanceof net.jini.lookup.entry.ServiceInfo) {
                    name = ((net.jini.lookup.entry.ServiceInfo)attr).name;
                    break;
                }
            }
        }
        return(name);
    }

    /**
     * Get the Host attribute
     *
     * @param attrs Array of attributes
     *
     * @return A Host object
     */
    public static Host getHost(final Entry[] attrs) {
        Host host = null;
        for (Entry attr : attrs) {
            if (attr.getClass().getName().equals(
                Host.class.getName())) {
                if (attr instanceof Host) {
                    host = (Host) attr;
                    break;
                } else {
                    /*
                     * This addresses the issue where the discovered service
                     * has a Host but there is a class loading
                     * problem, which results in the classes being loaded by
                     * sibling class loaders, and assignability doesnt work.
                     */
                    host = new Host();
                    try {
                        Field hn = attr.getClass().getDeclaredField("hostName");
                        host.hostName = (String) hn.get(attr);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return(host);
    }

    /**
     * Get the ComputeResourceInfo attribute
     *
     * @param attrs An array of attributes to search
     *
     * @return If found, the ComputeResourceInfo entry
     */
    public static ComputeResourceInfo getComputeResourceInfo(final Entry[] attrs) {
        ComputeResourceInfo computeResourceInfo = null;
        for (Entry attr : attrs) {
            if (attr.getClass().getName().equals(ComputeResourceInfo.class.getName())) {
                if (attr instanceof ComputeResourceInfo) {
                    computeResourceInfo = (ComputeResourceInfo) attr;
                    break;
                } else {
                    /*
                     * This addresses the issue where the discovered service
                     * has an ComputeResourceInfo but there is a class loading
                     * problem, which results in the classes being loaded by sibling
                     * class loaders, and assignability doesnt work.
                     */
                    computeResourceInfo = new ComputeResourceInfo();
                    try {
                        Field ha = attr.getClass().getDeclaredField("hostAddress");
                        Field hn = attr.getClass().getDeclaredField("hostName");
                        computeResourceInfo.hostAddress = (String) ha.get(attr);
                        computeResourceInfo.hostName = (String) hn.get(attr);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return (computeResourceInfo);
    }

    /**
     * Get the Adminstrable proxy for a service, and prepare the returned proxy
     * with the adminProxyPreparer
     *
     * @param proxy The service proxy
     * @return A prepared Administrable proxy
     *
     * @throws RemoteException If errors occur
     */
    public Object getPreparedAdmin(final Object proxy) throws RemoteException {
        Object admin = ((Administrable)proxy).getAdmin();
        return(adminProxyPreparer.prepareProxy(admin));
        /*
        if(CLI.getResolver().getLoginContext()!=null) {
            final Object temp = admin;
               try {
                admin =
                    Subject.doAsPrivileged(
                        CLI.getResolver().getLoginContext().getSubject(),
                        new PrivilegedExceptionAction() {
                            public Object run() throws Exception {
                                return(adminProxyPreparer.prepareProxy(temp));
                            }
                        },
                        null);
            } catch (PrivilegedActionException e) {
                e.printStackTrace();
            }
        }
        return(admin);
        */
    }

}

