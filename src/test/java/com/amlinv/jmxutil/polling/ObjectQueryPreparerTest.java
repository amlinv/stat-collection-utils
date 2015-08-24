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

import com.amlinv.jmxutil.MBeanLocationParameterSource;
import com.amlinv.jmxutil.annotation.MBeanAnnotationUtil;
import com.amlinv.jmxutil.annotation.MBeanAttribute;
import com.amlinv.jmxutil.annotation.MBeanLocation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Validate the preparer for object queries.
 *
 * Created by art on 8/22/15.
 */
public class ObjectQueryPreparerTest {
    public static final String TEST_ONAME_STR001 = "x-domain-x:x-key1-x=x-value1-x";
    public static final String TEST_ONAME_STR002 = "x-domain-x:x-key2-x=${x-param-x}";
    public static final String TEST_ONAME_STR002B = "x-domain-x:x-key2-x=x-value2-x";
    public static final String TEST_ONAME_STR003 = "x-domain-x:x-key3-x=x-value3-x";
    public static final String TEST_ATT_NAME001 = "x-att001-x";
    public static final String TEST_ATT_NAME002 = "x-att002-x";

    private ObjectQueryPreparer preparer;

    private Logger mockLog;
    private ParameterReplacer mockParameterReplacer;

    private TestClass001 testTarget001;
    private TestClass002 testTarget002;
    private TestClass003 testTarget003;

    /**
     * Setup common test data and interactions.
     *
     * @throws Exception
     */
    @Before
    public void setupTest() throws Exception {
        this.preparer = new ObjectQueryPreparer();

        this.mockLog = Mockito.mock(Logger.class);
        this.mockParameterReplacer = Mockito.mock(ParameterReplacer.class);

        this.testTarget001 = new TestClass001();
        this.testTarget002 = new TestClass002();
        this.testTarget003 = new TestClass003();
    }

    /**
     * Test getting and setting of the logger.
     *
     * @throws Exception
     */
    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.preparer.getLog());
        assertNotSame(this.mockLog, this.preparer.getLog());

        this.preparer.setLog(this.mockLog);
        assertSame(this.mockLog, this.preparer.getLog());
    }

    /**
     * Test getting and setting the parameter replacer.
     *
     * @throws Exception
     */
    @Test
    public void testGetSetParameterReplacer() throws Exception {
        assertNotNull(this.preparer.getParameterReplacer());
        assertNotSame(this.mockParameterReplacer, this.preparer.getParameterReplacer());

        this.preparer.setParameterReplacer(this.mockParameterReplacer);
        assertSame(this.mockParameterReplacer, this.preparer.getParameterReplacer());
    }

    /**
     * Test preparation of a target object with basic annotations.
     *
     * @throws Exception
     */
    @Test
    public void testPrepareObjectQuery() throws Exception {
        ObjectQueryInfo result;

        this.initPreparer();

        result = this.preparer.prepareObjectQuery(this.testTarget001);

        assertSame(this.testTarget001, result.getTarget());
        Map<String, Method> setters = result.getAttributeSetters();
        assertEquals(2, setters.size());
        assertEquals(TestClass001.class.getMethod("setAttribute1", String.class), setters.get(TEST_ATT_NAME001));
        assertEquals(TestClass001.class.getMethod("setAttribute2", String.class), setters.get(TEST_ATT_NAME002));
    }

    /**
     * Test preparation of an object using parameter replacement.
     *
     * @throws Exception
     */
    @Test
    public void testPrepareObjectQueryWithParameterReplacement() throws Exception {
        ObjectQueryInfo result;

        this.initPreparer();

        Mockito.when(this.mockParameterReplacer.replaceObjectNameParameters(TEST_ONAME_STR002, this.testTarget002))
                .thenReturn(TEST_ONAME_STR002B);

        result = this.preparer.prepareObjectQuery(this.testTarget002);

        assertSame(this.testTarget002, result.getTarget());
        assertEquals(new ObjectName(this.TEST_ONAME_STR002B), result.getObjectName());

        Map<String, Method> setters = result.getAttributeSetters();
        assertEquals(2, setters.size());
        assertEquals(TestClass002.class.getMethod("setAttribute1", String.class), setters.get(TEST_ATT_NAME001));
        assertEquals(TestClass002.class.getMethod("setAttribute2", String.class), setters.get(TEST_ATT_NAME002));
    }

    /**
     * Test preparation of an object which has no mbean location defined.
     *
     * @throws Exception
     */
    @Test
    public void testPrepareObjectNoMBeanLocation() throws Exception {
        this.initPreparer();

        ObjectQueryInfo result = this.preparer.prepareObjectQuery("no-query-data");

        assertNull(result);
        Mockito.verify(this.mockLog).warn("ignoring attempt to prepare to poll object that has no MBeanLocation");
    }

    /**
     * Test preparation of an object which has no setters defined.
     *
     * @throws Exception
     */
    @Test
    public void testPrepareObjectNoSetters() throws Exception {
        this.initPreparer();

        ObjectQueryInfo result = this.preparer.prepareObjectQuery(this.testTarget003);

        assertNull(result);
        Mockito.verify(this.mockLog)
                .warn("ignoring attempt to prepare to poll an MBean object with no attributes: onamePattern={}",
                       new Object[] { this.TEST_ONAME_STR003 });
    }


                                    ////             ////
                                    ////  INTERNALS  ////
                                    ////             ////

    /**
     * Initialize the preparer for testing.
     */
    protected void initPreparer () {
        this.preparer.setParameterReplacer(this.mockParameterReplacer);
        this.preparer.setLog(this.mockLog);
    }

    /**
     * Test class with basic MBeanLocation and MBeanAttribute definitions.
     */
    @MBeanLocation(onamePattern = TEST_ONAME_STR001)
    protected static class TestClass001 {
        private String attribute1;
        private String attribute2;

        @MBeanAttribute(name = TEST_ATT_NAME001, type = String.class)
        public void setAttribute1 (String value) {
            this.attribute1 = value;
        }

        @MBeanAttribute(name = TEST_ATT_NAME002, type = String.class)
        public void setAttribute2 (String value) {
            this.attribute2 = value;
        }
    }

    /**
     * Test class which implements MBeanLocationParameterSource
     */
    @MBeanLocation(onamePattern = TEST_ONAME_STR002)
    protected static class TestClass002 implements MBeanLocationParameterSource {
        private String attribute1;
        private String attribute2;

        @MBeanAttribute(name = TEST_ATT_NAME001, type = String.class)
        public void setAttribute1 (String value) {
            this.attribute1 = value;
        }

        @MBeanAttribute(name = TEST_ATT_NAME002, type = String.class)
        public void setAttribute2 (String value) {
            this.attribute2 = value;
        }

        @Override
        public String getParameter(String parameterName) {
            return "y-" + parameterName + "-value-y";
        }
    }

    /**
     * Test class with no setter methods.
     */
    @MBeanLocation(onamePattern =  TEST_ONAME_STR003)
    protected static class TestClass003 {
    }
}
