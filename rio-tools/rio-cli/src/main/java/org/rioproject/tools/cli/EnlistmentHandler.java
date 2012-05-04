/*
 * Copyright 2011 the original author or authors.
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
import org.rioproject.cybernode.Cybernode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Handles enlistment and release of Cybernodes
 */
public class EnlistmentHandler  {
    static final String ENLIST = "enlist";
    static final String RELEASE = "release";

    public static class EnlistHandler implements OptionHandler {

        @Override
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            return handleRequest(input, br, out);
        }

        @Override public String getUsage() {
            return ("usage: enlist ");
        }
    }

    public static class ReleaseHandler implements OptionHandler {

        @Override
        public String process(final String input, final BufferedReader br, final PrintStream out) {
            return handleRequest(input, br, out);
        }

        @Override public String getUsage() {
            return ("usage: release");
        }
    }

    private static ServiceItem[] getCybernodes(final String action) throws RemoteException {
        List<ServiceItem> list = new ArrayList<ServiceItem>();
        ServiceItem[] items = CLI.getInstance().getServiceFinder().findCybernodes(null, null);
        for(ServiceItem item : items) {
            Cybernode c = (Cybernode)item.service;
            if(action.equals(RELEASE)) {
                if(c.isEnlisted()) {
                    list.add(item);
                }
            } else {
                if(!c.isEnlisted()) {
                    list.add(item);
                }
            }
        }
        return list.toArray(new ServiceItem[list.size()]);
    }

    private static void printRequest(final PrintStream out, final String action) {
        out.print("Enter cybernode to " + action + " or \"c\" to cancel : ");
    }

    private static String handleRequest(final String input, final BufferedReader br, final PrintStream out) {
        if (out == null)
            throw new IllegalArgumentException("Must have an output PrintStream");
        BufferedReader reader = br;
        if (reader == null)
            reader = new BufferedReader(new InputStreamReader(System.in));

        StringTokenizer tok = new StringTokenizer(input);
        /* first token is "enlist" or "release" */
        String action = tok.nextToken();
        ServiceItem[] items;
        try {
            items = getCybernodes(action);
        } catch (RemoteException e) {
            e.printStackTrace();
            return ("Problem checking cybernode enlistment status , " +
                    "Exception : " + e.getLocalizedMessage() + "\n");
        }
        if(items.length==0) {
            StringBuilder sb = new StringBuilder();
            String word = action.equals(ENLIST)?"enlisted":"released";
            sb.append("All cybernodes are ").append(word);
            return sb.toString();
        }
        out.println(Formatter.asChoices(items) + "\n");
        printRequest(out, action);
        while (true) {
            try {
                String response = reader.readLine();
                if (response != null) {
                    if (response.equals("c"))
                        break;
                    try {
                        int num = Integer.parseInt(response);
                        if (num < 1 || num > (items.length + 1)) {
                            printRequest(out, action);
                        } else {
                            if (num == (items.length + 1)) {
                                for(ServiceItem item : items) {
                                    Cybernode cybernode = (Cybernode)item.service;
                                    handleAction(cybernode, action);
                                }
                            } else {
                                Cybernode cybernode = (Cybernode)items[num-1].service;
                                handleAction(cybernode, action);
                            }
                            break;
                        }
                    } catch (NumberFormatException e) {
                        out.println("Invalid choice [" + response + "]");
                        printRequest(out, action);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ("");
    }

    private static void handleAction(final Cybernode cybernode, final String action) throws RemoteException {
        if(action.equals(ENLIST))
            cybernode.enlist();
        else
            cybernode.release(true);

    }
}
