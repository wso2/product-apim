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

// Minimal SOAP mock backend for gateway SOAP-invocation integration tests. It accepts any SOAP 1.1/1.2
// request (text/xml or application/soap+xml) on any path and returns a static, well-formed SOAP envelope
// so a SOAP API proxied through the APIM gateway can be invoked end-to-end without an external service.
const express = require('express');
const bodyParser = require('body-parser');

const app = express();
const port = 3019;

app.use(bodyParser.text({ type: ['text/xml', 'application/soap+xml', 'application/xml'] }));

// Health check (plain GET) so readiness/debugging is easy.
app.get('/health', (req, res) => res.status(200).send('OK'));

// Respond to any POST with a fixed SOAP response envelope.
app.post('*', (req, res) => {
  const responseEnvelope =
    '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" ' +
    'xmlns:ns="http://ws.cdyne.com/PhoneVerify/query">' +
    '<soapenv:Body>' +
    '<ns:CheckPhoneNumberResponse>' +
    '<ns:CheckPhoneNumberResult>' +
    '<ns:Valid>true</ns:Valid>' +
    '<ns:Company>SOAP Stub</ns:Company>' +
    '</ns:CheckPhoneNumberResult>' +
    '</ns:CheckPhoneNumberResponse>' +
    '</soapenv:Body>' +
    '</soapenv:Envelope>';
  res.set('Content-Type', 'text/xml');
  res.status(200).send(responseEnvelope);
});

app.listen(port, () => {
  console.log(`SOAP stub backend running at http://nodebackend:${port}`);
});
