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

 const { v4: uuidv4 } = require('uuid'); // Using uuid library for random IDs

class Order {
    constructor(drinkName = '', additions = '') {
        this.orderId = uuidv4();
        this.drinkName = drinkName;
        this.additions = additions;
        this.cost = 0.0;
        this.locked = false;
        this.timestamp = new Date().toISOString();
    }
}

module.exports = Order;
