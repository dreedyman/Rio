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
package org.rioproject.tools.cli;

import java.io.*;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Handles directory commands
 *
 * @author Dennis Reedy
 */
public class DirHandler implements OptionHandler {
    public String process(final String input, final BufferedReader br, final PrintStream out) {
        if(out==null)
            throw new IllegalArgumentException("Must have an output PrintStream");
        BufferedReader reader = br;
        if(input.startsWith("ls") || input.startsWith("dir")) {
            File d = CLI.getInstance().currentDir;
            boolean details = false;
            StringTokenizer tok = new StringTokenizer(input);
            if(tok.countTokens()>1) {
                /* First token is "ls" */
                tok.nextToken();
                String option = tok.nextToken();
                if(option.equals("-l"))
                    details = true;
                else {
                    File temp = new File(d+File.separator+option);
                    if(temp.isDirectory()) {
                        d = temp;
                    } else {
                        out.println("Bad option "+option);
                        return("");
                    }
                }
            }
            File[] files = d.listFiles();
            if(files==null) {
                String path = CLI.getInstance().currentDir.getAbsolutePath();
                try {
                    path = CLI.getInstance().currentDir.getCanonicalPath();
                } catch(IOException e) {
                    /*ignore */
                }
                return("No files for current working directory \""+
                       path+"\"");
            }
            int sum = 0;
            for (File file : files) {
                sum += file.length();
            }

            out.println("total "+sum);
            File parent = d.getParentFile();
            if(parent!=null && details) {
                Date fileDate = new Date(parent.lastModified());
                out.println(getPerms(parent)+"   "+
                            parent.length()+"\t"+
                            fileDate.toString()+"\t"+
                            "..");
            }
            for (File file : files) {
                if (details) {
                    String tabs = "\t";
                    if (file.length() < 10)
                        tabs = tabs + "\t";
                    String perms = getPerms(file);
                    Date fileDate = new Date(file.lastModified());
                    out.println(perms + "   " +
                                file.length() + tabs +
                                fileDate.toString() + "\t" +
                                file.getName());
                } else {
                    out.println(file.getName());
                }
            }
        } else if(input.equals("pwd")) {
            try {
                out.println("\""+CLI.getInstance().currentDir.getCanonicalPath()+"\" "+
                            "is the current working directory");
            } catch(IOException e) {
                out.println("\""+CLI.getInstance().currentDir.getAbsolutePath()+"\" "+
                            "is the current working directory");
            }
        } else {
            StringTokenizer tok = new StringTokenizer(input);
            if(tok.countTokens()>1) {
                /* First token is "cd" */
                tok.nextToken();
                String value = tok.nextToken();
                if(!value.endsWith("*"))
                    changeDir(value, out);
            } else {
                if(reader==null)
                    reader = new BufferedReader(new InputStreamReader(System.in));
                out.print("(enter a directory to change to) ");
                try {
                    String response = reader.readLine();
                    if(response.length()==0) {
                        out.println("usage: cd directory");
                    } else {
                        changeDir(response, out);
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return("");
    }

    boolean changeDir(String dirName, PrintStream out) {
        return(changeDir(dirName, true, out));
    }

    static boolean changeDir(String dirName, boolean echoSuccess, PrintStream out) {
        String dirNameToUse = dirName;
        boolean changed = false;
        if(dirNameToUse.startsWith("..")) {
            dirNameToUse = CLI.getInstance().currentDir.getAbsolutePath()+File.separator+dirName;
        }
        if(dirNameToUse.equals("~")) {
            dirNameToUse = CLI.getInstance().homeDir;
        }
        /* See if the passed in property is a complete directory */
        File dir = new File(dirNameToUse);
        /* If its not, it may be a relative path */
        if(!dir.exists()) {
            dir = new File(CLI.getInstance().currentDir.getAbsolutePath()+
                           File.separator+
                           dirName);
            if(!dir.exists()) {
                out.println(dirName+": No such file or directory");
            }
        }
        if(dir.isDirectory()) {
            CLI.getInstance().currentDir = dir;
            if(echoSuccess) {
                try {
                    out.println(dir.getCanonicalPath());
                } catch(IOException e) {
                    e.printStackTrace();
                }
                out.println("Command successful");
            }
            changed = true;
        } else {
            out.println(dirName+": Not a directory");
        }
        return(changed);
    }

    String getPerms(File file) {
        String perms;
        if(file.isDirectory())
            perms = "d";
        else
            perms = "-";
        if(file.canRead())
            perms = perms+"r";
        else
            perms = perms+"-";
        if(file.canWrite())
            perms = perms+"w";
        else
            perms = perms+"-";
        return(perms);
    }

    public String getUsage() {
        return("usage: [ls [-l] | pwd | cd directory-name\n");
    }
}
