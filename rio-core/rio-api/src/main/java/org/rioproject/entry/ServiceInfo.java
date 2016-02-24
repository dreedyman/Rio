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
package org.rioproject.entry;

import net.jini.entry.AbstractEntry;
import org.rioproject.RioVersion;
import org.rioproject.config.Constants;

/**
 * @author Dennis Reedy
 */
public class ServiceInfo extends AbstractEntry {
    private static final long serialVersionUID = 1L;
    public String user;
    public String codeServer;
    public String rioHome;
    public String serviceName;
    public String serviceLog;
    public String rioVersion;
    public String version;

    public void initialize(String name, String version) {
        user = System.getProperty("user.name");
        rioHome = System.getProperty("rio.home");
        rioVersion = RioVersion.VERSION;
        this.version = version;
        codeServer = System.getProperty(Constants.WEBSTER);
        serviceName = name;
        serviceLog = String.format("%s/%s.log",
                                         System.getProperty("rio.log.dir"),
                                         System.getProperty("org.rioproject.service"));
    }
}
