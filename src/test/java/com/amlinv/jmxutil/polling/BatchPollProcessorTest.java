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

import com.amlinv.jmxutil.connection.impl.MBeanBatchCapableAccessConnection;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Verify operation of the BatchPollProcessor.
 *
 * Created by art on 8/24/15.
 */
public class BatchPollProcessorTest {
    private BatchPollProcessor processor;

    private AttributeInjector mockAttributeInjector;
    private ObjectQueryPreparer mockObjectQueryPreparer;
    private Logger mockLogger;

    private MBeanBatchCapableAccessConnection mockAccessConnection;
    private TestClass001 polled001;
    private ObjectQueryInfo mockObjectQueryInfo001;
    private ObjectName objectName001;

    private List<Object> polledObjects;
    private Set<String> attributeNames001;
    private Map<String, Method> attributeSetters001;
    private Map<ObjectName, List<String>> queryAttributeMap;
    private Map<ObjectName, List<Attribute>> resultAttributeMap;

    /**
     * Setup common test data and interactions.
     *
     * @throws Exception
     */
    @Before
    public void setupTest() throws Exception {
        this.processor = new BatchPollProcessor();

        this.mockAttributeInjector = Mockito.mock(AttributeInjector.class);
        this.mockObjectQueryPreparer = Mockito.mock(ObjectQueryPreparer.class);
        this.mockLogger = Mockito.mock(Logger.class);

        this.mockAccessConnection = Mockito.mock(MBeanBatchCapableAccessConnection.class);

        this.polledObjects = new LinkedList<>();
        this.polled001 = Mockito.mock(TestClass001.class);
        this.polledObjects.add(this.polled001);

        this.mockObjectQueryInfo001 = Mockito.mock(ObjectQueryInfo.class);
        this.objectName001 = Mockito.mock(ObjectName.class);
        this.attributeNames001 = new HashSet<>(Arrays.asList("x-att1-x", "x-att2-x"));
        this.attributeSetters001 = new HashMap<>();
        this.queryAttributeMap = new HashMap<>();
        this.resultAttributeMap = new HashMap<>();

        this.attributeSetters001.put("x-att1-x", TestClass001.class.getMethod("setStringValue", String.class));
        this.attributeSetters001.put("x-att2-x", TestClass001.class.getMethod("setIntValue", int.class));

        this.queryAttributeMap.put(this.objectName001, Arrays.asList("x-att1-x", "x-att2-x"));
        this.resultAttributeMap.put(this.objectName001,
                Arrays.asList(new Attribute("x-att1-x", "x-string-value-x"), new Attribute("x-att2-x", 11)));

        Mockito.when(this.mockObjectQueryPreparer.prepareObjectQuery(this.polled001))
                .thenReturn(this.mockObjectQueryInfo001);
        Mockito.when(this.mockObjectQueryInfo001.getTarget()).thenReturn(this.polled001);
        Mockito.when(this.mockObjectQueryInfo001.getObjectName()).thenReturn(this.objectName001);
        Mockito.when(this.mockObjectQueryInfo001.getAttributeNames()).thenReturn(this.attributeNames001);
        Mockito.when(this.mockObjectQueryInfo001.getAttributeSetters()).thenReturn(this.attributeSetters001);

        Mockito.when(this.mockAccessConnection
                .batchQueryAttributes(this.createAttributeMapMatcher(this.queryAttributeMap)))
                .thenReturn(this.resultAttributeMap);
    }

    /**
     * Verify operation of the getter and setter for attributeInjector.
     *
     * @throws Exception
     */
    @Test
    public void testGetSetAttributeInjector() throws Exception {
        assertNotNull(this.processor.getAttributeInjector());
        assertNotSame(this.mockAttributeInjector, this.processor.getAttributeInjector());

        this.processor.setAttributeInjector(this.mockAttributeInjector);
        assertSame(this.mockAttributeInjector, this.processor.getAttributeInjector());
    }

    /**
     * Verify operation of the getter and setter for objectQueryPreparer.
     *
     * @throws Exception
     */
    @Test
    public void testGetSetObjectQueryPreparer() throws Exception {
        assertNotNull(this.processor.getObjectQueryPreparer());
        assertNotSame(this.mockObjectQueryPreparer, this.processor.getObjectQueryPreparer());

        this.processor.setObjectQueryPreparer(this.mockObjectQueryPreparer);

        assertSame(this.mockObjectQueryPreparer, this.processor.getObjectQueryPreparer());
    }

