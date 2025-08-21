const express = require('express');
const nameRoutes = require('./routes/nameRoutes');

const app = express();
const port = 3016;

app.use('/', nameRoutes);

app.listen(port, () => {
    console.log(`name-checkThree server running at http://nodebackend:${port}`);
});
