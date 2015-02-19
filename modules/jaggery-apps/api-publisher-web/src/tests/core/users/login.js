var test0 = function() {
    var name = "login";
    var utils = require("/tests/utils.js");
    var login = require("/core/users/login.js");


    var result = login[name]("admin", "admin");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};