
var test0 = function() {
    var name = "getTopRatedAPIs";
    var utils = require("/tests/utils.js");
    var ratings = require("/core/ratings/ratings.js");
    var result = ratings[name](5);
    if(result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};