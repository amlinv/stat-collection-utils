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

import com.amlinv.jmxutil.MBeanLocationParameterSource;
import com.amlinv.jmxutil.annotation.MBeanAnnotationUtil;
import com.amlinv.logging.util.RepeatLogMessageSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Using reflection, prepare specific objects for query and injection of query results.
 *
 * Created by art on 8/22/15.
 */
public class ObjectQueryPreparer {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(ObjectQueryPreparer.class);

    private Logger log = DEFAULT_LOGGER;
    private RepeatLogMessageSuppressor logNoAttributeThrottle = new RepeatLogMessageSuppressor();

    private ParameterReplacer parameterReplacer = new ParameterReplacer();

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public ParameterReplacer getParameterReplacer() {
        return parameterReplacer;
    }

    public void setParameterReplacer(ParameterReplacer parameterReplacer) {
        this.parameterReplacer = parameterReplacer;
    }

    /**
     * Prepare to query the given object.
     *
     * @param obj the object to prepare; must be annotated appropriately with @MBeanLocation on the class and
     * @MBeanAttribute on setter methods.
     * @return the results of the prepared object.
     * @throws MalformedObjectNameException
     * @see com.amlinv.jmxutil.annotation.MBeanLocation
     * @see com.amlinv.jmxutil.annotation.MBeanAttribute
     */
    public ObjectQueryInfo prepareObjectQuery(Object obj) throws MalformedObjectNameException {

        ObjectQueryInfo result;

        //
        // Extract the mbean info from the object (TBD: cache this information ahead of time)
        //
        String onamePattern = MBeanAnnotationUtil.getLocationONamePattern(obj);

        if (onamePattern != null) {
            //
            // Locate the setters and continue only if at least one was found.
            //
            Map<String, Method> attributeSetters = MBeanAnnotationUtil.getAttributes(obj);

            if (attributeSetters.size() > 0) {
                String onameString;

                if (obj instanceof MBeanLocationParameterSource) {
                    onameString = this.parameterReplacer
                            .replaceObjectNameParameters(onamePattern, (MBeanLocationParameterSource) obj);
                } else {
                    onameString = onamePattern;
                }

                ObjectName oname = new ObjectName(onameString);

                result = new ObjectQueryInfo(obj, oname, attributeSetters);
            } else {
                this.logNoAttributeThrottle.warn(log,
                        "ignoring attempt to prepare to poll an MBean object with no attributes: onamePattern={}",
                        onamePattern);
                result = null;
            }
        } else {
            log.warn("ignoring attempt to prepare to poll object that has no MBeanLocation");
            result = null;
        }

        return result;
    }
}
