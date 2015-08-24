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

import com.amlinv.jmxutil.connection.impl.MBeanBatchCapableAccessConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by art on 8/22/15.
 */
public class BatchPollProcessor {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(BatchPollProcessor.class);

    private AttributeInjector attributeInjector = new AttributeInjector();
    private ObjectQueryPreparer objectQueryPreparer = new ObjectQueryPreparer();

    private Logger log = DEFAULT_LOGGER;

    private boolean shutdownInd = false;

    public AttributeInjector getAttributeInjector() {
        return attributeInjector;
    }

    public void setAttributeInjector(AttributeInjector attributeInjector) {
        this.attributeInjector = attributeInjector;
    }

    public ObjectQueryPreparer getObjectQueryPreparer() {
        return objectQueryPreparer;
    }

    public void setObjectQueryPreparer(ObjectQueryPreparer objectQueryPreparer) {
        this.objectQueryPreparer = objectQueryPreparer;
    }

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

    public void pollBatch (MBeanBatchCapableAccessConnection batchApi, List<Object> polledObjects)
            throws IOException {

        Map<ObjectName, List<String>> objectAttributes = new HashMap<>();
        Map<ObjectName, ObjectQueryInfo> objectQueryInfo = new HashMap<>();

        //
        // Collect the query details for all of the polled objects.
        //
        for (final Object onePolledObject : polledObjects) {
            // Stop as soon as possible if shutting down.
            if (shutdownInd) {
                return;
            }

            ObjectQueryInfo queryInfo = null;
            try {
                queryInfo = this.objectQueryPreparer.prepareObjectQuery(onePolledObject);

                if (queryInfo != null) {
                    objectAttributes.put(queryInfo.getObjectName(),
                            new LinkedList<String>(queryInfo.getAttributeNames()));
                    objectQueryInfo.put(queryInfo.getObjectName(), queryInfo);
                }
            } catch (MalformedObjectNameException malformedObjectNameExc) {
                this.log.info("invalid object name in query; skipping", malformedObjectNameExc);
            }
        }

        //
        // Poll them all in one batch now, if anything remains.
        //
        if (objectAttributes.size() > 0) {
            try {
                Map<ObjectName, List<Attribute>> objectAttValues = batchApi.batchQueryAttributes(objectAttributes);

                this.copyOutBatchAttributes(objectAttValues, objectQueryInfo);
            } catch (ReflectionException reflectionExc) {
                this.log.info("unexpected reflection exception during batch poll", reflectionExc);
            } catch (MalformedObjectNameException malformedObjectNameExc) {
                this.log.info("unexpected malformed object name during batch poll", malformedObjectNameExc);
            }
        } else {
            log.debug("nothing to poll after preparing {} objects", polledObjects.size());
        }
    }

    protected void copyOutBatchAttributes (Map<ObjectName, List<Attribute>> objectAttValues,
                                           Map<ObjectName, ObjectQueryInfo> objectQueryInfo) {

        for ( Map.Entry<ObjectName, List<Attribute>> entry : objectAttValues.entrySet() ) {
            ObjectName objectName = entry.getKey();

            ObjectQueryInfo queryInfo = objectQueryInfo.get(objectName);
            this.attributeInjector.copyOutAttributes(queryInfo.getTarget(), entry.getValue(),
                    queryInfo.getAttributeSetters(), queryInfo.getObjectName());
        }
    }

    public void shutdown() {
        this.shutdownInd = true;
    }

}
