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
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.opstring.*;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.tools.webster.Webster;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Utility for dealing with Monitor deploy, undeploy and redeploys
 *
 * @author Dennis Reedy
 */
public class MonitorControl {

    /**
     * Manages deployments
     */
    public static class DeployHandler implements OptionHandler {
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            if (out == null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            StringTokenizer tok = new StringTokenizer(input);
            String deployment = null;
            DeployOptions deployOptions = new DeployOptions();
            deployOptions.deployTimeout = (Long) CLI.getInstance().settings.get(CLI.DEPLOY_WAIT);
            BufferedReader reader = br;
            boolean oarDeployment = false;

            if (tok.countTokens() > 1) {
                /* First token is "deploy" */
                tok.nextToken();
                /* Next token is the deploy-descriptor */
                //deployment  = tok.nextToken();
                while (tok.hasMoreTokens()) {
                    String value = tok.nextToken();
                    if (value.endsWith(".xml") || value.endsWith(".groovy")) {
                        /* value is the deploy-descriptor */
                        deployment = value;
                    } else if (value.endsWith("oar")) {
                        /* value is the oar file */
                        deployment = value;
                        oarDeployment = true;
                    } else if (value.startsWith("-t")) {
                        StringTokenizer tok1 = new StringTokenizer(value, " =");
                        if (tok1.countTokens() < 2)
                            return (getUsage());
                        /* First token will be "delay" */
                        tok1.nextToken();
                        /* Next token must be the timeout value */
                        value = tok1.nextToken();
                        try {
                            deployOptions.deployTimeout = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            return (getUsage());
                        }
                    } else if (value.startsWith("-r")) {
                        StringTokenizer tok1 = new StringTokenizer(value, " =");
                        if (tok1.countTokens() < 2)
                            return (getUsage());
                        /* First token will be "-r" */
                        tok1.nextToken();
                        /* Next token must be the repositories value */
                        value = tok1.nextToken();
                        deployOptions.repositories = value;
                    } else if (value.startsWith("-")) {
                        /* strip the "-" off */
                        value = value.substring(1);
                        while (value.length() > 0) {
                            if (value.startsWith("i")) {
                                deployOptions.noPrompt = true;
                            } else if (value.startsWith("u")) {
                                deployOptions.update = true;
                            } else if (value.startsWith("v")) {
                                deployOptions.verbose = true;
                            }else {
                                return (getUsage());
                            }
                            value = value.substring(1);
                        }
                    } else {
                        /* If its none of the above then we must have a
                         * directory name to deploy */
                        deployment = value;
                        oarDeployment = true;
                    }
                }
            } else {
                return (getUsage());
            }
            OperationalString deploy = null;
            if (!oarDeployment) {
                try {
                    deploy = loadDeployment(deployment);
                    if (deploy == null) {
                        return (deployment + ": Cannot find file ["+deployment+"]\n");
                    }
                    if(deployOptions.repositories!=null) {
                        String[] parts = deployOptions.repositories.split(";");
                        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
                        for(String part : parts) {
                            RemoteRepository r = new RemoteRepository();
                            r.setUrl(part);
                            remoteRepositories.add(r);
                        }
                        for(ServiceElement service : deploy.getServices()) {
                            service.setRemoteRepositories(remoteRepositories);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return ("Problem loading [" + deployment + "], Exception : " + e.getLocalizedMessage() + "\n");
                }
            }

            ServiceItem[] items = CLI.getInstance().finder.findMonitors(null, null, deployOptions.verbose);
            if (items.length == 0)
                return ("No Provision Monitor instances discovered\n");

            if (items.length == 1 || deployOptions.noPrompt()) {
                if (oarDeployment) {
                    return (deploy(items[0], deployment, deployOptions, out));
                } else {
                    return (deploy(items[0], deploy, deployOptions, reader, out));
                }
            }
            if (reader == null)
                reader = new BufferedReader(new InputStreamReader(System.in));
            out.println(Formatter.asList(items) + "\n");
            printRequest(out);
            while (true) {
                try {
                    String response = reader.readLine();
                    if (response == null) {
                        break;
                    }
                    if (response.equals("c"))
                        break;
                    try {
                        int num = Integer.parseInt(response);
                        if (num < 1 || num > (items.length + 1)) {
                            printRequest(out);
                        } else {
                            if (oarDeployment) {
                                deploy(items[0], deployment, deployOptions, out);
                            } else {
                                deploy(items[num - 1], deploy, deployOptions, reader, out);
                            }
                            break;
                        }
                    } catch (NumberFormatException e) {
                        out.println("Invalid choice [" + response + "]");
                        printRequest(out);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return ("");
        }

        /**
         * Deploy an OperationalString using a selected monitor
         *
         * @param item The ServiceItem
         * @param deploy The OperationalString
         * @param deployOptions Deploy options
         * @param br A BufferedReader
         * @param out The printStream
         *
         * @return A String for the completed command
         */
        String deploy(final ServiceItem item,
                      final OperationalString deploy,
                      final DeployOptions deployOptions,
                      final BufferedReader br,
                      final PrintStream out) {
            try {
                DeployAdmin deployAdmin = (DeployAdmin)CLI.getInstance().getServiceFinder().getPreparedAdmin(item.service);
                Boolean wait = (Boolean) CLI.getInstance().settings.get(CLI.DEPLOY_BLOCK);
                if (deployAdmin.hasDeployed(deploy.getName())) {
                    if (deployOptions.update()) {
                        try {
                            out.print("The [" + deploy.getName() + "] is currently deployed, updating ...\n");
                            OperationalStringManager mgr = deployAdmin.getOperationalStringManager(deploy.getName());
                            mgr.update(deploy);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return ("Exception "+e.getClass()+":"+e.getLocalizedMessage()+", check log for details");
                        }
                    } else {
                        if (deployOptions.noPrompt()) {
                            return ("The [" + deploy.getName() + "] is currently deployed, interactive mode disabled, " +
                                    "returning");
                        }
                        out.print("The ["+deploy.getName()+"] is currently deployed, update the deployment? [y/n] ");
                        BufferedReader reader = br;
                        try {
                            if (reader == null)
                                reader = new BufferedReader(new InputStreamReader(System.in));
                            String response = reader.readLine();
                            if (response == null) {
                                return ("");
                            }
                            if (response.startsWith("y") || response.startsWith("Y")) {
                                OperationalStringManager mgr = deployAdmin.getOperationalStringManager(deploy.getName());
                                mgr.update(deploy);
                                return ("Updated");
                            } else {
                                return ("Skipped");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return ("Exception "+e.getClass()+":"+e.getLocalizedMessage()+", check log for details");
                        }
                    }
                } else {
                    if (deployOptions.getDeployTimeout() > 0 && wait) {
                        ServiceProvisionNotification spn = CLI.getInstance().provisionNotifier;
                        deployAdmin.deploy(deploy, spn.getServiceProvisionListener());
                        long t0 = System.currentTimeMillis();
                        out.println("Deploying [" +
                                    ServiceProvisionNotification.getDeploymentNames(deploy) + "], " +
                                    "total services ["+ServiceProvisionNotification.sumUpServices(deploy) + "] ...");
                        spn.notify(
                            ServiceProvisionNotification.sumUpServices(deploy),
                            deployOptions.getDeployTimeout());
                        long t1 = System.currentTimeMillis();
                        return ((deployOptions.verbose ?
                                 "Deployment notification time " +(t1 - t0) + " millis, Command completed"
                                                       : ""));
                    } else {
                        deployAdmin.deploy(deploy, null);
                    }
                }
                return ((deployOptions.verbose ? "Command completed" : ""));
            } catch (Exception e) {
                e.printStackTrace();
                return ("Problem deploying [" + deploy.getName() + "], " +
                        "Exception : " + e.getLocalizedMessage());
            }
        }

        /**
         * Deploy an OAR using a selected monitor
         *
         * @param item The ServiceItem
         * @param deployName The OAR or artifact name
         * @param deployOptions Deploy options
         * @param out A printStream for output
         * @return A String for the completed command
         */
        String deploy(final ServiceItem item, final String deployName, final DeployOptions deployOptions, final PrintStream out) {
            boolean isArtifact = false;
            try {
                new Artifact(deployName);
                isArtifact = true;
            } catch(Exception e) {
                /**/
            }
            OperationalString toDeploy = null;
            Webster embeddedWebster = null;
            URL oarUrl = null;
            //String oarUrl = null;
            try {
                if(!isArtifact) {
                    try {
                        oarUrl = new URL(deployName);
                    } catch(MalformedURLException e) {
                        File oarFile = getDeploymentFile(deployName);
                        OAR oar = new OAR(oarFile);
                        embeddedWebster = new Webster(0, oarFile.getParentFile().getAbsolutePath());
                        toDeploy = oar.loadOperationalStrings()[0];
                        oarUrl = new URL("http://"+embeddedWebster.getAddress()+":"+embeddedWebster.getPort()+"/"+oarFile.getName());
                    }
                    System.out.println("===> "+oarUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return("Problem resolving ["+deployName+"], Exception : "+e.getLocalizedMessage());
            }

            try {
                DeployAdmin deployAdmin =
                    (DeployAdmin)CLI.getInstance().getServiceFinder().getPreparedAdmin(item.service);
                Boolean wait = (Boolean)CLI.getInstance().settings.get(CLI.DEPLOY_BLOCK);

                if(deployOptions.getDeployTimeout()>0 && wait) {
                    ServiceProvisionNotification spn = CLI.getInstance().provisionNotifier;
                    String label = "Artifact";
                    if(isArtifact) {
                        deployAdmin.deploy(deployName, spn.getServiceProvisionListener());
                    } else {
                        label = "OAR";
                        try{
                            //deployAdmin.deploy(toDeploy, spn.getServiceProvisionListener());
                            deployAdmin.deploy(oarUrl, spn.getServiceProvisionListener());
                        } finally {
                            if(embeddedWebster!=null)
                                embeddedWebster.terminate();
                        }
                    }
                    long t0 = System.currentTimeMillis();
                    out.println("Deploying "+label+" ["+deployName+"] ...");
                    spn.notify(1, deployOptions.getDeployTimeout());
                    long t1 = System.currentTimeMillis();
                    return((deployOptions.verbose?
                            "Deployment notification time "+(t1-t0)+" millis, Command completed":""));
                } else {
                    if(isArtifact)
                        deployAdmin.deploy(deployName, null);
                    else
                        deployAdmin.deploy(toDeploy, null);
                }

                return((deployOptions.verbose?"Command completed":""));
            } catch (Exception e) {
                e.printStackTrace();
                return("Problem deploying ["+deployName+"], Exception : "+e.getLocalizedMessage());
            }

        }

        /*
         * Print the request to the operator
         */
        void printRequest(final PrintStream out) {
            out.print("Choose a Provision Monitor for " +
                      "deployment or \"c\" to cancel : ");
        }

        /**
         * @return Get the usage
         */
        public String getUsage() {
            StringBuilder b = new StringBuilder();
            b.append("\n");
            b.append("usage: deploy opstring " +
                     "[-t=deploy-timeout] " +
                     "[-r=repository][;repository]]" +
                     "[-iuv]");
            b.append("\n");
            b.append("\n");
            b.append("-i\tTurns off interactive prompting");
            b.append("\n");
            b.append("-u\tAutomatically update deployments");
            b.append("\n");
            b.append("-v\tVerbose mode");
            b.append("\n");
            b.append("-t\tTime in milliseconds to wait for deployment status");
            b.append("\n");
            b.append("-r\tRepositories to use for the resolution of artifacts");
            b.append("\n");
            return (b.toString());
        }

    }

    static class DeployOptions {
        boolean noPrompt;
        long deployTimeout;
        boolean verbose;
        boolean update;
        String repositories;

        long getDeployTimeout() {
            return (deployTimeout);
        }

        boolean noPrompt() {
            return (noPrompt);
        }

        boolean update() {
            return (update);
        }
    }

    /**
     * Manages undeployments
     */
    public static class UndeployHandler implements OptionHandler {
        @SuppressWarnings ("unchecked")
        public String process(final String input,
                              final BufferedReader br,
                              final PrintStream out) {
            if (out == null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            StringTokenizer tok = new StringTokenizer(input);
            String deployment = null;
            boolean verbose = false;
            if (tok.countTokens() > 1) {
                /* First token is "undeploy" */
                tok.nextToken();
                /* Next token is the opstring */
                deployment = tok.nextToken();
            }
            ServiceItem[] items = CLI.getInstance().finder.findMonitors(null, null, verbose);
            if (items.length == 0)
                return ("No Provision Monitor instances discovered\n");

            BufferedReader reader = br;
            if (deployment == null) {
                if (reader == null)
                    reader = new BufferedReader(new InputStreamReader(System.in));
                Map<String, DeployAdmin> map = getDeployedOpStrings(items);
                if (map.isEmpty()) {
                    return "Nothing is currently deployed\n";
                }
                int i = 1;
                Map.Entry<String, DeployAdmin>[] entries =
                    map.entrySet().toArray(new Map.Entry[map.size()]);
                for (Map.Entry<String, DeployAdmin> entry : entries) {
                    out.println("[" + (i++) + "] " + entry.getKey());
                }
                out.println();
                printRequest(out);
                while (true) {
                    try {
                        String response = reader.readLine();
                        if (response == null) {
                            break;
                        }
                        if (response.equals("c"))
                            break;
                        try {
                            int num = Integer.parseInt(response);
                            if (num < 1 || num > (items.length + 1)) {
                                printRequest(out);
                            } else {
                                try {
                                    String opStringName =
                                        entries[num - 1].getKey();
                                    DeployAdmin dAdmin =
                                        entries[num - 1].getValue();
                                    dAdmin.undeploy(opStringName);
                                    out.println("Command successful");
                                } catch (Exception e) {
                                    return ("Problem undeploying " +
                                            deployment + ", " +
                                            "Exception :" +
                                            e.getClass().getName() + ": " +
                                            e.getLocalizedMessage());
                                }
                            }
                            break;
                        } catch (NumberFormatException e) {
                            out.println("Invalid choice [" + response + "]");
                            printRequest(out);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return ("");
            }
            //OperationalString toUndeploy = null;
            String undeployName = null;
            try {
                new Artifact(deployment);
                undeployName = deployment;
            } catch(Exception e) {
                /* */
            }
            if (deployment.endsWith("oar")) {
                try {
                    File oarFile = getDeploymentFile(deployment);
                    OAR oar = new OAR(oarFile);
                    OperationalString toUndeploy = oar.loadOperationalStrings()[0];
                    undeployName = toUndeploy.getName();
                } catch (Exception e) {
                    return ("Problem loading [" + deployment + "], " +
                            "Exception : " + e.getLocalizedMessage() + "\n");
                }
            }

            if(undeployName==null) {
                try {
                    OperationalString toUndeploy = loadDeployment(deployment);
                    if (toUndeploy == null) {
                        return (deployment + ": Cannot find file\n");
                    }
                    undeployName = toUndeploy.getName();
                } catch (Exception e) {
                    return ("Problem loading [" + deployment + "], " +
                            "Exception : " + e.getLocalizedMessage() + "\n");
                }
            }

            try {
                DeployAdmin dAdmin =
                (DeployAdmin) CLI.getInstance().getServiceFinder().getPreparedAdmin(items[0].service);
                try {
                    dAdmin.undeploy(undeployName);
                    out.println("Command successful");
                } catch(OperationalStringException e) {
                    if(e.getMessage().startsWith("No deployment for")) {
                        return ("Command failed, no active deployment for " +
                                deployment);
                    } else {
                        return ("Problem undeploying " + deployment + ", " +
                                "Exception :" + e.getClass().getName() + ": " +
                                e.getLocalizedMessage());
                    }
                } catch (Exception e) {
                    return ("Problem undeploying " + deployment + ", " +
                            "Exception :" + e.getClass().getName() + ": " +
                            e.getLocalizedMessage());
                }
            } catch (Exception e) {
                return ("Problem undeploying " + deployment + ", " +
                        "Exception :" + e.getClass().getName() + ": " +
                        e.getLocalizedMessage());
            }
            return ("");
        }

        /*
         * Print the request to the operator
         */
        void printRequest(final PrintStream out) {
            out.print("Enter the OperationalString to undeploy " +
                      "or \"c\" to cancel : ");
        }

        public String getUsage() {
            return ("usage: undeploy [opstring]\n");
        }
    }

    static Map<String, DeployAdmin> getDeployedOpStrings(final ServiceItem[] items) {
        Map<String, DeployAdmin> map = new HashMap<String, DeployAdmin>();
        for (ServiceItem item : items) {
            try {
                DeployAdmin deployAdmin = (DeployAdmin) CLI.getInstance().getServiceFinder().
                        getPreparedAdmin(item.service);
                OperationalStringManager[] opMgrs = deployAdmin.getOperationalStringManagers();
                for (OperationalStringManager opMgr : opMgrs) {
                    OperationalString opString = opMgr.getOperationalString();
                    if (opMgr.isManaging()) {
                        map.put(opString.getName(), deployAdmin);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * Manages redeployments
     */
    public static class RedeployHandler implements OptionHandler {
        public String process(final String input,
                              final BufferedReader br,
                              final PrintStream out) {
            if (out == null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            StringTokenizer tok = new StringTokenizer(input);
            String deployment;
            boolean clean = false;
            long delay = 0;
            if (tok.countTokens() >= 2) {
                /* First token is "redeploy" */
                tok.nextToken();
                /* Next token must be the opstring */
                deployment = tok.nextToken();
                while (tok.hasMoreTokens()) {
                    String value = tok.nextToken();
                    if (value.equals("clean")) {
                        clean = true;
                    } else if (value.startsWith("delay")) {
                        StringTokenizer tok1 = new StringTokenizer(value, " =");
                        if (tok1.countTokens() < 2)
                            return (getUsage());
                        /* First token will be "delay" */
                        tok1.nextToken();
                        /* Next token must be the timeout value */
                        value = tok1.nextToken();
                        try {
                            delay = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            return (getUsage());
                        }
                    }
                }
            } else {
                return (getUsage());
            }
            if (deployment == null) {
                return (getUsage());
            }
            OperationalString deploy;
            try {
                deploy = loadDeployment(deployment);
                if (deploy == null) {
                    return (deployment + ": Cannot find file \n");
                }
            } catch (Exception e) {
                return ("Problem loading [" + deployment + "], " +
                        "Exception : " + e.getLocalizedMessage() + "\n");
            }

            ServiceItem[] items = CLI.getInstance().finder.findMonitors(null, null);
            if (items.length == 0)
                return ("No Provision Monitor instances discovered\n");
            OperationalStringManager primary = findOperationalStringManager(items, deploy);
            if (primary != null) {
                try {
                    ServiceProvisionNotification spn = CLI.getInstance().provisionNotifier;
                    primary.redeploy(null, null, clean, delay, spn.getServiceProvisionListener());
                    out.println("Command completed");
                } catch (Exception e) {
                    return ("Problem redeploying " + deployment + ", " +
                            "Exception :" + e.getClass().getName() + ": " +
                            e.getLocalizedMessage());
                }
            } else {
                return ("Command failed, no active deployment for " +deployment);
            }
            return ("");
        }

        public String getUsage() {
            return ("usage: redeploy opstring [clean] [delay=millis-to-delay]\n");
        }
    }

    /**
     * Load an OperationalString
     *
     * @param deployment The name of the file to load
     * @return An OperationalString
     *
     * @throws Exception If there are any errors
     */
    static OperationalString loadDeployment(final String deployment) throws Exception {
        OperationalString deploy = null;
        File deployFile = getDeploymentFile(deployment);
        if (deployFile.exists()) {
            OpStringLoader opStringLoader = new OpStringLoader(CLI.class.getClassLoader());
            OperationalString[] opstrings = opStringLoader.parseOperationalString(deployFile);
            deploy = opstrings[0];
        }
        return (deploy);
    }

    /**
     * Get the file to deploy from a file name
     *
     * @param deployment The name of the file to load
     * @return An OperationalString representing to loaded deployment
     *
     * @throws Exception If there are any errors
     */
    static File getDeploymentFile(final String deployment) throws Exception {
        File deployFile;
        if (deployment.startsWith(File.separator)) {
            deployFile = new File(deployment);
        } else if (deployment.contains(":")) {
            deployFile = new File(deployment);
        } else if (deployment.startsWith("~/")) {
            String toDeploy = deployment.substring(2);
            deployFile = new File(System.getProperty("user.home")+File.separator + toDeploy);
        } else {
            deployFile = new File(CLI.getInstance().currentDir.getCanonicalPath()+File.separator+deployment);
        }
        return (deployFile);
    }

    /**
     * Find the primary OperationalStringManager for a loaded deployment
     *
     * @param items ServiceItems to iterate across
     * @param deploy The OperationalString
     * @return The primary OperationalStringManager
     */
    static OperationalStringManager findOperationalStringManager(final ServiceItem[] items,
                                                                 final OperationalString deploy) {
        OperationalStringManager primary = null;
        for (ServiceItem item : items) {
            try {
                DeployAdmin deployAdmin =
                    (DeployAdmin) CLI.getInstance().getServiceFinder().getPreparedAdmin(item.service);
                OperationalStringManager[] opMgrs = deployAdmin.getOperationalStringManagers();
                for (OperationalStringManager opMgr : opMgrs) {
                    OperationalString opString = opMgr.getOperationalString();
                    if (opString.getName().equals(deploy.getName()) && opMgr.isManaging()) {
                        primary = opMgr;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (primary);
    }

}
