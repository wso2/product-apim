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

// Host-facing introspection for the SMTP capture sink (see controllers/mailSinkController.js). The recipient is a
// query parameter rather than a path segment because it is an email address.
const express = require('express');
const router = express.Router();
const { handleGetInbox, handleClearInbox } = require('../controllers/mailSinkController');

router.get('/inbox', handleGetInbox);
router.delete('/inbox', handleClearInbox);

module.exports = router;
