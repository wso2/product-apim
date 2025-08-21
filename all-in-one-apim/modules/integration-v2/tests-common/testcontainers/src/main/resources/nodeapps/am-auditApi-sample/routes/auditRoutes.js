const express = require('express');
const router = express.Router();
const auditController = require('../controllers/auditController');

router.get('/:apiId/assessmentreport', auditController.getResults);
router.post('/', auditController.postResults);
router.put('/:apiId', auditController.putResults);

module.exports = router;
