package org.rioproject.tools.ui.servicenotification;

import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import org.rioproject.event.RemoteServiceEvent;
import org.rioproject.event.RemoteServiceEventListener;
import org.rioproject.ui.Util;

/**
 * @author Dennis Reedy
 */
public class RemoteEventConsumer implements RemoteEventListener, RemoteServiceEventListener {
    private final RemoteEventTable remoteEventTable;

    public RemoteEventConsumer(RemoteEventTable remoteEventTable) {
        this.remoteEventTable = remoteEventTable;
    }

    public void notify(RemoteEvent event) {
        try {
            remoteEventTable.getDataModel().addItem((RemoteServiceEvent) event);
            remoteEventTable.notifyListeners();
            remoteEventTable.expandAll();
        } catch (Throwable t) {
            Util.showError(t, remoteEventTable, String.format("Notification of a %s", event.getClass().getName()));
        }
    }

    @Override
    public void notify(RemoteServiceEvent event) {
        notify((RemoteEvent)event);
    }
}
