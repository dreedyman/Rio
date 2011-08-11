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
package org.rioproject.exec;

import org.rioproject.resources.util.FileUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Helpful utilities
 *
 * @author Dennis Reedy
 */
public class Util {
    public static final String COMPONENT = ProcessManager.class.getPackage().getName();
    public static final String RESOURCE_ROOT=COMPONENT.replace('.', '/')+"/resources/";

    /**
     * Get a resource using the current context classloader
     *
     * @param resource The resource name
     *
     * @return The URL of the loaded resource
     *
     * @throws IOException If the resource cannot be loaded
     */
    public static URL getResource(String resource) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(RESOURCE_ROOT+resource);
        if(url==null)
            throw new IOException("unable to load "+RESOURCE_ROOT+resource+" " +
                                  "resource");
        return url;
    }

    /**
     * Run chmod
     *
     * @param file The file to chmod
     * @param perms The permissions to chmod
     *
     * @throws IOException If the command does not execute
     */
    public static void chmod(File file, String perms) throws IOException {
        if(perms==null)
            throw new IllegalArgumentException("cannot apply permissions if " +
                                               "perms is null");
        String toExec = FileUtils.getFilePath(file);
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("chmod "+perms+" "+toExec);
            process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Failed to chmod " +
                                  "["+file+"] " +
                                  "with permissions ["+perms+"]");
        } finally {
            if(process!=null) {
                close(process.getOutputStream());
                close(process.getInputStream());
                close(process.getErrorStream());
                process.destroy();
            }
        }
    }

    /**
     * Apply execute permissions for a file
     *
     * @param file The file to apply execute permissions on
     *
     * @throws IOException If the command does not execute
     */
    public static void chmodX(File file) throws IOException {
        boolean execChmod = false;
        try {
            Method setExecutable = File.class.getMethod("setExecutable",
                                                        boolean.class);
            setExecutable.invoke(file, true);
        } catch (Exception e) {
            execChmod = true;
        }

        if(execChmod) {
            String toExec = FileUtils.getFilePath(file);
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("chmod +x "+toExec);
                process.waitFor();
            } catch (InterruptedException e) {
                throw new IOException("Failed to chmod +x" +
                                      "["+file+"]");
            } finally {
                if(process!=null) {
                    close(process.getOutputStream());
                    close(process.getInputStream());
                    close(process.getErrorStream());
                    process.destroy();
                }
            }
        }
    }
    
    public static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    /**
     * Write contents to a file
     *
     * @param contents The contents to write
     * @param file The file to write to
     * 
     * @throws IOException If the file cannot be wrriten to
     */
    public static void writeFile(String contents, File file) throws
                                                             IOException {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file));
            out.write(contents);
        } finally {
            if(out!=null)
                out.close();
        }
    }

    /**
     * Run (exec) a shell script
     *
     * @param shellScript The shell script file to exec
     *
     * @return The exit value of the exec'ed process
     *
     * @throws IOException If the process cannot be created
     */
    public static int runShellScript(File shellScript) throws IOException {
        return runShellScript(shellScript, true);
    }

    /**
     * Run (exec) a shell script
     *
     * @param shellScript The shell script file to exec
     * @param sync If true wait for the process to complete
     *
     * @return The exit value of the exec'ed process
     *
     * @throws IOException If the process cannot be created
     */
    public static int runShellScript(File shellScript, boolean sync) throws IOException {
        String sh = FileUtils.getFilePath(shellScript);
        ProcessBuilder pb = new ProcessBuilder(sh);
        Process process = pb.start();
        int exitValue = 0;
        if(sync) {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
            } finally {
                close(process.getOutputStream());
                close(process.getInputStream());
                close(process.getErrorStream());
                process.destroy();
                exitValue = process.exitValue();
            }
        }
        return exitValue;
    }

    /**
     * Extend command line, inserting the root directory in front of the
     * command line of the extension
     *
     * @param root The root directory name
     * @param exec The ExecDescriptor
     *
     * @return A modified ExecDescriptor
     */
    public static ExecDescriptor extendCommandLine(String root,
                                                   ExecDescriptor exec) {
        String cmdLine = exec.getCommandLine();
        String workingDir = exec.getWorkingDirectory();
        if(workingDir!=null) {
            if(workingDir.endsWith(File.separator))
                cmdLine = workingDir + cmdLine;
            else
                cmdLine = workingDir + File.separator + cmdLine;
        }
        if(root!=null) {
            if(!root.endsWith(File.separator))
                root = root + File.separator;

            exec.setCommandLine(root + cmdLine);

            if(workingDir!=null && !workingDir.startsWith("/")) {
                exec.setWorkingDirectory(root + workingDir);
            }                                    
        }
        return (exec);
    }

    /**
     * Search and Replace
     *
     * @param str The string to replace tokens for
     * @param pattern The pattern to look for
     * @param replace The replacement for the pattern
     *
     * @return A string with tokens replaced
     */
    public static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e;
        StringBuffer result = new StringBuffer();

        while((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e+pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }
}
