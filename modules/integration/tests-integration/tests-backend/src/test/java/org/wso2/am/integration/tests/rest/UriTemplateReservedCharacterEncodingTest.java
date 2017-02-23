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
 */
package org.wso2.am.integration.tests.rest;

import org.apache.axis2.AxisFault;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.io.File;
import javax.xml.xpath.XPathExpressionException;

/*
This class provides test cases for APIMANAGER-5469
 */
public class UriTemplateReservedCharacterEncodingTest extends APIMIntegrationBaseTest {
    private LogViewerClient logViewerClient;
    String gatewaySessionCookie;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, AxisFault, XPathExpressionException {
        String synapseConfFile = "uri-template-encoding.xml";
        super.init();
        gatewaySessionCookie = createSession(gatewayContextMgt);
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + synapseConfFile, gatewayContextMgt, gatewaySessionCookie);
        logViewerClient = new LogViewerClient(
                gatewayContextMgt.getContextUrls().getBackEndUrl(), createSession(gatewayContextMgt));
    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a query param consist of" +
            " reserved character : ")
    public void testURITemplateExpandWithPercentEncoding() throws Exception {
        boolean isPercentEncoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/urlEncoded?queryParam=APIM:WSO2"),null);
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains("APIM%3AWSO2")) {
                isPercentEncoded = true;
                break;
            }
        }
        Assert.assertTrue(isPercentEncoded,
                "Reserved character should be percent encoded while uri-template expansion");

    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a query param consist of reserved " +
            "character : with percent encoding escaped at uri-template expansion")
    public void testURITemplateExpandWithEscapedPercentEncoding() throws Exception {
        boolean isPercentEncoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/escapeUrlEncoded?queryParam=APIM:WSO2"),
                null);
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains("APIM%3AWSO2")) {
                isPercentEncoded = true;
                break;
            }
        }
        Assert.assertFalse(isPercentEncoded,
                "Reserved character should not be percent encoded while uri-template expansion as escape enabled");

    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a path param consist of" +
            " reserved character : ")
    public void testURITemplateExpandWithPercentEncodingPathParamCase() throws Exception {
        boolean isPercentEncoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/urlEncoded/APIM:WSO2"),
                null);
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains("To: /services/test_2/APIM%3AWSO2")) {
                isPercentEncoded = true;
                break;
            }
        }
        Assert.assertTrue(isPercentEncoded,
                "Reserved character should be percent encoded while uri-template expansion");

    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a path param consist of reserved " +
            "character : with percent encoding escaped at uri-template expansion")
    public void testURITemplateExpandWithEscapedPercentEncodingPathParam() throws Exception {
        boolean isPercentEncoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/escapeUrlEncoded/APIM:WSO2"),
                null);
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains("To: /services/test_2/APIM%3AWSO2")) {
                isPercentEncoded = true;
                break;
            }
        }
        Assert.assertFalse(isPercentEncoded,
                "Reserved character should not be percent encoded while uri-template expansion as escape enabled");

    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a query param consist of" +
            " reserved space character ")
    public void testURITemplateParameterDecodingSpaceCharacterCase() throws Exception {
        boolean isPercentEncoded = false;
        boolean isMessageContextPropertyPercentDecoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/urlEncoded?queryParam=APIM%20WSO2"),
                null);
        String decodedMessageContextProperty="decodedQueryParamValue = APIM WSO2";
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains(decodedMessageContextProperty)) {
                isMessageContextPropertyPercentDecoded = true;
                continue;
            }
            if (message.contains("APIM%20WSO2")) {
                isPercentEncoded = true;
                continue;
            }
        }
        Assert.assertTrue(isMessageContextPropertyPercentDecoded,
                "Uri-Template parameters should be percent decoded at message context property");
        Assert.assertTrue(isPercentEncoded,
                "Reserved character should be percent encoded while uri-template expansion");
    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a query param consist of" +
            " reserved space character ")
    public void testURITemplateParameterDecodingTrailingPercentageCase() throws Exception {
        boolean isPercentEncoded = false;
        boolean isMessageContextPropertyPercentDecoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/urlEncoded?queryParam=aaa%21%40%21%25"),
                null);
        String decodedMessageContextProperty="decodedQueryParamValue = aaa!@!%";
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains(decodedMessageContextProperty)) {
                isMessageContextPropertyPercentDecoded = true;
                continue;
            }
            if (message.contains("aaa%21%40%21%25")) {
                isPercentEncoded = true;
                continue;
            }
        }
        Assert.assertTrue(isMessageContextPropertyPercentDecoded,
                "Uri-Template parameters should be percent decoded at message context property");
        Assert.assertTrue(isPercentEncoded,
                "Reserved character should be percent encoded while uri-template expansion");
    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a query param consist of" +
            " reserved + character ")
    public void testURITemplateParameterDecodingPlusCharacterCase() throws Exception {
        boolean isPercentEncoded = false;
        boolean isMessageContextPropertyPercentDecoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/urlEncoded?queryParam=APIM+WSO2"),
                null);
        String decodedMessageContextProperty="decodedQueryParamValue = APIM+WSO2";
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains(decodedMessageContextProperty)) {
                isMessageContextPropertyPercentDecoded = true;
                continue;
            }
            if (message.contains("APIM%2BWSO2")) {
                isPercentEncoded = true;
                continue;
            }
        }
        Assert.assertTrue(isMessageContextPropertyPercentDecoded,
                "Uri-Template parameters should be percent decoded at message context property");
        Assert.assertTrue(isPercentEncoded,
                "Reserved character should be percent encoded while uri-template expansion");
    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a query param consist of" +
            " reserved + character ")
    public void testURITemplateParameterDecodingWithPercentEncodingEscapedAtExpansion() throws Exception {
        boolean isPercentEncoded = false;
        boolean isMessageContextPropertyPercentDecoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(
                getAPIInvocationURLHttp("services/client/escapeUrlEncoded?queryParam=APIM+WSO2"),
                null);
        String decodedMessageContextProperty="decodedQueryParamValue = APIM+WSO2";
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains(decodedMessageContextProperty)) {
                isMessageContextPropertyPercentDecoded = true;
                continue;
            }
            if (message.contains("APIM%2BWSO2")) {
                isPercentEncoded = true;
                continue;
            }
        }
        Assert.assertTrue(isMessageContextPropertyPercentDecoded,
                "Uri-Template parameters should be percent decoded at message context property");
        Assert.assertFalse(isPercentEncoded,
                "Reserved character should not be percent encoded while uri-template expansion");
    }

    @Test(groups = { "wso2.am" }, description = "Sending http request with a path param consist of" +
            " whole URL including protocol , host , port etc. ")
    public void testURITemplateSpecialCaseVariableWithFullURL() throws Exception {
        boolean isPercentEncoded = false;
        logViewerClient.clearLogs();
        HttpResponse response = HttpRequestUtil.sendGetRequest(getAPIInvocationURLHttp(
                "services/client/special_case/" + getAPIInvocationURLHttp("services/test_2") + "/special_case"),
                null);
        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains("To: /services/test_2/special_case")) {
                isPercentEncoded = true;
                break;
            }
        }
        Assert.assertTrue(isPercentEncoded,
                "The Special case of of Full URL expansion should be identified and should not percent encode full URL");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
