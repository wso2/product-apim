
var test0 = function() {
    var name = "getAllTags";
    var utils = require("/tests/utils.js");
    var tags = require("/core/tags/tags.js");
    var result = tags[name](5);
    if(result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};