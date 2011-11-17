package org.rioproject.monitor;

import com.sun.jini.config.Config;
import net.jini.config.Configuration;
import org.rioproject.event.DispatchEventHandler;
import org.rioproject.event.EventHandler;
import org.rioproject.monitor.tasks.ProvisionMonitorEventTask;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends {@link ProvisionMonitorEvent}s
 */
public class ProvisionMonitorEventProcessor {
    /**
     * ThreadPool for sending ProvisionMonitorEvent notifications
     */
    private Executor monitorEventPool;
    /** Component name we use to find items in the configuration */
    static final String CONFIG_COMPONENT = "org.rioproject.monitor";
    static Logger logger = Logger.getLogger(ProvisionMonitorEventProcessor.class.getName());
    private EventHandler monitorEventHandler;

    public ProvisionMonitorEventProcessor(Configuration config) throws Exception {
        /*
         * Set up the pool for ProvisionMonitorEvent notifications
         */
        int provisionMonitorEventTaskPoolMaximum = 10;
        try {
            provisionMonitorEventTaskPoolMaximum =
                Config.getIntEntry(config,
                                   CONFIG_COMPONENT,
                                   "provisionMonitorEventTaskPoolMaximum",
                                   provisionMonitorEventTaskPoolMaximum,
                                   1,
                                   100);
        } catch (Throwable t) {
            logger.log(Level.WARNING,
                       "Exception getting provisionMonitorEventTaskPoolMaximum, use default of 10",
                       t);
        }
        monitorEventPool = Executors.newFixedThreadPool(provisionMonitorEventTaskPoolMaximum);
        monitorEventHandler = new DispatchEventHandler(ProvisionMonitorEvent.getEventDescriptor(), config);
    }

    public EventHandler getMonitorEventHandler() {
        return monitorEventHandler;
    }

    /**
     * Sends a ProvisionMonitorEvent using a thread obtained from a thread pool
     *
     * @param event The ProvisionMonitorEvent to send
     */
    public void processEvent(ProvisionMonitorEvent event) {
        monitorEventPool.execute(new ProvisionMonitorEventTask(monitorEventHandler, event));
    }
}
