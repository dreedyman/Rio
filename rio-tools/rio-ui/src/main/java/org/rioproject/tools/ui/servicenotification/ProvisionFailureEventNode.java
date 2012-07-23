package org.rioproject.tools.ui.servicenotification;

import org.rioproject.monitor.ProvisionFailureEvent;
import org.rioproject.tools.ui.Constants;
import org.rioproject.tools.ui.treetable.RemoteServiceEventNode;

/**
 * @author Dennis Reedy
 */
public class ProvisionFailureEventNode extends RemoteServiceEventNode<ProvisionFailureEvent> {
    private String status;

    public ProvisionFailureEventNode(ProvisionFailureEvent event) {
        super(event);
        status = "Pending";
    }

    @Override
    public Throwable getThrown() {
        return getEvent().getThrowable();
    }

    @Override
    public String getDescription() {
        return getEvent().getReason();
    }

    @Override
    public String getOperationalStringName() {
        return getEvent().getServiceElement().getOperationalStringName();
    }

    @Override
    public String getServiceName() {
        return getEvent().getServiceElement().getName();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getServiceName();
    }

    @Override
    public Object getValueAt(int column) {
        String value;
        if (column == 0) {
            value = getServiceName();
        } else if (column == 1) {
            value = getDescription();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }
}
