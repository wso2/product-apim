const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3015;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-checkTwo server running at http://nodebackend:${port}`);
});
