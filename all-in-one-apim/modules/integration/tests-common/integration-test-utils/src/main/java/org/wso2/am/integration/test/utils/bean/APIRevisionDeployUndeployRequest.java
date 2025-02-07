package org.wso2.am.integration.test.utils.bean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class APIRevisionDeployUndeployRequest {
    private static final Log log = LogFactory.getLog(APIRevisionDeployUndeployRequest.class);

    private String name;
    private boolean displayOnDevportal;
    private String vhost;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisplayOnDevportal() {
        return displayOnDevportal;
    }

    public void setDisplayOnDevportal(boolean displayOnDevportal) {
        this.displayOnDevportal = displayOnDevportal;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }
}
