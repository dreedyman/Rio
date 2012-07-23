package org.rioproject.tools.ui.servicenotification;

import org.rioproject.log.ServiceLogEvent;
import org.rioproject.tools.ui.Constants;
import org.rioproject.tools.ui.treetable.RemoteServiceEventNode;

import java.util.logging.LogRecord;

/**
 * @author Dennis Reedy
 */
public class ServiceLogEventNode extends RemoteServiceEventNode<ServiceLogEvent> {

    public ServiceLogEventNode(ServiceLogEvent event) {
        super(event);
    }

    @Override
    public Throwable getThrown() {
        return getEvent().getLogRecord().getThrown();
    }

    @Override
    public String getDescription() {
        LogRecord logRecord = getEvent().getLogRecord();
        return logRecord.getLevel().toString() + ": " + logRecord.getMessage();
    }

    @Override
    public String getOperationalStringName() {
        return getEvent().getOpStringName();
    }

    @Override
    public String getServiceName() {
        return getEvent().getServiceName() == null ? "" : getEvent().getServiceName();
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
            StringBuilder sb = new StringBuilder();
            sb.append(getDescription());
            sb.append(". ");
            Throwable t = getThrown();
            if (t != null) {
                if (t.getCause() != null) {
                    t = t.getCause();
                }
                sb.append(t).append(" Exception raised.");
            }
            value = sb.toString();
        } else {
            value = Constants.DATE_FORMAT.format(getDate());
        }
        return value;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
