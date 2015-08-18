/**
  * Copyright 2015 AML Innovation & Consulting LLC
  *
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package com.amlinv.jmxutil.connection.impl;

import com.amlinv.jmxutil.connection.MBeanAccessConnection;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public class JMXMBeanConnection implements MBeanAccessConnection {
    private final JMXConnector jmxConnector;
    private final MBeanServerConnection mBeanServerConnection;

    public JMXMBeanConnection(JMXConnector jmxConnector) throws IOException {
        this.jmxConnector = jmxConnector;
        this.mBeanServerConnection = this.jmxConnector.getMBeanServerConnection();
    }

    @Override
    public List<Attribute> getAttributes(ObjectName objectName, String... attributeNames)
            throws InstanceNotFoundException, IOException, ReflectionException {

        return this.mBeanServerConnection.getAttributes(objectName, attributeNames).asList();
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName pattern, QueryExp query) throws IOException {
        return this.mBeanServerConnection.queryNames(pattern, query);
    }

    @Override
    public void close() throws IOException {
        this.jmxConnector.close();
    }
}
