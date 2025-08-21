const express = require('express');
const router = express.Router();
const wildcardController = require('../controllers/wildcardController');

router.get('/*', wildcardController.handleRequest);
router.post('/*', wildcardController.handleRequest);
router.put('/*', wildcardController.handleRequest);
router.delete('/*', wildcardController.handleRequest);
router.patch('/*', wildcardController.handleRequest);
router.head('/*', wildcardController.handleRequest);

module.exports = router;
