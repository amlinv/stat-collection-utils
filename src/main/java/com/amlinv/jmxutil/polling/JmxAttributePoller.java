/**
 * Copyright 2015 AML Innovation & Consulting LLC
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amlinv.jmxutil.polling;

import com.amlinv.javasched.Scheduler;
import com.amlinv.javasched.SchedulerProcess;
import com.amlinv.javasched.SchedulerProcessExecutionSlip;
import com.amlinv.javasched.Step;
import com.amlinv.jmxutil.connection.MBeanAccessConnection;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import com.amlinv.jmxutil.connection.impl.MBeanBatchCapableAccessConnection;
import com.amlinv.logging.util.RepeatLogMessageSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Poller of JMX Attributes that polls periodically based on anotations on poll objects and stores the results back into
 * those objects.
 * <p/>
 * Created by art on 3/31/15.
 */
public class JmxAttributePoller {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JmxAttributePoller.class);

    private final List<Object> polledObjects;

    private Logger log = DEFAULT_LOGGER;

    private MBeanAccessConnectionFactory mBeanAccessConnectionFactory;
    private MBeanAccessConnection mBeanAccessConnection;

    private RepeatLogMessageSuppressor logInstanceNotFoundThrottle = new RepeatLogMessageSuppressor();

    private AttributeInjector attributeInjector = new AttributeInjector();
    private ObjectQueryPreparer objectQueryPreparer = new ObjectQueryPreparer();
    private BatchPollProcessor batchPollProcessor = new BatchPollProcessor();

    private boolean shutdownInd = false;
    private boolean pollActiveInd = false;
    private Scheduler scheduler;

    private ConcurrencyTestHooks concurrencyTestHooks = new ConcurrencyTestHooks();

    public JmxAttributePoller(List<Object> polledObjects) {
        this.polledObjects = polledObjects;
    }

    public MBeanAccessConnectionFactory getmBeanAccessConnectionFactory() {
        return mBeanAccessConnectionFactory;
    }

    public void setmBeanAccessConnectionFactory(MBeanAccessConnectionFactory mBeanAccessConnectionFactory) {
        this.mBeanAccessConnectionFactory = mBeanAccessConnectionFactory;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public List<Object> getPolledObjects() {
        return Collections.unmodifiableList(polledObjects);
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public AttributeInjector getAttributeInjector() {
        return attributeInjector;
    }

    public void setAttributeInjector(AttributeInjector attributeInjector) {
        this.attributeInjector = attributeInjector;
    }

    public BatchPollProcessor getBatchPollProcessor() {
        return batchPollProcessor;
    }

    public void setBatchPollProcessor(BatchPollProcessor batchPollProcessor) {
        this.batchPollProcessor = batchPollProcessor;
    }

    public void setConcurrencyTestHooks(ConcurrencyTestHooks concurrencyTestHooks) {
        this.concurrencyTestHooks = concurrencyTestHooks;
    }

    public ObjectQueryPreparer getObjectQueryPreparer() {
        return objectQueryPreparer;
    }

    public void setObjectQueryPreparer(ObjectQueryPreparer objectQueryPreparer) {
        this.objectQueryPreparer = objectQueryPreparer;
    }

    /**
     * Poll the configured objects now and store the results in the objects themselves.
     *
     * @throws IOException
     */
    public void poll() throws IOException {
        synchronized (this) {
            // Make sure not to check and create a connection if shutting down.
            if (shutdownInd) {
                return;
            }

            // Atomically indicate polling is active now so a caller can determine with certainty whether polling is
            //  completely shutdown.
            pollActiveInd = true;
        }

        try {
            this.checkConnection();

            this.concurrencyTestHooks.beforePollProcessorStart();

            if (this.mBeanAccessConnection instanceof MBeanBatchCapableAccessConnection) {
                this.batchPollProcessor.pollBatch((MBeanBatchCapableAccessConnection) this.mBeanAccessConnection,
                        this.polledObjects);
            } else {
                this.pollIndividually();
            }
        } catch (IOException ioExc) {
            this.safeClose(this.mBeanAccessConnection);
            this.mBeanAccessConnection = null;

            throw ioExc;
        } finally {
            this.concurrencyTestHooks.afterPollProcessorFinish();

            synchronized (this) {
                pollActiveInd = false;
                this.notifyAll();
            }
        }
    }



                                        ////             ////
                                        ////  INTERNALS  ////
                                        ////             ////

    /**
     * Poll all of the objects, one at a time.
     *
     * @return false => if polling completed normally; true => if polling stopped due to shutdown.
     * @throws IOException
     */
    protected boolean pollIndividually() throws IOException {
        this.concurrencyTestHooks.onStartPollIndividually();

        List<SchedulerProcessExecutionSlip> processExecutionSlipList = new LinkedList<>();
        for (final Object onePolledObject : this.polledObjects) {
            // Stop as soon as possible if shutting down.
            if (shutdownInd) {
                return true;
            }

            SchedulerProcess process = new PollOneObjectSchedulerProcess(onePolledObject);
            SchedulerProcessExecutionSlip executionSlip = this.scheduler.startProcess(process);
            processExecutionSlipList.add(executionSlip);
        }

        for (SchedulerProcessExecutionSlip oneExecutionSlip : processExecutionSlipList) {
            try {
                //
                // Wait for this process to complete
                //
                oneExecutionSlip.waitUntilComplete();


                //
                // Check for a failure
                //
                PollOneObjectSchedulerProcess process =
                        (PollOneObjectSchedulerProcess) oneExecutionSlip.getSchedulerProcess();

                Exception exc = process.getFailureException();
                if (exc != null) {
                    log.warn("failed to poll object", exc);

                    // Propagate IOExceptions since they most likely mean that the connection needs to be recovered.
                    if (exc instanceof IOException) {
                        throw (IOException) exc;
                    }
                }

            } catch (InterruptedException intExc) {
                log.info("interrupted while polling object");
            }
        }

        return false;
    }

    public void shutdown() {
        this.shutdownInd = true;
        this.batchPollProcessor.shutdown();

        synchronized (this) {
            this.notifyAll();
        }
    }

    public void waitUntilShutdown() throws InterruptedException {
        synchronized (this) {
            // Wait until shutdown is initiated.
            while (!this.shutdownInd) {
                this.concurrencyTestHooks.onWaitForShutdown();
                this.wait();
            }

            // Wait until any active polling stops.
            while (pollActiveInd) {
                this.concurrencyTestHooks.onWaitForPollInactive();
                this.wait();
            }
        }
    }

    protected void checkConnection() throws IOException {
        if (this.mBeanAccessConnection == null) {
            this.mBeanAccessConnection = this.mBeanAccessConnectionFactory.createConnection();
        }
    }

    protected void pollOneObject(Object obj)
            throws MalformedObjectNameException, IOException, ReflectionException, InvocationTargetException,
            IllegalAccessException {

        ObjectQueryInfo queryInfo = objectQueryPreparer.prepareObjectQuery(obj);

        if (queryInfo != null) {
            try {
                String[] attributeNames = new String[queryInfo.getAttributeSetters().size()];
                attributeNames = queryInfo.getAttributeSetters().keySet().toArray(attributeNames);

                //
                // Query the values now.
                //
                List<Attribute> attributeValues =
                        this.mBeanAccessConnection.getAttributes(queryInfo.getObjectName(), attributeNames);

                //
                // Finally, copy out the results.
                //
                this.attributeInjector.copyOutAttributes(obj, attributeValues, queryInfo.getAttributeSetters(),
                        queryInfo.getObjectName());
            } catch (InstanceNotFoundException infExc) {
                this.logInstanceNotFoundThrottle.debug(log, "instance not found on polling object: oname={}",
                        queryInfo.getObjectName(), infExc);
            }
        }
    }

    protected void safeClose(MBeanAccessConnection mBeanAccessConnector) {
        try {
            if (mBeanAccessConnector != null) {
                mBeanAccessConnector.close();
            }
        } catch (IOException ioExc) {
            log.warn("exception on shutdown of jmx connection to {}",
                    this.mBeanAccessConnectionFactory.getTargetDescription(), ioExc);
        }
    }

    /**
     * Process for polling a single object.
     */
    protected class PollOneObjectSchedulerProcess implements SchedulerProcess {
        private boolean done = false;
        private final Object target;
        private Exception failureException;

        public PollOneObjectSchedulerProcess(Object target) {
            this.target = target;
        }

        public Exception getFailureException() {
            return failureException;
        }

        @Override
        public Step getNextStep() {
            if (done || shutdownInd) {
                return null;
            }

            Step result = new Step() {
                @Override
                public void execute() {
                    try {
                        pollOneObject(target);
                    } catch (Exception exc) {
                        failureException = exc;
                    } finally {
                        done = true;
                    }
                }

                @Override
                public boolean isBlocking() {
                    return true;
                }
            };

            return result;
        }
    }

    /**
     * Hooks for internal testing purposes only.
     */
    protected static class ConcurrencyTestHooks {
        public void onWaitForShutdown() {
        }

        public void onWaitForPollInactive() {
        }

        public void onStartPollIndividually() {
        }

        public void beforePollProcessorStart() {
        }

        public void afterPollProcessorFinish() {
        }
    }
}
