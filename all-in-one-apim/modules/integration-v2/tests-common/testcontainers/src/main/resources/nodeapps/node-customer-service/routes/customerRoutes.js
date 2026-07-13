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
const router = express.Router();
const Customer = require('../models/customer');

let customers = {};
let currentId = 123;

// Initialize with one customer
//customers[currentId] = new Customer(currentId, 'John');
const names = ['John', 'Alice', 'Bob', 'John', 'Alice', 'Bob'];

for (let i = 0; i < names.length; i++) {
    const id = currentId + i;  // 123, 124, 125
    customers[id] = new Customer(id, names[i]);
}

// GET /customers/{id}/
router.get('/customers/:id', (req, res) => {
  const id = parseInt(req.params.id);
  console.log(`----invoking getCustomer, Customer id is: ${id}`);
  const customer = customers[id];
  if (customer) {
    res.json(customer);
  } else {
    res.status(404).send('Customer not found');
  }
});

// GET /sec/
router.get('/sec', (req, res) => {
  const authHeader = req.header('Authorization');
  console.log(`----invoking getSec: ${authHeader}`);
  res.type('text/plain').send(authHeader || '');
});

// GET /check-header/
router.get('/check-header', (req, res) => {
  const headerValue = req.header('x-request-header');
  console.log(`----invoking check-header, received: ${headerValue}`);

  if (headerValue === 'x-req-value') {
    res.status(200).json({ message: 'Valid header received' });
  } else {
    res.status(400).json({ error: 'Missing or invalid x-request-header' });
  }
});

// GET /handler/
router.get('/handler', (req, res) => {
  const header = req.header('Iwasat');
  console.log(`----invoking handler handler handler`);
  res.type('text/plain').send(header || '');
});

// GET /hello
router.get('/hello', (req, res) => {
  console.log('----invoking hello');
  res.type('text/plain').send('Hello World');
});

// PUT /customers/
router.put('/customers', (req, res) => {
  console.log(`----invoking updateCustomer`);
  const customer = JSON.parse(req.body);
  if (customers[customer.id]) {
    customers[customer.id] = customer;
    res.status(200).send();
  } else {
    res.status(304).send();
  }
});

// POST /customers/
router.post('/customers', (req, res) => {
  console.log(`----invoking addCustomer`);
  const customer = JSON.parse(req.body);
  customer.id = ++currentId;
  customers[customer.id] = customer;
  res.status(200).json(customer);
});

// POST /customers/name/
router.post('/customers/name', (req, res) => {
  const id = req.body;
  console.log(`----invoking getCustomerName, Customer id is: ${id}`);
  res.type('text/plain').send('Tom');
});

// DELETE /customers/{id}/
router.delete('/customers/:id', (req, res) => {
  const id = parseInt(req.params.id);
  console.log(`----invoking deleteCustomer, Customer id is: ${id}`);
  if (customers[id]) {
    delete customers[id];
    res.status(200).send();
  } else {
    res.status(304).send();
  }
});

// GET /echo/... — wildcard echo: returns the raw sub-path it received (any number of segments), echoing
// req.originalUrl. Used to verify the gateway forwards an encoded URI path segment to the backend when the API
// uses a uri-template resource + a {uri.var.x} templated endpoint (which double-appends: /echo/sub<val>/<val>,
// as the legacy Synapse backend expected). Scoped to /echo so it does not mask other routes.
router.get('/echo/*', (req, res) => {
  console.log(`----invoking echo, received: ${req.originalUrl}`);
  res.status(200).json({ received: req.originalUrl });
});

// GET /reflect-headers — reflects the request headers the backend received back in the response body, so a
// test can assert on headers the gateway injects towards the backend (e.g. the X-JWT-Assertion backend JWT
// carrying application-attribute claims). The legacy ApplicationAttributesTestCase used a header-echoing
// backend (jwt_backend) for exactly this. Scoped to /reflect-headers so it does not mask other routes.
router.get('/reflect-headers', (req, res) => {
  res.status(200).json({ headers: req.headers });
});

// POST /reflect-body — echoes the raw request body the backend received straight back in the response, so a
// test can assert on a gateway request-flow transformation (e.g. the jsonToXML operation policy converting a
// JSON request body to XML — content-type application/xml — before it reaches the backend). A route-level
// catch-all text parser captures the raw body for ANY content-type (the app-level parsers only cover
// text/plain, text/xml and json, so application/xml would otherwise arrive unparsed as {}). Scoped to
// /reflect-body so it does not mask other routes.
router.all('/reflect-body', express.text({ type: () => true }), (req, res) => {
  const body = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
  res.status(200).send(body);
});

