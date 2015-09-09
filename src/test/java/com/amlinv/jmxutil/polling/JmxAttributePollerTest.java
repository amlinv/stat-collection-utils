/*
 *   Copyright 2015 AML Innovation & Consulting LLC
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.amlinv.jmxutil.polling;

import com.amlinv.javasched.Scheduler;
import com.amlinv.javasched.SchedulerProcess;
import com.amlinv.javasched.SchedulerProcessExecutionSlip;
import com.amlinv.jmxutil.annotation.MBeanAttribute;
import com.amlinv.jmxutil.annotation.MBeanLocation;
import com.amlinv.jmxutil.connection.MBeanAccessConnection;
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import com.amlinv.jmxutil.connection.impl.MBeanBatchCapableAccessConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Created by art on 8/20/15.
 */
public class JmxAttributePollerTest {
    private static final Logger log = LoggerFactory.getLogger(JmxAttributePollerTest.class);

    public static final String TEST_ONAME_001_STR = "x-oname-domain-x:x-oname-key-x=x-oname-value1-x";
    public static final String TEST_ONAME_002_STR = "x-oname-domain-x:x-oname-key-x=x-oname-value2-x";

    private JmxAttributePoller poller;

    private MBeanAccessConnectionFactory mockMBeanAccessConnectionFactory;
    private Scheduler mockScheduler;

    private MBeanBatchCapableAccessConnection mockBatchCapableAccessConnection;
    private MBeanAccessConnection mockAccessConnection;
    private AttributeInjector mockAttributeInjector;
    private BatchPollProcessor mockBatchPollProcessor;
    private ObjectQueryPreparer mockObjectQueryPreparer;
    private JmxAttributePoller.ConcurrencyTestHooks mockTestHooks;

    private Logger mockLogger;

    private List<Object> polledObjects;
    private List<SchedulerProcessExecutionSlip> executionSlips;
    private List<SchedulerProcess> processes;

    private TestDataClass001 polled001;

    private ObjectName testObjectName001;
    private String accessConnectionFactoryDesc;

    @Before
    public void setupTest() throws Exception {
        this.polled001 = new TestDataClass001();

        this.polledObjects = new LinkedList<>();
        this.polledObjects.add("x-no-data-x");
        this.polledObjects.add(polled001);

        this.mockMBeanAccessConnectionFactory = Mockito.mock(MBeanAccessConnectionFactory.class);
        this.mockScheduler = Mockito.mock(Scheduler.class);

        this.mockBatchCapableAccessConnection = Mockito.mock(MBeanBatchCapableAccessConnection.class);
        this.mockAccessConnection = Mockito.mock(MBeanAccessConnection.class);
        this.mockAttributeInjector = Mockito.mock(AttributeInjector.class);
        this.mockBatchPollProcessor = Mockito.mock(BatchPollProcessor.class);
        this.mockObjectQueryPreparer = Mockito.mock(ObjectQueryPreparer.class);
        this.mockTestHooks = Mockito.mock(JmxAttributePoller.ConcurrencyTestHooks.class);

        this.mockLogger = Mockito.mock(Logger.class);

        this.poller = new JmxAttributePoller(this.polledObjects);

        this.processes = new LinkedList<>();
        this.executionSlips = new LinkedList<>();

        Answer<SchedulerProcessExecutionSlip> startProcessAnswer = createProcessExecutionSlipAnswer(null, null);
        Mockito.when(this.mockScheduler.startProcess(Mockito.any(SchedulerProcess.class)))
                .thenAnswer(startProcessAnswer);

        this.testObjectName001 = new ObjectName(this.TEST_ONAME_001_STR);

        this.accessConnectionFactoryDesc = "x-access-conn-factory-desc-x";
        Mockito.when(this.mockMBeanAccessConnectionFactory.getTargetDescription())
                .thenReturn(this.accessConnectionFactoryDesc);
    }

