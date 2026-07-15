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

// A REAL mock MCP server built on the official @modelcontextprotocol/sdk (Streamable HTTP transport). It backs
// the gateway MCP-server PROXY-mode invocation tests (MCPServerTestCase): the APIM gateway proxies a client's
// MCP JSON-RPC (initialize / tools/list / tools/call) to this server. Exposes three real tools (echo, add,
// get_pets) with actual dispatch — more faithful than the legacy WireMock canned stubs.
const express = require('express');
const { randomUUID } = require('crypto');
const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StreamableHTTPServerTransport } = require('@modelcontextprotocol/sdk/server/streamableHttp.js');
const { isInitializeRequest, ListToolsRequestSchema, CallToolRequestSchema } =
    require('@modelcontextprotocol/sdk/types.js');

const app = express();
const port = 3020;
app.use(express.json());

// Uses the SDK's LOW-LEVEL Server (McpServer is a thin wrapper over it) so the tools/list wire shape is exactly
// what we return — a CLEAN inputSchema (no $schema / additionalProperties) and NO `execution` field. McpServer's
// registerTool auto-injects those (newer MCP spec), which APIM 4.7.0's MCP feature-generator cannot map to URI
// templates ("no URI templates were produced" → 500). Matching the legacy tool shape keeps the gateway happy.
function buildServer() {
  const server = new Server({ name: 'wso2-mock-mcp', version: '1.0.0' }, { capabilities: { tools: {} } });
  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
      { name: 'echo', description: 'Echoes the provided message',
        inputSchema: { type: 'object', properties: { message: { type: 'string' } }, required: ['message'] } },
      { name: 'add', description: 'Adds two numbers',
        inputSchema: { type: 'object', properties: { a: { type: 'number' }, b: { type: 'number' } }, required: ['a', 'b'] } },
      { name: 'get_pets', description: 'Returns the list of pets',
        inputSchema: { type: 'object', properties: {}, required: [] } }
    ]
  }));
  server.setRequestHandler(CallToolRequestSchema, async (req) => {
    const name = req.params.name;
    const args = req.params.arguments || {};
    if (name === 'echo') {
      return { content: [{ type: 'text', text: String(args.message) }] };
    }
    if (name === 'add') {
      return { content: [{ type: 'text', text: String(args.a + args.b) }] };
    }
    if (name === 'get_pets') {
      return { content: [{ type: 'text', text: JSON.stringify([{ id: 1, name: 'max' }]) }] };
    }
    return { content: [{ type: 'text', text: 'unknown tool: ' + name }], isError: true };
  });
  return server;
}

// Session-keyed transports (stateful Streamable HTTP — the documented SDK pattern).
const transports = {};

app.post('/mcp', async (req, res) => {
  try {
    console.log('----MCP req method=' + (req.body && req.body.method) + ' sid=' + (req.headers['mcp-session-id'] || 'none'));
    const sessionId = req.headers['mcp-session-id'];
    let transport;
    if (sessionId && transports[sessionId]) {
      transport = transports[sessionId];
    } else if (!sessionId && isInitializeRequest(req.body)) {
      transport = new StreamableHTTPServerTransport({
        sessionIdGenerator: () => randomUUID(),
        // Return plain application/json responses (not SSE). Session semantics (Mcp-Session-Id) are preserved;
        // only the framing changes — APIM's MCP proxy/discovery consumes JSON (the legacy WireMock also
        // returned JSON), so SSE-framed responses break it.
        enableJsonResponse: true,
        onsessioninitialized: (sid) => { transports[sid] = transport; }
      });
      transport.onclose = () => {
        if (transport.sessionId) {
          delete transports[transport.sessionId];
        }
      };
      await buildServer().connect(transport);
    } else {
      res.status(400).json({
        jsonrpc: '2.0',
        error: { code: -32000, message: 'Bad Request: No valid session ID provided' },
        id: null
      });
      return;
    }
    await transport.handleRequest(req, res, req.body);
  } catch (e) {
    console.error('----MCP handler error:', e);
    if (!res.headersSent) {
      res.status(500).json({ jsonrpc: '2.0', error: { code: -32603, message: 'Internal error' }, id: null });
    }
  }
});

// GET (server->client SSE stream) and DELETE (session teardown) for the Streamable HTTP transport.
async function handleSessionRequest(req, res) {
  const sessionId = req.headers['mcp-session-id'];
  if (!sessionId || !transports[sessionId]) {
    res.status(400).send('Invalid or missing session ID');
    return;
  }
  await transports[sessionId].handleRequest(req, res);
}
app.get('/mcp', handleSessionRequest);
app.delete('/mcp', handleSessionRequest);

app.listen(port, () => {
  console.log(`Mock MCP server (official SDK) running at http://nodebackend:${port}/mcp`);
});
