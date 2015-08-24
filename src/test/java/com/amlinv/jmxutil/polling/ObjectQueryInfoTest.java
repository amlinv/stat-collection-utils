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

import org.junit.Test;

import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Validate QueryObjectInfo which is an immutable holder of the key information needed to query an object and store the
 * results of the query.
 *
 * Created by art on 8/22/15.
 */
public class ObjectQueryInfoTest {

    @Test
    public void testImmutableQueryInfo() throws Exception {
        Map<String, Method> setters = new HashMap<>();
        setters.put("x-att1-x", null);
        setters.put("x-att2-x", null);

        Object target = new Object();
        ObjectName oname = new ObjectName("x-domain-x:x-key-x=x-value-x");

        ObjectQueryInfo objectQueryInfo = new ObjectQueryInfo(target, oname, setters);

        assertSame(target, objectQueryInfo.getTarget());
        assertSame(oname, objectQueryInfo.getObjectName());
        assertSame(setters, objectQueryInfo.getAttributeSetters());
        assertEquals(setters.keySet(), objectQueryInfo.getAttributeNames());
    }
}