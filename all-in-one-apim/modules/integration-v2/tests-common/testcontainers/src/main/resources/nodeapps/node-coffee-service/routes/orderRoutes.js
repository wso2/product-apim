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