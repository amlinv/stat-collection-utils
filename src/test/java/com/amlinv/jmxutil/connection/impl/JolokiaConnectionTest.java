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

package com.amlinv.jmxutil.connection.impl;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pBulkRemoteException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pSearchRequest;
import org.jolokia.client.request.J4pSearchResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.ObjectName;
import javax.management.QueryExp;
import java.io.IOException;
import java.util.*;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;

/**
 * Unit test the JolokiaConnection which is used to collect data from Jolokia endpoints.  Note that PowerMock is used
 * here because some of the Jolokia classes are, sadly, "final".
 * <p/>
 * Created by art on 8/19/15.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JolokiaConnection.class, J4pReadResponse.class, J4pSearchResponse.class})
public class JolokiaConnectionTest {

    private JolokiaConnection jolokiaConnection;

    private J4pClient mockJ4pClient;
    private J4pReadResponse mockJ4pReadResponse1;
    private J4pReadResponse mockJ4pReadResponse2;
    private J4pSearchResponse mockJ4pSearchResponse;
    private Logger mockLogger;

    private ObjectName objectName1;
    private ObjectName objectName2;
    private QueryExp queryExp;
    private String att1;
    private String att2;
    private String att3;
    private String att4;
    private String value1;
    private String value2;
    private String value3;
    private String value4;

    @Before
    public void setupTest() throws Exception {
        //
        // SETUP COMMON MOCKS
        //
        this.mockJ4pClient = Mockito.mock(J4pClient.class);

        this.mockJ4pReadResponse1 = PowerMockito.mock(J4pReadResponse.class);
        this.mockJ4pReadResponse2 = PowerMockito.mock(J4pReadResponse.class);
        this.mockJ4pSearchResponse = PowerMockito.mock(J4pSearchResponse.class);

        this.jolokiaConnection = new JolokiaConnection(this.mockJ4pClient);
        this.mockLogger = Mockito.mock(Logger.class);


        //
        // SETUP COMMON TEST DATA
        //
        this.objectName1 = new ObjectName("x-domain-x:x-key1-x=x-value1-x");
        this.objectName2 = new ObjectName("x-domain-x:x-key1-x=x-value2-x");
        this.queryExp = new ObjectName("x-domain-x:x-query-x=*");
        this.att1 = "x-att1-x";
        this.att2 = "x-att2-x";
        this.att3 = "x-att3-x";
        this.att4 = "x-att4-x";
        this.value1 = "x-value1-x";
        this.value2 = "x-value2-x";
        this.value3 = "x-value3-x";
        this.value4 = "x-value4-x";


        //
        // SETUP COMMON MOCK INTERACTIONS
        //
        Mockito.when(this.mockJ4pReadResponse1.getValue(this.att1)).thenReturn(this.value1);
        Mockito.when(this.mockJ4pReadResponse1.getValue(this.att2)).thenReturn(this.value2);
        Mockito.when(this.mockJ4pReadResponse1.getValue(this.att3)).thenReturn(this.value3);
        Mockito.when(this.mockJ4pReadResponse1.getValue(this.att4)).thenReturn(this.value4);

        Mockito.when(this.mockJ4pReadResponse1.getObjectNames()).thenReturn(Arrays.asList(this.objectName1));
        Mockito.when(this.mockJ4pReadResponse1.getValue(this.objectName1, this.att1)).thenReturn(this.value1);
        Mockito.when(this.mockJ4pReadResponse1.getValue(this.objectName1, this.att2)).thenReturn(this.value2);
        Mockito.when(this.mockJ4pReadResponse2.getObjectNames()).thenReturn(Arrays.asList(this.objectName2));
        Mockito.when(this.mockJ4pReadResponse2.getValue(this.objectName2, this.att1)).thenReturn(this.value1);
        Mockito.when(this.mockJ4pReadResponse2.getValue(this.objectName2, this.att3)).thenReturn(this.value3);
        Mockito.when(this.mockJ4pReadResponse2.getValue(this.objectName2, this.att4)).thenReturn(this.value4);
    }

    @Test
    public void testConstructor() throws Exception {
        J4pClient mockClient = Mockito.mock(J4pClient.class);

        new JolokiaConnection(mockClient);
    }

    @Test
    public void testGetSetLogger() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //
        assertNotNull(this.jolokiaConnection.getLog());
        assertNotSame(this.mockLogger, this.jolokiaConnection.getLog());


        //
        // EXECUTE
        //
        this.jolokiaConnection.setLog(this.mockLogger);


        //
        // VALIDATE
        //
        assertSame(this.mockLogger, this.jolokiaConnection.getLog());
    }

    @Test
    public void testGetAttributes() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //
        Mockito.when(this.mockJ4pClient.execute(this.matchReadRequest(this.objectName1, this.att1, this.att2)))
                .thenReturn(this.mockJ4pReadResponse1);

        //
        // EXECUTE
        //
        List<Attribute> result = this.jolokiaConnection.getAttributes(this.objectName1, this.att1, this.att2);


        //
        // VALIDATE
        //
        assertEquals(2, result.size());
        assertEquals(this.att1, result.get(0).getName());
        assertEquals(this.value1, result.get(0).getValue());
        assertEquals(this.att2, result.get(1).getName());
        assertEquals(this.value2, result.get(1).getValue());
    }

    @Test
    public void testBatchQueryAttributes() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //
        Map<ObjectName, List<String>> requestParameters = new HashMap<>();

        requestParameters.put(this.objectName1, Arrays.asList(this.att1, this.att2));
        requestParameters.put(this.objectName2, Arrays.asList(this.att1, this.att3, this.att4));

        Mockito.when(this.mockJ4pClient.execute(this.matchReadRequestList(
                this.createJ4pReadRequestMatcher(this.objectName1, this.att1, this.att2),
                this.createJ4pReadRequestMatcher(this.objectName2, this.att1, this.att3, this.att4))))
                .thenReturn((List) Arrays.asList(this.mockJ4pReadResponse1, this.mockJ4pReadResponse2));

        //
        // EXECUTE
        //
        Map<ObjectName, List<Attribute>> result = this.jolokiaConnection.batchQueryAttributes(requestParameters);


        //
        // VALIDATE
        //
        assertEquals(2, result.size());
        assertAttributesMatch(result.get(this.objectName1), new Attribute(this.att1, this.value1),
                new Attribute(this.att2, this.value2));
        assertAttributesMatch(result.get(this.objectName2), new Attribute(this.att1, this.value1),
                new Attribute(this.att3, this.value3), new Attribute(this.att4, this.value4));
    }

    /**
     * Verify handling of a queryNames() operation.
     * @throws Exception
     */
    @Test
    public void testQueryNames() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //
        List<ObjectName> objectNames = new LinkedList<>();

        ObjectName testName1 = new ObjectName("x-domain-x:x-key-x=1");
        ObjectName testName2 = new ObjectName("x-domain-x:x-key-x=2");

        objectNames.add(testName1);
        objectNames.add(testName2);

        Mockito.when(this.mockJ4pClient.execute(this.matchSearchRequest(this.objectName1)))
                .thenReturn(this.mockJ4pSearchResponse);
        Mockito.when(this.mockJ4pSearchResponse.getObjectNames()).thenReturn(objectNames);


        //
        // EXECUTE
        //
        Set<ObjectName> result = this.jolokiaConnection.queryNames(this.objectName1, this.queryExp);


        //
        // VALIDATE
        //
        assertEquals(2, result.size());
        assertTrue(result.contains(testName1));
        assertTrue(result.contains(testName2));
    }

    /**
     * Verify handling of the close() method.
     *
     * @throws Exception
     */
    @Test
    public void testClose() throws Exception {
        // No-op
        this.jolokiaConnection.close();
    }

    /**
     * Verify handling of exceptions during execution of a getAttributes() request.
     *
     * @throws Exception
     */
    @Test
    public void testGetAttributesException() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //

        J4pException j4pExc = new J4pException("x-j4p-exc-x");

        Mockito.when(this.mockJ4pClient.execute(this.matchReadRequest(this.objectName1, this.att1, this.att2)))
                .thenThrow(j4pExc);

        try {
            //
            // EXECUTE
            //
            this.jolokiaConnection.getAttributes(this.objectName1, this.att1, this.att2);
            fail("missing expected exception");
        } catch (IOException thrown) {
            //
            // VALIDATE
            //
            assertSame(j4pExc, thrown.getCause());
            assertEquals("jolokia request failure", thrown.getMessage());
        }
    }

    /**
     * Verify handling of an exception during processing of a batch request.
     *
     * @throws Exception
     */
    @Test
    public void testBatchQueryAttributesException() throws Exception {
        J4pException j4pExc = new J4pException("x-j4p-exc-x");

        Mockito.when(this.mockJ4pClient.execute(Mockito.anyList())).thenThrow(j4pExc);

        try {
            this.jolokiaConnection.batchQueryAttributes(new HashMap<ObjectName, List<String>>());
            fail("missing expected exception");
        } catch (IOException thrown) {
            assertSame(j4pExc, thrown.getCause());
            assertEquals("jolokia request failure", thrown.getMessage());
        }
    }

    /**
     * Verify handling of a batch request for which only part of the request is successful.
     *
     * @throws Exception
     */
    @Test
    public void testCopyPartialOnBatchException() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //

        // Partial response - only 1 read-response of the 2; another exception
        J4pRemoteException response2Exc = new J4pRemoteException(null, "x-exc-x", null, 0, null, null);
        List responseList = Arrays.asList(this.mockJ4pReadResponse1, response2Exc);

        J4pBulkRemoteException bulkRemoteException = new J4pBulkRemoteException(responseList);

        Map<ObjectName, List<String>> requestParameters = new HashMap<>();

        requestParameters.put(this.objectName1, Arrays.asList(this.att1, this.att2));
        requestParameters.put(this.objectName2, Arrays.asList(this.att1, this.att3, this.att4));

        Mockito.when(this.mockJ4pClient.execute(this.matchReadRequestList(
                this.createJ4pReadRequestMatcher(this.objectName1, this.att1, this.att2),
                this.createJ4pReadRequestMatcher(this.objectName2, this.att1, this.att3, this.att4))))
                .thenThrow(bulkRemoteException);

        this.jolokiaConnection.setLog(this.mockLogger);


        //
        // EXECUTE
        //
        Map<ObjectName, List<Attribute>> result = this.jolokiaConnection.batchQueryAttributes(requestParameters);


        //
        // VALIDATE
        //
        assertEquals(1, result.size());
        assertAttributesMatch(result.get(this.objectName1), new Attribute(this.att1, this.value1),
                new Attribute(this.att2, this.value2));
        Mockito.verify(this.mockLogger).info("error on element of a bulk query", response2Exc);
    }

    /**
     * Validate handling of an invalid value type in the response data from a RemoteException.  Only J4pResponse and
     * Exception objects are expected.
     *
     * @throws Exception
     */
    @Test
    public void testBatchQueryBadResonseData() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //
        J4pBulkRemoteException mockRemoteException = Mockito.mock(J4pBulkRemoteException.class);
        Map<ObjectName, List<String>> requestParameters = new HashMap<>();

        Mockito.when(mockRemoteException.getResponses()).thenReturn((List) Arrays.asList("x-invalid-data-x"));

        Mockito.when(this.mockJ4pClient.execute(Mockito.anyList())).thenThrow(mockRemoteException);

        this.jolokiaConnection.setLog(this.mockLogger);


        //
        // EXECUTE
        //
        this.jolokiaConnection.batchQueryAttributes(requestParameters);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockLogger).info("unexpected response on element of a bulk query: class={}",
                String.class.getName());


        //
        // AGAIN WITH DEBUG LOGGING ENABLED
        //
        Mockito.when(this.mockLogger.isDebugEnabled()).thenReturn(true);
        this.jolokiaConnection.batchQueryAttributes(requestParameters);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockLogger).info("unexpected response on element of a bulk query: class={}; object={}",
                String.class.getName(), "x-invalid-data-x");
    }

    /**
     * Validate handling of a null value in the response data from a RemoteException.
     * @throws Exception
     */
    @Test
    public void testBatchQueryNullResponseData() throws Exception {
        //
        // SETUP TEST DATA AND INTERACTIONS
        //
        J4pBulkRemoteException mockRemoteException = Mockito.mock(J4pBulkRemoteException.class);
        Map<ObjectName, List<String>> requestParameters = new HashMap<>();

        List listWithNull = new LinkedList();
        listWithNull.add(null);

        Mockito.when(mockRemoteException.getResponses()).thenReturn(listWithNull);

        Mockito.when(this.mockJ4pClient.execute(Mockito.anyList())).thenThrow(mockRemoteException);

        this.jolokiaConnection.setLog(this.mockLogger);


        //
        // EXECUTE
        //
        this.jolokiaConnection.batchQueryAttributes(requestParameters);


        //
        // VALIDATE
        //
        Mockito.verify(this.mockLogger).info("unexpected null response on element of a bulk query");
    }

    /**
     * Validate handling of an exception thrown during process of the queryNames() method.
     * @throws Exception
     */
    @Test
    public void testQueryNamesException() throws Exception {
        J4pException j4pExc = new J4pException("x-j4p-exc-x");

        Mockito.when(this.mockJ4pClient.execute(this.matchSearchRequest(this.objectName1)))
                .thenThrow(j4pExc);

        try {
            Set<ObjectName> result = this.jolokiaConnection.queryNames(this.objectName1, this.queryExp);
            fail("missing expected exception");
        } catch (IOException caught) {
            assertSame(j4pExc, caught.getCause());
            assertEquals("jolokia request failure", caught.getMessage());
        }
    }

    /**
     * Create a matcher for a J4pReadRequest using the given object name and attribute names.
     *
     * @param expectedObjName object name to match on the J4pReadRequest.
     * @param expectedAttributes list of attributes expected in the J4pReadRequest.
     * @return Mockito matcher for J4pReadRequest objects.
     */
    protected J4pReadRequest matchReadRequest(final ObjectName expectedObjName, final String... expectedAttributes) {
        ArgumentMatcher<J4pReadRequest> argMatcher = createJ4pReadRequestMatcher(expectedObjName, expectedAttributes);

        return Mockito.argThat(argMatcher);
    }

    /**
     * Create a matcher for a list of J4pReadRequest objects.
     * Note that order does not matter; matching keys off of object name.
     *
     * @param expectedReadRequests list of read request matchers for the expected read requests in the list.
     * @return Mockito argument matcher for the list of J4pReadRequest objects.
     */
    protected List<J4pReadRequest> matchReadRequestList(final J4pReadRequestMatcher... expectedReadRequests) {
        //
        // Create a MAP for the matches keyed on object name.
        //
        ArgumentMatcher<List<J4pReadRequest>> argMatcher;

        final Map<ObjectName, J4pReadRequestMatcher> expectedMap = new HashMap<>();
        for (J4pReadRequestMatcher oneRequest : expectedReadRequests) {
            expectedMap.put(oneRequest.getObjectName(), oneRequest);
        }

        //
        // Create the argument matcher
        //
        argMatcher = new ArgumentMatcher<List<J4pReadRequest>>() {
            @Override
            public boolean matches(Object other) {
                // Make sure the value is a List.
                if (other instanceof List) {
                    List otherList = (List) other;

                    // Make sure the right number of values was received.
                    if (otherList.size() != expectedReadRequests.length) {
                        return false;
                    }

                    // Verify that each of the values received matches as-expected.
                    for (Object value : otherList) {
                        if (value instanceof J4pReadRequest) {
                            ObjectName actualObjectName = ((J4pReadRequest) value).getObjectName();
                            J4pReadRequestMatcher matcher = expectedMap.get(actualObjectName);

                            if (!matcher.matches(value)) {
                                return false;
                            }

                            // Prevent matching more than once on the same matcher
                            expectedMap.remove(actualObjectName);
                        } else {
                            return false;
                        }
                    }

                    return true;
                }

                return false;
            }
        };

        return Mockito.argThat(argMatcher);
    }

    /**
     * Create a matcher for a J4pReadRequest for the specified object name and list of attribute names.
     *
     * @param expectedObjName    object name expected in the J4pReadRequest.
     * @param expectedAttributes names of attributes expected in the request.
     * @return Mockito argument matcher for J4pReadRequests.
     */
    protected J4pReadRequestMatcher createJ4pReadRequestMatcher(final ObjectName expectedObjName,
                                                                final String... expectedAttributes) {
        J4pReadRequestMatcher argMatcher;
        argMatcher = new J4pReadRequestMatcher(expectedObjName) {
            @Override
            public boolean matches(Object other) {
                if (other instanceof J4pReadRequest) {
                    J4pReadRequest candidate = (J4pReadRequest) other;
                    Collection<String> atts = candidate.getAttributes();

                    // Does the expected object name match, and the number of attributes?
                    if (expectedObjName.equals(candidate.getObjectName()) && (atts.size() == expectedAttributes.length)) {
                        // Loop through all the expected attributes and see if any is missing in the actual attributes.
                        for (String oneAtt : expectedAttributes) {
                            if (!atts.contains(oneAtt)) {
                                return false;
                            }
                        }

                        // All there, this is a match.
                        return true;
                    }
                }
                return false;
            }
        };

        return argMatcher;
    }

    /**
     * Create a matcher for a J4pSearchRequest with the given search pattern.
     *
     * @param searchPattern search pattern to expect from the J4pSearchRequest.
     * @return argument matcher for use with Mockito.
     */
    protected J4pSearchRequest matchSearchRequest(final ObjectName searchPattern) {
        ArgumentMatcher<J4pSearchRequest> argMatcher;
        argMatcher = new ArgumentMatcher<J4pSearchRequest>() {
            @Override
            public boolean matches(Object other) {
                if (other instanceof J4pSearchRequest) {
                    J4pSearchRequest candidate = (J4pSearchRequest) other;
                    if (searchPattern.equals(candidate.getObjectName())) {
                        return true;
                    }
                }
                return false;
            }
        };

        return Mockito.argThat(argMatcher);
    }

    /**
     * Confirm that the list of attributes given matches the expected list passed as varags.
     *
     * @param attributeList list of attributes actually received.
     * @param expected      list of expected attributes.
     */
    protected void assertAttributesMatch(List<Attribute> attributeList, Attribute... expected) {
        // Verify the actual count matches the expected count.
        assertEquals(attributeList.size(), expected.length);

        // Create a set as the lists are not necessarily ordered.
        Set<Attribute> attributeSet = new HashSet<>(attributeList);

        // Now verify all of the expected attributes exist in the set.
        for (Attribute oneExpected : expected) {
            assertTrue(attributeSet.contains(oneExpected));
        }
    }

    /**
     * Argument matcher for J4pReadRequest objects that can be queried for the ObjectName.
     */
    protected abstract class J4pReadRequestMatcher extends ArgumentMatcher<J4pReadRequest> {
        private final ObjectName objectName;

        public J4pReadRequestMatcher(ObjectName objectName) {
            this.objectName = objectName;
        }

        public ObjectName getObjectName() {
            return objectName;
        }
    }
}
