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
const { WebSocketServer } = require('ws');
const { execute, subscribe, graphql } = require('graphql');
const { SubscriptionServer } = require('subscriptions-transport-ws');
const { makeExecutableSchema } = require('@graphql-tools/schema');
const languageRoutes = require('./routes/languageRoutes');

const app = express();
const port = 3003;

app.use(express.json());

app.use('/graphql', languageRoutes);

// --- Real, introspection-capable GraphQL endpoint (for "create GraphQL API from endpoint via introspection")
// The default /graphql above is a hand-rolled POST handler with NO introspection, so APIM's introspection query
// cannot derive a schema from it. /graphql-full is a proper graphql() handler over the languages schema, so it
// answers the standard __schema introspection query the publisher sends.
const languagesTypeDefs = `type Language { code: String name: String }\n`
  + `type Query { languages: [Language] language(code: String): Language }`;
const languagesSchema = makeExecutableSchema({
  typeDefs: languagesTypeDefs,
  resolvers: {
    Query: {
      languages: () => [{ code: 'en', name: 'English' }, { code: 'fr', name: 'French' }],
      language: (_root, args) => ({ code: args.code || 'en', name: 'English' })
    }
  }
});
app.post('/graphql-full', async (req, res) => {
  const body = req.body || {};
  const result = await graphql({ schema: languagesSchema, source: body.query || '', variableValues: body.variables });
  res.json(result);
});

// --- Raw SDL served at a URL (for "create GraphQL API from an SDL URL" — APIM fetches the SDL text).
app.get('/sdl', (req, res) => {
  res.type('text/plain').send(languagesTypeDefs);
});

// --- Auth-protected GraphQL endpoint (for GraphQL endpoint-security: the gateway must inject the credential).
// Requires `Authorization: Bearer graphql-secret`; otherwise 401. Behind the check it reuses the languages POST
// handler, so an authorised (gateway-injected) request returns the languages data.
app.use('/graphql-secured', (req, res, next) => {
  if (req.headers['authorization'] !== 'Bearer graphql-secret') {
    return res.status(401).json({ errors: [{ message: 'Unauthorized: missing or invalid backend credential' }] });
  }
  next();
}, languageRoutes);

const server = app.listen(port, () => {
  console.log(`AM GraphQL Server running at http://nodebackend:${port}`);
});

// --- GraphQL subscription endpoint for the gateway GraphQL-subscription invocation tests
// (GraphqlSubscriptionTestCase), built on the official `subscriptions-transport-ws` library over `ws`. This is
// the library that implements the `graphql-ws` SUBPROTOCOL (connection_init/connection_ack, start/data) that the
// APIM 4.7.0 gateway speaks — the newer npm `graphql-ws` package implements a DIFFERENT subprotocol
// (graphql-transport-ws) which the gateway does not negotiate (verified: it rejects the legacy subprotocol).
// Exposes a real executable schema whose `liftStatusChange` subscription emits one Lift event
// ({name:"Astra Express"}), matching the legacy SubscriptionWSServerImpl contract.
const typeDefs = `
  type Lift { id: ID! name: String! }
  type Query { _empty: String }
  type Subscription { liftStatusChange: Lift! }
`;
const resolvers = {
  Subscription: {
    liftStatusChange: {
      subscribe: async function* () {
        yield { liftStatusChange: { id: '1', name: 'Astra Express' } };
      }
    }
  }
};
const schema = makeExecutableSchema({ typeDefs, resolvers });

// Attach to a ws server bound to the http.Server (all upgrade paths on 3003), so the gateway's subscription
// proxy reaches it regardless of the upgrade path.
const wsServer = new WebSocketServer({ server });
SubscriptionServer.create({ schema, execute, subscribe }, wsServer);
console.log('----subscriptions-transport-ws (official lib) subscription server attached on port ' + port);
