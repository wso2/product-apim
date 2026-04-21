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

 const keyModel = require('../models/keyModel');

const getKey = (req, res) => {
    if (keyModel.getKeyStatus()) {
        const output = {
            action: "get",
            node: {
                key: "/jti/2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d",
                value: "true",
                expiration: "2019-03-21T07:25:19.721867Z",
                ttl: 3594,
                modifiedIndex: 71,
                createdIndex: 71
            }
        };
        res.status(200).json(output);
    } else {
        res.sendStatus(404);
    }
};

const postKey = (req, res) => {
    keyModel.setKeyStatus(true);
    res.sendStatus(200);
};

const putKey = (req, res) => {
    keyModel.setKeyStatus(true);
    res.sendStatus(201);
};

module.exports = {
    getKey,
    postKey,
    putKey
};
