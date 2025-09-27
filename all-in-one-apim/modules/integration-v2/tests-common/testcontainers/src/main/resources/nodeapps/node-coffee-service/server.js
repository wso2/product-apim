const express = require('express');
const orderRoutes = require('./routes/orderRoutes');
const paymentRoutes = require('./routes/paymentRoutes');

const app = express();
const port = 3000;

app.use('/api/orders', orderRoutes);
app.use('/api/payment', paymentRoutes);

app.listen(port, () => {
    console.log(`node-coffee-service server running at http://nodebackend:${port}`);
});
