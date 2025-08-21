const express = require('express');
const router = express.Router();
const etcdController = require('../controllers/etcdController');

// Path: /v2/keys/jti/2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d
const fullPath = '/2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d';

router.get(fullPath, etcdController.getKey);
router.post(fullPath, etcdController.postKey);
router.put(fullPath, etcdController.putKey);

module.exports = router;
