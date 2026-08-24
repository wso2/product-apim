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

// Raw-socket backend that answers with a NON-STANDARD HTTP reason phrase — the upstream for the custom
// status-message test (legacy APIMANAGER5326CustomStatusMsgTestCase, whose SimpleSocketServer this replaces).
// The assertion it unblocks is that the gateway PRESERVES the backend's reason phrase rather than normalising
// it to the canonical "Bad Request": an API pointed at http://nodebackend:3024 must answer with a status line
// containing `400 Custom response`.
//
// Written on net.createServer, NOT express/http, deliberately: node's http server derives the reason phrase
// from the status code and offers no way to emit an arbitrary one, so the status line has to be written to the
// socket literally. Everything else about the response is well formed (accurate Content-Length, explicit
// Connection: close) so the only unusual thing on the wire is the reason phrase under test.
//
// EVERY request on this port gets the custom response, whatever the path — the port is the selector, exactly
// as in the legacy socket server. `/custom-status` is the path tests should use so the intent reads clearly in
// the feature, but nothing here depends on it.

const net = require('net');

const port = process.env.PORT || 3024;
const path = '/custom-status';

// Cap on the request head we will buffer before giving up on a client that never terminates it.
const MAX_HEAD_BYTES = 64 * 1024;

const body = '<?xml version="1.0" encoding="UTF-8"?><test></test>';
const response = [
  'HTTP/1.1 400 Custom response',
  'Server: testServer',
  'Content-Type: text/xml; charset=UTF-8',
  'Content-Length: ' + Buffer.byteLength(body),
  'Connection: close',
  '',
  body
].join('\r\n');

const server = net.createServer((socket) => {
  let received = '';
  let answered = false;
  socket.on('data', (chunk) => {
    // One answer per connection: without this, a chunk arriving after end() still matches the buffered head
    // and ends the socket a second time, and that write-after-end is swallowed by the error handler below.
    if (answered) {
      return;
    }
    received += chunk.toString();
    if (received.length > MAX_HEAD_BYTES) {
      socket.destroy();
      return;
    }
    // Answer as soon as the request head is complete; the canned response is the same regardless of the
    // request, so there is no reason to read a body.
    if (received.includes('\r\n\r\n')) {
      answered = true;
      console.log('----invoking custom-status backend, request line: ' + received.split('\r\n')[0]);
      socket.end(response);
    }
  });
  socket.on('error', () => { /* ignore transient socket errors */ });
});

server.listen(port, () => {
  console.log(`Custom status-message backend running at http://nodebackend:${port}${path}`);
});
