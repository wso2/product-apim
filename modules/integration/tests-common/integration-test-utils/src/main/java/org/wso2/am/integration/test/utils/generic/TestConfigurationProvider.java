/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.test.utils.generic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;

public class TestConfigurationProvider {
	private static Log log = LogFactory.getLog(TestConfigurationProvider.class);
	private AutomationContext context;
	private static TestConfigurationProvider instance = new TestConfigurationProvider();

	private TestConfigurationProvider() {
		try {
			context = new AutomationContext();
		} catch (XPathExpressionException e) {
			log.error("Loading Automation Configuration failed", e);
		}
	}

	public static String getResourceLocation(String productName) {
		return FrameworkPathUtil.getSystemResourceLocation() + File.separator + "artifacts" +
		       File.separator + productName;
	}

	public static String getSecurityPolicyLocation() {
		return getResourceLocation() + File.separator + "security" + File.separator + "policies";
	}

	public static String getKeyStoreLocation() throws XPathExpressionException {
		return getResourceLocation() + File.separator +
		       instance.context.getConfigurationValue("//keystore/fileName/text()");
	}

	public static String getKeyStorePassword() throws XPathExpressionException {
		return getResourceLocation() + File.separator +
		       instance.context.getConfigurationValue("//keystore/password/text()");
	}

	public static String getKeyStoreType() throws XPathExpressionException {
		return getResourceLocation() + File.separator +
		       instance.context.getConfigurationValue("//keystore/type/text()");
	}

	public static String getTrustStoreLocation() throws XPathExpressionException {
		return getResourceLocation() + File.separator +
		       instance.context.getConfigurationValue("//truststore/fileName/text()");
	}

	public static String getTrustStorePassword() throws XPathExpressionException {
		return getResourceLocation() + File.separator +
		       instance.context.getConfigurationValue("//truststore/password/text()");
	}

	public static String getTrustStoreType() throws XPathExpressionException {
		return getResourceLocation() + File.separator +
		       instance.context.getConfigurationValue("//truststore/type/text()");
	}

	public static String getResourceLocation() {
		return FrameworkPathUtil.getSystemResourceLocation();
	}

	public static String getExecutionEnvironment() throws XPathExpressionException {
		return instance.context.getConfigurationValue("//executionEnvironment");
	}

	public static int getServiceDeploymentDelay() {
		try {
			return Integer.parseInt(instance.context.getConfigurationValue("//deploymentDelay"));
		} catch (XPathExpressionException e) {
			log.error("Error reading deploymentDelay from automation.xml", e);
			log.warn(
					"Service deployment Delay configuration not found. Running with default value " +
					"30000" + " mils");
		}
		//if there is an error, setting to default value 30000 milliseconds
		return 30000;
	}

	public static boolean isIntegration() throws XPathExpressionException {
		return "standalone".equalsIgnoreCase(getExecutionEnvironment());
	}

	public static boolean isPlatform() throws XPathExpressionException {
		return "platform".equalsIgnoreCase(getExecutionEnvironment());
	}

	public static AutomationContext getAutomationContext() {
		return instance.context;
	}
}
