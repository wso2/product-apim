var test0 = function() {
    var name = "getUser";
    var utils = require("/tests/utils.js");
    var get = require("/core/users/users.js");


    var result = get[name]();
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};