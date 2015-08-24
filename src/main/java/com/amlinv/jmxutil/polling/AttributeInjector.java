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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.ObjectName;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Inject attribute values into an object based on field names and setters.
 *
 * Created by art on 8/22/15.
 */
public class AttributeInjector {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(AttributeInjector.class);

    private Logger log = DEFAULT_LOGGER;

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    /**
     * For a JMX MBean idenfitied by an ObjectName, copy the values of the given attributes into the target object
     * using the specified setter methods mapped by attribute name.
     *
     * @param target object into which values are injected.
     * @param jmxAttributeValues values of the attributes to inject.
     * @param attributeSetters map of attribute name to setter used to determine how to inject each attribute's value.
     * @param objectName name of the MBean involved - used only for logging.
     */
    public void copyOutAttributes(Object target, List<Attribute> jmxAttributeValues,
                                  Map<String, Method> attributeSetters, ObjectName objectName) {

        this.copyOutAttributes(target, jmxAttributeValues, attributeSetters, "oname", objectName);
    }

    /**
     * Copy the values of the given attributes into the target object using the specified setter methods mapped by
     * attribute name.
     *
     * @param target object into which values are injected.
     * @param jmxAttributeValues values of the attributes to inject.
     * @param attributeSetters map of attribute name to setter used to determine how to inject each attribute's value.
     * @param identifierKey key, or name, of the identifier - used only for logging purposes.
     * @param identifier identifier to include in the output - used only for logging purposes.
     */
    protected void copyOutAttributes(Object target, List<Attribute> jmxAttributeValues,
                                     Map<String, Method> attributeSetters, String identifierKey, Object identifier) {

        for (Attribute oneAttribute : jmxAttributeValues) {
            String attributeName = oneAttribute.getName();

            Method setter = attributeSetters.get(attributeName);
            Object value = oneAttribute.getValue();

            try {
                //
                // Automatically down-convert longs to integers as-needed.
                //
                if ((setter.getParameterTypes()[0].isAssignableFrom(Integer.class)) ||
                    (setter.getParameterTypes()[0].isAssignableFrom(int.class))) {
                    if (value instanceof Long) {
                        value = ((Long) value).intValue();
                    }
                }
                setter.invoke(target, value);
            } catch (InvocationTargetException invocationExc) {
                this.log.info("invocation exception storing mbean results: {}={}; attributeName={}", identifierKey,
                        identifier, attributeName, invocationExc);
            } catch (IllegalAccessException illegalAccessExc) {
                this.log.info("illegal access exception storing mbean results: {}={}; attributeName={}", identifierKey,
                        identifier, attributeName, illegalAccessExc);
            } catch (IllegalArgumentException illegalArgumentExc) {
                this.log.info("illegal argument exception storing mbean results: {}={}; attributeName={}",
                        identifierKey, identifier, attributeName, illegalArgumentExc);
            }
        }
    }
}
