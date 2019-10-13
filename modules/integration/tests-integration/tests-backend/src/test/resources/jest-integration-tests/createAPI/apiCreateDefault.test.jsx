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

/* eslint-disable no-underscore-dangle */
const timeout = 20000;
jest.setTimeout(20000);

describe(
    'Should login to the publisher then create & publisher the api and finally delete it',
    () => {
        let page;
        const port = 9443 + global.PORT_OFFSET;
        const publisherURL = 'https://localhost:' + port + '/publisher';
        beforeAll(async () => {
            page = await global.__BROWSER__.newPage();
            await page._client.send('Network.clearBrowserCookies');
            await page._client.send('Network.clearBrowserCache');
            await page.goto(publisherURL);
            await page.type('input[name="username"]', 'admin');
            await page.type('input[name="password"]', 'admin');
            await Promise.all([
                page.$eval('#loginForm', form => form.submit()),
                page.waitForNavigation()
            ]);
        }, timeout);

        afterAll(async () => {
            await page.close();
        });

        test('Should create and Publish an API', async () => {
            await page.goto(publisherURL);
            await page.setViewport({ width: 1680, height: 671 });
            await page.waitForSelector('#itest-id-createapi');
            await page.click('#itest-id-createapi');
            await page.waitForSelector('#itest-id-createdefault');
            // If the element is accessible from the rendered output use page.click else use $eval
            await page.$eval('#itest-id-createdefault', e => e.click());
            await page.waitForSelector('#itest-id-apicontext-input');
            await page.type('input#itest-id-apicontext-input', 'sampleContext');
            await page.type('input#itest-id-apiname-input', 'sampleName');
            await page.type('input#itest-id-apiversion-input', 'sampleVersion');
            await page.type(
                'input#itest-id-apiendpoint-input',
                'https://www.sample.com/apis'
            );
            await page.$eval('#itest-id-apipolicies-input', e =>
                e.previousElementSibling.click()
            );
            await page.waitForSelector('#Bronze');
            await page.click('#Bronze');
            await page.keyboard.press('Escape');
            await page.waitForSelector(
                '#itest-id-apicreatedefault-createnpublish'
            );
            await Promise.all([
                page.$eval('#itest-id-apicreatedefault-createnpublish', e =>
                    e.click()
                ),
                page.waitForNavigation({ waitUntil: 'networkidle0' })
            ]);
            const currentPageURL = await page.url();
            expect(currentPageURL).toContain('/overview');
        });

        test('Should delete the above created API', async () => {
            await page.waitForSelector('#itest-id-deleteapi-icon-button');
            await page.click('#itest-id-deleteapi-icon-button');
            await page.waitForSelector('#itest-id-deleteconf');
            await Promise.all([
                page.click('#itest-id-deleteconf'),
                page.waitForNavigation({ waitUntil: 'networkidle0' })
            ]);
            const currentPageURL = await page.url();
            expect(currentPageURL).toContain('/publisher/apis');
        });
    },
    timeout
);