    /**
     * Verify operation of the getter and setter for log.
     *
     * @throws Exception
     */
    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.processor.getLog());
        assertNotSame(this.mockLogger, this.processor.getLog());

        this.processor.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.processor.getLog());
    }

    /**
     * Verify operation of a batch poll.
     *
     * @throws Exception
     */
    @Test
    public void testPollBatch() throws Exception {
        //
        // SETUP TEST
        //
        this.preparePoller();


        //
        // EXECUTE
        //
        this.processor.pollBatch(this.mockAccessConnection, this.polledObjects);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockAttributeInjector).copyOutAttributes(
                this.polled001,
                Arrays.asList(new Attribute("x-att1-x", "x-string-value-x"), new Attribute("x-att2-x", 11)),
                this.attributeSetters001,
                this.objectName001
        );
    }

    /**
     * Verify operation of the shutdown method.
     *
     * @throws Exception
     */
    @Test
    public void testShutdown() throws Exception {
        this.processor.shutdown();
    }

    /**
     * Verify an attempt to poll after shutdown.
     *
     * @throws Exception
     */
    @Test
    public void testPollBatchAfterShutdown() throws Exception {
        this.processor.shutdown();
        this.processor.pollBatch(this.mockAccessConnection, this.polledObjects);

        Mockito.verifyNoMoreInteractions(this.mockAccessConnection);
        Mockito.verifyNoMoreInteractions(this.mockObjectQueryPreparer);
    }

    /**
     * Verify handling of a MalformedObjectNameException on preparing to query an object.
     *
     * @throws Exception
     */
    @Test
    public void testMalformedObjectNameOnPrepareObjectQuery() throws Exception {
        //
        // SETUP TEST
        //
        MalformedObjectNameException monExc = new MalformedObjectNameException("x-mon-exc-x");
        Mockito.when(this.mockObjectQueryPreparer.prepareObjectQuery(this.polled001)).thenThrow(monExc);

        this.preparePoller();


        //
        // EXECUTE
        //
        this.processor.pollBatch(this.mockAccessConnection, this.polledObjects);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockLogger).info("invalid object name in query; skipping", monExc);
    }

    /**
     * Verify handling of a ReflectionException on performing a batch query.
     *
     * @throws Exception
     */
    @Test
    public void testBatchQueryReflectionException() throws Exception {
        //
        // SETUP TEST
        //
        ReflectionException refExc = new ReflectionException(new Exception("x-exc-x"));
        Mockito.when(this.mockAccessConnection
                .batchQueryAttributes(this.createAttributeMapMatcher(this.queryAttributeMap))).thenThrow(refExc);

        this.preparePoller();


        //
        // EXECUTE
        //
        this.processor.pollBatch(this.mockAccessConnection, this.polledObjects);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockLogger).info("unexpected reflection exception during batch poll", refExc);
    }

    /**
     * Verify handling of a MalformedObjectNameException while executing a batch query.
     *
     * @throws Exception
     */
    @Test
    public void testBatchQueryMalformedObjectNameException() throws Exception {
        //
        // SETUP TEST
        //
        MalformedObjectNameException monExc = new MalformedObjectNameException("x-mon-exc-x");
        Mockito.when(this.mockAccessConnection
                .batchQueryAttributes(this.createAttributeMapMatcher(this.queryAttributeMap))).thenThrow(monExc);

        this.preparePoller();


        //
        // EXECUTE
        //
        this.processor.pollBatch(this.mockAccessConnection, this.polledObjects);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockLogger).info("unexpected malformed object name during batch poll", monExc);
    }

    /**
     * Verify handling of an attempt to poll with no valid objects to query.
     *
     * @throws Exception
     */
    @Test
    public void testNothingToPoll() throws Exception {
        this.preparePoller();

        this.processor.pollBatch(this.mockAccessConnection, new LinkedList<Object>());

        Mockito.verify(this.mockLogger).debug("nothing to poll after preparing {} objects", 0);
    }

    /**
     * Verify handling of an attempt to poll an object for which the preparation returns a null query info result.
     *
     * @throws Exception
     */
    @Test
    public void testNullQueryInfoOnPrepare() throws Exception {
        this.preparePoller();
        Mockito.when(this.mockObjectQueryPreparer.prepareObjectQuery(this.polled001)).thenReturn(null);

        this.processor.pollBatch(this.mockAccessConnection, this.polledObjects);
    }



                                        ////             ////
                                        ////  INTERNALS  ////
                                        ////             ////

    /**
     * Prepare the poller with common test data and interactions.
     *
     * @throws Exception
     */
    protected void preparePoller() throws Exception {
        this.processor.setAttributeInjector(this.mockAttributeInjector);
        this.processor.setObjectQueryPreparer(this.mockObjectQueryPreparer);
        this.processor.setLog(this.mockLogger);
    }

    /**
     * Create a matcher for the given attribute map, accounting for possible differences in ordering of list data.
     *
     * @param attributeMap expected map of attributes by object name.
     * @return matcher for the expected attribute map.
     */
    protected Map<ObjectName, List<String>> createAttributeMapMatcher(final Map<ObjectName, List<String>> attributeMap) {
        ArgumentMatcher<Map<ObjectName, List<String>>> matcher = new ArgumentMatcher<Map<ObjectName, List<String>>>() {
            @Override
            public boolean matches(Object obj) {
                if (obj instanceof Map) {
                    Map candidate = (Map) obj;
                    if (candidate.size() != attributeMap.size()) {
                        return false;
                    }

                    for (ObjectName oneObjectName : attributeMap.keySet()) {
                        Object valueObj = candidate.get(oneObjectName);
                        if (! (valueObj instanceof List)) {
                            return false;
                        }

                        List valueList = (List) valueObj;
                        Set valueSet = new HashSet<>(valueList);

                        for (String value : attributeMap.get(oneObjectName)) {
                            if (! valueSet.contains(value)) {
                                return false;
                            }
                        }
                    }

                    return true;
                }

                return false;
            }
        };

        return Mockito.argThat(matcher);
    }

    /**
     * Test polling class.
     */
    protected static class TestClass001 {
        public void setStringValue(String value) {
        }

        public void setIntValue(int value) {
        }
    }
}
