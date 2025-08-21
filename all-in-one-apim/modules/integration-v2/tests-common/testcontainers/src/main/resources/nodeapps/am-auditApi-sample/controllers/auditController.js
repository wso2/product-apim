const fs = require('fs');
const path = require('path');

getResults = (req, res) => {
    const filePath = path.join(__dirname, '../data/test-audit-report.json');
    fs.readFile(filePath, 'utf-8', (err, data) => {
        if (err || !data) {
            return res.status(404).json({});
        }
        res.json(JSON.parse(data));
    });
};

postResults = (req, res) => {
    const filePath = path.join(__dirname, '../data/test-new-audit-api.json');
    fs.readFile(filePath, 'utf-8', (err, data) => {
        if (err || !data) {
            return res.status(404).json({});
        }
        res.json(JSON.parse(data));
    });
};

putResults = (req, res) => {
    const filePath = path.join(__dirname, '../data/test-update-audit-api.json');
    fs.readFile(filePath, 'utf-8', (err, data) => {
        if (err || !data) {
            return res.status(404).json({});
        }
        res.json(JSON.parse(data));
    });
};

module.exports = {
  getResults,
  postResults,
  putResults
};
