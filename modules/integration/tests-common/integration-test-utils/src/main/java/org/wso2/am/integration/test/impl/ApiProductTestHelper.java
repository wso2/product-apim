/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.am.integration.test.impl;

import org.testng.Assert;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.clients.store.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A collection of helper methods to aid in setting up and testing APIProducts
 */
public class ApiProductTestHelper {

    private RestAPIPublisherImpl restAPIPublisher;
    private RestAPIStoreImpl restAPIStore;

    public ApiProductTestHelper(RestAPIPublisherImpl restAPIPublisher, RestAPIStoreImpl restAPIStore) {

        this.restAPIPublisher = restAPIPublisher;
        this.restAPIStore = restAPIStore;
    }

    public APIProductDTO createAPIProductInPublisher(String provider, String name, String context, String version,
            List<APIDTO> apisToBeUsed, List<String> policies)
            throws ApiException {
        // Select resources from APIs to be used by APIProduct
        List<ProductAPIDTO> resourcesForProduct = getResourcesForProduct(apisToBeUsed);

        // Generate APIProductDTO
        APIProductDTO apiProductDTO = DtoFactory.createApiProductDTO(provider, name, context, version,
                resourcesForProduct, policies);

        // Create APIProduct and validate response code
        APIProductDTO responseData = restAPIPublisher.addApiProduct(apiProductDTO);

        // Validate that APIProduct resources returned in response data are same as originally selected API resources
        List<ProductAPIDTO> responseResources = responseData.getApis();
        verifyAPIProductDto(responseResources, resourcesForProduct);

        // Validate mandatory fields returned in response data
        Assert.assertTrue(provider.equalsIgnoreCase(responseData.getProvider()));
        Assert.assertEquals(responseData.getName(), name);
        Assert.assertEquals(responseData.getVersion(), version);
        if ("carbon.super".equals(restAPIPublisher.tenantDomain)) {
            Assert.assertEquals(responseData.getContext(), context);
        } else {
            Assert.assertEquals(responseData.getContext(),
                    String.format("/t/%s%s", restAPIPublisher.tenantDomain, context));
        }

        return responseData;
    }

    public WorkflowResponseDTO changeLifecycleStateOfApiProduct(String apiProductId, String action,
                                                                String lifecycleChecklist) throws ApiException, APIManagerIntegrationTestException {

        WorkflowResponseDTO workflowResponseDTO = restAPIPublisher.changeAPIProductLifeCycleStatus(apiProductId, action,
                lifecycleChecklist);
        Assert.assertNotNull(workflowResponseDTO);
        return workflowResponseDTO;
    }

    private void verifyAPIProductDto(List<ProductAPIDTO> expectedProduct, List<ProductAPIDTO> actualProduct) {

        for (ProductAPIDTO expectedAPI : expectedProduct) {
            ProductAPIDTO actual = null;
            for (ProductAPIDTO actualAPI : actualProduct) {
                if (expectedAPI.getName().equals(actualAPI.getName())) {
                    actual = actualAPI;
                    break;
                }
            }
            Assert.assertNotNull(actual);
            Assert.assertNotNull(expectedAPI.getOperations());
            Assert.assertNotNull(actual.getOperations());
            //verifyOperations(actual.getOperations(), expectedAPI.getOperations());
        }
    }

    public void verfiyApiProductInPublisher(APIProductDTO responseData) throws ApiException {
        // Validate APIProduct in publisher listing
        APIProductListDTO apiProductsList = restAPIPublisher.getAllApiProducts();

        List<APIProductInfoDTO> apiProducts = apiProductsList.getList();

        boolean isAPIProductInListing = false;
        int productCount = 0;
        for (APIProductInfoDTO apiProduct : apiProducts) {
            if (apiProduct.getId().equals(responseData.getId())) {
                isAPIProductInListing = true;
                ++productCount;
                verifyApiProductInfoWithApiProductDto(apiProduct, responseData);
            }
        }

        Assert.assertTrue(isAPIProductInListing);
        Assert.assertEquals(productCount, 1);

        // Validate APIProduct by Id
        APIProductDTO returnedProduct = restAPIPublisher.getApiProduct(responseData.getId());
        verifyAPIProductDTOFromPublisher(returnedProduct, responseData);
    }

