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
package org.rioproject.test;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.rioproject.config.Constants;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Configuration for a test case.
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class TestConfig {
    public enum LoggingSystem { JUL, LOGBACK }
    private String groups;
    private String locators;
    private Integer numCybernodes;
    private Integer numMonitors;
    private Integer numLookups;
    private String opString;
    private boolean autoDeploy;
    private TestManager testManager;
    private String testClassName;
    private String component;
    private boolean runHarvester;
    private long timeout;
    private LoggingSystem loggingSystem;

    TestConfig(String testClassName) {
        this.testClassName = testClassName;
        component = testClassName;
        int ndx = component.lastIndexOf(".");
        if (ndx > -1)
            component = component.substring(ndx + 1);
    }

    @SuppressWarnings ("unchecked")
    void loadConfig(String location) {
        URL url = null;
        boolean throwException = false;
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            File testConfigFile = new File(location);
            if (!testConfigFile.exists())
                throwException = true;
            else {
                try {
                    url = testConfigFile.toURI().toURL();
                } catch (MalformedURLException e1) {
                    throwException = true;
                }
            }
        }

        if (throwException) {
            throw new RuntimeException("Cannot load [" + location + "], it is not found or your location of the file " +
                                       "is incorrect. Yu have declared that your test [" + testClassName +"] requires " +
                                       "a configuration file, but the file cannot be loaded. Check the setting of the " +
                                       "org.rioproject.test.config system property");
        }
        ConfigObject config = new ConfigSlurper().parse(url);
        Map<String, Object> configMap = config.flatten();

        if (hasConfigurationFor(component, configMap)) {
            groups = getString(configMap.get(component + ".groups"));
            if (groups != null)
                System.setProperty(Constants.GROUPS_PROPERTY_NAME, groups);
            locators = getString(configMap.get(component + ".locators"));
            if (locators != null)
                System.setProperty(Constants.LOCATOR_PROPERTY_NAME, locators);
            numCybernodes = (Integer) configMap.get(component + ".numCybernodes");
            numMonitors = (Integer) configMap.get(component + ".numMonitors");
            numLookups = (Integer) configMap.get(component + ".numLookups");
            opString = getString(configMap.get(component + ".opstring"));
            Boolean b = (Boolean) configMap.get(component + ".autoDeploy");
            autoDeploy = b != null && b;
            testManager = (TestManager) configMap.get(component + ".testManager");
            b = (Boolean) configMap.get(component + ".harvest");
            runHarvester = b != null && b;
            String sTimeout = getString(configMap.get(component + ".timeout"));
            timeout = sTimeout==null?0:Long.parseLong(sTimeout);
            loggingSystem = (LoggingSystem) configMap.get(component + ".loggingSystem");
            if(loggingSystem==null) {
                loggingSystem = LoggingSystem.LOGBACK;
            }
        }
    }

    public String getGroups() {
        return groups;
    }

    public String getLocators() {
        return locators;
    }

    public Integer getNumCybernodes() {
        return numCybernodes == null ? 0 : numCybernodes;
    }

    public Integer getNumMonitors() {
        return numMonitors == null ? 0 : numMonitors;
    }

    public Integer getNumLookups() {
        return numLookups == null ? 0 : numLookups;
    }

    public String getOpString() {
        return opString;
    }

    public boolean autoDeploy() {
        return autoDeploy;
    }

    public TestManager getTestManager() {
        if (testManager == null)
            testManager = new TestManager(true);
        return testManager;
    }

    public boolean runHarvester() {
        return runHarvester;
    }

    public String getComponent() {
        return component;
    }

    public Long getTimeout() {
        return timeout;
    }

    public LoggingSystem getLoggingSystem() {
        return loggingSystem;
    }

    private boolean hasConfigurationFor(final String component, final Map<String, Object> map) {
        boolean hasConfig = false;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            int ndx = key.indexOf(".");
            if (ndx > 0)
                key = key.substring(0, ndx);
            if (key.equals(component)) {
                hasConfig = true;
                break;
            }
        }
        return hasConfig;
    }


    private String getString(Object o) {
        return o == null ? null : o.toString();
    }

}
