/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * under the License.
 */
const NodeEnvironment = require('jest-environment-node');
const puppeteer = require('puppeteer');
const fs = require('fs');
const os = require('os');
const path = require('path');

const DIR = path.join(os.tmpdir(), 'jest_puppeteer_global_setup');

/**
 *
 *
 * @class PuppeteerEnvironment
 * @extends {NodeEnvironment}
 */
class PuppeteerEnvironment extends NodeEnvironment {
    /**
     * Setup the puppeteer environment and set the global variables
     *
     * Note: If the `WSO2_PORT_OFFSET` environment variable is not set or have invalid value
     * check the code in >>> org/wso2/am/integration/tests/ui/APIMANAGERUIIntegrationTestRunner.java
     * The `WSO2_PORT_OFFSET` value is expected to set from Java TestNG (above file)
     * Will set the server port to 9443
     *  for navigating to URL if someone run the test directly from npm test (not executing command via TestNG)
     * @memberof PuppeteerEnvironment
     */
    async setup() {
        console.log('Setup Test Environment.');
        await super.setup();
        const wsEndpoint = fs.readFileSync(path.join(DIR, 'wsEndpoint'), 'utf8');
        if (!wsEndpoint) {
            throw new Error('wsEndpoint not found');
        }
        // eslint-disable-next-line no-underscore-dangle
        this.global.__BROWSER__ = await puppeteer.connect({
            browserWSEndpoint: wsEndpoint,
            ignoreHTTPSErrors: true,
        });
        const envVar = process.env.WSO2_PORT_OFFSET;
        console.log('WSO2_PORT_OFFSET=' + envVar);
        this.global.PORT_OFFSET = Number(envVar) || 0;
    }

    /**
     *
     *
     * @memberof PuppeteerEnvironment
     */
    async teardown() {
        console.log('Teardown Test Environment.');
        await super.teardown();
    }

    /**
     *
     *
     * @param {*} script
     * @returns
     * @memberof PuppeteerEnvironment
     */
    runScript(script) {
        return super.runScript(script);
    }
}

module.exports = PuppeteerEnvironment;
