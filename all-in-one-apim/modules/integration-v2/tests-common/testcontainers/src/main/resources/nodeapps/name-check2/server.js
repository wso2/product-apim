const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3010;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-check2 server running at http://nodebackend:${port}`);
});
