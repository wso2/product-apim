/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 Inc. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.am.integration.tests.config;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.nextgen.config.ConfigParser;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;
import org.wso2.am.integration.test.Constants;

import java.io.File;
import java.util.Map;

import static org.xmlunit.assertj.XmlAssert.assertThat;

/**
 * New Deployment Configuration Base Test
 */
public class TomlBasedConfigurationTestCase {

    String carbonHome;

    @BeforeClass
    public void setup() {

        carbonHome = System.getProperty("carbon.home");

    }

    @Test(dataProvider = "fullConfigScenarios")
    public void testDefaultValuesOfApiManagerXml(String scenario) throws Exception {

        File homeDir = new File(carbonHome);
        String deploymentConfiguration = FileUtils.getFile(getAMResourceLocation(), "fullConfigurations",
                scenario).getAbsolutePath();
        String configuration = FileUtils.getFile(homeDir, "repository", "resources", "conf").getAbsolutePath();

        ConfigParser configParser =
                new ConfigParser.ConfigParserBuilder()
                        .withDeploymentConfigurationPath(deploymentConfiguration)
                        .withInferConfigurationFilePath(configuration)
                        .withMappingFilePath(configuration)
                        .withValidatorFilePath(configuration)
                        .withTemplateFilePath(configuration)
                        .withDefaultValueFilePath(configuration)
                        .withUnitResolverFilePath(configuration)
                        .build();

        Map<String, String> outputFileContentMap = configParser.parse();
        outputFileContentMap.forEach((path, content) -> {
            String actualFilePath = getAMResourceLocation() + File.separator + "fullConfigurarions" + File.separator
                    + scenario + File.separator + path;
            if (new File(actualFilePath).exists()) {
                Diff difference = DiffBuilder.compare(content).withTest(Input.fromFile(actualFilePath)).ignoreComments()
                        .ignoreWhitespace().withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                        .checkForSimilar().build();
                if (difference.hasDifferences()) {
                    Assert.fail(difference.toString());
                }
            }
        });

    }

    @Test(dataProvider = "individualConfigScenarios")
    public void testIndividualValuesOfApiManagerXml(String scenario) throws Exception {

        File homeDir = new File(carbonHome);
        String deploymentConfiguration = getAMResourceLocation() + File.separator + "individualConfigurations"
                + File.separator + scenario;
        String configuration = FileUtils.getFile(homeDir, "repository", "resources", "conf").getAbsolutePath();
        ConfigParser configParser =
                new ConfigParser.ConfigParserBuilder()
                        .withDeploymentConfigurationPath(deploymentConfiguration)
                        .withInferConfigurationFilePath(configuration)
                        .withMappingFilePath(configuration)
                        .withValidatorFilePath(configuration)
                        .withTemplateFilePath(configuration)
                        .withDefaultValueFilePath(configuration)
                        .withUnitResolverFilePath(configuration)
                        .build();

        Map<String, String> outputFileContentMap;
        outputFileContentMap = configParser.parse();
        String apim = outputFileContentMap.get("repository/conf/api-manager.xml");

        //Check 'Default' environment
        String envHybXpathPrefix = "//Environments/Environment[Name=\""+ Constants.GATEWAY_ENVIRONMENT + "\"]";
        assertThat(apim).nodesByXPath(envHybXpathPrefix).haveAttribute("type", "hybrid");
        assertThat(apim).nodesByXPath(envHybXpathPrefix).haveAttribute("api-console", "true");
        assertThat(apim).nodesByXPath(envHybXpathPrefix).haveAttribute("isDefault", "true");
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/Name").isEqualTo(Constants.GATEWAY_ENVIRONMENT);
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/Description")
                .isEqualTo("This is a hybrid gateway " + "that handles both production and sandbox token traffic.");
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/ServerURL").isEqualTo("https://localhost:9443/services/");
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/Username").isEqualTo("admin");
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/Password").isEqualTo("admin");
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/GatewayEndpoint")
                .isEqualTo("https://localhost:8243,http://localhost:8280");
        assertThat(apim).valueByXPath(envHybXpathPrefix + "/GatewayWSEndpoint").isEqualTo("ws://localhost:9098");

        //Check 'Production' environment
        String envProdXpathPrefix = "//Environments/Environment[Name=\"Production\"]";
        assertThat(apim).nodesByXPath(envProdXpathPrefix).haveAttribute("type", "production");
        assertThat(apim).nodesByXPath(envProdXpathPrefix).haveAttribute("api-console", "false");
        assertThat(apim).nodesByXPath(envProdXpathPrefix).haveAttribute("isDefault", "false");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/Name").isEqualTo("Production");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/Description")
                .isEqualTo("This is a production gateway that handles production token traffic.");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/ServerURL").isEqualTo("https://localhost:9444/services/");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/Username").isEqualTo("admin");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/Password").isEqualTo("admin");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/GatewayEndpoint")
                .isEqualTo("https://localhost:8244,http://localhost:8281");
        assertThat(apim).valueByXPath(envProdXpathPrefix + "/GatewayWSEndpoint").isEqualTo("ws://localhost:9099");