    @Test
    public void testGetSetmBeanAccessConnectionFactory() throws Exception {
        assertNull(this.poller.getmBeanAccessConnectionFactory());

        this.poller.setmBeanAccessConnectionFactory(this.mockMBeanAccessConnectionFactory);
        assertSame(this.mockMBeanAccessConnectionFactory, this.poller.getmBeanAccessConnectionFactory());
    }

    @Test
    public void testGetSetScheduler() throws Exception {
        assertNull(this.poller.getScheduler());

        this.poller.setScheduler(this.mockScheduler);
        assertSame(this.mockScheduler, this.poller.getScheduler());
    }

    @Test
    public void testGetPolledObjects() throws Exception {
        assertEquals(this.polledObjects, this.poller.getPolledObjects());
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.poller.getLog());
        assertNotSame(this.mockLogger, this.poller.getLog());

        this.poller.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.poller.getLog());
    }

    @Test
    public void testGetSetAttributeInjector() throws Exception {
        assertNotNull(this.poller.getAttributeInjector());
        assertNotSame(this.mockAttributeInjector, this.poller.getAttributeInjector());

        this.poller.setAttributeInjector(this.mockAttributeInjector);
        assertSame(this.mockAttributeInjector, this.poller.getAttributeInjector());
    }

    @Test
    public void testGetSetBatchPollProcessor() throws Exception {
        assertNotNull(this.poller.getBatchPollProcessor());
        assertNotSame(this.mockBatchPollProcessor, this.poller.getBatchPollProcessor());

        this.poller.setBatchPollProcessor(this.mockBatchPollProcessor);
        assertSame(this.mockBatchPollProcessor, this.poller.getBatchPollProcessor());
    }

    @Test
    public void testGetSetObjectQueryPreparer() throws Exception {
        assertNotNull(this.poller.getObjectQueryPreparer());
        assertNotSame(this.mockObjectQueryPreparer, this.poller.getObjectQueryPreparer());

        this.poller.setObjectQueryPreparer(this.mockObjectQueryPreparer);
        assertSame(this.mockObjectQueryPreparer, this.poller.getObjectQueryPreparer());
    }

    @Test
    public void testPoll() throws Exception {
        this.setupPoller(false);

        this.poller.poll();

        assertEquals(2, processes.size());

        processes.get(0).getNextStep().execute();
        Mockito.verifyNoMoreInteractions(this.mockAccessConnection);

        processes.get(1).getNextStep().execute();
        assertEquals("x-value-x", this.polled001.getName());
    }

    @Test
    public void testPollBatch() throws Exception {
        this.setupPoller(true);

        this.poller.poll();

        Mockito.verify(this.mockBatchPollProcessor)
                .pollBatch(this.mockBatchCapableAccessConnection, this.polledObjects);
    }

    @Test
    public void testShutdown() throws Exception {
        this.poller.setBatchPollProcessor(this.mockBatchPollProcessor);
        this.poller.shutdown();

        Mockito.verify(this.mockBatchPollProcessor).shutdown();
    }

    @Test(timeout = 3000L)
    public void testWaitUntilShutdown() throws Exception {
        //
        // Setup the hook to ensure the wait() call before we call poller.shutdown().
        //
        final CountDownLatch shutdownWaitLatch = new CountDownLatch(1);
        Answer<Void> shutdownAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("onWaitForShutdown() called");
                shutdownWaitLatch.countDown();
                return null;
            }
        };
        Mockito.doAnswer(shutdownAnswer).when(this.mockTestHooks).onWaitForShutdown();

        //
        // Setup the hook to wait until a poll is active before we call waitUntilShutdown().
        //
        final CountDownLatch pollActiveLatch = new CountDownLatch(1);
        Answer<Void> waitPollActiveAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("beforePollProcessorStart() called");
                pollActiveLatch.countDown();

                return null;
            }
        };
        Mockito.doAnswer(waitPollActiveAnswer).when(this.mockTestHooks).beforePollProcessorStart();


        //
        // Setup the hook to signal when polling is active so we can make sure to call waitUntilShutdown() only while
        //  it is active.
        //
        final CountDownLatch pollFinishLatch = new CountDownLatch(1);
        Answer<Void> pollActiveAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("afterPollProcessorFinish() wait started");

                // Don't allow the poll to finish until signaled.
                pollFinishLatch.await();

                log.debug("afterPollProcessorFinish() wait finished");
                return null;
            }
        };
        Mockito.doAnswer(pollActiveAnswer).when(this.mockTestHooks).afterPollProcessorFinish();


        //
        // Setup the hook to ensure the wait() call on active poll before allowing the active poll to complete.
        //
        Answer<Void> waitOnPollAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                log.debug("onWaitForPollInactive() called");

                pollFinishLatch.countDown();

                return null;
            }
        };
        Mockito.doAnswer(waitOnPollAnswer).when(this.mockTestHooks).onWaitForPollInactive();

        //
        // Run waitUntilShutdown() in a separate thread so all the timing controls can be executed in this thread.
        //
        Thread waitUntilShutdownThread = new Thread() {
            @Override
            public void run() {
                try {
                    log.debug("starting waitUntilShutdown()");
                    poller.waitUntilShutdown();
                    log.debug("finished waitUntilShutdown()");
                } catch (InterruptedException intExc) {
                    throw new RuntimeException(intExc);
                }
            }
        };

        //
        // Run a poll() in a separate thread as it needs to be active across the execution of most of the test.
        //
        Thread runPollThread = new Thread() {
            @Override
            public void run() {
                try {
                    log.debug("starting poll()");
                    poller.poll();
                    log.debug("finished poll()");
                } catch (IOException ioExc) {
                    throw new RuntimeException(ioExc);
                }
            }
        };

        this.poller.setConcurrencyTestHooks(this.mockTestHooks);
        this.setupPoller(false);

        // Make sure a poll is active before initiating the shutdown wait
        runPollThread.start();

        pollActiveLatch.await();
        waitUntilShutdownThread.start();

        shutdownWaitLatch.await();
        this.poller.shutdown();

        // Make sure waitUntilShutdown() finishes.
        waitUntilShutdownThread.join();
    }

    @Test
    public void testPollAfterShutdown() throws Exception {
        this.poller.shutdown();

        this.poller.poll();
    }

    @Test
    public void testExecutionExceptionOnPollIndividually() throws Exception {
        //
        // SETUP
        //
        IOException ioExc = new IOException("x-io-exc-x");

        this.setupPoller(false);

        Answer<SchedulerProcessExecutionSlip> ioExcAnswer = createProcessExecutionSlipAnswer(ioExc, null);
        Mockito.doAnswer(ioExcAnswer).when(this.mockScheduler).startProcess(Mockito.any(SchedulerProcess.class));


        //
        // EXECUTE
        //
        try {
            this.poller.poll();

            //
            // VERIFY
            //
            fail("missing expected exception");
        } catch (IOException caught) {
            assertSame(ioExc, caught);

            Mockito.verify(this.mockLogger).warn("failed to poll object", ioExc);
            Mockito.verify(this.mockAccessConnection).close();
        }
    }

    @Test
    public void testInterruptedExceptionOnPollIndividually() throws Exception {
        InterruptedException intExc = new InterruptedException("x-intr-exc-x");

        Answer<SchedulerProcessExecutionSlip> intrExecAnswer = createProcessExecutionSlipAnswer(null, intExc);
        Mockito.doAnswer(intrExecAnswer).when(this.mockScheduler).startProcess(Mockito.any(SchedulerProcess.class));

        this.setupPoller(false);

        this.poller.poll();

        // Verify interruption logged once for each polled object
        Mockito.verify(this.mockLogger, Mockito.times(this.polledObjects.size()))
                .info("interrupted while polling object");
    }

    @Test
    public void testOtherExceptionOnPollIndividually() throws Exception {
        this.setupPoller(false);

        RuntimeException testExc = new RuntimeException("x-rt-exc-x");
        Answer<SchedulerProcessExecutionSlip> intrExecAnswer = createProcessExecutionSlipAnswer(testExc, null);
        Mockito.doAnswer(intrExecAnswer).when(this.mockScheduler).startProcess(Mockito.any(SchedulerProcess.class));


        this.poller.poll();

        Mockito.verify(this.mockLogger).warn("failed to poll object", testExc);
    }

    /**
     * Validate handling of an IOException on close of the access connection after procesing catches an IOException.
     *
     * @throws Exception
     */
    @Test
    public void testIOExceptionOnSafeClose() throws Exception {
        IOException ioExc1 = new IOException("x-io-exc1-x");
        IOException ioExc2 = new IOException("x-io-exc2-x");

        this.setupPoller(false);

        Answer<SchedulerProcessExecutionSlip> execExcAnswer = createProcessExecutionSlipAnswer(ioExc1, null);
        Mockito.doAnswer(execExcAnswer).when(this.mockScheduler).startProcess(Mockito.any(SchedulerProcess.class));
        Mockito.doThrow(ioExc2).when(this.mockAccessConnection).close();

        try {
            this.poller.poll();
            fail("missing expected exception");
        } catch (IOException caught) {
            assertSame(ioExc1, caught);

            Mockito.verify(this.mockLogger).warn("failed to poll object", ioExc1);
            Mockito.verify(this.mockLogger).warn("exception on shutdown of jmx connection to {}",
                    this.accessConnectionFactoryDesc, ioExc2);
        }
    }

    @Test
    public void testInstanceNotFoundOnPollIndividually() throws Exception {
        this.setupPoller(false);

        InstanceNotFoundException infExc = new InstanceNotFoundException("x-inst-not-found-ex-x");
        Mockito.when(mockAccessConnection.getAttributes(this.testObjectName001, "x-name-x"))
                .thenThrow(infExc);

        this.poller.poll();

        processes.get(1).getNextStep().execute();

        Mockito.verify(this.mockLogger)
                .debug("instance not found on polling object: oname={}", new Object[]{this.testObjectName001, infExc});
    }

    @Test
    public void testShutdownOnPollIndividually() throws Exception {
        Answer<Void> startPollIndividuallyAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                poller.shutdown();
                return null;
            }
        };

        Mockito.doAnswer(startPollIndividuallyAnswer).when(this.mockTestHooks).onStartPollIndividually();

        this.poller.setConcurrencyTestHooks(this.mockTestHooks);
        this.setupPoller(false);

        this.poller.poll();

        Mockito.verify(this.mockScheduler, Mockito.times(0)).startProcess(Mockito.any(SchedulerProcess.class));
    }

    /**
     * Call the original test hook implementations for code coverage purposes only.
     *
     * @throws Exception
     */
    @Test
    public void testHookCodeCoverage() throws Exception {
        JmxAttributePoller.ConcurrencyTestHooks hooks = new JmxAttributePoller.ConcurrencyTestHooks();

        hooks.onWaitForPollInactive();
        hooks.onWaitForShutdown();
        hooks.onStartPollIndividually();

        hooks.beforePollProcessorStart();
        hooks.afterPollProcessorFinish();
    }

    /**
     * Verify polling with an active connection does not open another connection.
     *
     * @throws Exception
     */
    @Test
    public void testCheckConnectionOnActiveConnection() throws Exception {
        this.setupPoller(false);

        this.poller.poll();
        this.poller.poll();

        Mockito.verify(this.mockMBeanAccessConnectionFactory, Mockito.times(1)).createConnection();
    }

    /**
     * Verify handling of a null connection on call to safeClose().
     *
     * @throws Exception
     */
    @Test
    public void testSafeCloseWithoutConnector() throws Exception {
        // Need to call safeClose() directly since there's no valid path through the code that should end up calling it
        //  with mbeanAccessConnector null.
        this.poller.safeClose(null);
    }

    /**
     * Verify the step created by a polling process are blocking.
     *
     * @throws Exception
     */
    @Test
    public void testSchedulerStepIsBlocking() throws Exception {
        JmxAttributePoller.PollOneObjectSchedulerProcess process = this.poller.new PollOneObjectSchedulerProcess("");
        assertTrue(process.getNextStep().isBlocking());
    }

    /**
     * Verify the polling proces only returns one step.
     *
     * @throws Exception
     */
    @Test
    public void testGetStepWhenProcessDone() throws Exception {
        JmxAttributePoller.PollOneObjectSchedulerProcess process = this.poller.new PollOneObjectSchedulerProcess("");
        process.getNextStep().execute();
        assertNull(process.getNextStep());
    }

    /**
     * Verify the polling process does not return a step after poller shutdown.
     *
     * @throws Exception
     */
    @Test
    public void testGetStepAfterShutdown() throws Exception {
        this.poller.shutdown();

        JmxAttributePoller.PollOneObjectSchedulerProcess process = this.poller.new PollOneObjectSchedulerProcess("");
        assertNull(process.getNextStep());
    }



                                        ////             ////
                                        ////  INTERNALS  ////
                                        ////             ////

    protected Answer<SchedulerProcessExecutionSlip> createProcessExecutionSlipAnswer(final Exception execExc,
                                                              final InterruptedException intrExc) {

        return new Answer<SchedulerProcessExecutionSlip>() {
            @Override
            public SchedulerProcessExecutionSlip answer(InvocationOnMock invocationOnMock) throws Throwable {
                JmxAttributePoller.PollOneObjectSchedulerProcess process =
                        invocationOnMock.getArgumentAt(0, JmxAttributePoller.PollOneObjectSchedulerProcess.class);

                processes.add(process);

                SchedulerProcessExecutionSlip result = Mockito.mock(SchedulerProcessExecutionSlip.class);
                executionSlips.add(result);

                if (execExc != null) {
                    Mockito.when(mockAccessConnection
                            .getAttributes(Mockito.eq(testObjectName001), (String[]) Mockito.anyVararg()))
                            .thenThrow(execExc);

                    process.getNextStep().execute();
                }

                if (intrExc != null) {
                    Mockito.doThrow(intrExc).when(result).waitUntilComplete();
                }

                Mockito.doReturn(process).when(result).getSchedulerProcess();

                return result;
            }
        };
    }

    protected void setupPoller(boolean batchInd) throws Exception {
        if (batchInd) {
            Mockito.when(this.mockMBeanAccessConnectionFactory.createConnection())
                    .thenReturn(this.mockBatchCapableAccessConnection);
        } else {
            Mockito.when(this.mockMBeanAccessConnectionFactory.createConnection())
                    .thenReturn(this.mockAccessConnection);
        }

        Mockito.when(this.mockAccessConnection.getAttributes(this.testObjectName001, "x-name-x"))
                .thenReturn(Arrays.asList(new Attribute("x-name-x", "x-value-x")));

        Mockito.when(this.mockObjectQueryPreparer.prepareObjectQuery(this.polled001)).thenReturn(
                new ObjectQueryInfo(this.polled001, this.testObjectName001, this.getTestDataClassSetters()));

        this.poller.setmBeanAccessConnectionFactory(this.mockMBeanAccessConnectionFactory);
        this.poller.setBatchPollProcessor(this.mockBatchPollProcessor);
        this.poller.setObjectQueryPreparer(this.mockObjectQueryPreparer);
        this.poller.setScheduler(this.mockScheduler);
        this.poller.setLog(this.mockLogger);
    }

    protected Map<String, Method> getTestDataClassSetters() throws Exception {
        Map<String, Method> result = new HashMap<>();
        result.put("x-name-x", TestDataClass001.class.getMethod("setName", String.class));

        return result;
    }

    @MBeanLocation(onamePattern = TEST_ONAME_001_STR)
    protected static class TestDataClass001 {
        private String name;

        public String getName() {
            return name;
        }

        @MBeanAttribute(name = "x-name-x", type = String.class)
        public void setName(String name) {
            this.name = name;
        }
    }
}
