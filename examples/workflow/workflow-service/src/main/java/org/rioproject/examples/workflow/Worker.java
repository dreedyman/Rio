/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.examples.workflow;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.space.JavaSpace;
import org.rioproject.associations.AssociationProxyUtil;
import org.rioproject.bean.PreAdvertise;
import org.rioproject.bean.PreDestroy;
import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.CounterWatch;
import org.rioproject.watch.PeriodicWatch;
import org.rioproject.watch.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A basic JavaSpace worker
 */
@SuppressWarnings("unused")
public class Worker {
    private JavaSpace space;
    private TransactionManager tranMgr;
    private WorkflowEntry template;
    private ExecutorService service;
    public static final String COMPONENT = "workflow";
    private long timeout;
    private Logger logger = LoggerFactory.getLogger(COMPONENT);
    String name;
    private boolean shutdown = false;
    private CounterWatch rate;
    private ThroughputWatch throughput;
    private StopWatch meanTime;
    private ServiceBeanContext context;

    public void setJavaSpace(JavaSpace space) {
        this.space = space;
    }

    public void setServiceBeanContext(ServiceBeanContext context) {
        this.context = context;
        name = context.getServiceElement().getName();
    }

    public void setTransactionManager(TransactionManager tranMgr) {
        this.tranMgr = AssociationProxyUtil.getService(tranMgr);
    }

    public void setParameters(Map<String, Object> parms) {
        String tmpl = (String)parms.get("template");
        State state = State.valueOf(tmpl);
        template = new WorkflowEntry(state);
    }

    @PreAdvertise
    public void startup() {
        if(space==null)
            throw new IllegalStateException("The JavaSpace should have been injected prior to this method being called");
        logger.info("PRE_ADVERTISE {}", context.getServiceElement().getName());
        rate = new CounterWatch("rate");
        throughput = new ThroughputWatch("throughput");
        meanTime = new StopWatch("meanTime");
        timeout = 1000*10;
        int workerTasks = 1;
        service = Executors.newFixedThreadPool(workerTasks);
        service.submit(new SpaceProcessor());
        context.getWatchRegistry().register(rate);
        context.getWatchRegistry().register(meanTime);
        context.getWatchRegistry().register(throughput);
    }

    @PreDestroy
    public void cleanup() {
        shutdown = true;
        throughput.stop();
        if (service != null)
            service.shutdownNow();
    }

    class SpaceProcessor implements Runnable {
        public void run() {
            while (!shutdown) {
                try {
                    Transaction tx = null;
                    if (tranMgr != null) {
                        tx = TransactionFactory.create(tranMgr, Lease.FOREVER).transaction;

                    }
                    WorkflowEntry entry = (WorkflowEntry)space.takeIfExists(template, tx, timeout);
                    if (entry == null && tx != null) {
                        tx.abort();
                    } else {
                        if(entry!=null) {
                            logger.info("Worker [{}] processing task: {}", name, entry);
                            meanTime.startTiming();
                            Entry result = entry.execute();
                            meanTime.stopTiming();
                            space.write(result, tx, timeout);
                            if(tx != null)
                                tx.commit();
                            rate.increment();
                            throughput.increment();
                        }
                    }
                } catch (InterruptedException e) {
                    logger.info("SpaceProcessor InterruptedException, exiting");
                    break;

                } catch (Exception e) {
                    logger.warn("Worker [{}] processing task", name, e);
                    break;
                }
            }
        }
    }

    public class ThroughputWatch extends PeriodicWatch {
        private int numberOfCalls = 0;

        public ThroughputWatch(String id) {
            super(id);
            super.setPeriod(1000);
        }

        public void checkValue() {
            super.addWatchRecord(new Calculable("taux", numberOfCalls / (getPeriod() / 1000)));
            numberOfCalls = 0;
        }

        public void increment() {
            numberOfCalls++;
        }
    }
}

