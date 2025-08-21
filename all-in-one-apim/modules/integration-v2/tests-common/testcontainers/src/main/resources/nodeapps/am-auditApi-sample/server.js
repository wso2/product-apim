const express = require('express');
const auditRoutes = require('./routes/auditRoutes');

const app = express();
const port = 3002;

app.use(express.json());

app.use('/auditapi', auditRoutes);

app.listen(port, () => {
  console.log(`AM Audit API running at http://nodebackend:${port}`);
});

