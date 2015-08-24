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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.management.*;
import javax.management.remote.JMXConnector;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by art on 8/18/15.
 */
public class JMXMBeanConnectionTest {

    private JMXMBeanConnection connection;

    private JMXConnector mockJmxConnector;
    private MBeanServerConnection mockMBeanServerConnection;
    private AttributeList mockAttributeList;
    private QueryExp mockQueryExp;

    private ObjectName objectName;
    private List<Attribute> testAttributeList;
    private Set<ObjectName> testQueryResult;

    @Before
    public void setupTest() throws Exception {
        this.mockJmxConnector = Mockito.mock(JMXConnector.class);

        this.mockMBeanServerConnection = Mockito.mock(MBeanServerConnection.class);
        this.mockAttributeList = Mockito.mock(AttributeList.class);
        this.mockQueryExp = Mockito.mock(QueryExp.class);

        this.objectName = new ObjectName("x-domain-x:x-object-key-x=x-object-value-x");
        this.testAttributeList = new LinkedList<>();

        Mockito.when(this.mockJmxConnector.getMBeanServerConnection()).thenReturn(this.mockMBeanServerConnection);
        Mockito.when(this.mockMBeanServerConnection.getAttributes(this.objectName, new String[] { "x-att-x" }))
                .thenReturn(this.mockAttributeList);
        Mockito.when(this.mockAttributeList.asList()).thenReturn(this.testAttributeList);
        Mockito.when(this.mockMBeanServerConnection.queryNames(this.objectName, this.mockQueryExp))
                .thenReturn(this.testQueryResult);

        this.connection = new JMXMBeanConnection(this.mockJmxConnector);
    }

    @Test
    public void testGetAttributes() throws Exception {
        List<Attribute> result = this.connection.getAttributes(this.objectName, "x-att-x");

        assertSame(this.testAttributeList, result);
    }

    @Test
    public void testQueryNames() throws Exception {
        Set<ObjectName> result = this.connection.queryNames(this.objectName, this.mockQueryExp);

        assertSame(this.testQueryResult, result);
    }

    @Test
    public void testClose() throws Exception {
        this.connection.close();

        Mockito.verify(this.mockJmxConnector).close();
    }
}