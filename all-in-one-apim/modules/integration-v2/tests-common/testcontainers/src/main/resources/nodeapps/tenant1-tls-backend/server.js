const express = require('express');
const fs = require('fs');
const https = require('https');
const path = require('path');

const app = express();
const router = express.Router();
const port = process.env.PORT || 3026;
const customers = { 123: { id: 123, name: 'John' } };

router.get('/customers/:id', (req, res) => {
  const customer = customers[parseInt(req.params.id, 10)];
  if (customer) return res.json(customer);
  return res.status(404).send('Customer not found');
});
app.use('/', router);
app.use('/jaxrs_basic/services/customers/customerservice', router);

const options = {
  key: fs.readFileSync(path.join(__dirname, 'certs', 'server.key')),
  cert: fs.readFileSync(path.join(__dirname, 'certs', 'server.crt'))
};
https.createServer(options, app).listen(port, () => {
  console.log(`Tenant TLS backend (self-signed, CN=tenantbackend) running at https://tenantbackend:${port}`);
});
