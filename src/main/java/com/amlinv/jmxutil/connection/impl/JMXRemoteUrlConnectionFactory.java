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
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Factory creating MBeanAccessConnection objects for JMX using remote URLs.
 *
 * Created by art on 4/1/15.
 */
public class JMXRemoteUrlConnectionFactory implements MBeanAccessConnectionFactory {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JMXRemoteUrlConnectionFactory.class);

    private final JMXServiceURL url;
    private JMXConnectorFactoryDelegate connectorFactoryDelegate;
    private JMXMBeanConnectionFactory jmxMBeanConnectionFactory;
    private Logger log = DEFAULT_LOGGER;

    public JMXRemoteUrlConnectionFactory(JMXServiceURL url) {
        this.url = url;

        this.connectorFactoryDelegate = new DefaultJmxConnectorFactoryDelegate();
        this.jmxMBeanConnectionFactory = new DefaultJmxMBeanConnectionFactory();
    }

    public JMXRemoteUrlConnectionFactory(String urlString) throws MalformedURLException {
        this(new JMXServiceURL(urlString));
    }

    public JMXConnectorFactoryDelegate getConnectorFactoryDelegate() {
        return connectorFactoryDelegate;
    }

    public void setConnectorFactoryDelegate(JMXConnectorFactoryDelegate connectorFactoryDelegate) {
        this.connectorFactoryDelegate = connectorFactoryDelegate;
    }

    public JMXMBeanConnectionFactory getJmxMBeanConnectionFactory() {
        return jmxMBeanConnectionFactory;
    }

    public void setJmxMBeanConnectionFactory(JMXMBeanConnectionFactory jmxMBeanConnectionFactory) {
        this.jmxMBeanConnectionFactory = jmxMBeanConnectionFactory;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    @Override
    public MBeanAccessConnection createConnection() throws IOException {
        JMXConnector jmxConnector = this.connectorFactoryDelegate.connect(this.url);

        boolean success = false;
        try {
            JMXMBeanConnection result = this.jmxMBeanConnectionFactory.create(jmxConnector);
            success = true;

            return  result;
        } finally {
            if ( ! success ) {
                try {
                    jmxConnector.close();
                } catch ( IOException ioExc ) {
                    this.log.info("IO exception closing jmx connector after mbean connection failure", ioExc);
                }
            } else {
                log.info("TBD999 XXX");
            }
        }
    }

    @Override
    public String getTargetDescription() {
        return "jmx:url=" + this.url;
    }
}
