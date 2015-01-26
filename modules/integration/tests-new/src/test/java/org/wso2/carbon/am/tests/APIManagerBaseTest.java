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

package org.wso2.carbon.am.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import javax.xml.xpath.XPathExpressionException;

public abstract class APIManagerBaseTest {
    protected Log log = LogFactory.getLog(getClass());

    protected AutomationContext automationContextStore;
    protected AutomationContext automationContextPublisher;

    protected void initPublisher(String productGroupName, String instanceName, TestUserMode userMode) throws XPathExpressionException {
        automationContextPublisher = new AutomationContext(productGroupName, instanceName, userMode);
    }

    protected void initPublisher( TestUserMode userMode) throws XPathExpressionException {
        automationContextPublisher = new AutomationContext("AM", userMode);
    }

    protected void initStore( TestUserMode userMode) throws XPathExpressionException {
        automationContextStore = new AutomationContext("AM", userMode);
    }
    protected void initStore(String productGroupName, String instanceName, TestUserMode userMode) throws XPathExpressionException {
        automationContextStore = new AutomationContext(productGroupName, instanceName, userMode);
    }

}