// --- Petstore routes for the gateway schema-validation tests (SchemaValidationTestCase) ---
// The gateway validates requests/responses against the imported petstore OpenAPI when the API has
// enableSchemaValidation=true. These routes return schema-shaped bodies so the SAME resources drive both
// the pass and the response-validation-fail outcomes. Request-validation failures (malformed body / missing
// required header) are rejected at the gateway BEFORE reaching the backend, so they need no branch here.

// GET /pets → a schema-valid Pets array. The gateway rejects the call earlier if the required X-Request-ID
// header is absent; when present, this valid array passes response validation → 200.
router.get('/pets', (req, res) => {
  res.status(200).json([{ id: 1, name: 'max' }]);
});

// POST /pets → a schema-valid Pet (id + name). A schema-valid request body therefore yields 200; an invalid
// body (missing required name) is rejected at the gateway before it reaches here.
router.post('/pets', (req, res) => {
  res.status(200).json({ id: 1, name: 'max' });
});

// GET /pets/:petId → branch on isAvailable so one resource covers both response-validation outcomes: with the
// query param it returns a schema-valid Pet (id+name → 200); without it returns a body missing the required
// `id`, so the gateway's RESPONSE schema validation fails (→ 500). Mirrors the petstore backend the legacy
// SchemaValidationTestCase drove.
router.get('/pets/:petId', (req, res) => {
  if (req.query.isAvailable !== undefined) {
    res.status(200).json({ id: parseInt(req.params.petId) || 1, name: 'max' });
  } else {
    res.status(200).json({ name: 'max' });
  }
});

// GET /pet/findByStatus → unsecured resource (x-auth-type None in the OAS). The negative case calls it without
// the required `status` query param, so the gateway rejects it before the backend; this valid array covers the
// (unused) valid path for completeness.
router.get('/pet/findByStatus', (req, res) => {
  res.status(200).json([{ id: 1, name: 'max' }]);
});

// --- Mock LLM (Mistral-style) endpoint for the gateway AI-API invocation tests (AIAPITestCase) ---
// An AI API's backend endpoint points here; the gateway proxies POST /chat/completions to it. Returns a
// Mistral-shaped chat-completion response (200) including the token-usage fields the AI provider extracts
// ($.model, $.usage.*). Mounted under /no-auth (the unsecured AI API's backend path) so the gateway-appended
// resource /v1/chat/completions resolves to /no-auth/v1/chat/completions.
function chatCompletion(req, res) {
  console.log('----invoking mock LLM chat/completions');
  const model = (req.body && req.body.model) ? req.body.model : 'mistral-small-latest';
  res.status(200).json({
    id: 'f821a1dd4df2492382ec9676b59ddcd3',
    object: 'chat.completion',
    created: 1727259070,
    model: model,
    choices: [
      {
        index: 0,
        message: { role: 'assistant', content: 'Claude Monet is among the most renowned French painters.', tool_calls: null },
        finish_reason: 'stop',
        logprobs: null
      }
    ],
    usage: { prompt_tokens: 12, total_tokens: 358, completion_tokens: 346 }
  });
}
router.post('/no-auth/v1/chat/completions', chatCompletion);
router.post('/v1/chat/completions', chatCompletion);

// Secured AI backend: requires the Authorization header the AI service provider injects on the backend leg
// (the provider's authenticationConfiguration is apikey/header "Authorization"; the API's endpoint_security
// carries the credential "Bearer 123"). A 200 through the gateway therefore PROVES APIM injected the configured
// backend credential — without it the backend rejects with 401. Mounted under /with-auth (the secured AI API's
// backend path), so the gateway-appended /v1/chat/completions resolves to /with-auth/v1/chat/completions.
function securedChatCompletion(req, res) {
  const auth = req.header('Authorization');
  if (auth !== 'Bearer 123') {
    console.log('----secured mock LLM rejected: Authorization header not injected (got: ' + auth + ')');
    return res.status(401).json({ error: 'unauthorized: backend credential not injected by the gateway' });
  }
  console.log('----secured mock LLM: Authorization header present, returning chat/completions');
  return chatCompletion(req, res);
}
router.post('/with-auth/v1/chat/completions', securedChatCompletion);

// Always-failing AI backend route — the FAILOVER TARGET for the modelFailover policy test. Returns 429 (the
// exact signal the legacy WireMock failover stub returned) so the gateway's failover policy abandons the target
// model/endpoint and retries the fallback model on the default (echoing) endpoint. A fallback-model response
// therefore PROVES failover kicked in.
router.post('/failover-target/v1/chat/completions', (req, res) => {
  console.log('----failover-target route hit → returning 429 to trigger failover');
  res.status(429).json({ error: 'model endpoint rate-limited (failover target)' });
});

module.exports = router;
