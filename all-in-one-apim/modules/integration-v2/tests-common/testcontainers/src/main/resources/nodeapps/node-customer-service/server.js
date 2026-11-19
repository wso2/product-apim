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

const app = express();
const port = process.env.PORT || 3001;

app.use(bodyParser.text({ type: 'text/plain' }));
app.use(bodyParser.text({ type: 'text/xml' }));
app.use(bodyParser.json());

// Deterministic Atom feed fixture for the legacy YouTube sample API invocation test. The public YouTube Data API
// used by the legacy test is retired, so serve the expected feed shape locally while preserving its request path.
app.get('/feeds/api/standardfeeds/most_popular', (req, res) => {
  const feed = '<?xml version="1.0" encoding="UTF-8"?>' +
    '<feed xmlns="http://www.w3.org/2005/Atom">' +
    '<category term="most_popular"/>' +
    '<entry><id>video-1</id></entry>' +
    '</feed>';
  res.type('application/atom+xml').status(200).send(feed);
});

// Match the bundled calculator webapp behavior for APIMANAGER2611EndpointValidationTestCase: canonicalize the
// context URL with a trailing-slash redirect, then serve the landing page for GET and HEAD.
app.all(/^\/am\/sample\/calculator\/v1$/, (req, res) => {
  res.redirect(302, `${req.path}/`);
});
app.get('/am/sample/calculator/v1/', (req, res) => {
  res.type('html').status(200).send('<!DOCTYPE html><html lang="en"><head><title>WSO2 API Manager - Sample Calculator Api</title></head><body><h2>WSO2 API Manager - Sample Calculator Api</h2></body></html>');
});

app.use('/jaxrs_basic/services/customers/customerservice', customerRoutes);
app.use('/jaxrs_basic/services/customers/customerservice', orderRoutes);

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
