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

 const Customer = require('../models/customer');

// Global (module-scoped) variables
let currentId = 123;
let customers = new Map();

// Initialize with one customer
const init = () => {
    const customer = new Customer();
    customer.setName("John");
    customer.setId(123);
    customers.set(customer.getId(), customer);
};

init();

// GET /customerservice/customers/:id
const getCustomer = (req, res) => {
    const id = req.params.id;
    console.log(`----invoking getCustomer, Customer id is: ${id}`);
    const idNumber = parseInt(id);
    const customer = customers.get(idNumber);

    if (customer) {
        res.json(customer.toJSON());
    } else {
        res.status(404).json({ error: 'Customer not found' });
    }
};

// PUT /customerservice/customers
const updateCustomer = (req, res) => {
    const customerData = req.body;
    console.log(`----invoking updateCustomer, Customer name is: ${customerData.name}`);

    const customer = customers.get(customerData.id);
    if (customer) {
        customer.setName(customerData.name);
        customers.set(customerData.id, customer);
        res.status(200).send();
    } else {
        res.status(304).send(); // Not Modified
    }
};

// POST /customerservice/customers
const addCustomer = (req, res) => {
    const customerData = req.body;
    console.log(`----invoking addCustomer, Customer name is: ${customerData.name}`);

    const customer = new Customer();
    customer.setId(++currentId);
    customer.setName(customerData.name);

    customers.set(customer.getId(), customer);
    res.status(200).json(customer.toJSON());
};

// GET /customerservice/customers/name
const getCustomerName = (req, res) => {
    const id = req.body.id;
    if (!id) {
            return res.status(400).send('Customer ID is required');
    }
    console.log(`----invoking getCustomerName, Customer id is: ${id}`);
    const customer = customers.get(id);
    if (!customer) {
        return res.status(404).send('Customer not found');
    }
    res.set('Content-Type', 'text/plain');
    res.send(customer.getName());
};

// DELETE /customerservice/customers/:id
const deleteCustomer = (req, res) => {
    const id = req.params.id;
    console.log(`----invoking deleteCustomer, Customer id is: ${id}`);
    const idNumber = parseInt(id);
    const customer = customers.get(idNumber);

    if (customer) {
        customers.delete(idNumber);
        res.status(200).send();
    } else {
        res.status(304).send(); // Not Modified
    }
};

module.exports = {
    getCustomer,
    updateCustomer,
    addCustomer,
    getCustomerName,
    deleteCustomer
};
