const express = require('express');
const bodyParser = require('body-parser');
const processInstanceRoutes = require('./routes/processInstanceRoutes');

const app = express();
const port = 3004;

app.use(bodyParser.json());

app.use('/process-instances', processInstanceRoutes);

app.listen(port, () => {
    console.log(`BPMN Process Server running at http://nodebackend:${port}`);
});
