const express = require('express');
const router = express.Router();
const controller = require('../controllers/processInstanceController');

router.post('/', controller.startInstance);
router.get('/', controller.getRequestInfo);
router.delete('/:id', controller.deleteTaskInstance);

module.exports = router;
