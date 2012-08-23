package org.rioproject.eventcollector.proxy;

import com.sun.jini.landlord.Landlord;
import net.jini.core.event.RemoteEventListener;
import net.jini.id.Uuid;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;

import java.io.IOException;

/**
 * Backend interface for the {@code EventCollector}.
 *
 * @author Dennis Reedy
 */
public interface EventCollectorBackend extends EventCollector, Landlord {
    void enableDelivery(Uuid uuid, RemoteEventListener remoteEventListener) throws UnknownEventCollectorRegistration,
                                                                                   IOException;
    void disableDelivery(Uuid uuid) throws UnknownEventCollectorRegistration, IOException;
}
