
var test0 = function() {
    var name = "getRecentlyAddedAPIs";
    var utils = require("/tests/utils.js");
    var recent = require("/core/recent/recent.js");
    var result = recent[name](5);
    if(result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};