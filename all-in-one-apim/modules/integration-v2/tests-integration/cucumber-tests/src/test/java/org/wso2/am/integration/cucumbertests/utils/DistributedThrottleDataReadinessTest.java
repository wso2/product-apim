/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/** Regressions for readiness XML parsing with the integration suite's legacy XML libraries present. */
public class DistributedThrottleDataReadinessTest {

    @Test
    public void parsesNamespacedPublisherConfiguration() throws Exception {
        var document = DistributedThrottleDataReadiness.parseXml(
                "<eventPublisher xmlns=\"http://wso2.org/carbon/eventpublisher\" name=\"jmsEventPublisher\">"
                        + "<from streamName=\"org.wso2.throttle.globalThrottle.stream\" version=\"1.0.0\"/>"
                        + "</eventPublisher>");
        Assert.assertEquals(document.getDocumentElement().getLocalName(), "eventPublisher");
        Assert.assertEquals(document.getElementsByTagNameNS("*", "from").getLength(), 1);
    }

    @Test
    public void parsesNamespacedBrokerResponse() throws Exception {
        var document = DistributedThrottleDataReadiness.parseXml(
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                        + "<s:Body><a:getSubscriptionsResponse xmlns:a=\"http://wso2.org/carbon/andes/admin/xsd\">"
                        + "<a:return><a:active>true</a:active></a:return>"
                        + "</a:getSubscriptionsResponse></s:Body></s:Envelope>");
        Assert.assertEquals(document.getElementsByTagNameNS("*", "getSubscriptionsResponse").getLength(), 1);
        Assert.assertEquals(document.getElementsByTagNameNS("*", "active").item(0).getTextContent(), "true");
    }

    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".*DOCTYPE.*")
    public void rejectsDocumentTypes() throws Exception {
        DistributedThrottleDataReadiness.parseXml("<!DOCTYPE root [<!ENTITY value 'unexpected'>]>"
                + "<root>&value;</root>");
    }
}
