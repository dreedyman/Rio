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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * Handle the list command
 *
 * @author Dennis Reedy
 */
public class ListHandler implements OptionHandler {
    static final String CYBERNODE = "cybernode";
    static final String MONITOR = "monitor";

    public String process(final String input, final BufferedReader br, final PrintStream out) {
        if(out==null)
            throw new IllegalArgumentException("Must have an output PrintStream");
        String modifiableInput = input;
        StringTokenizer tok = new StringTokenizer(modifiableInput);
        /* first token is "list" */
        String cmd = tok.nextToken();
        int options = 0;
        modifiableInput = modifiableInput.substring((modifiableInput.length()==cmd.length()?
                                 cmd.length():cmd.length()+1));
        String lookfor = getWhatToLookFor(modifiableInput);
        if(lookfor==null)
            return(getUsage());
        if(!lookfor.equals("all")) {
            modifiableInput = modifiableInput.substring((modifiableInput.length()==lookfor.length()?
                                     lookfor.length():lookfor.length()+1));
        }
        tok = new StringTokenizer(modifiableInput);
        while(tok.hasMoreTokens()) {
            String option = tok.nextToken();
            if(option.startsWith("timeout")) {
                StringTokenizer tok1 = new StringTokenizer(option, "= ");
                if(tok1.countTokens()<2)
                    return("Bad discovery timeout option : "+option);
                /* first token is "timeout" */
                tok1.nextToken();
                String value = tok1.nextToken();
                try {
                    long l = Long.parseLong(value);
                    CLI.getInstance().settings.put(CLI.DISCOVERY_TIMEOUT, l);
                } catch (NumberFormatException e) {
                    return("Bad discovery timeout value : "+value);
                }
            } else {
                out.println("Unknown option : "+option);
                return(getUsage());
            }

        }

        /* Setup entry filters */        
        Entry[] attrs = new Entry[0];

        Integer listLength = (Integer) CLI.getInstance().settings.get(CLI.LIST_LENGTH);
        ServiceItem[] items = null;
        boolean genericLister = true;
        if("all".equals(lookfor)) {
            items = CLI.getInstance().finder.find(null, attrs);
        } else if(lookfor.equals(CYBERNODE)) {
            items = CLI.getInstance().finder.findCybernodes(null, attrs);
            genericLister = false;
            Formatter.cybernodeLister(items, options, br, out);
        } else if(lookfor.equals(MONITOR)) {
            items = CLI.getInstance().finder.findMonitors(null, attrs);
            genericLister = false;
            Formatter.provisionManagerLister(items, options, br, out);
        }
        if(items!=null && items.length>0) {
            if(genericLister) {
                String[] array = Formatter.formattedArray(items, options);
                for(int i=0, lineCounter=1; i<array.length; i++,lineCounter++) {
                    if(lineCounter % listLength==0 && array.length > lineCounter) {
                        Formatter.promptMore(br, out);
                    }
                    out.println(array[i]);
                }
                out.println();
            }
        }
        return("");
    }

    /**
     * Get what to look for
     *
     * @param input What to look for
     *
     * @return Valid returns will be "all", "monitor" or "cybernode"
     */
    String getWhatToLookFor(String input) {
        String lookFor = "all";
        StringTokenizer tok = new StringTokenizer(input);
        if(tok.countTokens()>0) {
            String value = tok.nextToken();
            if(value.equals(MONITOR)) {
                lookFor = value;
            } else if(value.equals(CYBERNODE)) {
                lookFor = value;
            } else {
                return(null);
            }
        }
        return(lookFor);
    }

    public String getUsage() {
        StringBuilder usage = new StringBuilder();
        usage.append("usage: list [type]\n\n");
        usage.append("type:\n");
        usage.append("   ").append(MONITOR).append(" | ").append(CYBERNODE).append("\t\t");
        usage.append("Only one allowed, default is to list \"all\" discovered services\n\n");
        usage.append("Example: rio> list cybernode\n");
        return(usage.toString());
    }
}
