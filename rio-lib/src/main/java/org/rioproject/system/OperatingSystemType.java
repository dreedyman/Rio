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
package org.rioproject.system;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Vector;

/**
 * Utilities to help determine operating system type
 *
 * @author Dennis Reedy
 */
public class OperatingSystemType {
    /**
     * Linux identifier
     */
    public static final String LINUX = "linux";
    /**
     * Solaris identifier
     */
    public static final String SOLARIS = "sunos";
    /**
     * Mac identifier
     */
    public static final String MACINTOSH = "Mac";
    /**
     * Windows identifier
     */
    public static final String WINDOWS = "Windows";
    /**
     * Windows 2000 identifier
     */
    public static final String WINDOWS_2K = WINDOWS+" 2000";
    /**
     * Windows XP identifier
     */
    public static final String WINDOWS_XP = WINDOWS+" XP";
    /**
     * HP-UX identifier
     */
    public static final String HP_UX = "HP-UX";

    /**
     * Check if running on Linux
     *
     * @return If running on Linux return <code>true</code>, otherwise return
     *         <code>false</code>
     */
    public static boolean isLinux() {
        String opSys = System.getProperty("os.name");
        return (opSys.equalsIgnoreCase(LINUX));
    }

    /**
     * Check if running on Solaris
     *
     * @return If running on Solaris return <code>true</code>, otherwise return
     *         <code>false</code>
     */
    public static boolean isSolaris() {
        String opSys = System.getProperty("os.name");
        return (opSys.equalsIgnoreCase(SOLARIS));
    }

    /**
     * Check if running on Mac
     *
     * @return If running on Mac return <code>true</code>, otherwise return
     *         <code>false</code>
     */
    public static boolean isMac() {
        String opSys = System.getProperty("os.name");
        return (opSys.startsWith(MACINTOSH));
    }

    /**
     * Check if running on HP-UX
     *
     * @return If running on HP-UX return <code>true</code>, otherwise return
     *         <code>false</code>
     */
    public static boolean isHP() {
        String opSys = System.getProperty("os.name");
        return (opSys.equals(HP_UX));
    }

    /**
     * Check if running on Windows
     *
     * @return If running on Windows return <code>true</code>, otherwise return
     *         <code>false</code>
     */
    public static boolean isWindows() {
        String opSys = System.getProperty("os.name");
        return (opSys.startsWith(WINDOWS));
    }

    /**
     * Check if running on Windows 2000
     *
     * @return If running on Windows 2000 return <code>true</code>, otherwise
     *         return <code>false</code>
     */
    public static boolean isWindows2K() {
        String opSys = System.getProperty("os.name");
        return (opSys.startsWith(WINDOWS_2K));
    }

    /**
     * Check if running on Windows XP
     *
     * @return If running on Windows XP return <code>true</code>, otherwise
     *         return <code>false</code>
     */
    public static boolean isWindowsXP() {
        String opSys = System.getProperty("os.name");
        return (opSys.startsWith(WINDOWS_XP));
    }


    /**
     * Gets a string representing the pid of this program - Java VM
     *
     * @return A String that corresponds to the process ID (PID) of the JVM.
     * This method will create a shell using /bin/bash and obtain the $PPID
     * environment variable. If the operating system type is Windows, an
     * empty string is returned
     *
     * @throws IOException If the shell cannot be created
     * @throws InterruptedException If the process wait is interrupted
     */
    public static String getPid() throws IOException,InterruptedException {
        if(isWindows())
            return "";
        
        Vector<String> commands=new Vector<String>();
        commands.add("/bin/bash");
        commands.add("-c");
        commands.add("echo $PPID");
        ProcessBuilder pb=new ProcessBuilder(commands);

        Process pr=pb.start();
        pr.waitFor();
        if (pr.exitValue()==0) {
            BufferedReader outReader=new BufferedReader(new InputStreamReader(pr.getInputStream()));
            return outReader.readLine().trim();
        } else {
            System.out.println("Error while getting PID");
            return "";
        }
    }
}
