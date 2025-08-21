const express = require('express');
const router = express.Router();

const headerController = require('../controllers/headerController');

router.get('/sec', headerController.getSec);
router.get('/handler', headerController.getHandler);

module.exports = router;