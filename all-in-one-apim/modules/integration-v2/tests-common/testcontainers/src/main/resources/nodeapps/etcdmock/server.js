const express = require('express');
const etcdRoutes = require('./routes/etcdRoutes');

const app = express();
const port = 3006;

app.use('/v2/keys/jti', etcdRoutes);

app.listen(port, () => {
    console.log(`ETCD Mock Server is  running at http://nodebackend:${port}`);
});