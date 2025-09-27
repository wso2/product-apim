const keyModel = require('../models/keyModel');

const getKey = (req, res) => {
    if (keyModel.getKeyStatus()) {
        const output = {
            action: "get",
            node: {
                key: "/jti/2f3c1e3a-fe4c-4cd4-b049-156e3c63fc5d",
                value: "true",
                expiration: "2019-03-21T07:25:19.721867Z",
                ttl: 3594,
                modifiedIndex: 71,
                createdIndex: 71
            }
        };
        res.status(200).json(output);
    } else {
        res.sendStatus(404);
    }
};

const postKey = (req, res) => {
    keyModel.setKeyStatus(true);
    res.sendStatus(200);
};

const putKey = (req, res) => {
    keyModel.setKeyStatus(true);
    res.sendStatus(201);
};

module.exports = {
    getKey,
    postKey,
    putKey
};
