const express = require('express');
const router = express.Router();
const Customer = require('../models/customer');

let customers = {};
let currentId = 123;

// Initialize with one customer
customers[currentId] = new Customer(currentId, 'John');

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
