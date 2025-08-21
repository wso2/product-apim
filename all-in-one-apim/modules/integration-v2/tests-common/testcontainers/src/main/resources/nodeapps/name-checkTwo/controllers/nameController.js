const NameModel = require('../models/nameModel');

const getName = (req, res) => {
    const nameModel = new NameModel();
    const message = nameModel.getMessage();
    res.set('Content-Type', 'text/plain');
    res.send(message);
};

module.exports = {
    getName
};
