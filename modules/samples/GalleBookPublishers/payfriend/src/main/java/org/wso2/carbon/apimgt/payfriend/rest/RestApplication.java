package org.wso2.carbon.apimgt.payfriend.rest;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Jersey application configuration class.
 */
public class RestApplication extends ResourceConfig {

    public RestApplication() {
        packages("org.wso2.carbon.apimgt.payfriend.rest");
    }
}
