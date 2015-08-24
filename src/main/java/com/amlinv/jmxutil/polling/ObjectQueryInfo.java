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

import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Created by art on 8/22/15.
 */
public class ObjectQueryInfo {
    private final Object target;
    private final ObjectName objectName;
    private final Map<String, Method> attributeSetters;

    public ObjectQueryInfo(Object target, ObjectName objectName, Map<String, Method> attributeSetters) {
        this.target = target;
        this.objectName = objectName;
        this.attributeSetters = attributeSetters;
    }

    public Object getTarget() {
        return target;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public Map<String, Method> getAttributeSetters() {
        return attributeSetters;
    }

    public Set<String> getAttributeNames() {
        return attributeSetters.keySet();
    }
}
