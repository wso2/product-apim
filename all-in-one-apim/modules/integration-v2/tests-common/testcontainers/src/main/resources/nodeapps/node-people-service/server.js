const express = require('express');
const peopleRoutes = require('./routes/peopleRoutes');

const app = express();
const PORT = 3018;

app.use('/api/people', peopleRoutes);

app.listen(PORT, () => {
    console.log(`People service running on http://localhost:${PORT}`);
});
