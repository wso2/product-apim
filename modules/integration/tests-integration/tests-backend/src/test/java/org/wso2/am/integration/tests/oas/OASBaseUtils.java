/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.oas;

import org.junit.Assert;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OASBaseUtils {

    public static void validateSwaggerExtensionDataInPublisher(Map<String, Object> extensions, APIDTO apidto) {
        String authorizationHeader = apidto.getAuthorizationHeader();
        if(authorizationHeader == null){
            Assert.assertFalse(extensions.containsKey("x-wso2-auth-header"));
        } else {
            Assert.assertTrue(extensions.containsKey("x-wso2-auth-header"));
            Assert.assertEquals(authorizationHeader, extensions.get("x-wso2-auth-header"));
        }
        if (apidto.getApiThrottlingPolicy() != null) {
            Assert.assertTrue(extensions.containsKey("x-throttling-tier"));
            Assert.assertEquals(apidto.getApiThrottlingPolicy(), extensions.get("x-throttling-tier"));
        } else {
            Assert.assertFalse(extensions.containsKey("x-throttling-tier"));
        }

        String context = apidto.getContext() + "/" + apidto.getVersion();
        String organization = MultitenantUtils.getTenantDomain(apidto.getProvider());
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(organization) &&
                !context.contains("/t/" + organization)) {
            context = "/t/" + organization + "" + context;
        }
        Assert.assertEquals(context, extensions.get("x-wso2-basePath"));
        Assert.assertTrue(extensions.containsKey("x-wso2-cors"));
        Assert.assertTrue(extensions.containsKey("x-wso2-production-endpoints"));
        Assert.assertTrue(extensions.containsKey("x-wso2-sandbox-endpoints"));
        Assert.assertTrue(extensions.containsKey("x-wso2-transports"));
    }

    public static Map<String, Map<String, APIOperationsDTO>> getMapFromDTO(APIDTO apidto) {
        List<APIOperationsDTO> dtos = apidto.getOperations();
        Map<String, Map<String, APIOperationsDTO>> map = new HashMap<>();
        for (APIOperationsDTO dto : dtos) {
            if (!map.containsKey(dto.getTarget())) {
                map.put(dto.getTarget(), new HashMap<>());
            }
            Map<String, APIOperationsDTO> ops = map.get(dto.getTarget());
            if (!ops.containsKey(dto.getVerb())) {
                ops.put(dto.getVerb(), dto);
            }
        }
        return map;
    }
}
