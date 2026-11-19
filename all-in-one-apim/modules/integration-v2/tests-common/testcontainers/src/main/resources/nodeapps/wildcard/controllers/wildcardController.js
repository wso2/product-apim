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

 const WildcardModel = require('../models/wildcardModel');

exports.handleRequest = (req, res) => {
    // The API-product lifecycle parity fixture needs the legacy customer's five-route contract without sharing the
    // stateful customer-service backend (where DELETE would remove the customer before later lifecycle phases).
    // Scope this deterministic behavior to its dedicated endpoint prefix; every other wildcard caller keeps the
    // existing response unchanged.
    if (req.path.startsWith('/api-product-lifecycle/')) {
        const resourcePath = req.path.substring('/api-product-lifecycle'.length).replace(/\/+$/, '') || '/';
        if (req.method === 'GET' && /^\/customers\/\d+$/.test(resourcePath)) {
            return res.json({ id: 123, name: 'John' });
        }
        return res.send(WildcardModel.getDefaultMessage());
    }

    const body = req.body && Object.keys(req.body).length > 0
        ? req.body
        : null;

    if (body) {
        res.json(body);
    } else {
        res.send(WildcardModel.getDefaultMessage());
    }
};
