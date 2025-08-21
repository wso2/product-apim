const express = require('express');
const router = express.Router();
const languageController = require('../controllers/languageController');

router.post('/', languageController.getLanguages);

module.exports = router;
