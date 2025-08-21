const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3014;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-checkOne server running at http://nodebackend:${port}`);
});
