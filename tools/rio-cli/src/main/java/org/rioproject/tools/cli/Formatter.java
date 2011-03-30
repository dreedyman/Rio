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

import net.jini.core.lookup.ServiceItem;
import org.rioproject.core.OperationalStringManager;
import org.rioproject.core.ServiceBeanInstance;
import org.rioproject.core.ServiceElement;
import org.rioproject.core.provision.ServiceRecord;
import org.rioproject.cybernode.Cybernode;
import org.rioproject.monitor.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;
import org.rioproject.resources.util.TimeUtil;
import org.rioproject.tools.cli.ServiceFinder.ServiceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for formatting results
 *
 * @author Dennis Reedy
 */
public class Formatter {
    static final int EXPORT_CODEBASE = 1;
    static final int MAX_ITEM_LENGTH = 30;

    public static String asList(ServiceItem[] items) {
        return(asList(items, 0));
    }

    public static String asList(ServiceItem[] items, int options) {
        String[] array = formattedArray(items, options);
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<array.length; i++) {
            if(i>0)
                buffer.append("\n");
            buffer.append(array[i]);
        }
        return(buffer.toString());
    }

    public static String asChoices(ServiceItem[] items) {
        return(asChoices(items, 0));
    }

    public static String asChoices(ServiceItem[] items, int options) {
        StringBuffer buffer = new StringBuffer(asList(items, options));
        buffer.append(String.format("%n%-5s all",
                                    (Object[])
                                    new String[] {"["+(items.length+1)+"]"}));
        return(buffer.toString());
    }

    public static String[] formattedArray(ServiceItem[] items, int options) {
        List<OutputInfo> list = new ArrayList<OutputInfo>();
        ServiceInfo[] serviceInfo = CLI.getInstance().finder.getServiceInfo();
        for(int i=0; i<items.length; i++) {
            ServiceInfo sInfo = null;
            for (ServiceInfo aServiceInfo : serviceInfo) {
                ServiceItem item = aServiceInfo.getServiceItem();
                if (item.serviceID.equals(items[i].serviceID)) {
                    sInfo = aServiceInfo;
                    break;
                }
            }
            if(sInfo==null) {
                sInfo = new ServiceInfo(items[i]);
                Future<ServiceInfo> future = CLI.getInstance().finder.resolveServiceInfo(sInfo);
                try {
                    sInfo = future.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String name = sInfo.getServiceName();
            String[] groups = sInfo.getGroups();
            String host = sInfo.getHost();
            if(groups==null)
                groups = new String[]{"<?>"};

            String[] optionValues= new String[] {"", "", ""};
            String[] optionFormats = new String[] {"%s", "%s", "%s"};            
            if((options & EXPORT_CODEBASE) != 0) {
                String exportCodebase = getExportCodebase(items[i].service);
                /* The leading spaces are for 1.4 formatting, they'll be 
                 * stripped if we use the 1.5 format capabilities */
                optionValues[1] = "   "+exportCodebase;
                optionFormats[1] = "%-32s";
            }

            OutputInfo oi = new OutputInfo();
            oi.itemNum = "["+(i+1)+"]";
            oi.name = truncate(name);
            oi.groups = truncate(fromArray(groups));
            oi.host = truncate(host);
            oi.option0 = optionValues[0].trim();
            oi.option1 = optionValues[1].trim();
            oi.option2 = optionValues[2].trim();
            list.add(oi);
        }
        OutputInfo[] oi =
            list.toArray(new OutputInfo[list.size()]);
        List<String> output = new ArrayList<String>();
        int[] widths = new int[]{0, 0, 0, 0, 0, 0, 0};
        for (OutputInfo anOi : oi) {
            widths[0] = getLongest(anOi.itemNum, widths[0]);
            widths[1] = getLongest(anOi.name, widths[1]);
            widths[2] = getLongest(anOi.groups, widths[2]);
            widths[3] = getLongest(anOi.host, widths[3]);
            widths[4] = getLongest(anOi.option0, widths[4]);
            widths[5] = getLongest(anOi.option1, widths[5]);
            widths[6] = getLongest(anOi.option2, widths[6]);
        }
        /* add some padding */
        widths[0]+=2; widths[1]+=2; widths[2]+=2; widths[3]+=2;

        for (OutputInfo anOi : oi) {
            output.add(String.format("%-" + widths[0] + "s " +
                                     "%-" + widths[1] + "s " +
                                     "%-" + widths[2] + "s " +
                                     "%-" + widths[3] + "s " +
                                     getFormatString(widths[4]) + " " +
                                     getFormatString(widths[5]) + " " +
                                     getFormatString(widths[6]),
                                     (Object[]) new String[]{
                                         anOi.itemNum,
                                         anOi.name,
                                         anOi.groups,
                                         anOi.host,
                                         anOi.option0,
                                         anOi.option1,
                                         anOi.option2}));
        }

        return(output.toArray(new String[list.size()]));
    }

    /*
     * Container class for holding information to be formatted
     */
    private static class OutputInfo {
        String itemNum;
        String name;
        String groups;
        String host;
        String option0;
        String option1;
        String option2;
    }

    private static String truncate(String s) {
        if(s.length()> MAX_ITEM_LENGTH) {
            s = s.substring(0, (MAX_ITEM_LENGTH-3))+"...";
        }
        return(s);
    }

    /*
     * Get the longest length element
     */
    private static int getLongest(String s, int i) {
        return(Math.max(s.length(), i));
    }

    /*
     * Get the format string
     */
    private static String getFormatString(int w) {
        return((w==0?"%s":"%-"+w+"s"));
    }

    /**
     * Produce output for an array of Cybernodes
     * 
     * @param items Array of ServiceItems
     * @param options Options to use
     * @param br A BufferredReader, allows for the user to press enter for more
     * @param out The output PrintStream
     */
    public static void cybernodeLister(ServiceItem[] items,
                                       int options,
                                       BufferedReader br,
                                       PrintStream out) {
        Integer listLength =
            (Integer) CLI.getInstance().settings.get(CLI.LIST_LENGTH);
        String[] cybernodes = Formatter.formattedArray(items, options);
        for(int i=0, lineCounter=1; i<cybernodes.length; i++,lineCounter++) {
            if(lineCounter % listLength==0 && cybernodes.length > lineCounter) {
                out.println(cybernodes[i]);
                promptMore(br, out);
            } else {
                out.println(cybernodes[i]);
                if(items[i].service instanceof Cybernode) {
                    listCybernode((Cybernode)items[i].service,
                                  br,
                                  out,
                                  lineCounter);
                } else {
                    out.println("\tNot a Cybernode");
                }
            }
        }
        out.println();
    }

    /**
     * Produce output for an Cybernode
     *
     * @param cybernode The Cybernode
     * @param br A BufferredReader, allows for the user to press enter for more
     * @param out The output PrintStream
     * @param lineCounter the current line counter
     */
    private static void listCybernode(Cybernode cybernode,
                                      BufferedReader br,
                                      PrintStream out,
                                      int lineCounter) {
        Integer listLength =
            (Integer) CLI.getInstance().settings.get(CLI.LIST_LENGTH);
        try {
            ServiceRecord[] records =
                cybernode.getServiceRecords(
                    ServiceRecord.ACTIVE_SERVICE_RECORD);
            String status;
            try {
                status = cybernode.isEnlisted()?"enlisted":"released";
            } catch (RemoteException e) {
                status = e.getMessage();
            }
            out.println("\tStatus: "+status);
            if(records.length==0) {
                out.println("\tNo contained services");
            } else {
                /* Sort by opstring */
                Map<String, List<ServiceRecord>> map =
                    new HashMap<String, List<ServiceRecord>>();

                for (ServiceRecord record : records) {
                    String opstring =
                        record.getServiceElement().getOperationalStringName();
                    List<ServiceRecord> services;
                    if(map.containsKey(opstring)) {
                        services = map.get(opstring);
                    } else {
                        services = new ArrayList<ServiceRecord>();
                    }
                    services.add(record);
                    map.put(opstring, services);
                }

                for(Map.Entry<String, List<ServiceRecord>> entry : map.entrySet()) {
                    String opstring = entry.getKey();
                    out.println(opstring);
                    lineCounter++;
                    List<ServiceRecord> recordList = entry.getValue();
                    for (ServiceRecord record : recordList) {
                        String groups = fromArray(record.getServiceElement().
                            getServiceBeanConfig().
                            getGroups());
                        String elapsed =
                            TimeUtil.format(record.computeElapsedTime());
                        if(lineCounter % listLength==0)
                            promptMore(br, out);
                        System.out.format("\t%-18.18s %-18.18s %20s%n",
                                          record.getName(),
                                          groups,
                                          elapsed);
                        lineCounter++;
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Produce output for an array of Provision Managers
     * 
     * @param items Array of ServiceItems
     * @param options Options
     * @param br A BufferredReader, allows for the user to press enter for more
     * @param out The output PrintStream
     */
    public static void provisionManagerLister(ServiceItem[] items,
                                              int options,
                                              BufferedReader br,
                                              PrintStream out) {
        Integer listLength =
            (Integer) CLI.getInstance().settings.get(CLI.LIST_LENGTH);
        String[] provisioners = Formatter.formattedArray(items, options);
        for(int i=0, lineCounter=1; i<provisioners.length; i++,lineCounter++) {
            if(lineCounter % listLength==0)
                promptMore(br, out);
            out.println(provisioners[i]);
            try {
                if(items[i].service instanceof ProvisionMonitor) {
                    DeployAdmin deployAdmin =
                        (DeployAdmin)((ProvisionMonitor)items[i].service).getAdmin();
                    OperationalStringManager[] opMgrs =
                        deployAdmin.getOperationalStringManagers();
                    if(opMgrs.length==0)
                        out.println("\tNo Managed Deployments");
                    for (OperationalStringManager opMgr : opMgrs) {
                        listMgr(opMgr, br, out, lineCounter);
                    }
                } else {
                    out.println("\tNot a Provision Manager");
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        out.println();
    }

    /**
     * Produce output for an OperationalStringManager
     *
     * @param opMgr The OperationalStringManager
     * @param br A BufferredReader, allows for the user to press enter for more
     * @param out The output PrintStream
     * @param lineCounter the current line counter
     */
    private static void listMgr(OperationalStringManager opMgr,
                                BufferedReader br,
                                PrintStream out,
                                int lineCounter) {
        Integer listLength =
            (Integer) CLI.getInstance().settings.get(CLI.LIST_LENGTH);
        String pad = "    ";
        String role = "primary";
        try {
            if(!opMgr.isManaging())
                role = "backup";
            out.println("\t"+
                        opMgr.getOperationalString().getName()+
                        "\trole="+role);
            lineCounter++;
            if(lineCounter % listLength==0)
                promptMore(br, out);

            ServiceElement[] sElems =
                opMgr.getOperationalString().getServices();

            for (ServiceElement sElem : sElems) {
                String tabs = "\t";
                if (sElem.getName().length() < 10)
                    tabs = tabs + "\t";
                String pending = "pending=";
                if (role.equals("backup")) {
                    pending = pending + "<n/a>";
                } else {
                    pending = pending + opMgr.getPendingCount(sElem);
                }
                if(lineCounter % listLength==0)
                    promptMore(br, out);

                out.println("\t" + pad +
                            sElem.getName() + tabs +
                            "planned=" + sElem.getPlanned() + "\t" +
                            "actual=" + sElem.getActual() + "\t" +
                            pending);
                lineCounter++;
                ServiceBeanInstance[] instances =
                    opMgr.getServiceBeanInstances(sElem);
                for (ServiceBeanInstance instance : instances) {
                    Long id =
                        instance.getServiceBeanConfig().
                            getInstanceID();
                    if(lineCounter % listLength==0)
                        promptMore(br, out);
                    out.println("\t" + pad + pad +
                                "id=" + id + "\t\t" +
                                instance.getHostAddress());
                    lineCounter++;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the codebase for the service by getting the service's classloader
     * and extracting only the http: address and port number
     * 
     * @param service The service
     * 
     * @return A String codebase
     */
    public static String getExportCodebase(Object service) {
        URLClassLoader cl = (URLClassLoader)service.getClass().getClassLoader();
        URL[] urls = cl.getURLs();
        String exportCodebase = urls[0].toExternalForm();
        if(exportCodebase.indexOf(".jar") != -1) {
            int index = exportCodebase.lastIndexOf('/');
            if(index != -1)
                exportCodebase = exportCodebase.substring(0, index + 1);
        } else {
            System.out.println("Cannot determine export codebase");
        }
        /*
         * TODO: If the exportCodebase starts with httpmd, replace 
         * httpmd with http. Need to figure out a mechanism to use the 
         * httpmd in a better way
         */
        if(exportCodebase.startsWith("httpmd")) {
            exportCodebase = "http" + exportCodebase.substring(6);
        }
        return(exportCodebase);
    }

    /**
     * Utility to convert a String array to a comma delimited String
     * @param array An array of Strings, must not be null
     * 
     * @return A comma delimited String. If the array is empty, return a String 
     * with zero-length
     */
    public static String fromArray(String[] array) {
        return(fromArray(array, ", "));
    }

    /**
     * Utility to convert a String array to a delimited String
     * 
     * @param array An array of Strings, must not be null
     * @param delim Delimiter to use between Strings, must not be null
     * 
     * @return A comma delimited String. If the array is empty, return a String 
     * with zero-length
     */
    public static String fromArray(String[] array, String delim) {
        if(array==null)
            throw new NullPointerException("array is null");
        if(delim==null)
            throw new NullPointerException("delim is null");
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<array.length; i++) {
            if(i>0)
                buffer.append(delim);
            buffer.append(array[i]);
        }
        return(buffer.toString());
    }

    /**
     * Convert comma-separated String to array of Strings
     *
     * @param arg The comma separated string
     *
     * @return A converted array of strings
     */
    public static String[] toArray(String arg) {
        StringTokenizer tok = new StringTokenizer(arg, " ,");
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }

    public static void promptMore(BufferedReader br, PrintStream out) {
        out.print("-- more --");
        try {
            br.readLine();
        } catch (IOException e) {
            System.err.println("busted input");
        }
    }
}
