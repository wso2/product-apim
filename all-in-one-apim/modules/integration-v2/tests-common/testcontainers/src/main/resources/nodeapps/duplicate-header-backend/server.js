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
const duplicateHeaderRoutes = require('./routes/duplicateHeaderRoutes');
const digestAuthRoutes = require('./routes/digestAuthRoutes');
const mailSinkRoutes = require('./routes/mailSinkRoutes');
const { startSmtpSink } = require('./controllers/mailSinkController');

const app = express();
const port = process.env.PORT || 3005;
const smtpPort = process.env.SMTP_PORT || 3025;

app.use('/duplicate', duplicateHeaderRoutes);
// HTTP Digest protected upstream for DIGEST endpoint-security tests (no other backend here challenges).
app.use('/digest', digestAuthRoutes);
// Host-facing introspection of the SMTP capture sink below (this app already owns a published HTTP port, which
// is why the sink lives here rather than in a new app).
app.use('/mail', mailSinkRoutes);

app.listen(port, () => {
    console.log(`Duplicate Header Backend Server running at http://nodebackend:${port}`);
});

// SMTP capture sink for APIM's new-API-version subscriber notification mail. Reached in-network as
// nodebackend:3025; unpublished on purpose (see mailSinkController.js).
startSmtpSink(smtpPort);
