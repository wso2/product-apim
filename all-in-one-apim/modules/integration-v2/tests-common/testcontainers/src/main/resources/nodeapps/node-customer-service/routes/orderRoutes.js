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
const Order = require('../models/order');

let orders = {};

// Initialize with one order
orders[223] = new Order(223, 'order 223');

// GET /orders/{orderId}/
router.get('/orders/:orderId', (req, res) => {
  const id = parseInt(req.params.orderId);
  console.log(`----invoking getOrder, Order id is: ${id}`);
  const order = orders[id];
  if (order) {
    res.json(order);
  } else {
    res.status(404).send('Order not found');
  }
});

module.exports = router;
