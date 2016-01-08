/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

package org.wso2.carbon.apimgt.migration.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.APIHandlersDTO;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.HandlerPropertyDTO;

import java.util.List;

public class SynapseUtil {

    private static class ChildElementInfo {
        private Element element;
        private int position;

        public Element getElement() {
            return element;
        }

        public void setElement(Element element) {
            this.element = element;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }
    }

    public static APIHandlersDTO getHandlers(Document document) {
        Element handlersElement = (Element) document.getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_HANDLERS).item(0);
        NodeList handlerNodes = handlersElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_HANDLER);

        APIHandlersDTO apiHandlersDTO = new APIHandlersDTO();
        apiHandlersDTO.setHandlersElement(handlersElement);
        apiHandlersDTO.setHandlerNodes(handlerNodes);

        return apiHandlersDTO;
    }


    public static Element getHandler(Document document, final String matchingClassName) {
        APIHandlersDTO apiHandlersDTO =  getHandlers(document);
        ChildElementInfo childElementInfo = getHandler(apiHandlersDTO, matchingClassName);

        if (childElementInfo != null) {
            return childElementInfo.getElement();
        }

        return null;
    }


    public static Element createHandler(Document document, final String className,
                                                            List<HandlerPropertyDTO> propertieDTOs) {
        Element handler = document.createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_HANDLER);
        handler.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS, className);

        if (propertieDTOs != null) {
            for (HandlerPropertyDTO propertyDTO : propertieDTOs) {
                Element property = document.createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_PROPERTY);
                property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME, propertyDTO.getName());
                property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VALUE, propertyDTO.getValue());

                handler.appendChild(property);
            }
        }

        return handler;
    }

    public static void updateHandler(Document document, final String matchingClassName, Element updatedHandler) {
        String className = updatedHandler.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS);

        if (!matchingClassName.equals(className)) {
            throw new IllegalArgumentException("Attempting to replace a different element than what is provided. " +
                    "Class attribute of element to be replaced : " + matchingClassName + ", class attribute of updated " +
                    "element : " + className);
        }

        APIHandlersDTO apiHandlersDTO = getHandlers(document);

        ChildElementInfo childElementInfo = getHandler(apiHandlersDTO, matchingClassName);

        if (childElementInfo != null) {
            apiHandlersDTO.getHandlersElement().removeChild(childElementInfo.getElement());
            Node followingNode = apiHandlersDTO.getHandlerNodes().item(childElementInfo.getPosition());
            apiHandlersDTO.getHandlersElement().insertBefore(updatedHandler, followingNode);
        }
    }


    private static ChildElementInfo getHandler(APIHandlersDTO apiHandlersDTO, final String matchingClassName) {
        NodeList handlers = apiHandlersDTO.getHandlerNodes();

        for (int i = 0; i < handlers.getLength(); ++i) {
            Element handler = (Element) handlers.item(i);

            String className = handler.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS);

            if (matchingClassName.equals(className)) {
                ChildElementInfo childElementInfo =  new ChildElementInfo();
                childElementInfo.setElement(handler);
                childElementInfo.setPosition(i);

                return childElementInfo;
            }
        }

        return null;
    }
}
