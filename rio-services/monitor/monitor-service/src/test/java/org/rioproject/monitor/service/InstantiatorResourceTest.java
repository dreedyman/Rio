/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.monitor.service;

import net.jini.config.Configuration;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.deploy.*;
import org.rioproject.config.DynamicConfiguration;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.impl.system.SystemCapabilities;
import org.rioproject.net.HostUtil;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.ResolverConfiguration;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.connectivity.TCPConnectivity;
import org.rioproject.system.capability.platform.OperatingSystem;
import org.rioproject.system.capability.platform.ProcessorArchitecture;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Reedy
 */
public class InstantiatorResourceTest {
    private InstantiatorResource instantiatorResource;
    private DefaultOpStringManager manager;
    private ServiceElement service;

    static {
        System.setProperty("StaticCybernode", "true");
        Policy.setPolicy(new Policy() {
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return (perms);
            }

            public void refresh() {
            }

        });
        System.setSecurityManager(new SecurityManager());
    }

    @BeforeClass
    public static void setResolverJar() {
        String classPath = System.getProperty("java.class.path");
        String[] parts = classPath.split(File.pathSeparator);
        String resolverJar = null;
        for (String part : parts) {
            if (part.contains("resolver-aether")) {
                resolverJar = part;
                break;
            }
        }
        org.junit.Assert.assertNotNull(resolverJar);
        System.setProperty(ResolverConfiguration.RESOLVER_JAR, resolverJar);
    }

    @Before
    public void setup() throws IOException {
        SBI sbi = new SBI();
        List<MeasuredResource> measuredResources = new ArrayList<>();
        ComputeResourceUtilization resourceUtilization = new ComputeResourceUtilization("test",
                                                                                        sbi.inetAddress.getHostAddress(),
                                                                                        sbi.inetAddress.getHostName(),
                                                                                        measuredResources);
        SystemCapabilities systemCapabilities = new SystemCapabilities();
        PlatformCapability[] platformCapabilities = systemCapabilities.getPlatformCapabilities(new DynamicConfiguration());
        ResourceCapability resourceCapability = new ResourceCapability(sbi.inetAddress.getHostAddress(),
                                                                       sbi.inetAddress.getHostName(),
                                                                       true,
                                                                       platformCapabilities,
                                                                       resourceUtilization);
        instantiatorResource = new InstantiatorResource(null,
                                                        sbi,
                                                        "test",
                                                        sbi.uuid,
                                                        null,
                                                        resourceCapability,
                                                        50);
        service = createServiceElement();
        OperationalString opString = new OpString("test", null);

        opString.addService(service);
        OpStringManager parent = null;
        boolean active = true;
        Configuration config = new DynamicConfiguration();
        OpStringManagerController opStringManagerController = new OpStringManagerController();
        manager = new DefaultOpStringManager(opString,
                                             parent,
                                             config,
                                             opStringManagerController);
        manager.initialize(active);

    }


    @Test
    public void testMeetsGeneralRequirements() {
        ProvisionRequest request = createProvisionRequest();
        assertTrue(instantiatorResource.meetsGeneralRequirements(request));
    }

    @Test
    public void testMeetsQualitativeRequirements() {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createServiceLevelAgreements(true, true));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        assertEquals(0, notSupported.size());
    }

    @Test
    public void testDoesNotMeetArchitectureRequirements() {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createServiceLevelAgreements(false, true));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        assertTrue(notSupported.size() > 0);
    }

    @Test
    public void testDoesNotMeetOperatingSystemRequirements() {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createServiceLevelAgreements(true, false));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        assertTrue(notSupported.size() > 0);
    }

    @Test
    public void testMeetsQuantitativeRequirements() {
        ProvisionRequest request = createProvisionRequest();
        assertTrue(instantiatorResource.meetsQuantitativeRequirements(request));
    }

    @Test
    public void testMeetsClusterRequirements() throws SocketException {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createClusterServiceLevelAgreements(false,
                                                                                                  HostUtil.getFirstNonLoopbackAddress(
                                                                                                          true,
                                                                                                          false).getHostAddress()));

        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        assertEquals(0, notSupported.size());
    }

    @Test
    public void testMeetsClusterMultipleHostNameRequirements() throws UnknownHostException, SocketException {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createClusterServiceLevelAgreements(false,
                                                                                                  HostUtil.getFirstNonLoopbackAddress(
                                                                                                          true,
                                                                                                          false).getHostAddress(),
                                                                                                  "192.168.1.9"));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        assertEquals(0, notSupported.size());
    }

    @Test
    public void testMeetsExcludeClusterRequirements() throws SocketException {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createClusterServiceLevelAgreements(true,
                                                                                                  HostUtil.getFirstNonLoopbackAddress(
                                                                                                          true,
                                                                                                          false).getHostAddress()));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        assertEquals(1, notSupported.size());
    }

    @Test
    public void testMeetsExcludeMultipleClusterRequirements() throws ProvisionException, SocketException {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement
                ().setServiceLevelAgreements(createClusterServiceLevelAgreements(true,
                                                                                 HostUtil.getFirstNonLoopbackAddress(
                                                                                         true,
                                                                                         false).getHostAddress(),
                                                                                 "192.168.1.9"));
        Assert.assertFalse(instantiatorResource.canProvision(request));
    }

    @Test
    public void testMeetsExcludeWithMatching() throws ProvisionException {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createClusterServiceLevelAgreements(true, "10.0.1.33"));
        assertTrue(instantiatorResource.canProvision(request));
    }

    private ServiceElement createServiceElement() {
        ServiceElement serviceElement = TestUtil.makeServiceElement("foo", "test", 1);
        serviceElement.setServiceLevelAgreements(createServiceLevelAgreements(true, true));
        return serviceElement;
    }

    private ServiceLevelAgreements createServiceLevelAgreements(boolean matchArchitecture, boolean matchOpSys) {
        String[] architectures;
        if (matchArchitecture) {
            architectures = new String[]{"x86", "x86_64"};
        } else {
            architectures = new String[]{"sparc", "amd"};
        }
        String[] operatingSystems;
        if (matchOpSys) {
            operatingSystems = new String[]{"Mac OS X", "Windows", "Linux"};
        } else {
            operatingSystems = new String[]{"ES/390", "Ubuntu"};
        }
        ServiceLevelAgreements slas = new ServiceLevelAgreements();
        SystemRequirements systemRequirements = new SystemRequirements();
        for (String architecture : architectures) {
            SystemComponent systemComponent = new SystemComponent(ProcessorArchitecture.ID,
                                                                  ProcessorArchitecture.class.getName());
            systemComponent.put(ProcessorArchitecture.ARCHITECTURE, architecture);
            systemRequirements.addSystemComponent(systemComponent);
        }
        for (String opSys : operatingSystems) {
            SystemComponent operatingSystem = new SystemComponent(OperatingSystem.ID, OperatingSystem.class.getName());
            operatingSystem.put(OperatingSystem.NAME, opSys);
            systemRequirements.addSystemComponent(operatingSystem);
        }
        slas.setServiceRequirements(systemRequirements);
        return slas;
    }

    private ServiceLevelAgreements createClusterServiceLevelAgreements(boolean exclude,
                                                                       String... addresses) {
        ServiceLevelAgreements slas = new ServiceLevelAgreements();
        SystemRequirements systemRequirements = new SystemRequirements();
        slas.setServiceRequirements(systemRequirements);
        for (String address : addresses) {
            SystemComponent cluster = new SystemComponent(TCPConnectivity.ID);
            cluster.put(TCPConnectivity.HOST_ADDRESS, address);
            cluster.setExclude(exclude);
            systemRequirements.addSystemComponent(cluster);
        }
        return slas;
    }

    private ProvisionRequest createProvisionRequest() {
        return new ProvisionRequest(service,
                                    new ProvisionListener() {
                                        public void serviceProvisioned(ServiceBeanInstance serviceBeanInstance,
                                                                       InstantiatorResource resource) {
                                        }

                                        @Override
                                        public void uninstantiable(ProvisionRequest request) {
                                        }
                                    },
                                    manager,
                () -> 0
        );
    }

    static class SBI implements ServiceBeanInstantiator, Serializable {
        final Uuid uuid = UuidFactory.generate();
        final InetAddress inetAddress;

        SBI() throws UnknownHostException {
            inetAddress = InetAddress.getLocalHost();
        }

        public DeployedService instantiate(ServiceProvisionEvent event) {
            return null;
        }

        public void update(ServiceElement[] sElements, OperationalStringManager opStringMgr) {
        }

        public ServiceStatement[] getServiceStatements() {
            return new ServiceStatement[0];
        }

        public ServiceStatement getServiceStatement(ServiceElement sElem) {
            return null;
        }

        public ServiceRecord[] getServiceRecords(int filter) {
            return new ServiceRecord[0];
        }

        public ServiceBeanInstance[] getServiceBeanInstances(ServiceElement element) {
            return new ServiceBeanInstance[0];
        }

        public String getName() {
            return "test";
        }

        public Uuid getInstantiatorUuid() {
            return uuid;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }
    }
}
