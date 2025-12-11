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

module.exports = router;
