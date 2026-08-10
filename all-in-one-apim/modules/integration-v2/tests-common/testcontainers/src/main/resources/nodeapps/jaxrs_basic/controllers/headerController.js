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

 // GET /sec
 // Mirrors node-customer-service's /sec (the route the endpoint-security tests actually hit, on :3001):
 // an absent Authorization header yields an explicit marker, not an empty body, so "no credential was
 // injected" is a positive assertion instead of one that also passes when the request never arrived.
const getSec = (req, res) => {
    const authHeader = req.header('Authorization');
    console.log(`----invoking getSec: ${authHeader}`);
    res.type('text/plain').send(authHeader || 'no-authorization-header-received');
}

// GET /handler
const getHandler = (req, res) =>{
    const header = req.header('Iwasat');
    console.log(`----invoking handler handler handler`);
    res.type('text/plain').send(header || '');
}

module.exports = {
  getSec,
  getHandler
};
