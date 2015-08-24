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

import com.amlinv.jmxutil.connection.JMXConnectorFactoryDelegate;
import com.amlinv.jmxutil.connection.JMXMBeanConnectionFactory;
import com.amlinv.jmxutil.connection.MBeanAccessConnection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by art on 8/18/15.
 */
public class JMXRemoteUrlConnectionFactoryTest {

    private JMXRemoteUrlConnectionFactory factory;

    private JMXServiceURL mockJmxServiceUrl;
    private JMXConnectorFactoryDelegate mockConnectorFactoryDelegate;
    private JMXMBeanConnectionFactory mockJmxMBeanConnectionFactory;
    private JMXConnector mockJmxConnector;
    private JMXMBeanConnection mockJmxMBeanConnection;
    private Logger mockLog;

    @Before
    public void setupTest() throws Exception {
        this.mockJmxServiceUrl = Mockito.mock(JMXServiceURL.class);
        this.mockConnectorFactoryDelegate = Mockito.mock(JMXConnectorFactoryDelegate.class);
        this.mockJmxMBeanConnectionFactory = Mockito.mock(JMXMBeanConnectionFactory.class);
        this.mockJmxConnector = Mockito.mock(JMXConnector.class);
        this.mockJmxMBeanConnection = Mockito.mock(JMXMBeanConnection.class);
        this.mockLog = Mockito.mock(Logger.class);

        this.factory = new JMXRemoteUrlConnectionFactory(this.mockJmxServiceUrl);

        Mockito.when(this.mockConnectorFactoryDelegate.connect(this.mockJmxServiceUrl))
                .thenReturn(this.mockJmxConnector);
        Mockito.when(this.mockJmxMBeanConnectionFactory.create(this.mockJmxConnector))
                .thenReturn(this.mockJmxMBeanConnection);
    }

    @Test
    public void testAltConstructor() throws Exception {
        new JMXRemoteUrlConnectionFactory("service:jmx:rmi://127.0.0.1:1099/jndi/rmi://127.0.0.1:1099/jmxrmi");
    }

    @Test
    public void testGetSetConnectionFactoryDelegate () throws Exception {
        assertTrue(this.factory.getConnectorFactoryDelegate() instanceof DefaultJmxConnectorFactoryDelegate);

        this.factory.setConnectorFactoryDelegate(this.mockConnectorFactoryDelegate);
        assertSame(this.mockConnectorFactoryDelegate, this.factory.getConnectorFactoryDelegate());
    }

    @Test
    public void testGetSetJmxMbeanConnectionFactory() throws Exception {
        assertTrue(this.factory.getJmxMBeanConnectionFactory() instanceof DefaultJmxMBeanConnectionFactory);

        this.factory.setJmxMBeanConnectionFactory(this.mockJmxMBeanConnectionFactory);
        assertSame(this.mockJmxMBeanConnectionFactory, this.factory.getJmxMBeanConnectionFactory());
    }

    @Test
    public void testGetSetLog() throws Exception {
        assertNotNull(this.factory.getLog());
        assertNotSame(this.mockLog, this.factory.getLog());

        this.factory.setLog(this.mockLog);
        assertSame(this.mockLog, this.factory.getLog());
    }

    @Test
    public void testCreateConnection() throws Exception {
        this.initFactory();

        MBeanAccessConnection result = this.factory.createConnection();
        assertSame(this.mockJmxMBeanConnection, result);
    }

    @Test
    public void testCreateConnectionException() throws Exception {
        IOException ioExc = new IOException("x-io-exc-x");
        Mockito.when(this.mockJmxMBeanConnectionFactory.create(this.mockJmxConnector)).thenThrow(ioExc);

        this.initFactory();

        try {

            this.factory.createConnection();

            fail("missing expected exception");
        } catch (IOException thrown) {
            assertSame(ioExc, thrown);

            Mockito.verify(this.mockJmxConnector).close();
        }
    }

    @Test
    public void testExceptionOnCleanupCreateConnectionException() throws Exception {
        IOException ioExc1 = new IOException("x-io-exc1-x");
        IOException ioExc2 = new IOException("x-io-exc2-x");
        Mockito.when(this.mockJmxMBeanConnectionFactory.create(this.mockJmxConnector)).thenThrow(ioExc1);
        Mockito.doThrow(ioExc2).when(this.mockJmxConnector).close();

        this.initFactory();

        try {

            this.factory.createConnection();

            fail("missing expected exception");
        } catch (IOException thrown) {
            assertSame(ioExc1, thrown);

            Mockito.verify(this.mockJmxConnector).close();
            Mockito.verify(this.mockLog)
                    .info("IO exception closing jmx connector after mbean connection failure", ioExc2);
        }
    }

    @Test
    public void testGetTargetDescription() throws Exception {
        Mockito.when(this.mockJmxServiceUrl.toString()).thenReturn("x-jmx-service-url-x");

        assertEquals("jmx:url=x-jmx-service-url-x", this.factory.getTargetDescription());
    }

    protected void initFactory () {
        this.factory.setConnectorFactoryDelegate(this.mockConnectorFactoryDelegate);
        this.factory.setJmxMBeanConnectionFactory(this.mockJmxMBeanConnectionFactory);
        this.factory.setLog(this.mockLog);
    }
}