    private void verifyAPIProductDTOFromPublisher(APIProductDTO returnedProduct, APIProductDTO responseData) {

        Assert.assertEquals(returnedProduct.getId(), responseData.getId());
        Assert.assertEquals(returnedProduct.getName(), responseData.getName());
        Assert.assertEquals(returnedProduct.getContext(), responseData.getContext());
        Assert.assertEquals(returnedProduct.getDescription(), responseData.getDescription());
        Assert.assertEquals(returnedProduct.getProvider(), responseData.getProvider());
        Assert.assertEquals(returnedProduct.isHasThumbnail(), responseData.isHasThumbnail());
        Assert.assertEquals(returnedProduct.getState(), responseData.getState());
        Assert.assertEquals(returnedProduct.isEnableSchemaValidation(), responseData.isEnableSchemaValidation());
        Assert.assertEquals(returnedProduct.isResponseCachingEnabled(), responseData.isResponseCachingEnabled());
        Assert.assertEquals(returnedProduct.getCacheTimeout(), responseData.getCacheTimeout());
        Assert.assertEquals(returnedProduct.getVisibility(), responseData.getVisibility());
        Assert.assertEquals(returnedProduct.getVisibleRoles(), responseData.getVisibleRoles());
        Assert.assertEquals(returnedProduct.getVisibleTenants(), responseData.getVisibleTenants());
        Assert.assertEquals(returnedProduct.getAccessControl(), responseData.getAccessControl());
        Assert.assertEquals(returnedProduct.getAccessControlRoles(), responseData.getAccessControlRoles());
        Assert.assertEquals(returnedProduct.getApiType(), responseData.getApiType());
        Assert.assertEquals(returnedProduct.getTransport(), responseData.getTransport());
        Assert.assertEquals(returnedProduct.getTags(), responseData.getTags());
        Assert.assertEquals(returnedProduct.getPolicies(), responseData.getPolicies());
        Assert.assertEquals(returnedProduct.getApiThrottlingPolicy(), responseData.getApiThrottlingPolicy());
        Assert.assertEquals(returnedProduct.getAuthorizationHeader(), responseData.getAuthorizationHeader());
        Assert.assertEquals(returnedProduct.getSecurityScheme(), responseData.getSecurityScheme());
        Assert.assertEquals(returnedProduct.getSubscriptionAvailability(), responseData.getSubscriptionAvailability());
        Assert.assertEquals(returnedProduct.getSubscriptionAvailableTenants(),
                responseData.getSubscriptionAvailableTenants());
        Assert.assertEquals(returnedProduct.getAdditionalProperties(), responseData.getAdditionalProperties());
        Assert.assertEquals(returnedProduct.getMonetization(), responseData.getMonetization());
        Assert.assertEquals(returnedProduct.getBusinessInformation(), responseData.getBusinessInformation());
        Assert.assertEquals(returnedProduct.getCorsConfiguration(), responseData.getCorsConfiguration());
        Assert.assertEquals(returnedProduct.getCreatedTime(), responseData.getCreatedTime());
        Assert.assertEquals(returnedProduct.getLastUpdatedTime(), responseData.getLastUpdatedTime());
        Assert.assertEquals(returnedProduct.getScopes(), responseData.getScopes());
        verifyProductAPIDto(returnedProduct.getApis(), responseData.getApis());
    }

    private void verifyProductAPIDto(List<ProductAPIDTO> actual, List<ProductAPIDTO> expected) {

        for (ProductAPIDTO actualAPI : actual) {
            ProductAPIDTO matchedAPI = null;
            for (ProductAPIDTO expectedAPI : expected) {
                if (actualAPI.getName().equals(expectedAPI.getName())) {
                    matchedAPI = expectedAPI;
                    break;
                }
            }
            Assert.assertNotNull(matchedAPI);
            //verifyOperations(actualAPI.getOperations(),matchedAPI.getOperations());
        }

    }

