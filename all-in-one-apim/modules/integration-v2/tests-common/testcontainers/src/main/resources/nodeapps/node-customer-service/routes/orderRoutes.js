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
