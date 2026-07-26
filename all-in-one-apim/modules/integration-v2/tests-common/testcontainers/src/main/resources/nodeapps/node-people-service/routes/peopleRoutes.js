/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

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
