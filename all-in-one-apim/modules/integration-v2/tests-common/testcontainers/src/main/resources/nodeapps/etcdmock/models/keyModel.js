let keyPosted = false;

module.exports = {
    getKeyStatus: () => keyPosted,
    setKeyStatus: (status) => { keyPosted = status; }
};
