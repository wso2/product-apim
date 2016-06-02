/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.carbon.apimgt.migration.client._200Specific;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.SynapseDTO;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.SynapseUtil;

import java.util.List;

public class ResourceModifier200 {
    public static void updateSynapseConfigs(List<SynapseDTO> synapseDTOs) {

        for (SynapseDTO synapseDTO : synapseDTOs) {
            Element existingThrottleHandler = SynapseUtil
                    .getHandler(synapseDTO.getDocument(), Constants.SYNAPSE_API_VALUE_THROTTLE_HANDLER);

            if (existingThrottleHandler != null && !isThrottleHandlerUpdated(existingThrottleHandler)) {
                Element updatedThrottleHandler = SynapseUtil
                        .createHandler(synapseDTO.getDocument(), Constants.SYNAPSE_API_VALUE_THROTTLE_HANDLER, null);
                SynapseUtil.updateHandler(synapseDTO.getDocument(), Constants.SYNAPSE_API_VALUE_THROTTLE_HANDLER,
                        updatedThrottleHandler);
            }

            Element handlersElement = (Element) synapseDTO.getDocument()
                    .getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_HANDLERS)
                    .item(0);
            Element newAPIMgtLatencyStatsHandler = synapseDTO.getDocument()
                    .createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_HANDLER);
            newAPIMgtLatencyStatsHandler.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS,
                    Constants.SYNAPSE_API_VALUE_LATENCY_STATS_HANDLER);
            handlersElement.insertBefore(newAPIMgtLatencyStatsHandler, handlersElement.getFirstChild());
        }
    }

    private static boolean isThrottleHandlerUpdated(Element existingThrottleHandler) {
        NodeList properties = existingThrottleHandler.getChildNodes();

        for (int i = 0; i < properties.getLength(); ++i) {
            Node childNode = properties.item(i);

            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                String nameAttribute = ((Element) childNode).getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME);

                if (nameAttribute != null) {
                    return Constants.SYNAPSE_API_VALUE_RESOURCE_KEY.equals(nameAttribute);
                }
            }
        }

        return false;
    }
}