    private void verifyOperations(List<APIOperationsDTO> actual, List<APIOperationsDTO> expected) {

        Assert.assertEquals(actual.size(), expected.size());
        for (APIOperationsDTO actualOperation : actual) {
            APIOperationsDTO matchedOperation = null;
            for (APIOperationsDTO expectedOperation : expected) {
                if (actualOperation.getTarget().equals(expectedOperation.getTarget()) &&
                        actualOperation.getVerb().equals(expectedOperation.getVerb())) {
                    matchedOperation = expectedOperation;
                    break;
                }
            }
            Assert.assertNotNull(matchedOperation);
            Assert.assertEquals(actualOperation.getAuthType(), matchedOperation.getAuthType());
            Assert.assertEquals(new HashSet(actualOperation.getScopes()),
                    new HashSet(matchedOperation.getScopes()));
            Assert.assertEquals(new HashSet(actualOperation.getUsedProductIds()),
                    new HashSet(matchedOperation.getUsedProductIds()));
            Assert.assertEquals(actualOperation.getThrottlingPolicy(),
                    matchedOperation.getThrottlingPolicy());
        }

    }

    public org.wso2.am.integration.clients.store.api.v1.dto.APIDTO verifyApiProductInPortal(APIProductDTO apiProductDTO)
            throws org.wso2.am.integration.clients.store.api.ApiException, InterruptedException {

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO responseData = restAPIStore.getAPI(apiProductDTO.getId());

        // Validate mandatory fields returned in response data
        verifyApiDtoWithApiProduct(responseData, apiProductDTO);

        boolean isAPIProductInListing = false;
        int productCount = 0;
        int tries = 0;
        while (productCount == 0 && tries < 5) {
            APIListDTO apiList = restAPIStore.getAllAPIs();
            List<APIInfoDTO> apiInfos = apiList.getList();
            for (APIInfoDTO apiInfo : apiInfos) {
                if (apiInfo.getId().equals(apiProductDTO.getId())) {
                    isAPIProductInListing = true;
                    ++productCount;
                    verifyApiInfoDtoWithApiProduct(apiInfo, apiProductDTO);
                    break;
                }
            }
            tries++;
            if (productCount == 0) {
                Thread.sleep(5000);
            }
        }

        Assert.assertTrue(isAPIProductInListing);
        Assert.assertEquals(productCount, 1);

        return responseData;
    }

    /**
     * Returns a collection of API resources which can be used by an APIProduct,
     * by selecting all resources from each available API provided
     *
     * @param apiDTOs API List
     * @return Collection of API resources to be included in an APIProduct
     */
    private List<ProductAPIDTO> getResourcesForProduct(List<APIDTO> apiDTOs) {

        Map<APIDTO, Set<APIOperationsDTO>> selectedApiResourceMapping = new HashMap<>();

        // Pick two operations from each API to be used to create the APIProduct.
        for (APIDTO apiDto : apiDTOs) {
            selectOperationsFromAPI(apiDto, selectedApiResourceMapping);
        }

        return convertToProductApiResources(selectedApiResourceMapping);
    }

    /**
     * Select all resources from a given API. Resources will be picked sequentially, where the
     * resources itself will be unordered.
     *
     * @param apiDto                     API
     * @param selectedApiResourceMapping Collection for storing the selected resources against the respective API
     */
    private void selectOperationsFromAPI(APIDTO apiDto, Map<APIDTO, Set<APIOperationsDTO>> selectedApiResourceMapping) {

        List<APIOperationsDTO> operations = apiDto.getOperations();

        Set<APIOperationsDTO> selectedOperations = new HashSet<>();
        selectedApiResourceMapping.put(apiDto, selectedOperations);

        selectedOperations.addAll(operations);
    }

