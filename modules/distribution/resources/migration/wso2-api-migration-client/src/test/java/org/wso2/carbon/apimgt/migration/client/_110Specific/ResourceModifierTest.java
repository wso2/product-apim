/*
 *
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
 *
 */

package org.wso2.carbon.apimgt.migration.client._110Specific;

import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;


public class ResourceModifierTest {
    private static final String FILE_NAME_WORK_FLOW_EXTENSIONS_19 = "1.9-workflow-extensions.xml";
    private static final String FILE_NAME_WORK_FLOW_EXTENSIONS_110 = "1.10-workflow-extensions.xml";
    private static final String FILE_NAME_WORK_DEFAULT_TIERS_19 = "1.9-default-tiers.xml";
    private static final String FILE_NAME_WORK_DEFAULT_TIERS_110 = "1.10-default-tiers.xml";
    private static final String FILE_NAME_API_LIFE_CYCLE = "APILifeCycle.xml";
    private static final String FILE_NAME_EXECUTORLESS_API_LIFE_CYCLE = "APILifeCycleWithoutExecutors.xml";

    InputStream workflow19Stream;
    InputStream workflow110Stream;
    InputStream tiers19Stream;
    InputStream tiers110Stream;
    InputStream lifeCycleStream;
    InputStream executorlessLifeCycleStream;

    @BeforeClass
    public void setup() {
        workflow19Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE_NAME_WORK_FLOW_EXTENSIONS_19);
        workflow110Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE_NAME_WORK_FLOW_EXTENSIONS_110);
        tiers19Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE_NAME_WORK_DEFAULT_TIERS_19);
        tiers110Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE_NAME_WORK_DEFAULT_TIERS_110);
        lifeCycleStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE_NAME_API_LIFE_CYCLE);
        executorlessLifeCycleStream = Thread.currentThread().getContextClassLoader().
                                            getResourceAsStream(FILE_NAME_EXECUTORLESS_API_LIFE_CYCLE);
    }

    @AfterClass
    public void tearDown() {
        try {
            workflow19Stream.close();
            workflow110Stream.close();
            tiers19Stream.close();
            tiers110Stream.close();
            lifeCycleStream.close();
            executorlessLifeCycleStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testModifyWorkFlowExtensions() throws Exception {
        String workFlowExtensions19 = IOUtils.toString(workflow19Stream);
        String modifiedWorkFlowExtensions = ResourceModifier.modifyWorkFlowExtensions(workFlowExtensions19);

        Document modifiedDoc = ResourceUtil.buildDocument(modifiedWorkFlowExtensions, "modifiedWorkFlowExtensions");
        Document expectedDoc = ResourceUtil.buildDocument(workflow110Stream, FILE_NAME_WORK_FLOW_EXTENSIONS_110);

        Assert.assertEquals(compareXMLDocuments(expectedDoc, modifiedDoc), true);
    }

    @Test
    public void testModifyTiers() throws Exception {
        String tiers19 = IOUtils.toString(tiers19Stream);
        String modifiedTiers = ResourceModifier.modifyTiers(tiers19, "tiers.xml");

        Document modifiedDoc = ResourceUtil.buildDocument(modifiedTiers, "modifiedTiers");
        Document expectedDoc = ResourceUtil.buildDocument(tiers110Stream, FILE_NAME_WORK_DEFAULT_TIERS_110);

        Assert.assertEquals(compareXMLDocuments(expectedDoc, modifiedDoc), true);
    }

    private boolean compareXMLDocuments(Document expected, Document actual) {
        Element expectedDocElement = expected.getDocumentElement();
        Element actualDocElement = actual.getDocumentElement();

        if (!expectedDocElement.getTagName().equals(actualDocElement.getTagName())) {
            return false;
        }

        NodeList expectedNodes = expectedDocElement.getChildNodes();
        NodeList actualNodes =  actualDocElement.getChildNodes();

        if (expectedNodes.getLength() != actualNodes.getLength()) {
            return false;
        }

        for (int i = 0; i < expectedNodes.getLength(); ++i) {
            Node expectedNode = expectedNodes.item(i);
            Node actualNode = actualNodes.item(i);

            if (expectedNode.getNodeType() != actualNode.getNodeType()) {
                return false;
            }

            if (expectedNode.getNodeType() == Node.ELEMENT_NODE) {
                if (!((Element)expectedNode).getTagName().equals(((Element)actualNode).getTagName())) {
                    return false;
                }
            }
            else if (expectedNode.getNodeType() == Node.COMMENT_NODE){
                String expectedText = expectedNode.getTextContent();
                expectedText = expectedText.replaceAll("\\s", "");
                String actualText = actualNode.getTextContent();
                actualText = actualText.replaceAll("\\s", "");

                if (!expectedText.equals(actualText)) {
                    return false;
                }
            }

        }

        return true;
    }


    @Test
    public void testRemoveExecutorsFromAPILifeCycle() throws Exception {
        //URL apiLifeCycleURL = Thread.currentThread().getContextClassLoader().getResource(FILE_NAME_API_LIFE_CYCLE);
        String apiLifeCycle = IOUtils.toString(lifeCycleStream);
        String modifiedLifeCycle = ResourceModifier.removeExecutorsFromAPILifeCycle(apiLifeCycle);

        Document modifiedDoc = ResourceUtil.buildDocument(modifiedLifeCycle, "modifiedLifeCycle");
        Document expectedDoc = ResourceUtil.buildDocument(executorlessLifeCycleStream,
                                                                        FILE_NAME_EXECUTORLESS_API_LIFE_CYCLE);

        Assert.assertEquals(compareXMLDocuments(expectedDoc, modifiedDoc), true);
    }
}