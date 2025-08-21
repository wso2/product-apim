// routes/payment.routes.js
const express = require('express');
const router = express.Router();
const controller = require('../controllers/coffeeController');

router.post('/:orderId', controller.doPayment);        // Handles POST /api/payment/:orderId
router.get('/:orderId', controller.getPayment);        // Handles GET /api/payment/:orderId

module.exports = router;