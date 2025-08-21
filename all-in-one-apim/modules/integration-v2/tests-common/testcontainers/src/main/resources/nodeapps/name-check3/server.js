const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3012;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-check3 server running at http://nodebackend:${port}`);
});