    /**
     * Converts selected API resources to the APIProduct resource DTO format.
     *
     * @param selectedResources Selected API resources
     * @return Collection of APIProduct resources
     */
    private List<ProductAPIDTO> convertToProductApiResources(Map<APIDTO, Set<APIOperationsDTO>> selectedResources) {

        List<ProductAPIDTO> apiResources = new ArrayList<>();

        for (Map.Entry<APIDTO, Set<APIOperationsDTO>> entry : selectedResources.entrySet()) {
            APIDTO apiDto = entry.getKey();
            Set<APIOperationsDTO> operations = entry.getValue();

            apiResources.add(new ProductAPIDTO().
                    apiId(apiDto.getId()).
                    name(apiDto.getName()).
                    operations(new ArrayList<>(operations)));
        }

        return apiResources;
    }

    private void verifyApiProductInfoWithApiProductDto(APIProductInfoDTO apiProductInfoDTO, APIProductDTO apiProductDTO) {

        Assert.assertEquals(apiProductInfoDTO.getName(), apiProductDTO.getName());
        Assert.assertEquals(apiProductInfoDTO.getProvider(), apiProductDTO.getProvider());
        Assert.assertEquals(apiProductInfoDTO.getContext(), apiProductDTO.getContext());
        Assert.assertEquals(apiProductInfoDTO.getDescription(), apiProductDTO.getDescription());
        Assert.assertEquals(apiProductInfoDTO.isHasThumbnail(), apiProductDTO.isHasThumbnail());
        Assert.assertEquals(new HashSet<>(apiProductInfoDTO.getSecurityScheme()),
                new HashSet<>(apiProductDTO.getSecurityScheme()), "Security Scheme does not match");
        Assert.assertEquals(apiProductInfoDTO.getState(), apiProductDTO.getState());
    }

    private void verifyApiDtoWithApiProduct(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO, APIProductDTO apiProductDTO) {

        Assert.assertEquals(apiDTO.getId(), apiProductDTO.getId());
        Assert.assertEquals(apiDTO.getAdditionalProperties(), apiProductDTO.getAdditionalProperties());
        verifyBusinessInformation(apiDTO.getBusinessInformation(), apiProductDTO.getBusinessInformation());

        String context = apiProductDTO.getContext();
        String version =  apiProductDTO.getVersion();
        if (context.startsWith("/{version}")) {
            Assert.assertEquals(apiDTO.getContext(), context.replace("{version}", version));
        } else {
            Assert.assertEquals(apiDTO.getContext(), context.concat("/").concat(version));
        }

        Assert.assertEquals(apiDTO.getDescription(), apiProductDTO.getDescription());
        Assert.assertEquals(apiDTO.getLifeCycleStatus(), apiProductDTO.getState());
        Assert.assertEquals(apiDTO.getName(), apiProductDTO.getName());
        verifyResources(apiDTO.getOperations(), apiProductDTO.getApis());
        Assert.assertEquals(apiDTO.getProvider(), apiProductDTO.getProvider());
        verifyScopes(apiDTO.getScopes(), apiProductDTO.getScopes());
        Assert.assertEquals(new HashSet<>(apiDTO.getSecurityScheme()), new HashSet<>(apiProductDTO.getSecurityScheme()));
        Assert.assertEquals(new HashSet<>(apiDTO.getTags()), new HashSet<>(apiProductDTO.getTags()));
        verifyPolicies(apiDTO.getTiers(), apiProductDTO.getPolicies());
        Assert.assertEquals(new HashSet<>(apiDTO.getTransport()), new HashSet<>(apiProductDTO.getTransport()));
    }

    private void verifyApiInfoDtoWithApiProduct(APIInfoDTO apiInfo, APIProductDTO apiProductDTO) {

        Assert.assertEquals(apiInfo.getContext(), apiProductDTO.getContext());
        Assert.assertEquals(apiInfo.getDescription(), apiProductDTO.getDescription());
        Assert.assertEquals(apiInfo.getId(), apiProductDTO.getId());
        Assert.assertEquals(apiInfo.getLifeCycleStatus(), apiProductDTO.getState());
        Assert.assertEquals(apiInfo.getName(), apiProductDTO.getName());
        Assert.assertEquals(apiInfo.getProvider(), apiProductDTO.getProvider());
        Assert.assertEquals(new HashSet<>(apiInfo.getThrottlingPolicies()), new HashSet<>(apiProductDTO.getPolicies()));
    }

