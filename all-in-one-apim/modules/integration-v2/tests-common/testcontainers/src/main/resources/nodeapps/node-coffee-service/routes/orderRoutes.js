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

 // routes/order.routes.js
const express = require('express');
const router = express.Router();
const controller = require('../controllers/coffeeController');

router.post('/', controller.addOrder);                  // Handles POST /api/orders
router.get('/pending', controller.getPendingOrders);    // Handles GET /api/orders/pending
router.get('/:orderId', controller.getOrder);           // Handles GET /api/orders/:orderId
router.put('/', controller.updateOrder);                // Handles PUT /api/orders
router.put('/lock/:orderId', controller.lockOrder);     // Handles PUT /api/orders/lock/:orderId
router.delete('/:orderId', controller.removeOrder);     // Handles DELETE /api/orders/:orderId

module.exports = router;
