// routes/people.routes.js
const express = require('express');
const router = express.Router();
const peopleController = require('../controllers/peopleController');

// routes to controller functions
router.get('/', peopleController.getPeople);
router.post('/', peopleController.addPerson);
router.get('/:email', peopleController.getPersonByEmail);
router.put('/:email', peopleController.updatePerson);
router.delete('/:email', peopleController.deletePerson);
router.head('/:email', peopleController.checkPerson);

router.options('/options', peopleController.getOptions);

module.exports = router;