const express = require('express');
const router = express.Router();
const nameController = require('../controllers/nameController');

// GET /name
router.get('/name', nameController.getName);

module.exports = router;
