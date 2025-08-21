// controllers/people.controller.js
const Person = require('../models/Person');

// --- In-Memory Data Store & Initialization ---
const persons = new Map();

function init() {
    if (persons.size > 0) return;
    for (let count = 1; count <= 10; count++) {
        const email = `test-${count}@wso2.com`;
        const person = new Person(email, `testUser${count}`, `testLastName${count}`);
        persons.set(email, person);
    }
    console.log("Initial people data loaded.");
}
init();

// --- Controller Functions ---

const getPeople = (req, res) => {
    try {
        const page = parseInt(req.query.page || '1', 10);
        const pageSize = 5;
        const allPeople = Array.from(persons.values());
        const start = (page - 1) * pageSize;
        const end = start + pageSize;
        const paginatedPeople = allPeople.slice(start, end);
        res.status(200).json(paginatedPeople);
    } catch (error) {
        res.status(500).json({ message: "An internal server error occurred." });
    }
};

const addPerson = (req, res) => {
    const { email, firstName, lastName } = req.query;

    if (!email) {
        return res.status(400).json({ message: "Email query parameter is required." });
    }
    if (persons.has(email)) {
        return res.status(409).json({ message: `Person already exists: ${email}` });
    }

    const newPerson = new Person(email, firstName, lastName);
    persons.set(email, newPerson);
    const createdUrl = `${req.protocol}://${req.get('host')}${req.baseUrl}/${email}`;
    res.status(201).location(createdUrl).json(newPerson);
};

const getPersonByEmail = (req, res) => {
    const { email } = req.params;
    if (persons.has(email)) {
        res.status(200).json(persons.get(email));
    } else {
        res.status(404).json({ message: `Person not found: ${email}` });
    }
};

const updatePerson = (req, res) => {
    const { email } = req.params;
    const { firstName, lastName } = req.query;

    if (!persons.has(email)) {
        return res.status(404).json({ message: `Person not found: ${email}` });
    }

    const person = persons.get(email);
    if (firstName) person.firstName = firstName;
    if (lastName) person.lastName = lastName;

    persons.set(email, person);
    res.status(200).json(person);
};

const deletePerson = (req, res) => {
    const { email } = req.params;
    if (persons.has(email)) {
        persons.delete(email);
        res.status(200).send("OK");
    } else {
        res.status(404).json({ message: `Person not found: ${email}` });
    }
};

const checkPerson = (req, res) => {
    if (persons.has(req.params.email)) {
        res.status(200).end();
    } else {
        res.status(404).end();
    }
};

const getOptions = (req, res) => {
    res.set({
        'Access-Control-Allow-Methods': 'GET, POST, DELETE, PUT, OPTIONS, HEAD',
        'Access-Control-Allow-Headers': 'Content-Type'
    }).status(200).end();
};

module.exports = {
    getPeople,
    addPerson,
    getPersonByEmail,
    updatePerson,
    deletePerson,
    checkPerson,
    getOptions
};