/* * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * under the License. */


package org.wso2.am.integration.tests.jwt;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.testng.Assert.assertEquals;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class JWTRevocationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(JWTRevocationTestCase.class);
    private String consumerKey;
    private String consumerSecret;
    private String userName;
    private String password;
    private String jti;
    private String jtiExtracted;
    private final String KEYSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "wso2carbon.jks";
    private final String TRUSTSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "client-truststore.jks";
    private TopicConnection topicConnection;
    private TopicSession topicConsumerSession;
    private MessageConsumer consumer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        log.info("JWTRevocationTestCase  Initiated");
        APIPublisherRestClient apiPublisher;
        String publisherURLHttp;
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

        ServerConfigurationManager serverConfigurationManager;
        super.init();

        userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();

        //Setting the system properties to call the etcd endpoint
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        String apiManagerXml =
                getAMResourceLocation() + File.separator + "configFiles/revokejwttest/" + "api-manager.xml";
        String jndiPropeties =
                getAMResourceLocation() + File.separator + "configFiles/revokejwttest/" + "jndi.properties";

        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(apiManagerXml));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(jndiPropeties));
        serverConfigurationManager.restartGracefully();

        //Deploying the Mock ETCD Server
        String webAppName = "etcdmock";
        String sourcePath = org.wso2.am.integration.test.utils.generic.TestConfigurationProvider.getResourceLocation()
                + File.separator + "artifacts" + File.separator + "AM" + File.separator + "war" + File.separator
                + webAppName + ".war";
        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(),
                sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        WebAppDeploymentUtil
                .isWebApplicationDeployed(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webAppName);

        //Create an API and Publish it in the store
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher.login(userName, password);
        APIRequest apiRequest = new APIRequest("test", "test", new URL("http://localhost:6789"));
        apiRequest.setVisibility("public");
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest("test", userName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Create an Application with TokenType as JWT to retrieve consumer key and consumer secret
        apiStore.login(userName, password);
        apiStore.addApplicationWithTokenType("JWTTokenTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test", "JWT");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("test", userName);
        apiStore.subscribe(subscriptionRequest);
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("JWTTokenTestAPI-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        consumerKey = response.getJSONObject("data").getJSONObject("key").get("consumerKey").toString();
        consumerSecret = response.getJSONObject("data").getJSONObject("key").get("consumerSecret").toString();

        try {
            //create a subscriber to jwRevocation topic in the JMS broker
            createJMSConnection();
        } catch (NamingException e) {
            log.error("Error while Creating JMS Connection - Naming Exception:", e);
        } catch (JMSException e) {
            log.error("Error while Creating JMS Connection - JMS Exception:", e);
        }
        log.info("JWTRevocationTestCase  Environment Setup Completed");
    }

    /**
     * Method to Create a JMS Subscriber
     * @throws JMSException Error thrown while creating JMS connection
     * @throws NamingException Error thrown while creating Initial Context or when look up context
     */
    private void createJMSConnection() throws JMSException, NamingException {
        String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
        String CF_NAME_PREFIX = "connectionfactory.";
        String CF_NAME = "qpidConnectionfactory";
        String topicName = "tokenRevocation";
        String JMSConnectionURL = "amqp://admin:admin@clientid/carbon?brokerlist='tcp://localhost:6172'";
        TopicConnectionFactory connFactory;
        Topic topic;
        Properties properties;
        InitialContext ctx;
        properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, JMSConnectionURL);
        ctx = new InitialContext(properties);
        connFactory = (TopicConnectionFactory) ctx.lookup(CF_NAME);
        topicConnection = connFactory.createTopicConnection();
        topicConsumerSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        topic = topicConsumerSession.createTopic(topicName);
        // Consumer subscribes to customerTopic
        consumer = topicConsumerSession.createSubscriber(topic);
        topicConnection.start();
    }

    @Test(groups = { "wso2.am" }, description = "JWT revocation test")
    public void revokeRequestTestCase() throws Exception {
        log.info("revokeRequestTestCase  Initiated");
        //Retrieve a JWT
        String requestBody = "grant_type=password&username=" + userName + "&password=" + password;
        URL tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "token");
        Map<String, String> authenticationRequestHeaders = new HashMap<>();
        String basicAuthHeader = consumerKey + ":" + consumerSecret;
        byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes(StandardCharsets.UTF_8));
        authenticationRequestHeaders.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        JSONObject accessTokenGenerationResponse = new JSONObject(
                HTTPSClientUtils.doPost(tokenEndpointURL, requestBody, authenticationRequestHeaders).getData());
        String jwtAccessToken = accessTokenGenerationResponse.getString("access_token");
        //Extract the jti
        int firstDotSeparatorIndex = jwtAccessToken.indexOf('.');
        int secondSeparatorIndex = jwtAccessToken.indexOf('.', firstDotSeparatorIndex + 1);
        String JWTToken = jwtAccessToken.substring(firstDotSeparatorIndex + 1, secondSeparatorIndex);
        byte[] decodedJwt = Base64.decodeBase64(JWTToken.getBytes());
        JSONObject jsonObject = new JSONObject(new String(decodedJwt));
        jtiExtracted = jsonObject.get("jti").toString();
        //jti = "2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d";
        String input = "token=" + jtiExtracted;
        //Call the revoke Endpoint
        URL revokeEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "revoke");
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse;
        try {
            httpResponse = HTTPSClientUtils.doPost(revokeEndpointURL, input, authenticationRequestHeaders);
            assertEquals(httpResponse.getResponseCode(), 200);
        } catch (Exception e) {
            Assert.fail("Should not throw any exceptions" + e);
        }
        log.info("revokeRequestTestCase  Finished");
    }

    @Test(groups = { "wso2.am" }, description = "JWT revocation test", dependsOnMethods = "revokeRequestTestCase")
    public void checkETCDForRevokedJTITestCase() throws Exception {
        log.info("checkETCDForRevokedJTITTestCase  Initiated");
        String etcdEndpointURLs = "https://localhost:9943/etcdmock/v2/keys/jti/2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d";
        URL etcdEndpointURL = new URL("https://localhost:9943/etcdmock/v2/keys/jti/2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d");
        Map<String, String> authenticationRequestHeaders = new HashMap<>();
        authenticationRequestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String retrievedETCDJTIKey = "";
        String input ="";
        try {
            HTTPSClientUtils.doPost(etcdEndpointURL, input, authenticationRequestHeaders);
            JSONObject etcdResponse = new JSONObject(
                    HTTPSClientUtils.doGet(etcdEndpointURLs, authenticationRequestHeaders).getData());
            Iterator<String> keys = etcdResponse.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.equalsIgnoreCase("node")) {
                    // This is to retrieve the nested json object
                    JSONObject childObject = ((JSONObject) etcdResponse.get("node"));
                    Iterator<String> childKeys = childObject.keys();
                    while (childKeys.hasNext()) {
                        String childKey = childKeys.next();
                        // Iterating through the  child json object
                        if (childKey.equalsIgnoreCase("key")) {
                            retrievedETCDJTIKey = childObject.getString(childKey).substring(5);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Assert.fail("Should not throw any exceptions" + e);
        }
        jti= "2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d";
        assertEquals(retrievedETCDJTIKey, jti);
        log.info("checkETCDForRevokedJTITTestCase  Finished");
    }

    @Test(groups = { "wso2.am" }, description = "JWT revocation test", dependsOnMethods = "revokeRequestTestCase")
    public void checkJMSTopicForRevokedJTITestCase() throws Exception {
        log.info("checkJMSTopicForRevokedJTITTestCase  Initiated");
        //Checking the message content of the received JMS message
        Message message = consumer.receive();
        if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            assertEquals(mapMessage.getString("revokedToken"), jtiExtracted);
        }
        topicConsumerSession.close();
        topicConnection.close();
        log.info("checkJMSTopicForRevokedJTITTestCase  Finished");
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        super.cleanUp();
    }
}
