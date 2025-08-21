const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3008;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-check1 server running at http://nodebackend:${port}`);
});
