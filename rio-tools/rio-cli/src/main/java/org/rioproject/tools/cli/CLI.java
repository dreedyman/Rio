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

import com.sun.jini.config.Config;
import net.jini.config.*;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.DiscoveryLocatorManagement;
import net.jini.discovery.DiscoveryManagement;
import net.jini.lookup.entry.jmx.JMXProperty;
import net.jini.lookup.entry.jmx.JMXProtocolType;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;
import org.rioproject.impl.jmx.JMXUtil;
import org.rioproject.impl.service.ServiceStopHandler;
import org.rioproject.rmi.RegistryUtil;
import org.rioproject.impl.discovery.ReggieStat;
import org.rioproject.tools.webster.Webster;
import org.rioproject.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginContext;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

/**
 * The CLI class is a utility that provides a Command Line Interface (CLI) for
 * interfacing with the Rio components
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class CLI {
    final static long startTime = System.currentTimeMillis();
    final static String DISCOVERY_TIMEOUT="disco-timeout";
    final static String GROUPS="groups";
    final static String LOCATORS="locators";
    final static String SYS_PROPS="system-props";
    final static String DEPLOY_BLOCK="wait-on-deploy";
    final static String DEPLOY_WAIT="deploy-timeout";
    final static String LIST_LENGTH="list-length";
    public static final String COMPONENT = "org.rioproject.tools.cli";
    static final String CONFIG_COMPONENT = COMPONENT;
    public static Logger logger = LoggerFactory.getLogger(COMPONENT);
    protected String cliName;
    protected String prompt = "rio> ";
    static Configuration sysConfig;
    ServiceFinder finder;
    ServiceProvisionNotification provisionNotifier;
    String hostName;
    String hostAddress;
    boolean commandLine = true;
    final Map<String, Object> settings = new HashMap<String, Object>();
    protected final Map<String, OptionHandlerDesc> optionMap = new HashMap<String, OptionHandlerDesc>();
    String homeDir;
    File currentDir;
    File rioLog;
    Webster webster;
    PrintStream cliOutput;
    private String previous;
    protected static CLI instance;
    private static String userName;
    private static LoginContext loginContext;

    protected CLI() {
        cliName = "Rio";
    }

    public static synchronized CLI getInstance() {
        if (instance == null) {
            ensureSecurityManager();
            instance = new CLI();
        }
        return instance;
    }

    /**
     * Utility routine that sets a security manager if one isn't already
     * present.
     */
    protected synchronized static void ensureSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    LoginContext getLoginContext() {
        return loginContext;
    }

    String getHomeDir() {
        return homeDir;
    }

    File getRioLog() {
        return rioLog;
    }

    Webster getWebster() {
        return webster;
    }

    boolean getCommandLine() {
        return commandLine;
    }

    /**
     * Print usage
     */
    void printUsage() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Usage :\n");
        buffer.append("\t").append(getClass().getName()).append(
            " [commands] [options]");
        buffer.append("\n");
        buffer.append("commands\n");

        buffer.append("\tlist [type] [options]\n");
        buffer.append("\t\tList discovered services\n");
        buffer.append("\t\t    type\n");
        buffer.append("\t\t\tmonitor|cybernode\tLists details for either " +
                      "service\n");
        buffer.append("\t\t    options\n");
        buffer.append("\t\t\tcpu|codeserver\tOnly one allowed, " +
                      "default is to list \"all\"\n\n");

        buffer.append("\tdestroy [all]\n");
        buffer.append("\t\tDestroy a specific service " +
                      "(prompted), or all services\n\n");

        buffer.append("\tdeploy opstring [-t=deploy-timeout] [-icuvr]\n");
        buffer.append("\t\tDeploy an application\n");
        buffer.append("\t\t\t-i\tTurns off interactive prompting\n");
        buffer.append("\t\t\t-c\tVerifies codebase\n");
        buffer.append("\t\t\t-u\tAutomatically update deployments\n");
        buffer.append("\t\t\t-v\tVerbose mode\n");
        buffer.append("\t\t\t-t\tTime in milliseconds to wait for deployment status\n\n");
        buffer.append("\t\t\t-r\tRepositories to use for the resolution of artifacts\n\n");

        buffer.append("\tundeploy opstring\n");
        buffer.append("\t\tUndeploy an application\n\n");

        buffer.append("\tredeploy opstring [clean] [delay=millis-to-delay]\n");
        buffer.append("\t\tRedeploy an application\n");
        buffer.append("\n");
        buffer.append("options\n");
        buffer.append("\tgroups=group1,group2\t\tComma separated names of " +
                      "multicast groups to discover.\n");
        buffer.append("\t\t\t\t\tIf \"all\" is provided, this " +
                      "will be translated to\n");
        buffer.append("\t\t\t\t\tDiscoveryGroupManagment.ALL_GROUPS\n");
        buffer.append("\tlocators=jini://host[:port]\tComma separated names " +
                      "of lookup locators to discover\n");
        buffer.append("\tdiscoveryTimeout=millis" +
                      "\t\tDiscovery timeout (in milliseconds)\n");
        buffer.append("\thttpPort=port\t\t\tPort to use when starting Webster\n");
        buffer.append("\t-noHttp\t\t\t\tDo not start Webster, mutually " +
                "exclusive\n");
        buffer.append("\t\t\t\t\twith the httpPort option\n");

        for (OptionHandlerDesc option : optionMap.values()) {
            buffer.append("\t").append(option.getName()).append("\n");
            buffer.append("\t\t").append(option.getOptionHandler().getUsage());
        }

        cliOutput.println(buffer.toString());
    }

    /**
     * Get usage for an interactive session
     *
     * @return A formatted String for displaying usage of an interactive session
     */
    public String getInteractiveUsage() {
        StringBuilder buffer = new StringBuilder();
        String[] optionNames = getOptionNames();

        /* Iterate over the option names and construct a table of column
         * name lengths */
        int longest = 0;
        int col = 0;
        Map<Integer, Integer> columnLengths = new HashMap<Integer, Integer>();
        for(int i=0; i<optionNames.length; i++) {
            longest = optionNames[i].length()>longest?optionNames[i].length():longest;
            if(i > 0 && i%5 == 0) {
                columnLengths.put(col, longest);
                col=1;
                longest = columnLengths.get(1);
            } else {
                columnLengths.put(col, longest);
                col++;
                if(columnLengths.containsKey(col))
                    longest = columnLengths.get(col);
            }
        }
        StringBuilder buff = new StringBuilder();
        for(int i=0; i<optionNames.length; i++) {
            buff.append(optionNames[i]);
            if(i > 0 && i%5 == 0) {
                buffer.append(getOutput(buff.toString(), columnLengths));
                buff.delete(0, buff.length());
            } else {
                buff.append(" ");
            }
        }
        buffer.append(getOutput(buff.toString(), columnLengths));
        return buffer.toString();
    }

    private String getOutput(final String s, final Map<Integer, Integer> columnLengths) {
        String output;
        StringBuilder buffer = new StringBuilder();
        Object[] array = toArray(s);
        for(int i=0; i<array.length; i++) {
            if(i>0)
                buffer.append(" ");
            buffer.append("%-").append(columnLengths.get(i + 1) + 1).append("s");
        }
        output = String.format("%n"+buffer.toString(), toArray(s));

        return output;
    }

    private static Object[] toArray(final String s) {
        return toArray(s, null);
    }

    private static String[] toArray(final String s, final String delim) {
        StringTokenizer tok;
        if(delim==null)
            tok = new StringTokenizer(s);
        else
            tok = new StringTokenizer(s, delim);
        String[] array = new String[tok.countTokens()];
        int i = 0;
        while (tok.hasMoreTokens()) {
            array[i++] = tok.nextToken();
        }
        return array;
    }

    private String[] getOptionNames() {
        Set keys = optionMap.keySet();
        String[] names = new String[keys.size()+1];
        int i=1;
        names[0] = "";
        for (Object key : keys) {
            names[i++] = (String) key;
        }
        return(names);
    }

    /**
     * Terminate the ServiceFinder and optionally say goodbye
     *
     * @param print If true say thank you
     */
    void onExit(final boolean print) {
        finder.terminate();
        HttpHandler.stopWebster();
        if(print)
            cliOutput.println("\n"+
                               "Thank you for using the " + cliName +
                               " Interactive Shell Program" +
                               "\n");
        System.exit(0);
    }

    /**
     * Validate command argument
     *
     * @param command The command to validate, must not be null
     *
     * @return If the command is valid, return true, otherwise return false for
     * an invalid command
     *
     * @throws IllegalArgumentException if the command parameter is null
     */
    public boolean validCommand(final String command) {
        if(command == null)
            throw new IllegalArgumentException("command is null");
        boolean valid;
        /* Silly little easter eggs */
        valid = command.equalsIgnoreCase("ian") ||
                command.equalsIgnoreCase("sara") || optionMap.containsKey(command);
        return(valid);
    }

    public PrintStream getOutput() {
        return cliOutput;
    }

    private void setOutput(final PrintStream cliOutput) {
        this.cliOutput = cliOutput;
    }

    /**
     * Get an {@link org.rioproject.tools.cli.OptionHandler} for an option
     *
     * @param option The option to use, must not be null
     *
     * @return An {@link org.rioproject.tools.cli.OptionHandler} for the option.
     * A new OptionHandler is created each time. If an OptionHandler cannot
     * be found for the provided option name, return null
     *
     * @throws IllegalArgumentException if the option parameter is null
     */
    public OptionHandler getOptionHandler(final String option) {
        if(option == null)
            throw new IllegalArgumentException("option is null");
        OptionHandler handler = null;
        /* Silly little easter eggs */
        if(option.equalsIgnoreCase("ian")) {
            handler = new EasterEggHandler();
        } else if(option.equalsIgnoreCase("sara")) {
            handler = new EasterEggHandler();
        } else {
            OptionHandlerDesc desc = optionMap.get(option);
            if(desc==null)
                cliOutput.println("unknown option ["+option+"]");
            else
                handler = desc.getOptionHandler();
        }
        return handler;
    }

    protected void loadOptionHandlers(final Configuration config) throws ConfigurationException {
        OptionHandlerDesc[] defaultHandlers =
            new OptionHandlerDesc[] {
                new OptionHandlerDesc("list", ListHandler.class.getName()),
                new OptionHandlerDesc("destroy", StopHandler.class.getName()),
                new OptionHandlerDesc("deploy", MonitorControl.DeployHandler.class.getName()),
                new OptionHandlerDesc("redeploy", MonitorControl.RedeployHandler.class.getName()),
                new OptionHandlerDesc("undeploy", MonitorControl.UndeployHandler.class.getName()),
                new OptionHandlerDesc("set", SettingsHandler.class.getName()),
                new OptionHandlerDesc("ls", DirHandler.class.getName()),
                new OptionHandlerDesc("dir", DirHandler.class.getName()),
                new OptionHandlerDesc("pwd", DirHandler.class.getName()),
                new OptionHandlerDesc("cd", DirHandler.class.getName()),
                new OptionHandlerDesc("jconsole", JConsoleHandler.class.getName()),
                new OptionHandlerDesc("stats", StatsHandler.class.getName()),
                new OptionHandlerDesc("http", HttpHandler.class.getName()),
                new OptionHandlerDesc("enlist", EnlistmentHandler.EnlistHandler.class.getName()),
                new OptionHandlerDesc("release", EnlistmentHandler.ReleaseHandler.class.getName()),
                new OptionHandlerDesc("help", HelpHandler.class.getName())
            };
        OptionHandlerDesc[] optionHandlers =
            (OptionHandlerDesc[])config.getEntry(COMPONENT,
                                                 "optionHandlers",
                                                 OptionHandlerDesc[].class,
                                                 defaultHandlers);
        OptionHandlerDesc[] addOptionHandlers =
            (OptionHandlerDesc[])config.getEntry(COMPONENT,
                                                 "addOptionHandlers",
                                                 OptionHandlerDesc[].class,
                                                 new OptionHandlerDesc[0]);
        if(addOptionHandlers.length > 0) {
            if(logger.isDebugEnabled()) {
                StringBuilder buffer = new StringBuilder();
                for(int i = 0; i < addOptionHandlers.length; i++) {
                    if(i > 0)
                        buffer.append("\n");
                    buffer.append("    ").append(addOptionHandlers[i].toString());
                }
                logger.debug("addOptionHandlers\n{}",buffer.toString());
            }
            List<OptionHandlerDesc> list = new ArrayList<OptionHandlerDesc>();
            list.addAll(Arrays.asList(optionHandlers));
            list.addAll(Arrays.asList(addOptionHandlers));
            optionHandlers = list.toArray(new OptionHandlerDesc[list.size()]);
        }

        for (OptionHandlerDesc optionHandler : optionHandlers) {
            optionMap.put(optionHandler.getName(), optionHandler);
        }
    }

    /**
     * Set up interactive input behavior
     */
    void manageInteraction() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        cliOutput.println("Welcome to the " + cliName + " Interactive Shell Program. "+
                           "You can interact\n"+
                           "with the " + cliName + " using the " +
                           "following commands : "+
                           "\n"+
                           getInteractiveUsage()+
                           "\n\n"+
                           "For help on any of these commands type " +
                           "help [command].\n"+
                           "To leave this program type quit\n");
        cliOutput.print(prompt);
        while (true) {
            try {
                String input = br.readLine();
                if (input == null)
                    //end of stream
                    input = "exit";
                if (input.length() > 0) {
                    if(input.equals("q") || input.equals("quit") || input.equals("exit")) {
                        try {
                            br.close();
                        } catch (IOException ignore) {
                            /**/
                        }
                        onExit(true);
                    } else {
                        if(!input.equals("!!")) {
                            previous = input;
                        } else {
                            input = previous;
                        }
                        /* Check for input==null, again. The !! (previous)
                         * command may have been invoked with previous being
                         * null */
                        if(input!=null) {
                            String command = input;
                            StringTokenizer tok = new StringTokenizer(command);
                            if(tok.countTokens() > 0)
                                command = tok.nextToken();

                            if(!validCommand(command)) {
                                cliOutput.println("? Invalid command");
                                cliOutput.println(getInteractiveUsage()+"\n");
                            } else {
                                OptionHandler handler =
                                    getOptionHandler(command);
                                if(handler!=null) {
                                    String response =
                                        handler.process(input, br, cliOutput);
                                    if(response.length()>0)
                                        cliOutput.println(response);
                                }
                            }
                        }
                    }
                }
                cliOutput.print(prompt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }   

    /**
     * Command line option handler descriptor
     */
    public static class OptionHandlerDesc {
        String name;
        String optionHandlerClass;

        /**
         * Create an OptionHandlerDesc
         *
         * @param name The name of the option, must not be null
         * @param optionHandlerClass The OptionHandler classname. This class
         * will be instantiated using a zero-arg constructor. This parameter
         * must not be null
         */
        public OptionHandlerDesc(final String name, final String optionHandlerClass) {
            if(name==null)
                throw new IllegalArgumentException("name is null");
            if(optionHandlerClass ==null)
                throw new IllegalArgumentException("optionHandlerClass is null");
            this.name = name;
            this.optionHandlerClass = optionHandlerClass;
        }

        /**
         * @return The name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the OptionHandler
         *
         * @return OptionHandler An instance of the OptionHandler. A new
         * instance is created each time. If an exception is encountered
         * creating the OptionHandler a null is returned
         */
        public OptionHandler getOptionHandler() {
            OptionHandler oh = null;
            try {
                oh = (OptionHandler)Class.forName(optionHandlerClass).newInstance();
            } catch(Exception e) {
                e.printStackTrace();
                instance.cliOutput.println("Exception ["+e.getClass().getName()+"] " +
                                   "creating "+optionHandlerClass);
            }
            return(oh);
        }

        /**
         * String representation
         */
        public String toString() {
            return("name="+name+", " +
                   "OptionHandler="+optionHandlerClass);
        }
    }

    /**
     * Handle the destroy command
     */
    public static class StopHandler implements OptionHandler {

        public String process(final String input, final BufferedReader br, final PrintStream out) {
            BufferedReader reader = br;
            if(out==null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            if(reader==null)
                reader = new BufferedReader(new InputStreamReader(System.in));
            StringTokenizer tok = new StringTokenizer(input);
            /* first token is "destroy" */
            tok.nextToken();
            ServiceItem[] items = instance.getServiceFinder().find(null, null);

            if(tok.hasMoreTokens()) {
                String option = tok.nextToken();
                if(option.equals("all")) {
                    destroyAll(items, out);
                    return("");
                }
            }
            out.println(Formatter.asChoices(items)+"\n");
            printRequest(out);
            while(true) {
                try {
                    String response = reader.readLine();
                    if(response!=null) {
                        if(response.equals("c"))
                            break;
                        try {
                            int num = Integer.parseInt(response);
                            if(num<1 || num >(items.length+1)) {
                                printRequest(out);
                            } else {
                                if(num==(items.length+1)) {
                                    destroyAll(items, out);
                                } else {
                                    destroyService(items[num-1], out);
                                }
                                break;
                            }
                        } catch(NumberFormatException e) {
                            out.println("Invalid choice ["+response+"]");
                            printRequest(out);
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
            return("");
        }

        void printRequest(final PrintStream out) {
            out.print("Enter service to destroy or \"c\" to cancel : ");
        }

        public void destroyAll(final ServiceItem[] items, final PrintStream out) {
            for (ServiceItem item : items)
                    destroyService(item, out);
            System.out.println("Checking registry ...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
            destroyFromRegistry(out);
        }

        public void destroyService(final ServiceItem item, final PrintStream out) {
            ServiceStopHandler.destroyService(item.service,
                                              ServiceFinder.getName(item.attributeSets),
                                              out);
        }

        void destroyFromRegistry(final PrintStream out) {
            List<Registry> rmiRegistries = new ArrayList<Registry>();
            int port = RegistryUtil.DEFAULT_PORT;
            for(int i=0; i< RegistryUtil.getRegistryRetries(); i++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(port++);
                    rmiRegistries.add(registry);
                } catch (RemoteException e) {
                    //;
                }
            }
            if(!rmiRegistries.isEmpty()) {
                Registry[] registries = rmiRegistries.toArray(
                new Registry[rmiRegistries.size()]);
                for (Registry registry : registries) {
                    try {
                        String[] registered = registry.list();
                        for (String aRegistered : registered) {
                            Object proxy = registry.lookup(aRegistered);
                            ServiceStopHandler.destroyService(proxy, aRegistered, out);
                        }
                    } catch (ConnectException e) {
                        //out.println("Exception getting service from registry, "+
                        //            "Exception : "+e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Exception getting service " +
                                           "from registry, " +
                                           "Exception : " + e.getMessage());
                    }
                }
            } else {
                out.println("Could not locate RMI Registry using ports " +
                            RegistryUtil.DEFAULT_PORT+".."+port);
            }
        }
        public String getUsage() {
            return("usage: destroy [all]\n");
        }
    }

    /**
     * Handle the help command
     */
    protected static class HelpHandler implements OptionHandler {
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            StringTokenizer tok = new StringTokenizer(input);
            if(tok.countTokens()>1) {
                tok.nextToken();
                String option = tok.nextToken();
                if(option.equals("quit"))
                    return("usage: quit\n");
                if(option.equals("?"))
                    return("usage: ?\n");
                if(option.equals("ls"))
                    return("usage: ls [-l]\n");
                if(option.equals("cd"))
                    return("usage: cd [directory-name]\n");
                if(option.equals("pwd"))
                    return("usage: pwd\n");
                OptionHandler handler = instance.getOptionHandler(option);
                if(handler!=null)
                    return(handler.getUsage());
                return("unknown handler for "+option+"\n");
            }
            return(instance.getInteractiveUsage()+"\n");
        }
        public String getUsage() {
            return("usage: help [command]\n");
        }
    }

    /**
     * Handle the Set command
     */
    protected static class SettingsHandler implements OptionHandler {
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            if(out==null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            StringBuilder buffer = new StringBuilder();
            StringTokenizer tok = new StringTokenizer(input);
            if(tok.countTokens()>1) {
                while(tok.hasMoreTokens()) {
                    String token = tok.nextToken();
                    if(token.equals("set"))
                        continue;
                    if(!tok.hasMoreTokens()) {
                        out.println("You must specify a value for "+token);
                        break;
                    }
                    if(token.equals(DEPLOY_BLOCK)) {
                        String block = tok.nextToken();
                        if(block.equalsIgnoreCase("true") ||
                           block.equalsIgnoreCase("yes"))
                            instance.settings.put(DEPLOY_BLOCK, Boolean.TRUE);
                        else
                            instance.settings.put(DEPLOY_BLOCK, Boolean.FALSE);
                    } else if(token.equals(LIST_LENGTH)) {
                        String listLength = tok.nextToken();
                        try {
                            int i = Integer.parseInt(listLength);
                            instance.settings.put(LIST_LENGTH, i);
                        } catch (NumberFormatException e) {
                            return("Invalid "+LIST_LENGTH+" "+listLength);
                        }
                    } else if(token.equals(DEPLOY_WAIT)) {
                        String timeout = tok.nextToken();
                        try {
                            long l = Long.parseLong(timeout);
                            instance.settings.put(DEPLOY_WAIT, l);
                        } catch (NumberFormatException e) {
                            return("Invalid deploy-wait "+timeout);
                        }

                    } else if(token.equals(GROUPS)) {
                        String value = tok.nextToken();
                        String[] groups;
                        if(value.equalsIgnoreCase("all_groups") || value.equalsIgnoreCase("all")) {
                            instance.settings.put(GROUPS, DiscoveryGroupManagement.ALL_GROUPS);
                            groups = DiscoveryGroupManagement.ALL_GROUPS;
                        } else {
                            instance.settings.put(GROUPS, Formatter.toArray(value));
                            groups = Formatter.toArray(value);
                        }
                        DiscoveryManagement dMgr =
                            instance.getServiceFinder().getDiscoveryManagement();
                        if(dMgr instanceof DiscoveryGroupManagement) {
                            try {
                                ((DiscoveryGroupManagement)dMgr).setGroups(groups);
                            } catch(Throwable t) {
                                out.println("Exception setting groups : "+
                                            t.getMessage());
                            }
                        }
                    } else if(token.equals(LOCATORS)) {
                        String locator = tok.nextToken();
                        DiscoveryManagement dMgr =
                            instance.getServiceFinder().getDiscoveryManagement();
                        if(dMgr instanceof DiscoveryLocatorManagement) {
                            if(locator.equalsIgnoreCase("null")) {
                                ((DiscoveryLocatorManagement)dMgr).
                                    setLocators(new LookupLocator[0]);
                            } else {
                                try {
                                    ((DiscoveryLocatorManagement)dMgr).
                                        addLocators(new LookupLocator[]{
                                            new LookupLocator(locator)});
                                } catch(MalformedURLException e) {
                                    out.println("Bad locator format");
                                }
                            }
                        }
                    } else if(token.equals(SYS_PROPS)) {
                        String property = tok.nextToken();
                        StringTokenizer tok1 =
                            new StringTokenizer(property, "= ");
                        if(tok1.countTokens()<2)
                            return("Invalid system property definition "+
                                   property);
                        String name = tok1.nextToken();
                        String value = tok1.nextToken();
                        Properties props =
                            (Properties)instance.settings.get(SYS_PROPS);
                        props.put(name, value);
                        System.setProperty(name, value);
                        instance.settings.put(SYS_PROPS, props);

                    } else if(token.equals(DISCOVERY_TIMEOUT)) {
                        String timeout = tok.nextToken();
                        try {
                            long l = Long.parseLong(timeout);
                            instance.settings.put(DISCOVERY_TIMEOUT, l);
                        } catch (NumberFormatException e) {
                            return("Invalid discovery-timeout "+timeout);
                        }
                    } else {
                        out.println("Invalid property "+token);
                    }
                }
            } else {
                String[] groups = (String[])instance.settings.get(GROUPS);
                if(groups == DiscoveryGroupManagement.ALL_GROUPS)
                    groups = new String[]{"ALL_GROUPS"};

                buffer.append(GROUPS + "\t\t").append(Formatter.fromArray(groups));
                buffer.append("\n");
                DiscoveryManagement dMgr =
                    instance.getServiceFinder().getDiscoveryManagement();
                LookupLocator[] locators = null;
                if(dMgr instanceof DiscoveryLocatorManagement) {
                    locators =
                        ((DiscoveryLocatorManagement)dMgr).getLocators();
                }
                String sLocators = null;
                if(locators!=null) {
                    String[] sLocs = new String[locators.length];
                    for(int i=0; i<sLocs.length; i++)
                        sLocs[i] = locators[i].toString();
                    sLocators = Formatter.fromArray(sLocs);
                }
                buffer.append(LOCATORS + "\t").append(sLocators);
                Properties props =
                    (Properties)instance.settings.get(SYS_PROPS);
                buffer.append("\n");

                int i=0;
                for(Enumeration en=props.keys(); en.hasMoreElements();) {
                    String key = (String)en.nextElement();
                    String value = props.getProperty(key);
                    if(i==0)
                        buffer.append(SYS_PROPS + "\t")
                            .append(key)
                            .append("=").append(value);
                    else
                        buffer.append("\t\t")
                            .append(key)
                            .append("=")
                            .append(value);
                    buffer.append("\n");
                    i++;
                }
                //buffer.append(SYS_PROPS+"\t"+props.toString());
                //buffer.append("\n");
                buffer.append(LIST_LENGTH + "\t").append(
                    instance.settings.get(LIST_LENGTH));
                buffer.append("\n");
                buffer.append(DISCOVERY_TIMEOUT + "\t").append(
                    instance.settings.get(DISCOVERY_TIMEOUT));
                buffer.append("\n");
                buffer.append(DEPLOY_BLOCK + "\t").append(
                    instance.settings.get(DEPLOY_BLOCK));
                buffer.append("\n");
                buffer.append(DEPLOY_WAIT + "\t").append(
                    instance.settings.get(DEPLOY_WAIT));
            }
            return(buffer.toString());
        }

        public String getUsage() {
            return("usage: set ["+GROUPS+" | "+LOCATORS+" | "+
                   SYS_PROPS+" | "+LIST_LENGTH+" | "+DISCOVERY_TIMEOUT+"| "+
                   DEPLOY_BLOCK+" | "+DEPLOY_WAIT+"]\n");
        }
    }

    /**
     * Handle http command
     */
    public static class HttpHandler implements OptionHandler {

        public String process(final String input, final BufferedReader br, final PrintStream out) {
            BufferedReader reader = br;
            if(out==null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            StringTokenizer tok = new StringTokenizer(input);
            if(tok.countTokens()<1)
                return(getUsage());
            int port  = 0;
            String roots = null;
            /* The first token is the "http" token */
            tok.nextToken();
            while(tok.hasMoreTokens()) {
                String value = tok.nextToken();
                if(value.equals("stop")) {
                    if(instance.webster==null) {
                        return("No HTTP server running\n");
                    } else {
                        stopWebster();
                        return("Command successful\n");
                    }
                }
                if(value.startsWith("port")) {
                    StringTokenizer tok1 = new StringTokenizer(value, " =");
                    if(tok1.countTokens()<2)
                        return(getUsage());
                    /* First token will be "port" */
                    tok1.nextToken();
                    /* Next token must be the port value */
                    String sPort = tok1.nextToken();
                    try {
                        port = Integer.parseInt(sPort);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        return("Bad port-number value : "+sPort+"\n");
                    }
                } else {
                    roots = value;
                }
            }
            if(instance.webster!=null) {
                out.print("An HTTP server is already running on port "+
                          "["+instance.webster.getPort()+"], "+
                          "serving ["+instance.webster.getRoots()+"], stop this "+
                          "and continue [y/n] ? ");
                if(reader==null)
                    reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    String response = reader.readLine();
                    if(response!=null) {
                        if(response.startsWith("y") ||
                           response.startsWith("Y")) {
                            stopWebster();
                            if(createWebster(port, roots, out))
                                out.println("Command successful\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return("Problem reading user input, "+
                           "Exception :"+e.getClass().getName()+": "+
                           e.getLocalizedMessage()+"\n");
                }
            } else {
                if(createWebster(port, roots, out))
                    out.println("Command successful\n");
            }
            return("");
        }

        public String getUsage() {
            return("usage: http [[port=port-num] | [roots]] | [stop]\n\n"+
                   "    roots\tsemicolon separated list of directories.\n"+
                   "         \tIf not provided the root directory will be:\n"+
                   "         \t["+debugGetDefaultRoots()+"]");
        }

        /**
         * Create a Webster instance
         *
         * @param port The port to use
         * @param roots Webster's roots
         * @param out A printstream for output
         *
         * @return True if created
         */        
        public boolean createWebster(final int port,
                                     final String roots,
                                     final PrintStream out) {
            return(createWebster(port, roots, false, out));
        }

        public static String debugGetDefaultRoots() {
            String deployDir = System.getProperty("RIO_HOME")+
                               File.separator+
                               "deploy";
            String rioLibDir = System.getProperty("RIO_HOME")+
                               File.separator+
                               "lib";
            String rioLibDLDir = System.getProperty("RIO_HOME")+
                                 File.separator+
                                 "lib-dl";
            return(deployDir+";"+rioLibDir+";"+rioLibDLDir);
        }

        /**
         * Create a Webster instance
         *
         * @param port The port to use
         * @param roots Webster's roots
         * @param quiet Run without output
         * @param out A printstream for output
         *
         * @return True if created
         */
        public static boolean createWebster(final int port,
                                            final String roots,
                                            boolean quiet,
                                            final PrintStream out) {
            if(out==null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            try {
                String deployDir = System.getProperty("RIO_HOME")+
                                   File.separator+
                                   "deploy";
                String rioLibDir = System.getProperty("RIO_HOME")+
                                   File.separator+
                                   "lib";
                String rioLibDLDir = System.getProperty("RIO_HOME")+
                                     File.separator+
                                     "lib-dl";

                String systemRoots = deployDir+";"+rioLibDir+";"+rioLibDLDir;
                String realRoots = (roots == null?systemRoots:roots);
                instance.webster = new Webster(port,
                                      realRoots,
                                      instance.hostAddress);
            } catch(Exception e) {
                e.printStackTrace();
                out.println("Problem creating HTTP server, "+
                                   "Exception :"+e.getClass().getName()+": "+
                                   e.getLocalizedMessage()+"\n");
                return (false);
            }

            if(!quiet) {
                out.println("Address : http://"+instance.webster.getAddress()+":"+
                                   instance.webster.getPort());
                out.println("Root(s) : "+instance.webster.getRoots());
            }
            return (true);
        }

        /**
         * Stop the webster instance
         */
        public static void stopWebster() {
            if(instance.webster!=null)
                instance.webster.terminate();
            instance.webster = null;
        }
    }

    /**
     * Handle stats command
     */
    public static class StatsHandler implements OptionHandler {
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            if(out==null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            long currentTime = System.currentTimeMillis();
            out.println();
            out.println("Rio version: "+instance.getOfficialVersion()+", " +
                        "build: "+instance.getBuildNumber());
            out.println("User: "+System.getProperty("user.name"));
            out.println("Home directory: "+instance.getHomeDir());
            if(!instance.commandLine) {
                out.println("Login time: "+new Date(startTime).toString());
                out.println("Time logged in: "+
                            TimeUtil.format(currentTime-startTime));
                if(instance.getRioLog()!=null)
                    out.println("Log file : "+instance.getRioLog().getAbsolutePath());
                Webster web = instance.getWebster();
                if(web==null) {
                    out.println("http: No HTTP server started");
                } else {

                    out.println("http: Address : http://"+web.getAddress()+":"+
                                web.getPort()+"\n"+
                                "       Root(s) : "+web.getRoots());
                }

                ReggieStat[] rStats =
                    instance.getServiceFinder().getReggieStats(ReggieStat.DISCOVERED);
                out.println("Lookup Service Discovery Statistics");
                if(rStats.length==0)
                    out.println("\tNo lookup services discovered");
                for (ReggieStat rStat : rStats) {
                    long baseTime = (rStat.getBaseTime() == 0 ?
                                     startTime : rStat.getBaseTime());
                    long t = ((rStat.getEventTime() - baseTime) / 1000);
                    out.format("\t%-18.18s %-22.22s %-6s%n",
                               rStat.getMachine() +
                               ":" +
                               rStat.getPort(),
                               Formatter.fromArray(
                                   rStat.getGroups()),
                               t + " secs");
                }
                rStats = instance.getServiceFinder().getReggieStats(ReggieStat.DISCARDED);
                out.println("Lookup Service Discarded Statistics");
                if(rStats.length==0)
                    out.println("\tNo lookup services discarded");
                for (ReggieStat rStat : rStats) {
                    out.format("\t%-18.18s %-22.22s %s%n",
                               rStat.getMachine() +
                               ":"
                               + rStat.getPort(),
                               Formatter.fromArray(
                                   rStat.getGroups()),
                               new Date(rStat.getEventTime()).
                                   toString());

                }
                out.println("Service Information Statistics");
                Map<ServiceFinder.ServiceInfo, ServiceFinder.InfoFetchStat> stats =
                    instance.getServiceFinder().getServiceInfoFetchMap();
                if(stats.isEmpty()) {
                    out.println("\tNo pending information requests");
                } else {
                    out.println("\tNum pending : "+stats.size());
                    int i=0;
                    for(Map.Entry<ServiceFinder.ServiceInfo,
                        ServiceFinder.InfoFetchStat> entry : stats.entrySet()) {
                        ServiceFinder.InfoFetchStat ifs = entry.getValue();
                        long pending = currentTime - ifs.starTime;
                        out.format("\t  [" + i + "] %-28.28s %-12s " +
                                   "blocked : %-10s%n",
                                   ifs.name,
                                   ifs.host,
                                   TimeUtil.format(pending));

                        i++;
                    }
                }
            } else {
                out.println();
            }
            return("");
        }

        public String getUsage() {
            return("usage: stats");
        }
    }

    String getOfficialVersion() {
        return RioVersion.VERSION;
    }

    String getBuildNumber() {
        return RioVersion.getBuildNumber();
    }

    /**
     * Empty impl
     */
    protected static class EmptyHandler implements OptionHandler {
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            return input+" not implemented yet";
        }
        public String getUsage() {
            return "";
        }
    }

    /**
     * Handle jconsole command
     */
    protected static class JConsoleHandler implements OptionHandler {
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            if (out == null)
                throw new IllegalArgumentException("Must have an output PrintStream");
            BufferedReader reader = br;
            try {
                StringTokenizer tok = new StringTokenizer(input);
                String jmxServiceURL = null;
                boolean canceled = false;
                if(tok.countTokens()==1) {
                    ServiceItem[] items =
                        instance.getServiceFinder().find(null,
                                        new Entry[] {new JMXProtocolType(),
                                                     new JMXProperty()});
                    if(items.length==0)
                        return("No service instances with JMXProtocolType " +
                               "and JMXProperty " +
                               "entries discovered\n");
                    if(items.length==1) {
                        jmxServiceURL =
                            JMXUtil.getJMXConnection(items[0].attributeSets);
                    } else {
                        if (reader==null)
                            reader = new BufferedReader(new InputStreamReader(System.in));
                        out.println(Formatter.asList(items)+"\n");
                        printRequest(out);
                        while(true) {
                            try {
                                String response = reader.readLine();
                                if(response!=null) {
                                    if(response.equals("c")) {
                                        canceled = true;
                                        break;
                                    }
                                    try {
                                        int num = Integer.parseInt(response);
                                        if(num<1 || num >(items.length+1)) {
                                            printRequest(out);
                                        } else {
                                            Entry[] attrs =
                                                items[num-1].attributeSets;
                                            jmxServiceURL =
                                                JMXUtil.getJMXConnection(
                                                    attrs);
                                            break;
                                        }
                                    } catch(NumberFormatException e) {
                                        out.println("Invalid choice "+
                                                    "["+response+"]");
                                        printRequest(out);
                                    }
                                }
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(jmxServiceURL!=null) {
                        execJConsole(jmxServiceURL);
                        return(onSuccess());
                    } else if(canceled) {
                        return("");
                    } else {
                        return("Command failed, invalid JMXProperty entry\n");
                    }
                } else if(tok.countTokens()>1) {
                    /* First token is "jconsole" */
                    tok.nextToken();
                    /* Next token is the deploy-descriptor */
                    jmxServiceURL  = tok.nextToken();
                    execJConsole(jmxServiceURL);
                    return(onSuccess());
                } else {
                    return(getUsage());
                }

            } catch(IOException e) {
                out.println("Command failed, "+
                            e.getClass().getName()+": "+
                            e.getLocalizedMessage());
            }
            return "";
        }

        String onSuccess() {
            return "Launching jconsole, command successful\n";
        }

        void execJConsole(final String jmxServiceURL) throws IOException {
            Runtime.getRuntime().exec("jconsole "+jmxServiceURL);
        }

        /*
         * Print the request to the operator
         */
        void printRequest(final PrintStream out) {
            out.print("Choose a service for jconsole support or \"c\" to cancel : ");
        }

        public String getUsage() {
            return("usage: jconsole [jmx-connection-string");
        }
    }

    /**
     * Handle easter eggs command
     */
    protected static class EasterEggHandler implements OptionHandler {
       public String process(final String input, final BufferedReader br, final PrintStream out) {
           if(input.equalsIgnoreCase("ian"))
               return("World's best son!");
           if(input.equalsIgnoreCase("sara"))
               return ("World's best daughter!");
           return("I want an easter egg");
       }


       public String getUsage() {
           return("Dennis Reedy");
       }

       boolean exists(final String fName) {
           File file = new File(fName);
           return(file.exists());
       }

       String locateFailed(final String name) {
           return("Cannot locate "+name+", command failed\n");
       }
   }

    /*
     * Set system properties from configuration
     */
    private Properties getConfiguredSystemProperties() throws ConfigurationException {
        Configuration config = getConfiguration();
        Properties sysProps = new Properties();
        String[] systemProperties =
            (String[])config.getEntry("org.rioproject.start", "systemProperties", String[].class, new String[0]);
        if(systemProperties.length > 0) {
            if(systemProperties.length%2 != 0) {
                System.err.println("systemProperties elements has odd length : "+systemProperties.length);
            } else {
                for(int i = 0; i < systemProperties.length; i += 2) {
                    String name = systemProperties[i];
                    String value = systemProperties[i+1];
                    sysProps.setProperty(name, value);
                }
            }
        }
        return sysProps;
    }

    /*
     * Initialize runtime settings
     */
    private void initSettings(final String[] groups, final LookupLocator[] locators, final long discoTimeout)
        throws ConfigurationException {
        settings.put(GROUPS, groups);
        settings.put(LOCATORS, locators);
        Properties props = new Properties();
        String rioHome = System.getProperty("RIO_HOME");
        if(rioHome == null)
            throw new RuntimeException("RIO_HOME must be set");
        props.put("java.protocol.handler.pkgs", "net.jini.url");
        Properties addedProps = getConfiguredSystemProperties();
        props.putAll(addedProps);
        Properties sysProps = System.getProperties();
        sysProps.putAll(props);
        System.setProperties(sysProps);
        settings.put(SYS_PROPS, props);
        settings.put(DISCOVERY_TIMEOUT, discoTimeout);
        settings.put(DEPLOY_BLOCK, Boolean.TRUE);
        settings.put(DEPLOY_WAIT, (long) 5 * 1000);
        settings.put(LIST_LENGTH, 20);
        if(homeDir==null)
            homeDir = System.getProperty("user.dir");
        currentDir = new File(homeDir);
        if(!commandLine) {
            String logDirPath = System.getProperty(CONFIG_COMPONENT+".logDir", rioHome+File.separator+"logs");
            File logDir = new File(logDirPath);
            if(!logDir.exists()) {
                if(!logDir.mkdir()) {
                    System.err.println("Unable to create log directory at "+logDir.getAbsolutePath());
                }
            }
            rioLog = new File(logDir, "rio.log");
            if(rioLog.exists()) {
                rioLog.delete();
            }
            if(logDir.exists()) {
                try {
                    System.setErr(new PrintStream(new FileOutputStream(rioLog)));
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            System.err.println("===============================================");
            System.err.println(cliName + " Interactive Shell Program\n"+
                               "Log creation : "+new Date(startTime).toString()+"\n"+
                               "Operator : "+System.getProperty("user.name"));
            System.err.println("===============================================");
            if(addedProps.size()>0) {
                StringBuilder buff = new StringBuilder();
                for (Map.Entry<Object, Object> entry : addedProps.entrySet()) {
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();
                    buff.append("\n");
                    buff.append("    ").append(key).append("=").append(value);
                }
                System.err.println("Added System Properties {"+buff.toString()+"\n}");
            }
        }
    }

    /**
     * Get the ServiceFinder
     *
     * @return The ServiceFinder
     */
    public synchronized ServiceFinder getServiceFinder() {
        if(finder==null) {
            String[] groups = DiscoveryGroupManagement.ALL_GROUPS;
            try {
                finder = new ServiceFinder(groups, null, getConfiguration());
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return(finder);
    }

    /**
     * Get the Configuration
     *
     * @return The SystemConfiguration
     */
    public synchronized Configuration getConfiguration() {
        if (sysConfig == null)
            sysConfig = EmptyConfiguration.INSTANCE;
        return sysConfig;
    }

    /**
     * Initialize the CLI, parsing arguments, loading configuration and creating
     * optional OptionHandler instances
     *
     * @param args Command line arguments to parse, must not be null
     *
     * @return Array of string args to be parsed
     *
     * @throws Throwable if anything at all happens that is not supposed to happen
     */
    public static String[] initCLI(final String[] args) throws Throwable {
        if(args==null)
            throw new IllegalArgumentException("args is null");
        final LinkedList<String> commandArgs = new LinkedList<String>();
        commandArgs.addAll(Arrays.asList(args));
        if(args.length>=1) {
            boolean createConfig = false;
            if(args[0].endsWith(".config") || args[0].endsWith(".groovy")) {
                createConfig = true;
                commandArgs.removeFirst();
            }
            if(createConfig) {
                sysConfig = ConfigurationProvider.getInstance(new String[]{args[0]});
            }
        }

        if(sysConfig==null)
            sysConfig = EmptyConfiguration.INSTANCE;

        userName = (String)sysConfig.getEntry(CONFIG_COMPONENT, "userName", String.class, null);
        loginContext = null;
        try {
            loginContext =
                (LoginContext) Config.getNonNullEntry(sysConfig, CONFIG_COMPONENT, "loginContext", LoginContext.class);
        } catch (NoSuchEntryException e) {
            // leave null
        }


        String[] result;
        if (loginContext != null) {
            loginContext.login();
            try {
                result = Subject.doAsPrivileged(loginContext.getSubject(),
                                                new PrivilegedExceptionAction<String[]>() {
                                                    public String[] run() throws Exception {
                                                        return (doInit(args, commandArgs));
                                                    }
                                                },
                                                null);
            } catch (PrivilegedActionException e) {
                throw e.getCause();
            }
        } else {
            result = doInit(args, commandArgs);
        }
        
        return result;
    }

    static String[] doInit(final String[] arguments, final LinkedList<String> commandArgs) throws Exception {
        getInstance();
        instance.setOutput((PrintStream)sysConfig.getEntry(CONFIG_COMPONENT, "output", PrintStream.class, System.out));

        String[] groups =
            (String[])sysConfig.getEntry(CONFIG_COMPONENT, "groups", String[].class, DiscoveryGroupManagement.ALL_GROUPS);

        LookupLocator[] locators =
            (LookupLocator[])sysConfig.getEntry(CONFIG_COMPONENT, "locators", LookupLocator[].class, null);

        long discoveryTimeout =
            (Long) sysConfig.getEntry(CONFIG_COMPONENT, "discoveryTimeout", long.class, (long) 1000 * 5);

        int httpPort = (Integer) sysConfig.getEntry(CONFIG_COMPONENT, "httpPort", int.class, 0);

        boolean noHttp = (Boolean) sysConfig.getEntry(CONFIG_COMPONENT, "noHttp", boolean.class, Boolean.FALSE);
        /* Look to see if the operator has provided a starting directory,
         * groups, locators, discovery timeout, httpPort or ignore http */
        String homeDir = null;

        String args[] = new String[arguments.length];
        System.arraycopy(arguments, 0, args, 0, args.length);
        for (String arg : args) {
            if (arg.startsWith("homeDir")) {
                String[] values = arg.split("=");
                homeDir = values[1].trim();
                commandArgs.remove(arg);
            } else if (arg.startsWith("groups")) {
                String[] values = arg.split("=");
                String groupsArg = values[1].trim();
                groups = toArray(groupsArg, " \t\n\r\f,");
                System.setProperty(Constants.GROUPS_PROPERTY_NAME, groupsArg);
                for (int j = 0; j < groups.length; j++) {
                    if (groups[j].equalsIgnoreCase("all")) {
                        groups = DiscoveryGroupManagement.ALL_GROUPS;
                        break;
                    }
                }
                commandArgs.remove(arg);
            } else if (arg.startsWith("locators")) {
                String[] values = arg.split("=");
                String locatorsArg = values[1].trim();
                String[] locatorArray = toArray(locatorsArg,
                                                " \t\n\r\f,");
                List<LookupLocator> list = new ArrayList<LookupLocator>();
                if (locators != null) {
                    list.addAll(Arrays.asList(locators));
                }
                for (String aLocatorArray : locatorArray) {
                    list.add(new LookupLocator(aLocatorArray));
                }
                locators = list.toArray(new LookupLocator[list.size()]);
                commandArgs.remove(arg);
            } else if (arg.startsWith("discoveryTimeout")) {
                String[] values = arg.split("=");
                String timeoutArg = values[1].trim();
                discoveryTimeout = Long.parseLong(timeoutArg);
                commandArgs.remove(arg);
            } else if (arg.startsWith("httpPort")) {
                String[] values = arg.split("=");
                String httpPortArg = values[1].trim();
                httpPort = Integer.parseInt(httpPortArg);
                commandArgs.remove(arg);
            } else if (arg.startsWith("-noHttp")) {
                noHttp = true;
                commandArgs.remove(arg);
            }
        }

        /* Reset the args parameter, removing the config parameter */
        args = commandArgs.toArray(new String[commandArgs.size()]);

        if(args.length==0)
            instance.commandLine = false;
        instance.initSettings(groups, locators, discoveryTimeout);
        if(homeDir!=null) {
            DirHandler.changeDir(homeDir, false, instance.getOutput());
        }
        instance.finder = new ServiceFinder(groups, locators, sysConfig);
        instance.provisionNotifier = new ServiceProvisionNotification(sysConfig);
        InetAddress inetAddress = InetAddress.getLocalHost();
        instance.hostName = inetAddress.getHostName();
        instance.hostAddress = inetAddress.getHostAddress();
        if(!noHttp)
            HttpHandler.createWebster(httpPort, null, true, instance.getOutput());
        /* Load the OptionHandlers from the configuration */
        instance.loadOptionHandlers(sysConfig);

        if(args.length==0) {
            instance.manageInteraction();
        } else {
            if(instance.validCommand(args[0])) {
                OptionHandler handler = instance.getOptionHandler(args[0]);
                if(handler!=null) {
                    String response = handler.process(Formatter.fromArray(args, " "), null, instance.getOutput());
                    if(response.length()>0)
                        instance.getOutput().println(response);
                }
            } else {
                instance.printUsage();
            }
            instance.onExit(false);
        }

        return args;
    }

    public static class LoginCallbackHandler implements CallbackHandler {

        public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callback;
                    /* If username not supplied in configuration, prompt the user
                     * for the username. */
                    if (userName == null) {
                        System.out.print(nc.getPrompt());
                        String response = (new BufferedReader(new
                            InputStreamReader(System.in))).readLine();
                        nc.setName(response);
                    } else {
                        nc.setName(userName);
                        System.out.println("Username: " + userName);
                    }
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callback;
                    String prompt = pc.getPrompt();
                    /* If prompt has an optional entry, skip */
                    if (!prompt.contains("optional")) {
                        System.out.print(prompt);
                        String tmpPassword = (new BufferedReader(new
                            InputStreamReader(System.in))).readLine();
                        pc.setPassword(tmpPassword.toCharArray());
                    }
                } else if (callback instanceof ConfirmationCallback) {
                    System.out.println("\n");
                }
            }
        }
    }
       
    public static void main(final String[] args) {
        ensureSecurityManager();
        try {
            initCLI(args);
        } catch (Throwable t) {
            if(t.getCause()!=null)
                t = t.getCause();
            System.out.println("Exception initializing system ["+
                               t.getClass().getName()+": "+
                               t.getMessage()+"], check log for details");
            t.printStackTrace();
            System.exit(1);
        }        
    }
}
