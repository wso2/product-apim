const express = require('express');
const bodyParser = require('body-parser');
const customerRoutes = require('./routes/customerRoutes');
const headerRoutes = require('./routes/headerRoutes');
const orderRoutes = require('./routes/orderRoutes');

const app = express();
const port = 3007;

app.use(bodyParser.json());
app.use(bodyParser.text({ type: 'text/plain' }));

app.use('/services/customers/customerservice/customers', customerRoutes);
app.use('/services/customers/customerservice/orders', orderRoutes);
app.use('/services/customers/customerservice/headers', headerRoutes);

app.listen(port, () => {
    console.log(`JAX-RS Basic service running at http://nodebackend:${port}`);
});
