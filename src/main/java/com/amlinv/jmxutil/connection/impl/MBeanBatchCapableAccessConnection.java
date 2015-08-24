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

import com.amlinv.jmxutil.connection.MBeanAccessConnection;

import javax.management.Attribute;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by art on 5/7/15.
 */
public interface MBeanBatchCapableAccessConnection extends MBeanAccessConnection {
    /**
     * Execute a batch query of the attributes for multiple object names.  Each object name may query its own set of
     * attributes.
     *
     * @param objectAttNames set of object names for which to query attributes mapped to the list of attributes for
     *                       each object name.
     * @return map of attribute values for each object name; note an object name will be missing from the map if it
     * resulted in an error.
     * @throws IOException
     * @throws ReflectionException
     */
    Map<ObjectName, List<Attribute>> batchQueryAttributes(Map<ObjectName, List<String>> objectAttNames)
            throws IOException, ReflectionException, MalformedObjectNameException;
}
