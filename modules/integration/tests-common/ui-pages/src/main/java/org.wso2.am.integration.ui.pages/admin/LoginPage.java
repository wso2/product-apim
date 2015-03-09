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

package org.wso2.am.integration.ui.pages.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;

/**
 * Login page class - contains methods to login to wso2 products.
 */
public class LoginPage {
    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public LoginPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!(driver.getCurrentUrl().contains("login.jsp"))) {
            // Alternatively, we could navigate to the login page, perhaps logging out first
            throw new IllegalStateException("This is not the login page");
        }
    }

    /**
     * Provide facility to log into the products using user credentials
     *
     * @param userName login user name
     * @param password login password
     * @return reference to Home page
     * @throws java.io.IOException if mapper.properties file not found
     */
    public HomePage loginAs(String userName, String password) throws IOException {
        log.info("Login as " + userName);
        WebElement userNameField = driver.findElement(By.name(uiElementMapper.getElement("login.username")));
        WebElement passwordField = driver.findElement(By.name(uiElementMapper.getElement("login.password")));
        userNameField.sendKeys(userName);
        passwordField.sendKeys(password);
        driver.findElement(By.className(uiElementMapper.getElement("login.sign.in.button"))).click();
        return new HomePage(driver);
    }

}
