package org.rioproject.eventcollector.service;

import com.sun.jini.landlord.LeasedResource;
import net.jini.security.ProxyPreparer;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Dennis Reedy
 */
public class RegistrationManager {
    private final File persistentRegistrationDirectory;
    private static final Logger logger = Logger.getLogger(RegistrationManager.class.getName());

    public RegistrationManager(EventCollectorContext context) {
        persistentRegistrationDirectory = new File(context.getPersistentDirectoryRoot(), "registrations");
        if(!persistentRegistrationDirectory.exists()) {
            if(persistentRegistrationDirectory.mkdirs() && logger.isLoggable(Level.INFO)) {
                logger.fine(String.format("Created %s", persistentRegistrationDirectory.getPath()));
            }
        }
        String[] dirListing = persistentRegistrationDirectory.list();
        int numListed = dirListing==null?0:dirListing.length;
        logger.info(String.format("Persistent registration directory: %s, have %d persisted ",
                                  persistentRegistrationDirectory.getPath(),
                                  numListed));
    }

    public void persist(RegisteredNotification registeredNotification) {
        File file = new File(persistentRegistrationDirectory,
                             getRegisteredNotificationFileName(registeredNotification));

        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(registeredNotification);
            if(logger.isLoggable(Level.FINE))
                logger.fine(String.format("Wrote %d bytes to %s", file.length(), file.getPath()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write to disk", e);
        } finally {
            if(outputStream!=null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Trying to close OOS", e);
                }
            }
        }
    }

    public List<RegisteredNotification> getRegisteredNotifications(ProxyPreparer listenerPreparer) {
        List<RegisteredNotification> registeredNotifications = new LinkedList<RegisteredNotification>();
        File[] files = persistentRegistrationDirectory.listFiles();
        if(files==null) {
            logger.warning(String.format("%s returned null file array", persistentRegistrationDirectory.getPath()));
            return registeredNotifications;
        }
        for(File file : files) {
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(file));
                RegisteredNotification registeredNotification = (RegisteredNotification)inputStream.readObject();
                if(ensure(registeredNotification)) {
                    registeredNotification.restore(listenerPreparer);
                    registeredNotifications.add(registeredNotification);
                } else {
                    if(file.delete()) {
                        logger.fine("Removed expired registration");
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                           String.format("Could not read serialized event [%s] from disk", file.getPath()),
                           e);
            } finally {
                if(inputStream!=null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Trying to close OIS", e);
                    }
                }
            }
        }
        return registeredNotifications;
    }

    public void remove(RegisteredNotification registeredNotification) {
        File file = new File(persistentRegistrationDirectory,
                             getRegisteredNotificationFileName(registeredNotification));
        if(file.exists()) {
            if(file.delete() && logger.isLoggable(Level.FINE)) {
                logger.fine("Removed registration.");
            }
        }
    }

    public void update(RegisteredNotification registeredNotification) {
        File file = new File(persistentRegistrationDirectory,
                             getRegisteredNotificationFileName(registeredNotification));
        if(file.exists()) {
            remove(registeredNotification);
        }
        persist(registeredNotification);
    }

    private String getRegisteredNotificationFileName(RegisteredNotification registeredNotification) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(registeredNotification.getCookie());
        nameBuilder.append(".reg");
        return nameBuilder.toString();
    }

    private boolean ensure(final LeasedResource resource) {
        return(resource.getExpiration() > System.currentTimeMillis());
    }
}
