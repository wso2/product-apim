const Request = require('../models/Request');
const BusinessKey = require('../models/BusinessKey');
const Data = require('../models/Data');
const ProcessInfo = require('../models/ProcessInfo');

let storedRequest = null;
let storedBusinessKey = null;

const startInstance = (req, res) => {
    const request = new Request(
        req.body.tenantId,
        req.body.processDefinitionKey,
        req.body.businessKey,
        req.body.variables
    );

    storedRequest = request;
    storedBusinessKey = request.businessKey;

    res.status(201).location('/sample').json(request);
};

const getRequestInfo = (req, res) => {
    const debugInfo = req.query.debugInfo;
    const businessKey = req.query.businessKey;

    if (debugInfo === 'startRequest') {
        return res.json(storedRequest);
    } else if (debugInfo === 'processInfoRequest') {
        return res.json(new BusinessKey(businessKey));
    } else if (businessKey) {
        const dataEntry = new Data("12345");
        const processInfo = new ProcessInfo([dataEntry], 1, 1);
        return res.json(processInfo);
    }
    res.status(400).send("Missing required query parameters");
};

const deleteTaskInstance = (req, res) => {
    res.status(204).send();
};

module.exports = {
    startInstance,
    getRequestInfo,
    deleteTaskInstance
};
