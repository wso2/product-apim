package org.wso2.carbon.am.tests.sample;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;

/**
 * This is related to public jira - https://wso2.org/jira/browse/ESBJAVA-3380 &
 * https://wso2.org/jira/browse/APIMANAGER-3076 
 * This class tests the conversion of json with special characters to xml.
 */
public class ESBJAVA3380TestCase extends APIManagerIntegrationTest {

	private ServerConfigurationManager serverConfigurationManager;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {

		super.init(0);
		/*
		 * If test run in external distributed deployment you need to copy
		 * following resources accordingly. configFiles/json_to_xml/axis2.xml
		 * configFiles/json_to_xml/synapse.properties
		 */

		serverConfigurationManager = new ServerConfigurationManager(
				amServer.getBackEndUrl());
		serverConfigurationManager.applyConfiguration(new File(ProductConstant
				.getResourceLocations(ProductConstant.AM_SERVER_NAME)
				+ File.separator + "configFiles/json_to_xml/" + "axis2.xml"));
		serverConfigurationManager.applyConfiguration(new File(ProductConstant
				.getResourceLocations(ProductConstant.AM_SERVER_NAME)
				+ File.separator
				+ "configFiles/json_to_xml/"
				+ "synapse.properties"));
		super.init(0);

		String apiMngrSynapseConfigPath = "/artifacts/AM/synapseconfigs/property/json_to_xml.xml";
		String relativeFilePath = apiMngrSynapseConfigPath.replaceAll(
				"[\\\\/]", File.separator);
		OMElement apiMngrSynapseConfig = esbUtils
				.loadClasspathResource(relativeFilePath);

		esbUtils.updateESBConfiguration(setEndpoints(apiMngrSynapseConfig),
				amServer.getBackEndUrl(), amServer.getSessionCookie());

	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.integration_all })
	@Test(groups = { "wso2.am" }, description = "Json to XML Test sample")
	public void jsonToXmlTestCase() throws Exception {

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.clear();
		requestHeaders.put("Content-Type", "application/json");
		// Send the payload with special character ":"
		String payload = "{ \"http://purl.org/dc/elements/1.1/creator\" : \"url\"}";
		HttpResponse response = null;

		try {
			response = HttpRequestUtil.doPost(new URL(
					getApiInvocationURLHttp("Weather/1.0.0")), payload,
					requestHeaders);
		} catch (Exception e) {
			Assert.assertFalse(
					e.getLocalizedMessage().contains("Connection error"),
					"Problem in converting json to xml");
		}

		Assert.assertEquals(response.getResponseCode(), 404,
				"Response code mismatched while Json to XML test case");
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
		serverConfigurationManager.restoreToLastConfiguration();
	}

}
