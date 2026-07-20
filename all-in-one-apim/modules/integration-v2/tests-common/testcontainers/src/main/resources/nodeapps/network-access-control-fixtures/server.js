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

// Static fixtures backend for the network access-control allow-mode integration tests. Served on the
// shared docker network as http://nodebackend:3021/... . Two documents:
//   /schema-fragment.yaml - a valid OpenAPI schema object; the allow-mode positive control's remote $ref
//                           resolves to this, proving an allow-listed host IS fetched (not blocked).
//   /outer.yaml           - a schema whose nested $ref targets a NON-allow-listed loopback host, proving the
//                           crawl re-validates nested references even under allow mode (nested ref blocked).
// Pure Node http (no dependencies) so the node image build needs no extra npm packages.
const http = require('http');

const port = process.env.PORT || 3021;

const SCHEMA_FRAGMENT =
    'type: object\n' +
    'properties:\n' +
    '  id:\n' +
    '    type: string\n' +
    '  name:\n' +
    '    type: string\n';

const OUTER_WITH_BLOCKED_NESTED =
    'type: object\n' +
    'properties:\n' +
    '  nested:\n' +
    "    $ref: 'http://127.0.0.1/nested-blocked.yaml'\n";

const server = http.createServer((req, res) => {
    const path = req.url.split('?')[0];
    if (path === '/schema-fragment.yaml') {
        res.writeHead(200, { 'Content-Type': 'application/yaml' });
        res.end(SCHEMA_FRAGMENT);
    } else if (path === '/outer.yaml') {
        res.writeHead(200, { 'Content-Type': 'application/yaml' });
        res.end(OUTER_WITH_BLOCKED_NESTED);
    } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('not found');
    }
});

server.listen(port, () => {
    console.log(`Network access-control fixtures server running at http://nodebackend:${port}`);
});
