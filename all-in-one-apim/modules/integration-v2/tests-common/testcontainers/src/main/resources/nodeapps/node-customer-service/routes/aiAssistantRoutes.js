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

// Canned-response stub for the WSO2 (Choreo) AI service that the APIM AI-assistant default implementations call
// (Marketplace Assistant, Design Assistant, API Chat). Used by the AI-assistant integration tests (PR #13920):
// the APIM server's [apim.ai] endpoint is pointed at http://nodebackend:3001 and the default resource paths
// (/ai/marketplace-assistant/chat, /ai/spec-populator/api-count, /ai/api-design-assistant/*, /ai/api-chat/prepare)
// resolve to the routes below. APIUtil.invokeAIService treats HTTP 201 as success for chat/execute; the
// API-count GET expects HTTP 200.
//
// Credentials are the single source of truth for valid-vs-invalid (there are no separate "unauthorized" routes).
// APIUtil.executeAIRequest attaches ONE of two credentials depending on the [apim.ai] config branch:
//   - auth-token branch  (apim.ai.token set, no token_endpoint): header  API-KEY: <token>            -> resources
//   - key branch         (apim.ai.key + token_endpoint set):     POST Authorization: Basic <key> to /token,
//                                                                 then  Authorization: Bearer <access_token> -> resources
// So this stub validates:
//   /token          -> issues a bearer ONLY for VALID_AI_KEY; any other Basic key => 401 (invalid_client). A bad
//                      key makes AccessTokenGenerator.getAccessToken() return null, so the follow-up resource call
//                      arrives as "Authorization: Bearer null" and is rejected by aiAuth below.
//   resource routes -> aiAuth accepts a valid API-KEY (auth-token branch) OR the issued bearer (key branch);
//                      anything else => 401.
// The invalid-key overlay (configFiles/aiInvalidKey/deployment.toml) sets apim.ai.key to a value that is NOT
// VALID_AI_KEY, so the token exchange fails and every AI resource call ends in a 401 - exercising the REST
// layer's credential-error mapping: chat / generate-payload / api-chat prepare -> 401, marketplace api-count -> 500.
const express = require('express');
const router = express.Router();

// Credentials this stub accepts. The happy-path overlay (customAuthHeaderAndAppSharing) sets apim.ai.token to
// VALID_API_KEY; a positive key-branch caller would present VALID_AI_KEY. The invalid-key overlay deliberately
// uses neither.
const VALID_API_KEY = 'integration-v2-ai-token';   // auth-token branch: APIM sends header  API-KEY: <token>
const VALID_AI_KEY = 'integration-v2-ai-key';       // key branch: APIM sends  Authorization: Basic <key>  to /token
const ISSUED_BEARER = 'mock-access-token';          // the access_token /token mints for VALID_AI_KEY

// Resource-route auth guard. Accepts either the auth-token branch's API-KEY header or the key branch's issued
// bearer; rejects everything else (including the "Bearer null" produced by a failed token exchange) with 401.
function aiAuth(req, res, next) {
  const apiKey = req.get('API-KEY');
  const authz = req.get('Authorization') || '';
  const bearer = authz.replace(/^Bearer\s+/i, '').trim();
  if (apiKey === VALID_API_KEY || bearer === ISSUED_BEARER) {
    return next();
  }
  console.log('----AI stub: rejecting resource call with invalid credentials');
  return res.status(401).json({ error: 'invalid credentials' });
}

// OAuth token endpoint (key branch). Issues a bearer only for the valid key; any other Basic key => 401.
router.post('/token', (req, res) => {
  const authz = req.get('Authorization') || '';
  const presentedKey = authz.replace(/^Basic\s+/i, '').trim();
  if (presentedKey !== VALID_AI_KEY) {
    console.log('----AI stub: OAuth token endpoint rejecting invalid key');
    return res.status(401).json({ error: 'invalid_client' });
  }
  console.log('----AI stub: OAuth token endpoint issuing access token');
  res.status(200).json({ access_token: ISSUED_BEARER, expires_in: 3600 });
});

// POST /ai/marketplace-assistant/chat -> body maps onto MarketplaceAssistantResponseDTO { response, apis }
router.post('/marketplace-assistant/chat', aiAuth, (req, res) => {
  console.log('----AI stub: marketplace-assistant chat invoked');
  res.status(201).json({
    response: 'These are the available APIs matching your query.',
    apis: []
  });
});

// GET /ai/spec-populator/api-count -> body maps onto MarketplaceAssistantApiCountResponseDTO { count, limit }
router.get('/spec-populator/api-count', aiAuth, (req, res) => {
  console.log('----AI stub: marketplace-assistant api-count invoked');
  res.status(200).json({ count: 42, limit: 100 });
});

// Benign endpoints hit asynchronously by MarketplaceAssistantApiPublisherNotifier on API publish/delete in a
// block whose apim.ai.endpoint points here - return success so the notifier does not log spurious errors. Not
// guarded: they are fire-and-forget and their status does not affect any assertion.
router.post('/spec-populator/publish-api', (req, res) => {
  res.status(201).json({});
});
router.post('/spec-populator/remove-api', (req, res) => {
  res.status(200).json({});
});

// POST /ai/api-design-assistant/generate-api-payload -> raw body is passed through as DesignAssistant "payload"
// (the Publisher REST layer wraps it as { generatedPayload: <body> } and returns 200).
router.post('/api-design-assistant/generate-api-payload', aiAuth, (req, res) => {
  console.log('----AI stub: design-assistant generate-api-payload invoked');
  res.status(201).send('Mock design payload generated by the AI stub');
});

// POST /ai/api-design-assistant/chat -> body maps onto DesignAssistantChatResponseDTO { backendResponse, ... }
router.post('/api-design-assistant/chat', aiAuth, (req, res) => {
  console.log('----AI stub: design-assistant chat invoked');
  res.status(201).json({ backendResponse: 'Mock design assistant chat response' });
});

// POST /ai/api-chat/prepare -> body maps onto ApiChatResponseDTO { result, ... } (returned as 201 by the
// DevPortal REST layer). Only the known 'result' field is set so Jackson deserialization succeeds.
router.post('/api-chat/prepare', aiAuth, (req, res) => {
  console.log('----AI stub: api-chat prepare invoked');
  res.status(201).json({ result: 'Mock API chat prepare result' });
});

module.exports = router;
