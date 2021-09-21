package org.rioproject.cybernode.service;

import net.jini.admin.Administrable;
import net.jini.core.event.EventRegistration;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.lookup.ServiceDiscoveryEvent;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.config.DynamicConfiguration;
import org.rioproject.deploy.ProvisionManager;
import org.rioproject.deploy.ServiceBeanInstantiator;
import org.rioproject.impl.system.ComputeResource;
import org.rioproject.impl.watch.WatchRegistry;
import org.rioproject.opstring.ServiceBeanConfig;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.servicebean.ComputeResourceManager;
import org.rioproject.servicebean.ServiceBeanContext;
import org.rioproject.servicebean.ServiceBeanManager;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProvisionManagerHandlerTest {
    private ProvisionManagerHandler provisionManagerHandler;

    @Before
    public void init() throws Exception {
        ServiceBeanInstantiator serviceBeanInstantiator = mock(ServiceBeanInstantiator.class,
                                                               withSettings().serializable());
        CybernodeImpl cybernode = new CybernodeImpl();

        ServiceBeanContext serviceBeanContext = mock(ServiceBeanContext.class);
        when(serviceBeanContext.getConfiguration()).thenReturn(new DynamicConfiguration());

        ServiceBeanManager serviceBeanManager = mock(ServiceBeanManager.class);
        when(serviceBeanContext.getServiceBeanManager()).thenReturn(serviceBeanManager);

        when(serviceBeanManager.getServiceID()).thenReturn(UuidFactory.generate());
        WatchRegistry watchRegistry = mock(WatchRegistry.class);
        when(serviceBeanContext.getWatchRegistry()).thenReturn(watchRegistry);

        ComputeResourceManager computeResourceManager = mock(ComputeResourceManager.class);
        when(computeResourceManager.getComputeResource()).thenReturn(new ComputeResource());
        when(serviceBeanContext.getComputeResourceManager()).thenReturn(computeResourceManager);
        ServiceBeanConfig config = new ServiceBeanConfig();
        config.setName("test");
        config.setConfigurationParameters(new HashMap<>());
        when(serviceBeanContext.getServiceBeanConfig()).thenReturn(config);

        ServiceElement serviceElement = new ServiceElement();
        serviceElement.setServiceBeanConfig(config);
        when(serviceBeanContext.getServiceElement()).thenReturn(serviceElement);
        cybernode.initialize(serviceBeanContext);
        provisionManagerHandler = new ProvisionManagerHandler(new CybernodeAdapter(serviceBeanInstantiator,
                                                                                   cybernode),
                                                              100,
                                                              new DynamicConfiguration());

    }
    @Test
    public void testProvisionManagerMultiDiscovery() {
        ServiceItem postItem = createProvisionManagerServiceItem();
        ServiceDiscoveryEvent discoveryEvent = new ServiceDiscoveryEvent(this, null, postItem);
        provisionManagerHandler.serviceAdded(discoveryEvent);
        assertEquals(1, provisionManagerHandler.getLeaseTable().size());
        assertEquals(1, provisionManagerHandler.getProvisionerMap().size());

        provisionManagerHandler.serviceAdded(discoveryEvent);

        assertEquals(1, provisionManagerHandler.getLeaseTable().size());
        assertEquals(1, provisionManagerHandler.getProvisionerMap().size());
    }

    @Test
    public void testProvisionManagerAddAndRemove() {
        ServiceItem item = createProvisionManagerServiceItem();
        ServiceDiscoveryEvent discoveredEvent = new ServiceDiscoveryEvent(this, null, item);
        provisionManagerHandler.serviceAdded(discoveredEvent);
        assertEquals(1, provisionManagerHandler.getLeaseTable().size());
        assertEquals(1, provisionManagerHandler.getProvisionerMap().size());

        ServiceDiscoveryEvent removedEvent = new ServiceDiscoveryEvent(this, item, null);
        provisionManagerHandler.serviceRemoved(removedEvent);

        assertEquals(0, provisionManagerHandler.getLeaseTable().size());
        assertEquals(0, provisionManagerHandler.getProvisionerMap().size());
    }

    private ServiceItem createProvisionManagerServiceItem() {
        Uuid uuid = UuidFactory.generate();
        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        ProvisionManager provisionManager =
                (ProvisionManager) Proxy.newProxyInstance(ProvisionManager.class.getClassLoader(),
                               new Class[]{ProvisionManager.class, Administrable.class},
                               new IH());
        return new ServiceItem(serviceID, provisionManager,null);
    }

    private static class IH implements InvocationHandler,
                                       Serializable {

        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args) throws RemoteException {
            if (method.getName().equals("register")) {
                Lease lease = mock(Lease.class);
                when(lease.getExpiration()).thenReturn(System.currentTimeMillis() + 5000);
                return new EventRegistration(1,
                                             null,
                                             lease,
                                             1);

            }
            if (method.getName().equals("getAdmin")) {
                throw new RemoteException();
            }
            return null;
        }
    }
}