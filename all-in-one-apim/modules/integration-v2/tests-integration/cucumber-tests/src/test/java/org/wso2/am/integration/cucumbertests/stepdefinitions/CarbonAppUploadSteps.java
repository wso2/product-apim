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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.axis2.client.Stub;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.application.mgt.stub.upload.CarbonAppUploaderStub;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.am.testcontainers.ApimRuntime;
import org.testcontainers.containers.Container;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** Uploads the legacy CAR through CarbonAppUploader and verifies it at the documented Carbon deployment path. */
public class CarbonAppUploadSteps {

    private static final String CAR_FILE = "artifacts/carbonapps/carapp_1.0.0.car";
    private static final String CAR_FILE_NAME = "carapp_1.0.0.car";
    private static final Protocol TEST_HTTPS_PROTOCOL = new Protocol("https", new TrustAllSocketFactory(), 443);

    private File carFile;

    @Given("the legacy CAR fixture is available")
    public void theLegacyCarFixtureIsAvailable() throws IOException {
        URL resource = getClass().getClassLoader().getResource(CAR_FILE);
        Assert.assertNotNull(resource, "CAR fixture is missing from test resources: " + CAR_FILE);
        try {
            carFile = Paths.get(resource.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid CAR fixture URI: " + resource, e);
        }
        Assert.assertTrue(carFile.isFile() && carFile.length() > 0,
                "CAR fixture is missing or empty: " + carFile.getAbsolutePath());
    }

    @When("I upload the CAR as the acting Carbon administrator")
    public void iUploadCarAsActingCarbonAdministrator() throws Exception {
        User actor = Identity.actingActor();
        CarbonAppUploaderStub uploader = new CarbonAppUploaderStub(serviceUrl("CarbonAppUploader"));
        authenticate(uploader, actor);

        UploadedFileItem item = new UploadedFileItem();
        item.setFileName(CAR_FILE_NAME);
        // Carbon's uploader contract represents CAR archives as jar file items (same as the legacy client).
        item.setFileType("jar");
        item.setDataHandler(new DataHandler(new FileDataSource(carFile)));
        uploader.uploadApp(new UploadedFileItem[] {item});
    }

    @Then("the uploaded CAR should be present in the Carbon applications deployment directory")
    public void theUploadedCarShouldBePresentInCarbonApplicationsDirectory() throws Exception {
        Object container = TestContext.get("blockApimContainer");
        Assert.assertTrue(container instanceof ApimRuntime, "No live APIM container is available for CAR readback");
        String tomlPath = ((ApimRuntime) container).getContainerTomlPath();
        String serverHome = tomlPath.substring(0, tomlPath.indexOf("/repository/conf/"));
        String containerPath = serverHome + "/repository/deployment/server/carbonapps/" + CAR_FILE_NAME;
        Container.ExecResult result = ((ApimRuntime) container).execInContainer("sh", "-c",
                "test -f '" + containerPath + "' && sha256sum '" + containerPath + "'");
        Assert.assertEquals(result.getExitCode(), 0,
                "CarbonAppUploader did not persist the CAR under repository/deployment/server/carbonapps: "
                        + result.getStderr());
        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(java.nio.file.Files.readAllBytes(carFile.toPath())));
        Assert.assertTrue(result.getStdout().startsWith(expectedHash),
                "The persisted CAR differs from the uploaded fixture: " + result.getStdout());
    }

    private static String serviceUrl(String service) {
        String baseUrl = Utils.getBaseUrl();
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "services/" + service;
    }

    private static void authenticate(Stub stub, User actor) throws RemoteException {
        // The test HTTP client uses a dedicated trust-all context instead of the host JVM's default trust store.
        // Give the legacy Axis2/Commons-HttpClient stubs the same TLS behavior without changing global protocols.
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.CUSTOM_PROTOCOL_HANDLER, TEST_HTTPS_PROTOCOL);
        CarbonUtils.setBasicAccessSecurityHeaders(actor.getUserName(), actor.getPassword(), stub._getServiceClient());
    }

    /** Per-stub HTTPS socket factory compatible with the self-signed certificate on the dynamic APIM container. */
    private static final class TrustAllSocketFactory implements SecureProtocolSocketFactory {

        private final SSLSocketFactory delegate;

        private TrustAllSocketFactory() {
            try {
                X509TrustManager trustAll = new X509TrustManager() {
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) { }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) { }
                };
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[] {trustAll}, new SecureRandom());
                delegate = context.getSocketFactory();
            } catch (Exception e) {
                throw new IllegalStateException("Could not initialize the test HTTPS socket factory", e);
            }
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return delegate.createSocket(host, port);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
            return delegate.createSocket(host, port, localAddress, localPort);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
                                  HttpConnectionParams params) throws IOException, ConnectTimeoutException {
            Socket socket = new Socket();
            socket.bind(new InetSocketAddress(localAddress, localPort));
            socket.connect(new InetSocketAddress(host, port), params.getConnectionTimeout());
            return delegate.createSocket(socket, host, port, true);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return delegate.createSocket(socket, host, port, autoClose);
        }
    }
}
