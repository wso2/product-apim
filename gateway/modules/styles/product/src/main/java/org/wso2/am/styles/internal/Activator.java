/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.styles.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;



/**
 *  Activator class for AM styles bundle
 */
public class Activator implements BundleActivator {
    private static final Log log = LogFactory.getLog(Activator.class);
    public void start(BundleContext bundleContext) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Activating the bundle org.wso2.am.styles");
        }

    }

    public void stop(BundleContext context) throws Exception {

    }
}