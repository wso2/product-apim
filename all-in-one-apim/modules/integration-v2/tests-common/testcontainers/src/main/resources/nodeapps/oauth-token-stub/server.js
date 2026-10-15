/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

// Local OAuth token endpoint for key manager fixtures whose connector requests a token when the key manager is
// loaded. Served on the shared docker network as http://nodebackend:3028/... . The Auth0 connector POSTs a
// client_credentials grant to the configured token endpoint as soon as a key manager event reaches a node, so the
// Auth0 fixtures point here instead of at a real Auth0 tenant.
//   POST .../oauth/token - 200 with an access token in the shape the connector reads
//                          (access_token, token_type, expires_in, scope).
// Every other request is a 404. Pure Node http (no dependencies) so the node image build needs no extra npm
// packages.
const http = require('http');

const port = process.env.PORT || 3028;

const TOKEN_RESPONSE = JSON.stringify({
    access_token: 'oauth-token-stub-access-token',
    token_type: 'Bearer',
    expires_in: 86400,
    scope: ''
});

const server = http.createServer((req, res) => {
    const path = req.url.split('?')[0];
    // Drain the form body before answering so the client never sees the connection reset mid-write.
    req.resume();
    req.on('end', () => {
        if (req.method === 'POST' && path.endsWith('/oauth/token')) {
            // One line per issued token, so a run's backend log shows which key manager loads reached the stub.
            console.log(`OAuth token stub issued a token for ${req.method} ${path}`);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(TOKEN_RESPONSE);
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('not found');
        }
    });
});

server.listen(port, () => {
    console.log(`OAuth token stub running at http://nodebackend:${port}`);
});
