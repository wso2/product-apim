const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');

// Order routes
router.get('/:orderId', orderController.getOrder);

module.exports = router;