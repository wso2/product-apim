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

package org.wso2.am.integration.cucumbertests.utils.clients;

import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/** Focused regressions for SOAP-only MIME decoding, including the captured registry metadata response. */
public class SoapResponseDecodingTest {

    private static final String SOAP = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<s:Body><value>registry résumé</value></s:Body></s:Envelope>";

    @Test
    public void capturedRegistryResponseContainsReadableCollectionMetadata() throws Exception {
        Properties metadata = new Properties();
        try (InputStream stream = resource("registry-mtom-response.properties")) {
            metadata.load(stream);
        }
        byte[] body;
        try (InputStream stream = resource("registry-mtom-response.bin")) {
            body = stream.readAllBytes();
        }
        var response = SimpleHTTPClient.constructResponse(response(body, metadata.getProperty("contentType"), 200), true);
        Assert.assertTrue(response.getData().startsWith("<?xml"));
        Assert.assertEquals(Utils.getNodeTextsByXPath(response.getData(), "//*[local-name()='resourceType']"),
                java.util.List.of("collection"));
        Assert.assertTrue(response.getData().contains("/_system"));
        Assert.assertFalse(response.getData().contains("--MIMEBoundary"));
    }

    @Test
    public void rootContentIdSelectsSoapAfterAnAttachmentAndDecodesBase64() throws Exception {
        String boundary = "focused-boundary";
        String body = "--" + boundary + "\r\nContent-Type: application/octet-stream\r\n"
                + "Content-ID: <attachment>\r\n\r\nnot XML\r\n"
                + "--" + boundary + "\r\nContent-Type: application/xop+xml; charset=UTF-8; type=\"text/xml\"\r\n"
                + "Content-Transfer-Encoding: base64\r\nContent-ID: <soap-root>\r\n\r\n"
                + Base64.getEncoder().encodeToString(SOAP.getBytes(StandardCharsets.UTF_8))
                + "\r\n--" + boundary + "--\r\n";
        String contentType = "multipart/related; boundary=\"" + boundary
                + "\"; type=\"application/xop+xml\"; start=\"<soap-root>\"; start-info=\"text/xml\"";
        var response = SimpleHTTPClient.constructResponse(response(body.getBytes(StandardCharsets.UTF_8),
                contentType, 500), true);
        Assert.assertEquals(response.getData(), SOAP);
        Assert.assertEquals(response.getResponseCode(), 500);
        Assert.assertEquals(response.getResponseMessage(), "Focused SOAP status");
        Assert.assertEquals(response.getHeaders().get("cOnTeNt-TyPe"), contentType);
        Assert.assertEquals(response.getHeaders().get("X-Focused-Header"), "preserved");
    }

    @Test
    public void plainSoapIsUnchanged() throws Exception {
        var response = SimpleHTTPClient.constructResponse(response(SOAP.getBytes(StandardCharsets.UTF_8),
                "text/xml; charset=UTF-8", 200), true);
        Assert.assertEquals(response.getData(), SOAP);
        Assert.assertEquals(response.getResponseCode(), 200);
    }

    @Test
    public void bodylessSoapIsUnchanged() throws Exception {
        BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 202, "Accepted");
        var decoded = SimpleHTTPClient.constructResponse(response, true);
        Assert.assertEquals(decoded.getData(), "");
        Assert.assertEquals(decoded.getResponseCode(), 202);
    }

    @Test
    public void ordinaryHttpRetainsRawMultipartBody() throws Exception {
        byte[] body;
        try (InputStream stream = resource("registry-mtom-response.bin")) {
            body = stream.readAllBytes();
        }
        var response = SimpleHTTPClient.constructResponse(response(body,
                "multipart/related; boundary=\"unused-for-ordinary-http\"", 200), false);
        Assert.assertEquals(response.getData(), new String(body, StandardCharsets.UTF_8));
    }

    @Test(expectedExceptions = IOException.class)
    public void malformedMultipartFailsWithAnIoException() throws Exception {
        SimpleHTTPClient.constructResponse(response("not a MIME package".getBytes(StandardCharsets.UTF_8),
                "multipart/related; boundary=\"missing\"; type=\"application/xop+xml\"", 200), true);
    }

    private static BasicHttpResponse response(byte[] body, String contentType, int status) {
        BasicHttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, "Focused SOAP status");
        response.setEntity(new ByteArrayEntity(body));
        response.addHeader("cOnTeNt-TyPe", contentType);
        response.addHeader("X-Focused-Header", "preserved");
        return response;
    }

    private static InputStream resource(String name) throws IOException {
        InputStream stream = SoapResponseDecodingTest.class.getResourceAsStream("/artifacts/soap/" + name);
        if (stream == null) {
            throw new IOException("Missing SOAP regression fixture " + name);
        }
        return stream;
    }
}
