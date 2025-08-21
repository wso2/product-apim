const express = require('express');
const wildcardRoutes = require('./routes/wildcardRoutes');

const app = express();
const port = 3017;

app.use('/', wildcardRoutes);

app.listen(port, () => {
    console.log(`wildcard server running at http://nodebackend:${port}`);
});
