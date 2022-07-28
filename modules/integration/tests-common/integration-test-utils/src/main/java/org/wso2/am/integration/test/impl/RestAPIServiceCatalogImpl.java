/*
 * Copyright (c) 2022, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.test.impl;

import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.ApiResponse;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.ServiceDTO;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.ServiceInfoListDTO;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.ServiceListDTO;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class RestAPIServiceCatalogImpl {
    private String tenantDomain = null;

    ServicesApi servicesApi = new ServicesApi();

    public RestAPIServiceCatalogImpl(String username, String password, String tenantDomain){
        ApiClient apiClient = new ApiClient();
        String basicEncoded =
                DatatypeConverter.printBase64Binary((username + ':' + password).getBytes(StandardCharsets.UTF_8));
        apiClient.addDefaultHeader("Authorization", "Basic " + basicEncoded);
        apiClient.setDebugging(true);
        apiClient.setBasePath("https://localhost:9943/api/am/service-catalog/v1");
        apiClient.setReadTimeout(600000);
        apiClient.setConnectTimeout(600000);
        apiClient.setWriteTimeout(600000);
        servicesApi.setApiClient(apiClient);
        this.tenantDomain = tenantDomain;
    }

    public ServiceListDTO retrieveServices(String name, String version, String definitionType, String key,
                                           Boolean shrink, String sortBy, String sortOrder, Integer limit,
                                           Integer offset) throws ApiException {
        return servicesApi.searchServices(name, version, definitionType, key, shrink, sortBy, sortOrder, limit, offset);
    }

    public ServiceDTO createService(ServiceDTO serviceMetadata, File definitionFile, String inlineContent)
            throws ApiException {
        return servicesApi.addService(serviceMetadata, definitionFile, inlineContent);
    }

    public ServiceDTO retrieveServiceById(String serviceId) throws ApiException {
        return servicesApi.getServiceById(serviceId);
    }

    public ServiceDTO updateService(String serviceId, ServiceDTO serviceMetadata, File definitionFile,
                                    String inlineContent) throws ApiException {
        return servicesApi.updateService(serviceId, serviceMetadata, definitionFile, inlineContent);
    }

    public ApiResponse deleteService(String serviceId) throws ApiException {
        return servicesApi.deleteServiceWithHttpInfo(serviceId);
    }

    public String retrieveServiceDefinition(String serviceId) throws ApiException {
        return servicesApi.getServiceDefinition(serviceId);
    }

    public APIListDTO retrieveServiceUsage(String serviceId) throws ApiException {
        return servicesApi.getServiceUsage(serviceId);
    }

    public ServiceInfoListDTO importService(File file, Boolean overwrite, String verifier) throws ApiException {
        return servicesApi.importService(file, overwrite,verifier);
    }

    public File exportService(String name, String version) throws ApiException {
        return servicesApi.exportService(name, version);
    }

}
