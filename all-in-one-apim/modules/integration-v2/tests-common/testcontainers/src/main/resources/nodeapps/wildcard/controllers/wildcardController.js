const WildcardModel = require('../models/wildcardModel');

exports.handleRequest = (req, res) => {
    const body = req.body && Object.keys(req.body).length > 0
        ? req.body
        : null;

    if (body) {
        res.json(body);
    } else {
        res.send(WildcardModel.getDefaultMessage());
    }
};
