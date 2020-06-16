package org.rioproject.impl.fdh;

import net.jini.admin.Administrable;
import net.jini.core.lookup.ServiceID;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.junit.Assert;
import org.junit.Test;
import org.rioproject.impl.opstring.OpStringLoader;
import org.rioproject.opstring.OperationalString;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dennis Reedy
 */
public class PooledFaultDetectionHandlerTest {
    @Test
    public void testPooledFDH() throws Exception {
        PooledFaultDetectionHandler fdh = new PooledFaultDetectionHandler();
        fdh.configure(getConfigurationFromOpstring());
        fdh.register((service, serviceID) -> System.out.println("service: " + ((PhonyBologna)service).getId() +
                                                                " failed, remaining: "+fdh.getServiceCount()));
        sendABunch(0, 100, fdh);
        int count = 0;
        while(fdh.getServiceCount()>0) {
            Thread.sleep(500);
            count++;
            if(count==20) {
                System.out.println("***************************\n  count==10 \n***************************");
                sendABunch(100, 150, fdh);
            }
        }
    }

    private Properties getConfigurationFromOpstring() throws Exception {
        OpStringLoader opStringLoader = new OpStringLoader();
        opStringLoader.setDefaultGroups("banjo");
        String baseDir = System.getProperty("user.dir");
        File fdhOpStringFile = new File(baseDir, "src/test/resources/opstrings/fdh.groovy");
        OperationalString[] opStrings = opStringLoader.parseOperationalString(fdhOpStringFile);
        Assert.assertNotNull(opStrings);
        Assert.assertEquals("Should have only 1 opstring", 1, opStrings.length);
        Assert.assertEquals("Should have 1 service", 1, opStrings[0].getServices().length);
        Assert.assertEquals(1, opStrings[0].getServices()[0].getServiceBeanConfig().getGroups().length);
        return opStrings[0].getServices()[0].getServiceBeanConfig().getFDHProperties();
    }

    private void sendABunch(int startAt, int count, PooledFaultDetectionHandler fdh) throws Exception {
        System.out.println("sending a bunch, "+startAt+", "+count);
        for(int i=startAt; i<count; i++) {
            Uuid uuid = UuidFactory.generate();
            fdh.monitor(new PhonyBolognaImpl(i), new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
        }
    }

    interface PhonyBologna extends Administrable {
        int getId();
    }

    static class PhonyBolognaImpl implements PhonyBologna {
        final int id;
        final int onCount;
        final AtomicInteger count = new AtomicInteger(0);

        PhonyBolognaImpl(int id) {
            this.id = id;
            Random r = new Random();
            onCount = r.nextInt(10);
        }

        @Override public int getId() {
            return id;
        }

        @Override public Object getAdmin() throws RemoteException {
            System.out.println(String.format("service: %-4s invoked, count: %s, fail on: %s", id, count.get(), onCount));
            if(count.getAndIncrement()==onCount) {
                throw new RemoteException("jk");
            }
            return new Object();
        }
    }

}