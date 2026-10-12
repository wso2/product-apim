/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

const fs = require('fs');
const path = require('path');

// Read back by the V2 security-audit scenario to prove APIM takes POST for the first audit and PUT for re-audit.
// This records actual HTTP requests received by the mock; the fixed report fixtures alone cannot distinguish
// those product paths because both fixtures intentionally return the same audit UUID.
const receivedMethods = [];

resetObservability = (req, res) => {
    receivedMethods.length = 0;
    res.status(200).json({ reset: true });
};

getObservability = (req, res) => {
    res.status(200).json({ methods: receivedMethods.slice() });
};

getResults = (req, res) => {
    receivedMethods.push('GET');
    const filePath = path.join(__dirname, '../data/test-audit-report.json');
    fs.readFile(filePath, 'utf-8', (err, data) => {
        if (err || !data) {
            return res.status(404).json({});
        }
        res.json(JSON.parse(data));
    });
};

postResults = (req, res) => {
    receivedMethods.push('POST');
    const filePath = path.join(__dirname, '../data/test-new-audit-api.json');
    fs.readFile(filePath, 'utf-8', (err, data) => {
        if (err || !data) {
            return res.status(404).json({});
        }
        res.json(JSON.parse(data));
    });
};

putResults = (req, res) => {
    receivedMethods.push('PUT');
    const filePath = path.join(__dirname, '../data/test-update-audit-api.json');
    fs.readFile(filePath, 'utf-8', (err, data) => {
        if (err || !data) {
            return res.status(404).json({});
        }
        res.json(JSON.parse(data));
    });
};

module.exports = {
  getResults,
  postResults,
  putResults,
  resetObservability,
  getObservability
};