    private void verifyBusinessInformation(APIBusinessInformationDTO portalBusinessInfo,
                                           APIProductBusinessInformationDTO publisherBusinessInfo) {

        Assert.assertEquals(portalBusinessInfo.getBusinessOwner(), publisherBusinessInfo.getBusinessOwner());
        Assert.assertEquals(portalBusinessInfo.getBusinessOwnerEmail(), publisherBusinessInfo.getBusinessOwnerEmail());
        Assert.assertEquals(portalBusinessInfo.getTechnicalOwner(), publisherBusinessInfo.getTechnicalOwner());
        Assert.assertEquals(portalBusinessInfo.getTechnicalOwnerEmail(), publisherBusinessInfo.getTechnicalOwnerEmail());
    }

    private void verifyResources(List<org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO> storeAPIOperations,
                                 List<ProductAPIDTO> publisherAPIProductOperations) {

        storeAPIOperations.sort((o1, o2) -> {
            if (o1.getTarget().equals(o2.getTarget())) {
                return o1.getVerb().compareTo(o2.getVerb());
            } else {
                return o1.getTarget().compareTo(o2.getTarget());
            }
        });

        List<APIOperationsDTO> productOperations = new ArrayList<>();
        for (ProductAPIDTO publisherAPIProductOperation : publisherAPIProductOperations) {
            productOperations.addAll(publisherAPIProductOperation.getOperations());
        }

        productOperations.sort((o1, o2) -> {
            if (o1.getTarget().equals(o2.getTarget())) {
                return o1.getVerb().compareTo(o2.getVerb());
            } else {
                return o1.getTarget().compareTo(o2.getTarget());
            }
        });

        Assert.assertEquals(storeAPIOperations.size(), productOperations.size());

        for (int i = 0; i < storeAPIOperations.size(); ++i) {
            org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO apiOperationsDTO = storeAPIOperations.get(i);
            APIOperationsDTO operationsDTO = productOperations.get(i);

            Assert.assertEquals(apiOperationsDTO.getTarget(), operationsDTO.getTarget());
            Assert.assertEquals(apiOperationsDTO.getVerb(), operationsDTO.getVerb());
        }
    }

    private void verifyScopes(List<ScopeInfoDTO> scopeInfoDTOs, List<APIScopeDTO> apiScopeDTOS) {

        scopeInfoDTOs.sort(Comparator.comparing(ScopeInfoDTO::getName));
        List<ScopeDTO> scopeDTOs = apiScopeDTOS.stream().map(APIScopeDTO::getScope).collect(Collectors.toList());
        scopeDTOs.sort(Comparator.comparing(ScopeDTO::getName));

        Assert.assertEquals(scopeInfoDTOs.size(), scopeDTOs.size());

        for (int i = 0; i < scopeInfoDTOs.size(); ++i) {
            ScopeInfoDTO scopeInfoDTO = scopeInfoDTOs.get(i);
            ScopeDTO scopeDTO = scopeDTOs.get(i);

            Assert.assertEquals(scopeInfoDTO.getDescription(), scopeDTO.getDescription());
            Assert.assertEquals(scopeInfoDTO.getName(), scopeDTO.getName());
            Assert.assertEquals(new HashSet<>(scopeInfoDTO.getRoles()), new HashSet<>(scopeDTO.getBindings()));
        }

    }

    private void verifyPolicies(List<APITiersDTO> apiTiersDTOs, List<String> policies) {

        Assert.assertEquals(apiTiersDTOs.size(), policies.size());

        apiTiersDTOs.sort(Comparator.comparing(APITiersDTO::getTierName));
        policies.sort(Comparator.naturalOrder());

        for (int i = 0; i < apiTiersDTOs.size(); ++i) {
            APITiersDTO apiTiersDTO = apiTiersDTOs.get(i);
            Assert.assertEquals(apiTiersDTO.getTierName(), policies.get(i));
        }
    }

}
