
var test0 = function() {
    var name = "login";
    var utils = require("/tests/utils.js");
    var user = require("/core/user/user.js");
    var result = user[name]("admin", "admin");
    if(result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};