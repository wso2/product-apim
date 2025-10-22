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
