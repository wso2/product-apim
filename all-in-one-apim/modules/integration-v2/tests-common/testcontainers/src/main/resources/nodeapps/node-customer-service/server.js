const express = require('express');
const bodyParser = require('body-parser');
const customerRoutes = require('./routes/customerRoutes');
const orderRoutes = require('./routes/orderRoutes');

const app = express();
const port = 8080;

app.use(bodyParser.text({ type: 'text/plain' }));
app.use(bodyParser.text({ type: 'text/xml' }));
app.use(bodyParser.json());

app.use('/jaxrs_basic/services/customers/customerservice', customerRoutes);
app.use('/jaxrs_basic/services/customers/customerservice', orderRoutes);

app.listen(port, () => {
  console.log(`Customer Service API running at http://nodebackend:${port}`);
});
