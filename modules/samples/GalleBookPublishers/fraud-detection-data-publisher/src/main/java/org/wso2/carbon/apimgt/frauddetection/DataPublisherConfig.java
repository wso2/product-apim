package org.wso2.carbon.apimgt.frauddetection;

/**
 * This class encapsulates the configuration for WSO2 DAS
 */
public class DataPublisherConfig {
    private String dasHost;
    private String dasPort;
    private String dasUsername;
    private String dasPassword;
    private String streamName;
    private String streamVersion;

    public String getDasHost() {
        return dasHost;
    }

    public void setDasHost(String dasHost) {
        this.dasHost = dasHost;
    }

    public String getDasPort() {
        return dasPort;
    }

    public void setDasPort(String dasPort) {
        this.dasPort = dasPort;
    }

    public String getDasUsername() {
        return dasUsername;
    }

    public void setDasUsername(String dasUsername) {
        this.dasUsername = dasUsername;
    }

    public String getDasPassword() {
        return dasPassword;
    }

    public void setDasPassword(String dasPassword) {
        this.dasPassword = dasPassword;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getStreamVersion() {
        return streamVersion;
    }

    public void setStreamVersion(String streamVersion) {
        this.streamVersion = streamVersion;
    }
}
