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

import com.amlinv.jmxutil.connection.*;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Created by art on 8/18/15.
 */
public class JMXJvmIdConnectionFactoryTest {
    private JMXJvmIdConnectionFactory factory;

    private VirtualMachine mockVirtualMachine;

    private Logger mockLogger;
    private VirtualMachineAttacher mockAttacher;
    private JMXServiceUrlFactory mockJmxServiceUrlFactory;
    private JMXConnectorFactoryDelegate mockJmxConnectorFactoryDelegate;
    private JMXMBeanConnectionFactory mockJmxMbeanConnectorFactory;
    private JMXServiceURL mockJmxServiceUrl;
    private JMXConnector mockJmxConnector;
    private JMXMBeanConnection mockJmxMbeanConnection;

    private Properties mockAgentProperties;
    private Properties mockSystemProperties;

    @Before
    public void setupTest() throws Exception {
        this.factory = new JMXJvmIdConnectionFactory("x-jvmid-x");

        this.mockVirtualMachine = Mockito.mock(VirtualMachine.class);

        this.mockLogger = Mockito.mock(Logger.class);
        this.mockAttacher = Mockito.mock(VirtualMachineAttacher.class);
        this.mockJmxServiceUrlFactory = Mockito.mock(JMXServiceUrlFactory.class);
        this.mockJmxConnectorFactoryDelegate = Mockito.mock(JMXConnectorFactoryDelegate.class);
        this.mockJmxMbeanConnectorFactory = Mockito.mock(JMXMBeanConnectionFactory.class);

        this.mockJmxServiceUrl = Mockito.mock(JMXServiceURL.class);
        this.mockJmxConnector = Mockito.mock(JMXConnector.class);
        this.mockJmxMbeanConnection = Mockito.mock(JMXMBeanConnection.class);

        this.mockAgentProperties = Mockito.mock(Properties.class);
        this.mockSystemProperties = Mockito.mock(Properties.class);

        Mockito.when(this.mockJmxServiceUrlFactory.createJMXServiceUrl("x-url-x")).thenReturn(this.mockJmxServiceUrl);
        Mockito.when(this.mockJmxConnectorFactoryDelegate.connect(this.mockJmxServiceUrl))
                .thenReturn(this.mockJmxConnector);
        Mockito.when(this.mockJmxMbeanConnectorFactory.create(this.mockJmxConnector))
                .thenReturn(this.mockJmxMbeanConnection);
    }

    @Test
    public void testGetSetLog () {
        assertNotNull(this.factory.getLog());
        assertNotSame(this.mockLogger, this.factory.getLog());

        this.factory.setLog(this.mockLogger);
        assertSame(this.mockLogger, this.factory.getLog());
    }

    @Test
    public void testGetSetAttacher() {
        assertTrue(this.factory.getAttacher() instanceof DefaultVirtualMachineAttacher);

        this.factory.setAttacher(this.mockAttacher);
        assertSame(this.mockAttacher, this.factory.getAttacher());
    }

    @Test
    public void testGetSetJmxServiceFactory () {
        assertTrue(this.factory.getJmxServiceUrlFactory() instanceof DefaultJmxServiceUrlFactory);

        this.factory.setJmxServiceUrlFactory(this.mockJmxServiceUrlFactory);
        assertSame(this.mockJmxServiceUrlFactory, this.factory.getJmxServiceUrlFactory());
    }

    @Test
    public void testGetSetJmxConnectorFactoryDelegate () {
        assertTrue(this.factory.getJmxConnectorFactoryDelegate() instanceof DefaultJmxConnectorFactoryDelegate);

        this.factory.setJmxConnectorFactoryDelegate(this.mockJmxConnectorFactoryDelegate);
        assertSame(this.mockJmxConnectorFactoryDelegate, this.factory.getJmxConnectorFactoryDelegate());
    }

    @Test
    public void testGetSetJmxMbeanConnectorFactory () {
        assertTrue(this.factory.getJmxMBeanConnectionFactory() instanceof DefaultJmxMBeanConnectionFactory);

        this.factory.setJmxMBeanConnectionFactory(this.mockJmxMbeanConnectorFactory);
        assertSame(this.mockJmxMbeanConnectorFactory, this.factory.getJmxMBeanConnectionFactory());
    }