        //JWT related
        String jwtConfigXpathPrefix = "//JWTConfiguration";
        assertThat(apim).valueByXPath(jwtConfigXpathPrefix + "/EnableJWTGeneration").isEqualTo("true");
        assertThat(apim).valueByXPath(jwtConfigXpathPrefix + "/JWTHeader").isEqualTo("X-JWT-Assertion_CUSTOM");
        assertThat(apim).valueByXPath(jwtConfigXpathPrefix + "/ClaimsRetrieverImplClass")
                .isEqualTo("org.wso2.carbon.apimgt.impl.token.DefaultClaimsRetriever");
        assertThat(apim).valueByXPath(jwtConfigXpathPrefix + "/ConsumerDialectURI")
                .isEqualTo("http://wso2.org/claims_custom");
        assertThat(apim).valueByXPath(jwtConfigXpathPrefix + "/SignatureAlgorithm").isEqualTo("SHA256withRSA_CUSTOM");
        assertThat(apim).valueByXPath(jwtConfigXpathPrefix + "/JWTGeneratorImpl")
                .isEqualTo("org.wso2.carbon.apimgt.keymgt.token.JWTGenerator");

        //CacheConfigurations
        String cacheConfigXpathPrefix = "//CacheConfigurations";
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnableGatewayTokenCache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/TokenCacheExpiry").isEqualTo("1200");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnableGatewayResourceCache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnableKeyManagerTokenCache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnableRecentlyAddedAPICache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnableScopeCache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnablePublisherRoleCache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/EnableJWTClaimCache").isEqualTo("true");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/JWTClaimCacheExpiry").isEqualTo("1200");
        assertThat(apim).valueByXPath(cacheConfigXpathPrefix + "/TagCacheDuration").isEqualTo("180000");

        //Analytics
        String analyticsConfigXpathPrefix = "//Analytics";
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorServerURL").isEqualTo(
                "{ tcp://analytics1:7611,tcp://analytics2:7611 },{ tcp://analytics1:7612|tcp://analytics2:7612 }"
                        .trim());
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorAuthServerURL").isEqualTo(
                "{ ssl://analytics1:7711,ssl://analytics2:7711 },{ ssl://analytics1:7712|ssl://analytics2:7712 }"
                        .trim());
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorUsername")
                .isEqualTo("analytics.user");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorPassword")
                .isEqualTo("analytics.pass");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StatsProviderImpl")
                .isEqualTo("org.wso2.carbon.apimgt.usage.client.impl.APIUsageStatisticsRestClientImpl");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorRestApiURL")
                .isEqualTo("https://analytics.com:7444");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorRestApiUsername")
                .isEqualTo("analytics.user");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/StreamProcessorRestApiPassword")
                .isEqualTo("analytics.pass");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/PublisherClass")
                .isEqualTo("org.wso2.carbon.apimgt.usage.publisher.APIMgtUsageDataBridgeDataPublisher");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/PublishResponseMessageSize").isEqualTo("true");
        assertThat(apim).valueByXPath(analyticsConfigXpathPrefix + "/PublishResponseMessageSize").isEqualTo("true");

        //Analytics
        String keyValidatorConfigXpathPrefix = "//APIKeyValidator";
        assertThat(apim).valueByXPath(keyValidatorConfigXpathPrefix + "/ServerURL")
                .isEqualTo("https://localhost:9443/services/");
        assertThat(apim).valueByXPath(keyValidatorConfigXpathPrefix + "/Username").isEqualTo("km.user");
        assertThat(apim).valueByXPath(keyValidatorConfigXpathPrefix + "/Password").isEqualTo("km.pass");
        assertThat(apim).valueByXPath(keyValidatorConfigXpathPrefix + "/ConnectionPool/MaxIdle").isEqualTo("101");
        assertThat(apim).valueByXPath(keyValidatorConfigXpathPrefix + "/ConnectionPool/InitIdleCapacity")
                .isEqualTo("51");
        assertThat(apim).valueByXPath(keyValidatorConfigXpathPrefix + "/KeyValidationHandlerClassName")
                .isEqualTo("org.wso2.carbon.apimgt.keymgt.handlers.DefaultKeyValidationHandler");

        //OAuthConfigurations
        String oAuthConfigXpathPrefix = "//OAuthConfigurations";
        assertThat(apim).valueByXPath(oAuthConfigXpathPrefix + "/RemoveOAuthHeadersFromOutMessage").isEqualTo("false");
        assertThat(apim).valueByXPath(oAuthConfigXpathPrefix + "/ApplicationTokenScope")
                .isEqualTo("am_custom_application_scope");
        assertThat(apim).valueByXPath(oAuthConfigXpathPrefix + "/AuthorizationHeader").isEqualTo("X-Authorization");
        assertThat(apim).hasXPath(oAuthConfigXpathPrefix + "/ScopeWhitelist/Scope[text()=\"^device_.*\"]");
        assertThat(apim).hasXPath(oAuthConfigXpathPrefix + "/ScopeWhitelist/Scope[text()=\"openid\"]");
        assertThat(apim).hasXPath(oAuthConfigXpathPrefix + "/ScopeWhitelist/Scope[text()=\"custom_device\"]");
        assertThat(apim).valueByXPath(oAuthConfigXpathPrefix + "/RevokeAPIURL")
                .isEqualTo("https://localhost:8243/revoke");
        assertThat(apim).valueByXPath(oAuthConfigXpathPrefix + "/EncryptPersistedTokens").isEqualTo("true");
        assertThat(apim).valueByXPath(oAuthConfigXpathPrefix + "/EnableTokenHashMode").isEqualTo("true");

        //APIStore
        String storeConfigXpathPrefix = "//APIStore";
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/GroupingExtractor")
                .isEqualTo("org.wso2.carbon.apimgt.impl.DefaultGroupIDExtractorImpl");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/RESTApiGroupingExtractor")
                .isEqualTo("org.wso2.carbon.apimgt.impl.DefaultGroupIDExtractorImpl");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/DefaultGroupExtractorClaimUri")
                .isEqualTo("http://wso2.org/claims/organization");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/CompareCaseInsensitively").isEqualTo("false");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/URL").isEqualTo("https://localhost:9443/devportal");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/DisplayMultipleVersions").isEqualTo("true");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/DisplayAllAPIs").isEqualTo("true");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/DisplayComments").isEqualTo("false");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/DisplayRatings").isEqualTo("false");
        assertThat(apim).valueByXPath(storeConfigXpathPrefix + "/isStoreForumEnabled").isEqualTo("false");

        String appConfigExternalAttrXPath = storeConfigXpathPrefix + "/ApplicationConfiguration/ApplicationAttributes/Attribute[Name=\"External Reference Id\"]";
        assertThat(apim).valueByXPath(appConfigExternalAttrXPath + "/Name").isEqualTo("External Reference Id");
        assertThat(apim).valueByXPath(appConfigExternalAttrXPath + "/Description").isEqualTo("Sample description of the attribute");
        assertThat(apim).nodesByXPath(appConfigExternalAttrXPath).haveAttribute("required", "true");

        String appConfigInternalAttrXPath = storeConfigXpathPrefix + "/ApplicationConfiguration/ApplicationAttributes/Attribute[Name=\"Internal Reference Id\"]";
        assertThat(apim).valueByXPath(appConfigInternalAttrXPath + "/Name").isEqualTo("Internal Reference Id");
        assertThat(apim).valueByXPath(appConfigInternalAttrXPath + "/Description").isEqualTo("Sample description of the internal attribute");
        assertThat(apim).nodesByXPath(appConfigInternalAttrXPath).haveAttribute("required", "false");

        //APIStore
        String publisherConfigXpathPrefix = "//APIPublisher";
        assertThat(apim).valueByXPath(publisherConfigXpathPrefix + "/EnableAPIDocVisibilityLevels").isEqualTo("true");

        //CORSConfiguration
        String corsConfigXpathPrefix = "//CORSConfiguration";
        assertThat(apim).valueByXPath(corsConfigXpathPrefix + "/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(corsConfigXpathPrefix + "/Access-Control-Allow-Origin")
                .isEqualTo("origin1,origin2");
        assertThat(apim).valueByXPath(corsConfigXpathPrefix + "/Access-Control-Allow-Methods")
                .isEqualTo("GET,PUT,POST,DELETE,PATCH,OPTIONS");
        assertThat(apim).valueByXPath(corsConfigXpathPrefix + "/Access-Control-Allow-Headers")
                .isEqualTo("authorization,Access-Control-Allow-Origin,Content-Type,SOAPAction,X-custom");
        assertThat(apim).valueByXPath(corsConfigXpathPrefix + "/Access-Control-Allow-Credentials").isEqualTo("true");

        //ThrottlingConfigurations
        String throttleConfigXpathPrefix = "//ThrottlingConfigurations";
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/EnableAdvanceThrottling").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/TrafficManager/Type").isEqualTo("Binary");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/TrafficManager/ReceiverUrlGroup")
                .isEqualTo("{ tcp://tm1:9611,tcp://tm2:9612 },{ tcp://tm1:9611|tcp://tm2:9612 }");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/TrafficManager/AuthUrlGroup")
                .isEqualTo("{ ssl://tm1:9711,ssl://tm2:9712 },{ ssl://tm1:9711|ssl://tm2:9712 }");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/TrafficManager/Username").isEqualTo("tm.user");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/TrafficManager/Password").isEqualTo("tm.pass");

        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/DataPublisher/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/DataPublisher/DataPublisherPool/MaxIdle")
                .isEqualTo("1001");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/DataPublisher/DataPublisherPool/InitIdleCapacity")
                .isEqualTo("101");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/DataPublisher/DataPublisherThreadPool/CorePoolSize")
                .isEqualTo("201");
        assertThat(apim)
                .valueByXPath(throttleConfigXpathPrefix + "/DataPublisher/DataPublisherThreadPool/MaxmimumPoolSize")
                .isEqualTo("1002");
        assertThat(apim)
                .valueByXPath(throttleConfigXpathPrefix + "/DataPublisher/DataPublisherThreadPool/KeepAliveTime")
                .isEqualTo("202");

        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/PolicyDeployer/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/PolicyDeployer/ServiceURL")
                .isEqualTo("https://localhost:9443/services/");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/PolicyDeployer/Username").isEqualTo("tm.user");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/PolicyDeployer/Password").isEqualTo("tm.pass");

        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/BlockCondition/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/BlockCondition/InitDelay").isEqualTo("300000");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/BlockCondition/Period").isEqualTo("3600000");

        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/JMSConnectionDetails/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/JMSConnectionDetails/Destination")
                .isEqualTo("throttleData");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/JMSConnectionDetails/InitDelay")
                .isEqualTo("300000");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix
                + "/JMSConnectionDetails/JMSConnectionParameters/transport.jms.ConnectionFactoryJNDIName")
                .isEqualTo("TopicConnectionFactory");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix
                + "/JMSConnectionDetails/JMSConnectionParameters/transport.jms.DestinationType").isEqualTo("topic");
        assertThat(apim).valueByXPath(
                throttleConfigXpathPrefix + "/JMSConnectionDetails/JMSConnectionParameters/java.naming.factory.initial")
                .isEqualTo("org.wso2.andes.jndi.PropertiesFileInitialContextFactory");
        //todo: need to validate
        //        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/JMSConnectionDetails/JMSConnectionParameters/connectionfactor   y.TopicConnectionFactory").isEqualTo("amqp://tm.user:tm.pass@clientid/carbon?brokerlist='tcp://tm1:5672?retries='5'%26connectdelay='50';tcp://tm2:5673?retries='5'%26connectdelay='50';'");

        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/EnableUnlimitedTier").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/EnableHeaderConditions").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/EnableJWTClaimConditions").isEqualTo("true");
        assertThat(apim).valueByXPath(throttleConfigXpathPrefix + "/EnableQueryParamConditions").isEqualTo("true");

        //WorkflowConfigurations
        String wfConfigXpathPrefix = "//WorkflowConfigurations";
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/ServerUrl").isEqualTo("https://localhost:9445/bpmn");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/ServerUser").isEqualTo("wf.user");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/ServerPassword").isEqualTo("wf.pass");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/WorkflowCallbackAPI")
                .isEqualTo("https://localhost:9443/api/am/publisher/v0.16/workflows/update-workflow-status");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/TokenEndPoint").isEqualTo("https://localhost:8243/token");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/DCREndPoint")
                .isEqualTo("https://localhost:9443/client-registration/v0.17/register");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/DCREndPointUser").isEqualTo("wf.user");
        assertThat(apim).valueByXPath(wfConfigXpathPrefix + "/DCREndPointPassword").isEqualTo("wf.pass");

        //SwaggerCodegen
        String sdkConfigXpathPrefix = "//SwaggerCodegen/ClientGeneration";
        assertThat(apim).valueByXPath(sdkConfigXpathPrefix + "/GroupId").isEqualTo("org.wso2.group");
        assertThat(apim).valueByXPath(sdkConfigXpathPrefix + "/ArtifactId").isEqualTo("org.wso2.artifact");
        assertThat(apim).valueByXPath(sdkConfigXpathPrefix + "/ModelPackage").isEqualTo("org.wso2.package.model");
        assertThat(apim).valueByXPath(sdkConfigXpathPrefix + "/ApiPackage").isEqualTo("org.wso2.package.api");
        assertThat(apim).valueByXPath(sdkConfigXpathPrefix + "/SupportedLanguages").isEqualTo("android,java,scala");

        //OpenTracer
        String tracingConfigXpathPrefix = "//OpenTracer/";
        assertThat(apim).valueByXPath(tracingConfigXpathPrefix + "/RemoteTracer/Enabled").isEqualTo("true");
        assertThat(apim).valueByXPath(tracingConfigXpathPrefix + "/RemoteTracer/Name").isEqualTo("zipkin1");
        assertThat(apim).valueByXPath(tracingConfigXpathPrefix + "/RemoteTracer/Properties/HostName")
                .isEqualTo("wso2.org");
        assertThat(apim).valueByXPath(tracingConfigXpathPrefix + "/RemoteTracer/Properties/Port").isEqualTo("9412");
        assertThat(apim).valueByXPath(tracingConfigXpathPrefix + "/LogTracer/Enabled").isEqualTo("true");
    }

    private String getAMResourceLocation() {

        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM";
    }

    @DataProvider(name = "fullConfigScenarios")
    public Object[][] getFullConfigurationScenarios() {

        return new Object[][]{
                {"scenario1"}
        };
    }

    @DataProvider(name = "individualConfigScenarios")
    public Object[][] getIndividualConfigurationScenarios() {

        return new Object[][]{
                {"scenario1"}
        };
    }
}
