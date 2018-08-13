/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import feign.Client;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.util.auth.OAuth;

/**
 * APIClient Builder for build Feign Clients With Given Feign Interface.
 */
public class ApiClient {
    public interface Api {
    }

    protected ObjectMapper objectMapper;
    private String basePath;
    private Feign.Builder feignBuilder;
    public static final String KEY_MANAGER_CERT_ALIAS = "wso2carbon";

    public ApiClient(String basePath, String username, String password, String scopes) throws
            AMIntegrationTestException {
        this.basePath = basePath;
        objectMapper = createObjectMapper();
        feignBuilder = Feign.builder()
                .encoder(new FormEncoder(new JacksonEncoder(objectMapper)))
                .decoder(new JacksonDecoder(objectMapper))
                .errorDecoder(new RestAPIErrorDecoder())
                .logger(new Slf4jLogger()).requestInterceptor(new OAuth(username, password, scopes))
                .client(new Client.Default(AMIntegrationSSLSocketFactory.getSSLSocketFactory(KEY_MANAGER_CERT_ALIAS),
                        (hostname, sslSession) -> true));
        ;
    }

    public ApiClient(String basePath, String username, String password) throws AMIntegrationTestException {
        this.basePath = basePath;
        objectMapper = createObjectMapper();
        feignBuilder = Feign.builder()
                .encoder(new FormEncoder(new JacksonEncoder(objectMapper)))
                .decoder(new JacksonDecoder(objectMapper))
                .errorDecoder(new RestAPIErrorDecoder())
                .logger(new Slf4jLogger()).requestInterceptor(new BasicAuthRequestInterceptor(username, password))
                .client(new Client.Default(AMIntegrationSSLSocketFactory.getSSLSocketFactory(KEY_MANAGER_CERT_ALIAS),
                        (hostname, sslSession) -> true));
    }


    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setDateFormat(new RFC3339DateFormat());
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

    /**
     * Creates a feign client for given API interface.
     * <p>
     * Usage:
     * ApiClient apiClient = new ApiClient();
     * apiClient.setBasePath("http://localhost:8080");
     * XYZApi api = apiClient.buildClient(XYZApi.class);
     * XYZResponse response = api.someMethod(...);
     *
     * @param <T>         Type
     * @param clientClass Client class
     * @return The Client
     */
    public <T extends Api> T buildClient(Class<T> clientClass) {
        return feignBuilder.target(clientClass, basePath);
    }

}
