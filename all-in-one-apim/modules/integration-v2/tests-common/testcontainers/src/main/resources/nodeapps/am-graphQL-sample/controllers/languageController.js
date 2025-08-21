const fs = require('fs');
const path = require('path');
const Language = require('../models/language');

getLanguages = (req, res) => {
    const filePath = path.join(__dirname, '../data/languages.json');

    fs.readFile(filePath, 'utf8', (err, data) => {
        if (err || !data) {
            return res.status(500).json({ error: 'Failed to read languages data' });
        }

        try {
            const json = JSON.parse(data);
            const languages = json.languages.map(lang => new Language(lang.name, lang.code));
            return res.json(languages);
        } catch (parseError) {
            return res.status(500).json({ error: 'Failed to parse languages data' });
        }
    });
};

module.exports = {
  getLanguages
};
