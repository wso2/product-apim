package org.wso2.carbon.apimgt.rest.integration.tests.util;

import java.util.List;
import java.util.Map;

public class UserGroups {

    private List<String> creator;
    private List<String> publisher;
    private List<String> subscriber;
    private List<String> admin;

    public List<String> getCreator() {
        return creator;
    }

    public void setCreator(List<String> creator) {
        this.creator = creator;
    }

    public List<String> getPublisher() {
        return publisher;
    }

    public void setPublisher(List<String> publisher) {
        this.publisher = publisher;
    }

    public List<String> getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(List<String> subscriber) {
        this.subscriber = subscriber;
    }

    public List<String> getAdmin() {
        return admin;
    }

    public void setAdmin(List<String> admin) {
        this.admin = admin;
    }
}
