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

 const handleGetRequest = (req, res) => {
    res.status(200);
    res.setHeader('Content-Type', 'application/json');

    res.append('Set-Cookie', '12wesdsfdffdsfff');
    res.append('Set-Cookie', '3456wesfdsfdsfdf');

    res.write(JSON.stringify({ RestResponse: "true" }));
    res.end();
};

// Emits TWO `Transfer-Encoding: chunked` response headers. Express/Node normalise hop-by-hop headers, so the
// status line + header block is written as raw bytes straight to the socket (the body follows chunk-encoded).
const handleTransferEncodingRequest = (req, res) => {
    const body = JSON.stringify({ RestResponse: "duplicateTransferEncoding" });
    const raw = 'HTTP/1.1 200 OK\r\n'
        + 'Server: testServer\r\n'
        + 'Content-Type: application/json\r\n'
        + 'Transfer-Encoding: chunked\r\n'
        + 'Transfer-Encoding: chunked\r\n'
        + 'Connection: close\r\n'
        + '\r\n'
        + Buffer.byteLength(body).toString(16) + '\r\n' + body + '\r\n0\r\n\r\n';

    const socket = res.socket;
    // Detach so Node does not append its own (normalised) response onto the same socket.
    if (typeof res.detachSocket === 'function') {
        res.detachSocket(socket);
    }
    socket.write(raw);
    socket.end();
};

module.exports = { handleGetRequest, handleTransferEncodingRequest };
