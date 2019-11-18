/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ProductAPIDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DtoUtils {

    public static APIProductDTO createApiProductDTO(String provider, String name, String context, List<ProductAPIDTO> apis) {
        return new APIProductDTO().
                accessControl(APIProductDTO.AccessControlEnum.NONE).
                visibility(APIProductDTO.VisibilityEnum.PUBLIC).
                apis(apis).
                context(context).
                name(name).
                provider(provider);
    }

    public static List<ProductAPIDTO> getProductApiResources(Map<APIDTO, Set<APIOperationsDTO>> selectedResources) {
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
}
