/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.restapi;

import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.TestFrameworkUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import java.io.File;

import static org.testng.Assert.assertEquals;


public class DataDrivenTestUtils {
    private void setKeysForRestClient() {
        System.setProperty("javax.net.ssl.trustStore", "/repository/resources/security/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

    }

    public String testRestAPI(String url, String requestPayLoad, Map<String,String> header, Map<String, String> queryParams,
                              String responsePayLoad){
        return "test";

    }


    public ClientResponse geneticRestRequestPost(String resourceUrl, String contentType,
                                                 String acceptMediaType, Object postBody,
                                                 Map<String, String> queryParamMap,
                                                 Map<String, String> headerMap,
                                                 String cookie) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(resourceUrl);

        // Resource resource = client.resource(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        Form form = new Form();
        if (contentType == MediaType.APPLICATION_FORM_URLENCODED) {
            for (String formField : postBody.toString().split("&")) {
                form.param(formField.split("=")[0], formField.split("=")[1]);
            }
            response = builder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
        } else if (contentType == MediaType.MULTIPART_FORM_DATA) {
            for (String formField : postBody.toString().split("&")) {
                form.param(formField.split("=")[0], formField.split("=")[1]);
            }
            response = builder.post(Entity.entity(form, MediaType.MULTIPART_FORM_DATA));
        } else if (contentType == MediaType.APPLICATION_JSON) {
            response = builder.post(Entity.entity(Entity.json(postBody), MediaType.APPLICATION_JSON));
        } else if (contentType == MediaType.APPLICATION_XML) {
            response = builder.post(Entity.entity(Entity.xml(postBody), MediaType.APPLICATION_XML));
        }
        client.close();
        return ((ClientResponse) response);
    }

    public ClientResponse geneticRestRequestGet(String resourceUrl,
                                                String acceptMediaType,
                                                Map<String, String> queryParamMap,
                                                Map<String, String> headerMap,
                                                String cookie) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(resourceUrl);

        // Resource resource = client.resource(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        response = builder.get();
        client.close();
        return ((ClientResponse) response);
    }

    public ClientResponse geneticRestRequestPut(String resourceUrl, String contentType,
                                                String acceptMediaType, Object postBody,
                                                Map<String, String> queryParamMap,
                                                Map<String, String> headerMap,
                                                String cookie) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);

        Response response = null;
        Form form = new Form();
        if (contentType == MediaType.APPLICATION_FORM_URLENCODED) {
            for (String formField : postBody.toString().split("&")) {
                form.param(formField.split("=")[0], formField.split("=")[1]);
            }
            response = builder.put(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
        } else if (contentType == MediaType.MULTIPART_FORM_DATA) {
            for (String formField : postBody.toString().split("&")) {
                form.param(formField.split("=")[0], formField.split("=")[1]);
            }
            response = builder.put(Entity.entity(form, MediaType.MULTIPART_FORM_DATA));
        } else if (contentType == MediaType.APPLICATION_JSON) {
            response = builder.put(Entity.entity(Entity.json(postBody), MediaType.APPLICATION_JSON));
        } else if (contentType == MediaType.APPLICATION_XML) {
            response = builder.put(Entity.entity(Entity.xml(postBody), MediaType.APPLICATION_XML));
        }
        client.close();
        return ((ClientResponse) response);
    }


    public ClientResponse geneticRestRequestDelete(String resourceUrl,
                                                   String acceptMediaType,
                                                   Map<String, String> queryParamMap,
                                                   Map<String, String> headerMap,
                                                   String cookie) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        response = builder.delete();
        client.close();
        return ((ClientResponse) response);
    }

    public ClientResponse geneticRestRequestHead(String resourceUrl,
                                                 String acceptMediaType,
                                                 Map<String, String> queryParamMap,
                                                 Map<String, String> headerMap,
                                                 String cookie) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        response = builder.head();
        client.close();
        return ((ClientResponse) response);
    }

    private Invocation.Builder getBuilder(String acceptMediaType, Map<String, String> queryParamMap,
                                          Map<String, String> headerMap, String cookie,
                                          WebTarget target) {
        // Resource resource = client.resource(resourceUrl);
        if (!(queryParamMap.size() <= 0)) {
            for (Map.Entry<String, String> queryParamEntry : queryParamMap.entrySet()) {
                target.queryParam(queryParamEntry.getKey(), queryParamEntry.getValue());
            }
        }
        Invocation.Builder builder = target.request(acceptMediaType);
        if (!(headerMap.size() <= 0)) {
            for (Map.Entry<String, String> headerEntry : headerMap.entrySet()) {
                builder.header(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        if (cookie != null) {
            builder.cookie(new Cookie(cookie.split("=")[0], cookie.split("=")[1]));
        }
        builder.accept(acceptMediaType);
        return builder;
    }


}

