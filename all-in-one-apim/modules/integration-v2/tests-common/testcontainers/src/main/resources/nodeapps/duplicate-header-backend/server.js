const express = require('express');
const duplicateHeaderRoutes = require('./routes/duplicateHeaderRoutes');

const app = express();
const port = 3005;

app.use('/duplicate', duplicateHeaderRoutes);

app.listen(port, () => {
    console.log(`Duplicate Header Backend Server running at http://nodebackend:${port}`);
});
