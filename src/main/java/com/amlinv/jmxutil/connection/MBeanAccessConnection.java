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

package com.amlinv.jmxutil.connection;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by art on 5/7/15.
 */
public interface MBeanAccessConnection {
    /**
     * Query the specified attributes of the mbean with the given object name.  May be called multiple times
     * concurrently.
     *
     * @param objectName object name of the mbean to query.
     * @param attributeNames names of the attributes to retrieve.
     * @return list of attribute values.
     * @throws InstanceNotFoundException if the mbean with the given object name does not exist.
     * @throws IOException
     * @throws ReflectionException
     */
    List<Attribute> getAttributes(ObjectName objectName, String... attributeNames) throws InstanceNotFoundException,
            IOException, ReflectionException;

    /**
     * Query object names of mbeans given an object name pattern and, optionally, a query expression.
     *
     * @param pattern object name pattern to query.
     * @param query optional query expression.
     * @return set of object names matching the given pattern and query expression.
     * @throws IOException
     * @throws MalformedObjectNameException
     */
    Set<ObjectName> queryNames(ObjectName pattern, QueryExp query) throws IOException, MalformedObjectNameException;

    /**
     * Close the connection.
     *
     * @throws IOException
     */
    void close() throws IOException;
}
