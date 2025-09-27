const handleGetRequest = (req, res) => {
    res.status(200);
    res.setHeader('Content-Type', 'application/json');

    res.append('Set-Cookie', '12wesdsfdffdsfff');
    res.append('Set-Cookie', '3456wesfdsfdsfdf');

    res.write(JSON.stringify({ RestResponse: "true" }));
    res.end();
};

module.exports = { handleGetRequest };
