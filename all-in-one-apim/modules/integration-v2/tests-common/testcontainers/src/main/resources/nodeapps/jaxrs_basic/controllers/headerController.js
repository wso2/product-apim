// GET /sec
const getSec = (req, res) => {
    const authHeader = req.header('Authorization');
    console.log(`----invoking getSec: ${authHeader}`);
    res.type('text/plain').send(authHeader || '');
}

// GET /handler
const getHandler = (req, res) =>{
    const header = req.header('Iwasat');
    console.log(`----invoking handler handler handler`);
    res.type('text/plain').send(header || '');
}

module.exports = {
  getSec,
  getHandler
};