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

    public String process(String input, BufferedReader br, PrintStream out) {
        if(out==null)
            throw new NullPointerException("Must have an output PrintStream");
        StringTokenizer tok = new StringTokenizer(input);
        /* first token is "list" */
        String cmd = tok.nextToken();
        int options = 0;
        input = input.substring((input.length()==cmd.length()?
                                 cmd.length():cmd.length()+1));
        String lookfor = getWhatToLookFor(input);
        if(lookfor==null)
            return(getUsage());
        if(!lookfor.equals("all")) {
            input = input.substring((input.length()==lookfor.length()?
                                     lookfor.length():lookfor.length()+1));
        }
        tok = new StringTokenizer(input);
        while(tok.hasMoreTokens()) {
            String option = tok.nextToken();
            if(option.equals("codeserver"))
                options |= Formatter.EXPORT_CODEBASE;
            else if(option.startsWith("timeout")) {
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

        Integer listLength =
            (Integer) CLI.getInstance().settings.get(CLI.LIST_LENGTH);
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
                if(!isValidOption(value))
                    return(null);
            }
        }
        return(lookFor);
    }

    /**
     * Determine if the input contains a valid option
     *
     * @param input The option
     *
     * @return True if valid
     */
    boolean isValidOption(String input) {
        boolean valid = true;
        StringTokenizer tok = new StringTokenizer(input);
        while(tok.hasMoreTokens()) {
            String option = tok.nextToken();
            valid = option.equals("codeserver");
            if(!valid) {
                break;
            }
        }
        return(valid);
    }

    public String getUsage() {        
        return("usage: list [type] [options]\n\n"+
               "type:\n" +
               "   "+MONITOR+" | "+CYBERNODE+"\t\t"+
               "Only one allowed, default is to list \"all\" discovered services\"\n"+
               "\noptions:\n"+
               "   codeserver\t"+
               "Use when the type is either \""+CYBERNODE+"\" " +
               "or \""+MONITOR+"\n\n"+
               "Example: rio> list cybernode\n");
    }
}
