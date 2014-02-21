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
package org.rioproject.monitor.service;

import net.jini.config.Configuration;
import net.jini.core.event.UnknownEventException;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.junit.*;
import org.rioproject.deploy.*;
import org.rioproject.impl.config.DynamicConfiguration;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.impl.system.SystemCapabilities;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.sla.ServiceLevelAgreements;
import org.rioproject.system.ComputeResourceUtilization;
import org.rioproject.system.MeasuredResource;
import org.rioproject.system.ResourceCapability;
import org.rioproject.system.capability.PlatformCapability;
import org.rioproject.system.capability.platform.OperatingSystem;
import org.rioproject.system.capability.platform.ProcessorArchitecture;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.security.*;
import java.util.*;

/**
 * @author Dennis Reedy
 */
public class InstantiatorResourceTest {
    InstantiatorResource instantiatorResource;
    DefaultOpStringManager manager;
    ServiceElement service;
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
        for(String part : parts) {
            if(part.contains("resolver-aether")) {
                resolverJar = part;
                break;
            }
        }
        junit.framework.Assert.assertNotNull(resolverJar);
        System.setProperty(ResolverHelper.RESOLVER_JAR, resolverJar);
    }

    @Before
    public void setup() throws IOException {
        SBI sbi = new SBI();
        List<MeasuredResource> measuredResources = new ArrayList<MeasuredResource>();
        ComputeResourceUtilization resourceUtilization = new ComputeResourceUtilization("test",
                                                                                        sbi.inetAddress
                                                                                            .getHostAddress(),
                                                                                        sbi.inetAddress.getHostName(),
                                                                                        measuredResources);
        SystemCapabilities systemCapabilities = new SystemCapabilities();
        PlatformCapability[] platformCapabilities = systemCapabilities.getPlatformCapabilities(new DynamicConfiguration());
        ResourceCapability resourceCapability = new ResourceCapability(sbi.inetAddress.getHostAddress(),
                                                                       sbi.inetAddress.getHostName(),
                                                                       true,
                                                                       platformCapabilities,
                                                                       resourceUtilization);
        instantiatorResource = new InstantiatorResource(/*new MarshalledObject<ServiceBeanInstantiator>(sbi)*/null,
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
                                             active,
                                             config,
                                             opStringManagerController);

    }


    @Test
    public void testMeetsGeneralRequirements() throws Exception {
        ProvisionRequest request = createProvisionRequest();
        Assert.assertTrue(instantiatorResource.meetsGeneralRequirements(request));
    }

    @Test
    public void testMeetsQualitativeRequirements() throws Exception {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createServiceLevelAgreements(true, true));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        Assert.assertEquals(0, notSupported.size());
    }

    @Test
    public void testDoesNotMeetArchitectureRequirements() throws Exception {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createServiceLevelAgreements(false, true));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        Assert.assertTrue(notSupported.size() > 0);
    }

    @Test
    public void testDoesNotMeetOperatingSystemRequirements() throws Exception {
        ProvisionRequest request = createProvisionRequest();
        request.getServiceElement().setServiceLevelAgreements(createServiceLevelAgreements(true, false));
        Collection<SystemComponent> notSupported = instantiatorResource.meetsQualitativeRequirements(request);
        Assert.assertTrue(notSupported.size()>0);
    }

    @Test
    public void testMeetsQuantitativeRequirements() throws Exception {
        ProvisionRequest request = createProvisionRequest();
        Assert.assertTrue(instantiatorResource.meetsQuantitativeRequirements(request));
    }

    ServiceElement createServiceElement() {
        ServiceElement serviceElement = TestUtil.makeServiceElement("foo", "test", 1);
        serviceElement.setServiceLevelAgreements(createServiceLevelAgreements(true, true));
        return serviceElement;
    }

    ServiceLevelAgreements createServiceLevelAgreements(boolean matchArchitecture, boolean matchOpSys) {
        String[] architectures;
        if(matchArchitecture) {
            architectures = new String[]{"x86", "x86_64"};
        } else {
            architectures = new String[]{"sparc", "amd"};
        }
        String[] operatingSystems;
        if(matchOpSys) {
            operatingSystems = new String[]{"Mac OS X", "Windows", "Linux"};
        } else {
            operatingSystems = new String[]{"ES/390", "Ubuntu"};
        }
        ServiceLevelAgreements slas = new ServiceLevelAgreements();
        SystemRequirements systemRequirements = new SystemRequirements();
        for (String architecture : architectures) {
            Map<String, Object> attributeMap = new HashMap<String, Object>();
            attributeMap.put(ProcessorArchitecture.NAME, "Processor");
            attributeMap.put(ProcessorArchitecture.ARCHITECTURE, architecture);
            SystemComponent systemComponent = new SystemComponent("Processor",
                                                                  ProcessorArchitecture.class.getName(),
                                                                  attributeMap);
            systemRequirements.addSystemComponent(systemComponent);
        }
        for (String opSys : operatingSystems) {
            Map<String, Object> attributeMap = new HashMap<String, Object>();
            attributeMap.put(OperatingSystem.NAME, opSys);
            SystemComponent operatingSystem =
                new SystemComponent("OperatingSystem", OperatingSystem.class.getName(), attributeMap);
            systemRequirements.addSystemComponent(operatingSystem);
        }
        slas.setServiceRequirements(systemRequirements);
        return slas;
    }

    ProvisionRequest createProvisionRequest() {
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
                                    new InstanceIDManager() {
                                        @Override
                                        public long getNextInstanceID() {
                                            return 0;
                                        }
                                    }
        );
    }

    class SBI implements ServiceBeanInstantiator, Serializable {
        final Uuid uuid = UuidFactory.generate();
        final InetAddress inetAddress;

        SBI() throws UnknownHostException {
            inetAddress = InetAddress.getLocalHost();
        }

        public DeployedService instantiate(ServiceProvisionEvent event) throws ServiceBeanInstantiationException,
                                                                               UnknownEventException {
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

        public String getName() throws RemoteException {
            return "test";
        }

        public Uuid getInstantiatorUuid() throws RemoteException {
            return uuid;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }
    }
}
