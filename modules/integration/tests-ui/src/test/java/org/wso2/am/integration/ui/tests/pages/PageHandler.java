/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */


package org.wso2.am.integration.ui.tests.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.wso2.am.integration.ui.tests.util.UIElementMapper;

import java.io.IOException;

/**
 * Basic functions needed by Page. This is the supper class of all page classes.
 */
public class PageHandler {
    private WebDriver driver;

    public PageHandler(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Get the element value from mapper.properties by passing the key.
     *
     * @param key Key of the UI element in the mapper.properties
     * @return the value of the given UI element in mapper.properties
     */
    protected String getElementByKey(String key) throws IOException {
        return UIElementMapper.getElement(key);
    }


    /**
     * Fill the text box by element id
     *
     * @param ids   key of the UI element Id
     * @param value text that need to set to text box
     */
    protected void fillTextBoxById(String ids, String value) throws IOException {
        driver.findElement(By.id(getElementByKey(ids))).sendKeys(value);
    }

    /**
     * Fill the text box by element id, where the value is send as CharSequence
     *
     * @param ids   key of the UI element Id
     * @param value ext that need to set to text box
     */
    protected void fillTextBoxById(String ids, CharSequence value) throws IOException {
        driver.findElement(By.id(getElementByKey(ids))).sendKeys(value);
    }

    /**
     * Fill the text box by element xPath
     *
     * @param xPath key of the UI element xPath
     * @param value ext that need to set to text box
     */
    protected void fillTextBoxByXPath(String xPath, String value) throws IOException {
        driver.findElement(By.xpath(getElementByKey(xPath))).sendKeys(value);
    }

    /**
     * Fill the text box by element CssSelector
     *
     * @param cssSelector key of the UI element Css Selector
     * @param value       ext that need to set to text box
     */
    protected void fillTextBoxByCssSelector(String cssSelector, String value) throws IOException {
        driver.findElement(By.cssSelector(getElementByKey(cssSelector))).sendKeys(value);
    }

    /**
     * Click element by element Id
     *
     * @param id key of the UI element Id
     */
    protected void clickElementById(String id) throws IOException {
        driver.findElement(By.id(getElementByKey(id))).click();
    }

    /**
     * Click element by element Link Text
     *
     * @param linkText key of the UI element Link Text
     */
    protected void clickElementByLinkText(String linkText) throws IOException {
        driver.findElement(By.linkText(getElementByKey(linkText))).click();
    }

    /**
     * Click element by element xPath
     *
     * @param xPath key of the UI element xPath
     */
    protected void clickElementByXpath(String xPath) throws IOException {
        driver.findElement(By.xpath(getElementByKey(xPath))).click();
    }


    protected void clickElementByCssSelector(String cssSelector) throws IOException {
        driver.findElement(By.cssSelector(getElementByKey(cssSelector))).click();
    }


    /**
     * Wait until the given element to be visible by element Link Text. Max Waiting time should provide as second parameter.
     *
     * @param linkText key of the UI element Link Text
     * @param seconds  max seconds to wait
     */
    protected void waitUntilElementVisibilityByLinkText(String linkText, long seconds)
            throws IOException {
        WebDriverWait waitDriver = new WebDriverWait(driver, seconds);
        waitDriver.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(getElementByKey(linkText))));
    }

    /**
     * Wait until the given element to be visible by element Id. Max Waiting time should provide as second parameter.
     *
     * @param id      key of the UI element Id
     * @param seconds max seconds to wait
     */
    protected void waitUntilElementVisibilityById(String id, long seconds) throws IOException {
        WebDriverWait waitDriver = new WebDriverWait(driver, seconds);
        waitDriver.until(ExpectedConditions.visibilityOfElementLocated(By.id(getElementByKey(id))));
    }

    /**
     * Wait until the given element to be visible by element Class Name. Max Waiting time should provide as second parameter.
     *
     * @param className key of the UI element Class Name
     * @param seconds   max seconds to wait
     */
    protected void waitUntilElementVisibilityByClassName(String className, long seconds)
            throws IOException {
        WebDriverWait waitDriver = new WebDriverWait(driver, seconds);
        waitDriver.until(ExpectedConditions.visibilityOfElementLocated(By.className(getElementByKey(className))));
    }

    /**
     * Return the value of ".getText()" operation of given element, by element xPath
     *
     * @param xPath key of the UI element xPath
     * @return value of ".getText()" operation of given element.
     */
    protected String getTextOfElementByXPath(String xPath) throws IOException {
        return driver.findElement(By.xpath(getElementByKey(xPath))).getText();
    }

    /**
     * Return the value of ".getText()" operation of given element, by element Class Name
     *
     * @param className key of the UI element Class Name
     * @return value of ".getText()" operation of given element.
     */
    protected String getTextOfElementByClassName(String className) throws IOException {
        return driver.findElement(By.className(getElementByKey(className))).getText();
    }

    /**
     * Return the value of ".getText()" operation of given element, by element Id
     *
     * @param id key of the UI element Id
     * @return value of ".getText()" operation of given element.
     */
    protected String getTextOfElementById(String id) throws IOException {
        return driver.findElement(By.id(getElementByKey(id))).getText();
    }

}
