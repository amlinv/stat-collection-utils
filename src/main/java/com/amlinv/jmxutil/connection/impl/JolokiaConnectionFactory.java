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
import com.amlinv.jmxutil.connection.MBeanAccessConnectionFactory;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by art on 4/1/15.
 */
public class JolokiaConnectionFactory implements MBeanAccessConnectionFactory {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(JolokiaConnectionFactory.class);

    /**
     * Full URL for accessing Jolokia (e.g. http://localhost:8161/api/jolokia).
     */
    private final String jolokiaUrl;

    private Logger log = DEFAULT_LOGGER;

    public JolokiaConnectionFactory(String initJolokiaUrl) {
        this.jolokiaUrl = initJolokiaUrl;
    }

    @Override
    public MBeanAccessConnection createConnection() throws IOException {
        J4pClient client = J4pClient
                .url(this.jolokiaUrl)
                .pooledConnections()
                .maxConnectionPoolTimeout(30000)    // 30 seconds; default is 0.5 seconds -- far too short.
                .maxTotalConnections(5)
                .build();

        JolokiaConnection connection = new JolokiaConnection(client);

        return connection;
    }

    @Override
    public String getTargetDescription() {
        return "jolokia:url=" + this.jolokiaUrl;
    }
}
