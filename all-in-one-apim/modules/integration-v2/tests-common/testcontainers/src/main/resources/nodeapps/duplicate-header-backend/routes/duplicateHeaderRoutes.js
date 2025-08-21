const express = require('express');
const router = express.Router();
const { handleGetRequest } = require('../controllers/duplicateHeaderController');

router.get('/', handleGetRequest);

module.exports = router;
