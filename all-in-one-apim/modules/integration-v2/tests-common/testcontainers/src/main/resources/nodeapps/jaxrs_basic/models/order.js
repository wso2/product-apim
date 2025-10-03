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

 const Product = require('./product');

class Order {
    constructor(id = null, description = '') {
        this.id = id;
        this.description = description;
        this.products = new Map();
        this.init();
    }

    getId() {
        return this.id;
    }

    setId(id) {
        this.id = id;
    }

    getDescription() {
        return this.description;
    }

    setDescription(description) {
        this.description = description;
    }

    getProduct(productId) {
        console.log(`----invoking getProduct with id: ${productId}`);
        return this.products.get(parseInt(productId));
    }

    init() {
        const product = new Product();
        product.setId(323);
        product.setDescription("product 323");
        this.products.set(product.getId(), product);
    }

    toJSON() {
        return {
            id: this.id,
            description: this.description,
            products: Array.from(this.products.values()).map(p => p.toJSON())
        };
    }
}

module.exports = Order;
