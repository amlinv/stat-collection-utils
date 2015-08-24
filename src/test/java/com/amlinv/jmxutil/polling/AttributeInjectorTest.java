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

import com.sun.jdi.InvocationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.management.Attribute;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Validate operation of the AttributeExtractor
 * Created by art on 8/22/15.
 */
public class AttributeInjectorTest {
    private AttributeInjector injector;

    private TestTarget target;
    private List<Attribute> attributeList;
    private Map<String, Method> attributeSetters;

    private ObjectName objectName;

    private Logger mockLogger;

    @Before
    public void setupTest() throws Exception {
        this.mockLogger = Mockito.mock(Logger.class);

        this.injector = new AttributeInjector();

        this.target = new TestTarget();
        this.attributeList = new LinkedList<>();
        this.attributeSetters = new HashMap<>();

        this.objectName = new ObjectName("x-domain-x:x-key-x=x-value-x");

        this.attributeList.add(new Attribute("x-att-str-name-x", "x-att-str-value-x"));
        this.attributeList.add(new Attribute("x-att-int-name-x", 7));
        this.attributeList.add(new Attribute("x-att-Integer-name-x", 11));

        this.attributeSetters.put("x-att-str-name-x", TestTarget.class.getMethod("setStringValue", String.class));
        this.attributeSetters.put("x-att-int-name-x", TestTarget.class.getMethod("setIntValue", int.class));
        this.attributeSetters.put("x-att-Integer-name-x", TestTarget.class.getMethod("setIntegerValue", Integer.class));
        this.attributeSetters.put("x-throw-x", TestTarget.class.getMethod("throwWhenCalled", String.class));
        this.attributeSetters.put("x-inaccessible-x", TestTarget.class.getDeclaredMethod("inaccessible", String.class));

        this.injector.setLog(this.mockLogger);
    }

    @Test
    public void testGetSetLog() throws Exception {
        this.injector = new AttributeInjector();  // Start fresh

        assertNotNull(this.injector.getLog());
        assertNotSame(this.mockLogger, this.injector.getLog());

        this.injector.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.injector.getLog());
    }

    @Test
    public void testCopyOutAttributes() throws Exception {
        this.injector.copyOutAttributes(this.target, this.attributeList, this.attributeSetters, this.objectName);

        assertEquals("x-att-str-value-x", this.target.stringValue);
        assertEquals(7, this.target.intValue);
        assertEquals(Integer.valueOf(11), this.target.integerValue);
    }

    @Test
    public void testCopyOutWithLongForIntFields() throws Exception {
        this.attributeList.clear();

        this.attributeList.add(new Attribute("x-att-int-name-x", 17L));
        this.attributeList.add(new Attribute("x-att-Integer-name-x", 22L));

        this.injector.copyOutAttributes(this.target, this.attributeList, this.attributeSetters, this.objectName);

        assertEquals(17, this.target.intValue);
        assertEquals(Integer.valueOf(22), this.target.integerValue);
    }

    @Test
    public void testInvocationExceptionOnSetter() throws Exception {
        this.attributeList.clear();
        this.attributeList.add(new Attribute("x-throw-x", "x-ignored-x"));

        this.injector.copyOutAttributes(this.target, this.attributeList, this.attributeSetters, this.objectName);

        Mockito.verify(this.mockLogger)
                .info(Mockito.eq("invocation exception storing mbean results: {}={}; attributeName={}"),
                        Mockito.eq("oname"), Mockito.eq(objectName), Mockito.eq("x-throw-x"),
                        Mockito.any(InvocationException.class));
    }

    @Test
    public void testIllegalArgumentOnSetter() throws Exception {
        this.attributeList.clear();
        this.attributeList.add(new Attribute("x-att-str-name-x", 0));

        this.injector.copyOutAttributes(this.target, this.attributeList, this.attributeSetters, this.objectName);

        Mockito.verify(this.mockLogger)
                .info(Mockito.eq("illegal argument exception storing mbean results: {}={}; attributeName={}"),
                        Mockito.eq("oname"), Mockito.eq(objectName), Mockito.eq("x-att-str-name-x"),
                        Mockito.any(IllegalArgumentException.class));
    }

    @Test
    public void testIllegalAccessOnSetter() throws Exception {
        this.attributeList.clear();
        this.attributeList.add(new Attribute("x-inaccessible-x", ""));

        this.injector.copyOutAttributes(this.target, this.attributeList, this.attributeSetters, this.objectName);

        Mockito.verify(this.mockLogger)
                .info(Mockito.eq("illegal access exception storing mbean results: {}={}; attributeName={}"),
                        Mockito.eq("oname"), Mockito.eq(objectName), Mockito.eq("x-inaccessible-x"),
                        Mockito.any(IllegalAccessException.class));
    }

    protected static class TestTarget {
        public String stringValue;
        public int intValue;
        public Integer integerValue;

        public void setStringValue(String value) {
            this.stringValue = value;
        }

        public void setIntValue(int value) {
            this.intValue = value;
        }

        public void setIntegerValue(Integer value) {
            this.integerValue = value;
        }

        public void throwWhenCalled(String ignored) {
            throw new RuntimeException("x-rt-exc-x");
        }

        private void inaccessible(String ignored) {
        }
    }
}