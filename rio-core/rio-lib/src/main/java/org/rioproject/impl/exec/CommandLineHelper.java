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
package org.rioproject.impl.exec;

import org.rioproject.config.Constants;
import org.rioproject.impl.system.OperatingSystemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * A helper to create and verify a command line that can be used to exec a service in it's own JVM.
 *
 * @author Dennis Reedy
 */
public final class CommandLineHelper {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineHelper.class);

    private CommandLineHelper() {}

    /**
     * Generate a classpath that can be used to start a Rio service using a service starter
     *
     * @return A classpath that can be used to start a Rio service using a service starter
     */
    public static String generateRioStarterClassPath() {
        StringBuilder builder = new StringBuilder();
        File rioLib = new File(System.getProperty("RIO_HOME"), "lib");
        builder.append(getFiles(rioLib, "rio-start", "groovy-all"));
        File javaHome = new File(System.getProperty("JAVA_HOME", System.getProperty("java.home")));
        File javaLib = new File(javaHome, "lib");
        logger.info("java lib: {}", javaLib.getPath());
        String toolsJar = getFiles(javaLib, "tools");
        if (toolsJar.length() > 0) {
            builder.append(":").append(toolsJar);
        } else {
            File javaHomeParent = javaHome.getParentFile();
            File javaHomeParentLib = new File(javaHomeParent, "lib");
            logger.info("java lib: {}", javaHomeParentLib.getPath());
            toolsJar = getFiles(javaHomeParentLib, "tools");
            if (toolsJar.length() > 0) {
                builder.append(":").append(toolsJar);
            } else {
                logger.error("Unable to find tools.jar");
            }
        }
        File rioLoggingLib = new File(rioLib, "logging");
        builder.append(":").append(getFiles(rioLoggingLib));
        return builder.toString();
    }

    /**
     * Get the path to the java executable based on the "java.home" system property
     *
     * @return The path to the java executable
     */
    public static String getJava() {
        StringBuilder jvmBuilder = new StringBuilder();
        jvmBuilder.append(buildDirectory(System.getProperty("JAVA_HOME", System.getProperty("java.home")), "bin"));
        jvmBuilder.append("java");
        jvmBuilder.append(" ");
        return jvmBuilder.toString();
    }

    /**
     * Get a class-path string
     *
     * @param classPath A {@code String} of class-path items
     *
     * @return A {@code String} that includes -cp followed by the class-path items
     */
    public static String getClassPath(String classPath) {
        StringBuilder cpBuilder = new StringBuilder();
        cpBuilder.append("-cp").append(" ").append(classPath).append(" ");
        return cpBuilder.toString();
    }

    /**
     * Get the path of the starter for service-bean-exec
     *
     * @param rioHome The location of Rio
     *
     * @return The canonical path of the starter for service-bean-exec
     *
     * @throws IOException if the canonical path cannot be returned
     */
    public static String getStarterConfig(final String rioHome) throws IOException {
        String startConfig = System.getProperty(Constants.START_SERVICE_BEAN_EXEC_CONFIG);
        if(startConfig!=null)
            return startConfig;
        StringBuilder configBuilder = new StringBuilder();
        configBuilder.append(rioHome).append(File.separator).append("config").append(File.separator);
        configBuilder.append("start-service-bean-exec.groovy");
        File f = new File(configBuilder.toString());
        if(!f.exists())
            throw new IOException(configBuilder.toString()+" does not exist, unable to fork service");
        return f.getCanonicalPath();
    }

    /**
     * Get the standard Rio JVM arguments
     *
     * @return The JVM arguments for java.rmi.server.useCodebaseOnly, java.protocol.handler.pkgs, java.security.policy
     * and RIO_HOME
     */
    static String getStandardJVMArgs() {
        String rioHome = System.getProperty("RIO_HOME");
        StringBuilder argsBuilder = new StringBuilder();

        argsBuilder.append(getOption("java.protocol.handler.pkgs", "org.rioproject.url"));
        argsBuilder.append(" ");
        argsBuilder.append(getOption("java.rmi.server.useCodebaseOnly", "false"));
        argsBuilder.append(" ");
        String policyDir = buildDirectory(rioHome, "policy");
        argsBuilder.append(getOption("java.security.policy", policyDir+"policy.all"));
        argsBuilder.append(" ");
        argsBuilder.append(getOption("RIO_HOME", rioHome));
        argsBuilder.append(" ");

        return argsBuilder.toString();
    }

    /**
     * Create the options to pass to the JVM for starting a service bean in it's own JVM.
     *
     * @param normalizedServiceName The service name with no illegal characters
     * @param serviceBindName The name the service will use to bind to the RMI Registry
     * @param sRegPort the RMI Registry port (as a string) to bind to
     * @param declaredJVMOptions Service specific JVM options
     * @param logDir The directory for service logging
     *
     * @return A String suitable for to pass to the JVM for starting a service bean in it's own JVM.
     */
    public static String createInputArgs(final String normalizedServiceName,
                                  final String serviceBindName,
                                  final String sRegPort,
                                  final String declaredJVMOptions,
                                  final String logDir) {
        StringBuilder extendedJVMOptions = new StringBuilder();
        extendedJVMOptions.append(getStandardJVMArgs());
        if(declaredJVMOptions!=null)
            extendedJVMOptions.append(declaredJVMOptions);
        extendedJVMOptions.append(" ");
        extendedJVMOptions.append(getOption("RIO_LOG_DIR", logDir));
        extendedJVMOptions.append(" ");
        extendedJVMOptions.append(getOption("org.rioproject.service", normalizedServiceName));
        extendedJVMOptions.append(" ");
        extendedJVMOptions.append("-XX:HeapDumpPath=").append(logDir);
        StringBuilder argsBuilder = new StringBuilder();
        String jvmInputArgs = JVMOptionChecker.getJVMInputArgs(extendedJVMOptions.toString());
        String rioHome = buildDirectory(System.getProperty("RIO_HOME"));

        /* Check logging configuration */
        argsBuilder.append(getLoggerConfig(jvmInputArgs, rioHome));

        argsBuilder.append(jvmInputArgs);
        if(OperatingSystemType.isWindows()) {
            argsBuilder.append("-XX:OnOutOfMemoryError=\"taskkill /F /PID %%p\"");
        } else {
            argsBuilder.append("-XX:OnOutOfMemoryError=\"kill -9 %p\"");
        }
        argsBuilder.append(" ");
        argsBuilder.append(getOption(Constants.REGISTRY_PORT, sRegPort));
        argsBuilder.append(" ");
        argsBuilder.append(getOption(Constants.SERVICE_BEAN_EXEC_NAME, serviceBindName));
        argsBuilder.append(" ");
        argsBuilder.append(getOption(Constants.PROCESS_ID, VirtualMachineHelper.getID()));
        argsBuilder.append(" ");
        logger.trace("Resulting JVM Options for service [{}]: {}", serviceBindName, jvmInputArgs);
        return argsBuilder.toString();
    }

    /**
     * Get the starter class
     *
     * @return The classname used as the service starter
     */
    public static String getStarterClass() {
        return "org.rioproject.start.ServiceStarter";
    }

    private static String getLoggerConfig(String inputArgs, String rioHome) {
        StringBuilder logger = new StringBuilder();
        if(usingLogback()) {
            /* Check if logback is being used, if so set logback configuration */
            if(!inputArgs.contains("logback.configurationFile")) {
                logger.append(getOption("logback.configurationFile",
                                             buildDirectory(rioHome, "config", "logging")+"logback.groovy"));
                logger.append(" ");
            }
        } else {
            if(!inputArgs.contains("java.util.logging.config.file")) {
                logger.append(getOption("java.util.logging.config.file",
                                             buildDirectory(rioHome, "config", "logging")+"rio-logging.properties"));
                logger.append(" ");
            }
        }
        return logger.toString();
    }

    private static boolean usingLogback() {
        File rioLib = new File(System.getProperty("RIO_HOME"), "lib");
        File rioLogging = new File(rioLib, "logging");
        String loggingJars = getFiles(rioLogging);
        return loggingJars.contains("logback");
    }

    private static String getOption(final String option, final String value) {
        StringBuilder optionBuilder = new StringBuilder();
        optionBuilder.append("-D").append(option).append("=").append(value);
        return optionBuilder.toString();
    }

    private static String buildDirectory(String... dirs) {
        StringBuilder dirBuilder = new StringBuilder();
        for(String dir : dirs) {
            if(dirBuilder.length()>0)
                dirBuilder.append(File.separator);
            dirBuilder.append(dir);
        }
        dirBuilder.append(File.separator);
        return dirBuilder.toString();
    }

    private static String getFiles(File dir, String... names) {
        StringBuilder builder = new StringBuilder();
        File[] files = dir.listFiles();
        if (files != null) {
            if (names.length > 0) {
                for (File file : files) {
                    for (String name : names) {
                        if (file.getName().startsWith(name)) {
                            append(file.getPath(), builder);
                        }
                    }
                }
            } else {
                for (File file : files) {
                    if (!file.isDirectory())
                        append(file.getPath(), builder);
                }
            }
        }
        return builder.toString();
    }

    private static void append(String s, StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append(":");
        }
        builder.append(s);
    }
}
