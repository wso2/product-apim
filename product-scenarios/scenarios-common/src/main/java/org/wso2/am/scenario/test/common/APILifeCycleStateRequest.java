package org.wso2.am.scenario.test.common;

import org.wso2.am.integration.test.utils.bean.AbstractRequest;

public class APILifeCycleStateRequest extends AbstractRequest {

    private String name;
    private String status;
    private String provider;
    private String version = "1.0.0";
    private String publishToGateway = "true";
    private String deprecateOldVersions = "";
    private String requireResubscription = "";

    public APILifeCycleStateRequest(String apiName, String provider, String status) {

        this.name = apiName;
        this.status = status;
        this.provider = provider;
    }

    public void setAction() {

        this.setAction("updateStatus");
    }

    public void init() {

        this.addParameter("name", this.name);
        this.addParameter("status", this.status);
        this.addParameter("provider", this.provider);
        this.addParameter("version", this.version);
        this.addParameter("publishToGateway", this.publishToGateway);
        this.addParameter("deprecateOldVersions", this.deprecateOldVersions);
        this.addParameter("requireResubscription", this.requireResubscription);
    }
}

