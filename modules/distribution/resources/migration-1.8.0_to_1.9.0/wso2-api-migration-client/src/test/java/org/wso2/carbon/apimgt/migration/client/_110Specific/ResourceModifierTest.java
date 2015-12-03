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
import org.xml.sax.InputSource;

import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class ResourceModifierTest {
    InputStream workflow19Stream;
    InputStream workflow110Stream;
    InputStream tiers19Stream;
    InputStream tiers110Stream;

    @BeforeClass
    public void setup() {
        workflow19Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("1.9-workflow-extensions.xml");
        workflow110Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("1.10-workflow-extensions.xml");
        tiers19Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("1.9-default-tiers.xml");
        tiers110Stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("1.10-default-tiers.xml");
    }

    @AfterClass
    public void tearDown() {
        try {
            workflow19Stream.close();
            workflow110Stream.close();
            tiers19Stream.close();
            tiers110Stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testModifyWorkFlowExtensions() throws Exception {
        String workFlowExtensions19 = IOUtils.toString(workflow19Stream);
        String modifiedWorkFlowExtensions = ResourceModifier.modifyWorkFlowExtensions(workFlowExtensions19);

        String workFlowExtensions110 = IOUtils.toString(workflow110Stream);

        Document modifiedDoc = loadXMLFromString(modifiedWorkFlowExtensions);
        Document expectedDoc = loadXMLFromString(workFlowExtensions110);

        Assert.assertEquals(compareXMLDocuments(expectedDoc, modifiedDoc), true);
    }

    @Test
    public void testModifyTiers() throws Exception {
        String tiers19 = IOUtils.toString(tiers19Stream);
        String modifiedTiers = ResourceModifier.modifyTiers(tiers19, "tiers.xml");

        System.out.println(modifiedTiers);

        String tiers110 = IOUtils.toString(tiers110Stream);

        Document modifiedDoc = loadXMLFromString(modifiedTiers);
        Document expectedDoc = loadXMLFromString(tiers110);

        Assert.assertEquals(compareXMLDocuments(expectedDoc, modifiedDoc), true);
    }


    private Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
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

    }
}