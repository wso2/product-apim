const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3011;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-check2_SB server running at http://nodebackend:${port}`);
});
