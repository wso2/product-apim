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

 const express = require('express');
const bodyParser = require('body-parser');
const { WebSocketServer } = require('ws');
const customerRoutes = require('./routes/customerRoutes');
const orderRoutes = require('./routes/orderRoutes');
const aiAssistantRoutes = require('./routes/aiAssistantRoutes');

const app = express();
const port = 3001;

app.use(bodyParser.text({ type: 'text/plain' }));
app.use(bodyParser.text({ type: 'text/xml' }));
app.use(bodyParser.json());

app.use('/jaxrs_basic/services/customers/customerservice', customerRoutes);
app.use('/jaxrs_basic/services/customers/customerservice', orderRoutes);
// Canned AI-service stub for the Marketplace Assistant default-implementation happy-path test (PR #13920).
// APIM's [apim.ai] endpoint points at http://nodebackend:3001 and the default resource paths are under /ai.
app.use('/ai', aiAssistantRoutes);

const server = app.listen(port, () => {
  console.log(`Customer Service API running at http://nodebackend:${port}`);
});

// --- WebSocket echo endpoint for the gateway WebSocket-API invocation tests (WebSocketAPITestCase) ---
// Built on the official `ws` library. The gateway's WebsocketHandler (apim.ws.port=9099) upgrades a client WS
// connection and proxies it here (the WS API's ws:// endpoint points at this server). It ECHOES each text
// message back UPPERCASED — the exact contract the legacy Jetty WebSocketServerImpl used (client asserts
// response == message.toUpperCase()). Attached to the same http.Server (port 3001), so any upgrade path is
// accepted (the gateway strips the API context before proxying).
const wss = new WebSocketServer({ server });
wss.on('connection', (socket) => {
  console.log('----WS connection accepted (ws lib)');
  socket.on('message', (data) => {
    const msg = data.toString();
    console.log('----WS echo, received: ' + msg);
    socket.send(msg.toUpperCase());
  });
  socket.on('error', () => { /* ignore transient socket errors */ });
});