    @Test
    public void testCreateConnection() throws Exception {
        this.initFactory();

        Mockito.when(this.mockAttacher.attach("x-jvmid-x")).thenReturn(this.mockVirtualMachine);
        Mockito.when(this.mockVirtualMachine.getAgentProperties()).thenReturn(this.mockAgentProperties);
        Mockito.when(this.mockVirtualMachine.getSystemProperties()).thenReturn(this.mockSystemProperties);

        Mockito.when(
                this.mockAgentProperties
                        .getProperty(JMXJvmIdConnectionFactory.COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY))
                .thenReturn("x-url-x");

        MBeanAccessConnection result = this.factory.createConnection();

        assertSame(result, this.mockJmxMbeanConnection);

    }

    @Test
    public void testCreateConnectionUsingSystemProperties() throws Exception {
        this.initFactory();

        Mockito.when(this.mockAttacher.attach("x-jvmid-x")).thenReturn(this.mockVirtualMachine);
        Mockito.when(this.mockVirtualMachine.getAgentProperties()).thenReturn(this.mockAgentProperties);
        Mockito.when(this.mockVirtualMachine.getSystemProperties()).thenReturn(this.mockSystemProperties);

        Mockito.when(
                this.mockAgentProperties
                        .getProperty(JMXJvmIdConnectionFactory.COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY))
                .thenReturn(null)
                .thenReturn("x-url-x");
        Mockito.when(this.mockSystemProperties.getProperty("java.home")).thenReturn("x-java-home-x");

        MBeanAccessConnection result = this.factory.createConnection();

        assertSame(result, this.mockJmxMbeanConnection);
        Mockito.verify(this.mockVirtualMachine)
                .loadAgent("x-java-home-x" + File.separator + "lib" + File.separator + "management-agent.jar");
    }

    @Test
    public void testCreateConnectionFailedToGetUrl() throws Exception {
        this.initFactory();

        Mockito.when(this.mockAttacher.attach("x-jvmid-x")).thenReturn(this.mockVirtualMachine);
        Mockito.when(this.mockVirtualMachine.getAgentProperties()).thenReturn(this.mockAgentProperties);
        Mockito.when(this.mockVirtualMachine.getSystemProperties()).thenReturn(this.mockSystemProperties);

        Mockito.when(
                this.mockAgentProperties
                        .getProperty(JMXJvmIdConnectionFactory.COM_SUN_LOCAL_CONNECTOR_ADDRESS_PROPERTY))
                .thenReturn(null);

        MBeanAccessConnection result = this.factory.createConnection();
        assertNull(result);
        Mockito.verify(this.mockLogger).warn("failed to find the local connection url for jvm: jvmId={}", "x-jvmid-x");
    }

    @Test
    public void testAssertOnConnect() throws Exception {
        AttachNotSupportedException ansExc = new AttachNotSupportedException("x-attach-not-supported-exc-x");

        Mockito.when(this.mockAttacher.attach("x-jvmid-x")).thenThrow(ansExc);

        this.initFactory();

        MBeanAccessConnection result = this.factory.createConnection();
        assertNull(result);
        Mockito.verify(this.mockLogger).warn("failed to connect to jvm: jvmId={}", "x-jvmid-x", ansExc);
    }

    @Test
    public void testGetTargetDescription() throws Exception {
        assertEquals("jvmId=x-jvmid-x", this.factory.getTargetDescription());
    }

    protected void initFactory () {
        this.factory.setAttacher(this.mockAttacher);
        this.factory.setJmxServiceUrlFactory(this.mockJmxServiceUrlFactory);
        this.factory.setJmxConnectorFactoryDelegate(this.mockJmxConnectorFactoryDelegate);
        this.factory.setJmxMBeanConnectionFactory(this.mockJmxMbeanConnectorFactory);
        this.factory.setLog(this.mockLogger);
    }
}
