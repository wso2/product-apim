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

// HTTPS backend presenting a SELF-SIGNED certificate the gateway does NOT trust out of the box — the upstream
// for the endpoint-certificate invocation arc (legacy APIEndpointCertificateTestCase): an API pointed at
// https://nodebackend:3023 answers 500 until the backend's certificate is uploaded via the publisher
// /endpoint-certificates API, then 200, then 500 again once the certificate is deleted.
//
// The key/certificate under ./certs are COMMITTED FIXTURES, deliberately not generated at build/run time: the
// certificate a test uploads must be the very one this server presents, and a regenerated pair would drift
// from the uploaded .cer and surface as a 500 indistinguishable from the behaviour under test. The same
// certificate is committed for upload as
// tests-integration/cucumber-tests/src/test/resources/artifacts/certs/endpoint/nodebackend.cer.
// See ./certs/README.md for the exact openssl command that produced them.
//
// CN and subjectAltName are both `nodebackend` because the gateway's HTTP sender performs hostname
// verification against the host in the endpoint URL (https://nodebackend:3023) — any other CN/SAN makes the
// post-upload 200 unreachable.

const express = require('express');
const fs = require('fs');
const https = require('https');
const path = require('path');

const app = express();
const port = process.env.PORT || 3023;

// Same customer fixture as node-customer-service (ids 123.. over these names), so an API pointed here returns
// the ordinary customer body a test already knows how to assert: GET /customers/123 -> {"id":123,"name":"John"}.
const names = ['John', 'Alice', 'Bob', 'John', 'Alice', 'Bob'];
const customers = {};
for (let i = 0; i < names.length; i++) {
  const id = 123 + i;
  customers[id] = { id: id, name: names[i] };
}

const router = express.Router();

// GET /customers/{id}
router.get('/customers/:id', (req, res) => {
  const id = parseInt(req.params.id);
  console.log(`----invoking getCustomer over TLS, Customer id is: ${id}`);
  const customer = customers[id];
  if (customer) {
    res.json(customer);
  } else {
    res.status(404).send('Customer not found');
  }
});

// Mounted at the root AND under the jaxrs_basic prefix node-customer-service uses, so an API can point its
// endpoint at either https://nodebackend:3023 or
// https://nodebackend:3023/jaxrs_basic/services/customers/customerservice and reach the same resource.
app.use('/', router);
app.use('/jaxrs_basic/services/customers/customerservice', router);

const options = {
  key: fs.readFileSync(path.join(__dirname, 'certs', 'server.key')),
  cert: fs.readFileSync(path.join(__dirname, 'certs', 'server.crt'))
};

https.createServer(options, app).listen(port, () => {
  console.log(`TLS backend (self-signed, CN=nodebackend) running at https://nodebackend:${port}`);
});
