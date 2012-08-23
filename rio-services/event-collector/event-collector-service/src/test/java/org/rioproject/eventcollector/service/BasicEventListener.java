package org.rioproject.eventcollector.service;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dennis Reedy
 */
public class BasicEventListener implements RemoteEventListener {
    private final List<RemoteEvent> events = new ArrayList<RemoteEvent>();

    RemoteEventListener export() throws ExportException {
        Exporter exporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                                  new BasicILFactory(),
                                                  false,
                                                  true);
        return (RemoteEventListener) exporter.export(this);
    }

    int eventCollectionCount() {
        return events.size();
    }

    public String printEventCollection() {
        StringBuilder builder = new StringBuilder();
        for(RemoteEvent event : events) {
            if(builder.length()>0)
                builder.append("\n");
            builder.append(event.toString());
        }
        return builder.toString();
    }

    public void notify(RemoteEvent remoteEvent) throws UnknownEventException, RemoteException {
        events.add(remoteEvent);
    }
}