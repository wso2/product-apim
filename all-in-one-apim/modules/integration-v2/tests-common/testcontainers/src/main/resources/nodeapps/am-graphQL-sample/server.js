const express = require('express');
const languageRoutes = require('./routes/languageRoutes');

const app = express();
const port = 3003;

app.use(express.json());

app.use('/graphql', languageRoutes);

app.listen(port, () => {
  console.log(`AM GraphQL Server running at http://nodebackend:${port}`);
});

