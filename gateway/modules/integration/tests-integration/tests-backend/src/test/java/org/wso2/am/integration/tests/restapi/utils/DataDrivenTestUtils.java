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

package org.wso2.am.integration.tests.restapi.utils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;

/**
 * This class is used to send respective requests based on the request
 */
public class DataDrivenTestUtils {


    private void setKeysForRestClient() {
        System.setProperty("javax.net.ssl.trustStore", "/repository/resources/security/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

    }

    /**
     * This method is used to send the request to the REST API based on its type
     *
     * @param method http method to be used
     * @param resourceUrl url of the resource
     * @param queryParameters map containing query parameters
     * @param requestHeaders map containing request headers
     * @param requestPayload request payload/body
     * @param cookie cookie string if any
     * @return response of the relevant request
     */
    public Response sendRequestToRESTAPI(String method, String resourceUrl, Map<String, String> queryParameters,
                                         Map<String, String> requestHeaders, String requestPayload, String cookie) {

        Response response = null;

        if (RESTAPITestConstants.GET_METHOD.equalsIgnoreCase(method)) {
            response = geneticRestRequestGet(resourceUrl, RESTAPITestConstants.APPLICATION_JSON_CONTENT,
                    queryParameters, requestHeaders, cookie);
        } else if (RESTAPITestConstants.POST_METHOD.equalsIgnoreCase(method)) {
            response = geneticRestRequestPost(resourceUrl, RESTAPITestConstants.APPLICATION_JSON_CONTENT,
                    RESTAPITestConstants.APPLICATION_JSON_CONTENT, requestPayload, queryParameters, requestHeaders,
                    cookie);
        } else if (RESTAPITestConstants.PUT_METHOD.equalsIgnoreCase(method)) {
            response = geneticRestRequestPut(resourceUrl, RESTAPITestConstants.APPLICATION_JSON_CONTENT,
                    RESTAPITestConstants.APPLICATION_JSON_CONTENT, requestPayload, queryParameters, requestHeaders,
                    cookie);
        } else if (RESTAPITestConstants.DELETE_METHOD.equalsIgnoreCase(method)) {
            response = geneticRestRequestDelete(resourceUrl, RESTAPITestConstants.APPLICATION_JSON_CONTENT,
                    queryParameters, requestHeaders, cookie);
        }
        return response;
    }

    /**
     *This method is used to send the POST request
     *
     * @param resourceUrl url of the resource
     * @param contentType content type
     * @param acceptMediaType accepted media type
     * @param postBody payload
     * @param queryParamMap map containing query parameters
     * @param headerMap map containing headers
     * @param cookie cookie string if any
     * @return response of the POST request
     */
    public Response geneticRestRequestPost(String resourceUrl, String contentType, String acceptMediaType,
                                           Object postBody, Map<String, String> queryParamMap,
                                           Map<String, String> headerMap, String cookie) {

        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        WebTarget target = client.target(resourceUrl);
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
            response = builder.post(Entity.json(postBody));
        } else if (contentType == MediaType.APPLICATION_XML) {
            response = builder.post(Entity.entity(Entity.xml(postBody), MediaType.APPLICATION_XML));
        }
        client.close();
        return response;
    }

    /**
     *This method is used to send the GET request
     *
     * @param resourceUrl url of the resource
     * @param acceptMediaType accepted media type
     * @param queryParamMap map containing query parameters
     * @param headerMap map containing headers
     * @param cookie cookie string if any
     * @return response of the GET request
     */
    public Response geneticRestRequestGet(String resourceUrl, String acceptMediaType, Map<String, String> queryParamMap,
                                          Map<String, String> headerMap, String cookie) {
        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        WebTarget target = client.target(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        response = builder.get();
        client.close();
        return response;
    }

    /**
     *This method is used to send the PUT request
     *
     * @param resourceUrl url of the resource
     * @param contentType content type
     * @param acceptMediaType accepted media type
     * @param postBody payload
     * @param queryParamMap map containing query parameters
     * @param headerMap map containing headers
     * @param cookie cookie string if any
     * @return response of the PUT request
     */
    public Response geneticRestRequestPut(String resourceUrl, String contentType, String acceptMediaType,
                                          Object postBody, Map<String, String> queryParamMap,
                                          Map<String, String> headerMap, String cookie) {

        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
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
            response = builder.put(Entity.json(postBody));
        } else if (contentType == MediaType.APPLICATION_XML) {
            response = builder.put(Entity.entity(Entity.xml(postBody), MediaType.APPLICATION_XML));
        }
        client.close();
        return response;
    }

    /**
     *This method is used to send the DELETE request
     *
     * @param resourceUrl url of the resource
     * @param acceptMediaType accepted media type
     * @param queryParamMap map containing query parameters
     * @param headerMap map containing headers
     * @param cookie cookie string if any
     * @return response of the DELETE request
     */
    public Response geneticRestRequestDelete(String resourceUrl, String acceptMediaType,
                                             Map<String, String> queryParamMap, Map<String, String> headerMap,
                                             String cookie) {

        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        WebTarget target = client.target(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        response = builder.delete();
        client.close();
        return response;
    }

    /**
     *This method is used to send the HEAD request
     *
     * @param resourceUrl url of the resource
     * @param acceptMediaType accepted media type
     * @param queryParamMap map containing query parameters
     * @param headerMap map containing headers
     * @param cookie cookie string if any
     * @return response of the HEAD request
     */
    public Response geneticRestRequestHead(String resourceUrl, String acceptMediaType,
                                           Map<String, String> queryParamMap, Map<String, String> headerMap,
                                           String cookie) {
        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        WebTarget target = client.target(resourceUrl);
        Invocation.Builder builder = getBuilder(acceptMediaType, queryParamMap, headerMap, cookie, target);
        Response response = null;
        response = builder.head();
        client.close();
        return response;
    }

    /**
     * This method builds a builder based on client request invocation
     *
     * @param acceptMediaType accepted media type
     * @param queryParamMap map containing query parameters
     * @param headerMap map containing headers
     * @param cookie cookie string if any
     * @param target web target resource
     * @return a builder based on client request
     */
    private Invocation.Builder getBuilder(String acceptMediaType, Map<String, String> queryParamMap,
                                          Map<String, String> headerMap, String cookie,
                                          WebTarget target) {

        if (!(queryParamMap.size() <= 0)) {
            for (Map.Entry<String, String> queryParamEntry : queryParamMap.entrySet()) {
                target = target.queryParam(queryParamEntry.getKey(), queryParamEntry.getValue());
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
        return builder;
    }

}
