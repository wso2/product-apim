/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.streamingapis.websub.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.tests.streamingapis.StreamingApiTestUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Represents the test webhook sender.
 */
public class WebhookSender {
    private final Log log = LogFactory.getLog(WebhookSender.class);
    private String payloadUrl;
    private String secretKey;

    private AtomicInteger webhooksSent = new AtomicInteger(0);

    public WebhookSender(String payloadUrl, String secret) {
        this.payloadUrl = payloadUrl;
        this.secretKey = secret;
    }

    public int getWebhooksSent() {
        return webhooksSent.get();
    }

    public void setWebhooksSent(int webhooksSent) {
        this.webhooksSent.set(webhooksSent);
    }

    public void send() throws Exception {
        String body = "{\"Hello\" : \"World\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("x-hub-signature", generateXHubSignature(body));
        HttpResponse response = HttpRequestUtil.doPost(new URL(payloadUrl), body, headers);
        if (response.getResponseCode() == 200) {
            webhooksSent.incrementAndGet();
            log.info("Webhook sent successfully");
        } else {
            log.error("Webhook was not successfully sent");
        }
    }

    private String generateXHubSignature(String body) throws InvalidKeyException, NoSuchAlgorithmException {
        return "sha1=" + StreamingApiTestUtils.calculateRFC2104HMAC(body, secretKey);
    }

}
