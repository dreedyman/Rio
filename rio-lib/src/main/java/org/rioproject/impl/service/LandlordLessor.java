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
package org.rioproject.impl.service;

import com.sun.jini.config.Config;
import com.sun.jini.landlord.*;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.export.Exporter;
import net.jini.id.ReferentUuid;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.impl.config.ExporterConfig;
import org.rioproject.impl.util.TimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * The LandlordLessor manages leased resources using the Landlord protocol.
 * 
 * <p>
 * The LandlordLessor supports the following configuration entries; where each
 * configuration entry name is associated with the component name <span *=""
 * style="font-family: monospace;">org.rioproject.resources.servicecore</span>
 * <br>
 * </p>
 * <ul>
 * <li><span style="font-weight: bold;">landlordExporter </span> <table
 * cellpadding="2" *="" cellspacing="2" border="0" style="text-align: left;
 * width: 100%;"> <tbody>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Type:</td>
 * <td style="vertical-align: top;">{@link net.jini.export.Exporter}</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Default:</td>
 * <td style="vertical-align: top;">A new {@link net.jini.jeri.BasicJeriExporter} with
 * <ul>
 * <li>a {@link net.jini.jeri.tcp.TcpServerEndpoint} created on a random port,
 * </li>
 * <li>a {@link net.jini.jeri.BasicILFactory}, </li>
 * <li>distributed garbage collection turned off, </li>
 * <li>keep alive on. </li>
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Description:</td>
 * <td style="vertical-align: top;">Specifies the Exporter to use to export
 * this service. This entry is obtained at service start and restart.</td>
 * </tr>
 * </tbody> </table></li>
 * </ul>
 *
 *
 *
 * <ul>
 * <li><span style="font-weight: bold;">reapingInterval</span> <table
 * cellpadding="2" *="" cellspacing="2" border="0" style="text-align: left;
 * width: 100%;"> <tbody>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Type:</td>
 * <td style="vertical-align: top;">long</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Default:</td>
 * <td style="vertical-align: top;">10 seconds
 * <br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Description:</td>
 * <td style="vertical-align: top;">The amount of time (in milliseconds) to check for expired leases.
 * Must be a positive long value.</td>
 * </tr>
 * </tbody> </table>
 * </li>
 * </ul>
 *
 * * <ul>
 * <li><span style="font-weight: bold;">landlordLeasePeriodPolicy </span> <table
 * cellpadding="2" *="" cellspacing="2" border="0" style="text-align: left;
 * width: 100%;"> <tbody>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Type:</td>
 * <td style="vertical-align: top;">{@link com.sun.jini.landlord.LeasePeriodPolicy}</td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Default:</td>
 * <td style="vertical-align: top;">A new
 * {@link com.sun.jini.landlord.FixedLeasePeriodPolicy} that allows
 * leases up to one day, and grants one hour leases for duration requests of
 * {@link net.jini.core.lease.Lease#ANY}</code>
 * <br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top; text-align: right; font-weight: bold;">Description:</td>
 * <td style="vertical-align: top;">Policy used to determine the length of
 * initial grants and renewals of the leases on entries. Obtained at service
 * start and restart.</td>
 * </tr>
 * </tbody> </table>
 * </li>
 * </ul>
 *
 * @author Dennis Reedy
 */
public class LandlordLessor extends ResourceLessor implements Landlord,
                                                              ReferentUuid,
                                                              ServerProxyTrust {
    /** The default time for a Lease: 1 hour */
    public static final long DEFAULT_LEASE_TIME = TimeConstants.ONE_HOUR;
    /** The maximum time for a Lease: 1 day */
    public static final long DEFAULT_MAX_LEASE_TIME = TimeConstants.ONE_DAY;
    /** This LandlordLessor's uuid */
    private final Uuid uuid;
    /** The Remote Landlord */
    private final Landlord landlord;
    /** The Exporter for the Landlord */
    private Exporter exporter;
    /** Factory we use to create leases */
    private final LeaseFactory leaseFactory;
    /** LeasePolicy */
    private LeasePeriodPolicy leasePolicy;
    /** Component for reading configuration entries and getting the Logger */
    private static final String COMPONENT = LandlordLessor.class.getPackage().getName();
    private static Logger logger = LoggerFactory.getLogger(COMPONENT);

    /**
     * Create a LandlordLessor
     * 
     * @param config The Configuration object used to initialize operational
     * values.
     *
     * @throws RemoteException if errors occur setting up infrastructure
     */
    public LandlordLessor(final Configuration config) throws RemoteException {
        this(config, null);
    }

    /**
     * Create a LandlordLessor
     * 
     * @param config The Configuration object used to initialize operational
     * values.
     * @param leasePolicy A LeasePeriodPolicy object to be used for the 
     * LandlordLessor.
     *
     * @throws RemoteException if errors occur setting up infrastructure
     */
    public LandlordLessor(final Configuration config, final LeasePeriodPolicy leasePolicy) throws RemoteException {
        super();
        if (config == null)
            throw new IllegalArgumentException("config is null");
           
        /* Get the LeasePeriodPolicy */
        final LeasePeriodPolicy defaultLeasePeriodPolicy =
            (leasePolicy==null? new FixedLeasePeriodPolicy(DEFAULT_MAX_LEASE_TIME, DEFAULT_LEASE_TIME): leasePolicy);

        try {
            this.leasePolicy = (LeasePeriodPolicy)Config.getNonNullEntry(config,
                                                                         COMPONENT,
                                                                         "landlordLeasePeriodPolicy",
                                                                         LeasePeriodPolicy.class,
                                                                         defaultLeasePeriodPolicy);
        } catch (ConfigurationException e) {
            logger.warn("Getting LeasePeriodPolicy in LandlordLessor", e);
        }

        try {
            final long reapingInterval = Config.getLongEntry(config,
                                                             COMPONENT,
                                                             "reapingInterval",
                                                             TimeConstants.ONE_SECOND*10,
                                                             1,
                                                             Long.MAX_VALUE);
            setReapingInterval(reapingInterval);
        } catch (ConfigurationException e) {
            logger.warn("Getting reapingInterval in LandlordLessor", e);
        }
        
        /* Create the default Exporter */
        try {            
            exporter = ExporterConfig.getExporter(config, COMPONENT, "landlordExporter");
        } catch (ConfigurationException e) {
            logger.warn("Getting Exporter in LandlordLessor", e);
        }
        landlord = (Landlord) exporter.export(this);
        uuid = UuidFactory.generate(); 
        leaseFactory = new LeaseFactory(landlord, uuid);
    }

    /**
     * Stop the LandlordLessor
     * 
     * @param force if true, unexports the LandlordLessor even if there are
     * pending or in-progress calls; if false, only unexports the LandlordLessor
     * if there are no pending or in-progress calls
     *
     * @return True or false if the unexport was successful
     */
    public boolean stop(final boolean force) {
        super.stop();
        boolean unexported = false;
        try {
            unexported = exporter.unexport(force);
        } catch (Exception e) {
            if(logger.isTraceEnabled()) {
                logger.trace("Unexporting LandlordLessor", e);
            }
        }
        return unexported;
    }

    /**
     * Concrete implementation of parent class
     * 
     * @see ResourceLessor#newLease
     */
    public Lease newLease(final LeasedResource resource, final long duration) throws LeaseDeniedException {
        LeasePeriodPolicy.Result leasePeriod = leasePolicy.grant(resource, duration);
        Lease lease = leaseFactory.newLease(resource.getCookie(), leasePeriod.expiration);
        resource.setExpiration(leasePeriod.expiration);
        addLeasedResource(resource);
        notifyLeaseRegistration(resource);
        return lease;
    }

    /**
     * Called by the lease when its <code>renew</code> method is called. <br>
     * 
     * @param cookie Associated with the lease when it was created
     * @param extension The duration argument passed to the
     * <code>Lease.renew()</code> call
     * @return The new duration the lease should have
     */
    public long renew(final Uuid cookie, final long extension) throws LeaseDeniedException, UnknownLeaseException {
        LeasedResource resource = getLeasedResource(cookie);
        long granted;
        if (resource == null)
            throw new UnknownLeaseException("No lease for cookie: " + cookie);
        long now = System.currentTimeMillis();
        if (resource.getExpiration() <= now) {
            UnknownLeaseException e = new UnknownLeaseException("Lease has already expired");
            if(logger.isTraceEnabled()) {
                logger.trace("Lease has already expired by [{}] milliseconds, [{}] seconds",
                             (now-resource.getExpiration()),  ((now-resource.getExpiration())/1000));
            }
            throw e;
        }
        LeasePeriodPolicy.Result leasePeriod = leasePolicy.renew(resource, extension);
        resource.setExpiration(leasePeriod.expiration);
        granted = leasePeriod.duration;
        addLeasedResource(resource);
        notifyLeaseRenewal(resource);
        return granted;
    }

    /**
     * Called by the lease map when its <code>renewAll</code> method is
     * called.
     * 
     * @param cookie Associated with each lease when it was created <br>
     * @param extension The duration argument for each lease from the map
     * @return The results of the renew
     */
    public Landlord.RenewResults renewAll(final Uuid[] cookie, final long[] extension) {
        int size = cookie.length;
        long[] granted = new long[size];
        Exception[] denied = new Exception[size];
        for (int i = 0; i < size; i++) {
            try {
                granted[i] = renew(cookie[i], extension[i]);
                denied[i] = null;
            } catch (Exception e) {
                denied[i] = e;
            }
        }
        return new Landlord.RenewResults(granted, denied);
    }

    /**
     * Called by the lease when its <code>cancel</code> method is called. <br>
     * 
     * @param cookie Associated with the lease when it was created
     */
    public void cancel(final Uuid cookie) throws UnknownLeaseException {
        if (!remove(cookie))
            throw new UnknownLeaseException("No lease for cookie: " + cookie);
    }

    /**
     * Called by the lease map when its <code>cancelAll</code> method is
     * called.
     * 
     * @param cookies Associated with the lease when it was created <br>
     */
    public Map<Uuid, Exception> cancelAll(final Uuid[] cookies) {
        Map<Uuid, Exception> exceptionMap = null;
        for (Uuid cookie : cookies) {
            try {
                cancel(cookie);
            } catch (Exception e) {
                if (exceptionMap == null) {
                    exceptionMap = new HashMap<Uuid, Exception>();
                }
                exceptionMap.put(cookie, e);
            }
        }
        /*
         * If all the leases specified in the cookies could be cancelled return
         * null. Otherwise, return a Map that for each failed cancel attempt
         * maps the corresponding cookie object to an exception describing the
         * failure.
         */
        return exceptionMap;
    }

    public TrustVerifier getProxyVerifier() throws RemoteException {
        return(new LandlordProxyVerifier(landlord, uuid));
    }

    /**
     * Return the <code>Uuid</code> that has been assigned to the resource this
     * proxy represents.
     *
     * @return the <code>Uuid</code> associated with the resource this proxy
     *         represents. Will not return <code>null</code>.
     */
    public Uuid getReferentUuid() {
        return(uuid);
    }
}
