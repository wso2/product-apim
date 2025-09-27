const express = require('express');
const router = express.Router();
const customerController = require('../controllers/customerController');

router.post('/', customerController.addCustomer);

router.get('/name', customerController.getCustomerName);
router.get('/:id', customerController.getCustomer);

router.put('/', customerController.updateCustomer);

router.delete('/:id', customerController.deleteCustomer);

module.exports = router;