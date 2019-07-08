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
const qs = require('qs');

describe(
    'Publisher application user authentication tests',
    () => {
        let page;
        beforeAll(async () => {
            page = await global.__BROWSER__.newPage();
        }, timeout);

        beforeEach(async () => {
            await page._client.send('Network.clearBrowserCookies');
            await page._client.send('Network.clearBrowserCache');
            let port = 9443 + global.PORT_OFFSET;
            console.log(port);
            await page.goto('https://localhost:' + port + '/publisher-new');
        });

        afterAll(async () => {
            await page.close();
        });

        test('should able to login without error', async () => {
            await page.type('input[name="username"]', 'admin');
            await page.type('input[name="password"]', 'admin');
            await Promise.all([
                page.$eval('#loginForm', form => form.submit()),
                page.waitForNavigation()
            ]);
            await page.click('input#approveCb[type="radio"]');
            const consentSelector = await page.$(
                'input#consent_select_all[type="checkbox"]'
            );
            if (consentSelector)
                await page.click('input#consent_select_all[type="checkbox"]');
            await Promise.all([
                page.click('#approve'),
                page.waitForNavigation({ waitUntil: 'load' })
            ]);
            const expectedCookies = [
                'AM_ACC_TOKEN_DEFAULT_P2',
                'JSESSIONID',
                'opbs',
                'WSO2_AM_TOKEN_1_Default',
                'commonAuthId',
                'AM_REF_TOKEN_DEFAULT_P2'
            ];
            let availableCookies = await page.cookies();
            availableCookies = availableCookies.map(
                cookieObject => cookieObject.name
            );
            expectedCookies.forEach(expectedCookie =>
                expect(availableCookies).toContain(expectedCookie)
            );
        });

        test('should not able to login with an invalid username', async () => {
            await page.type('input[name="username"]', 'chuckNorris');
            await page.type('input[name="password"]', 'chuckNorris');

            await Promise.all([
                page.$eval('#loginForm', form => form.submit()),
                page.waitForNavigation({ waitUntil: 'load' })
            ]);
            const { authFailure } = qs.parse(page.url().split('?')[1]);
            expect(authFailure).toEqual('true');
        });

        test.todo('should return to original location after login');
    },
    timeout
);
