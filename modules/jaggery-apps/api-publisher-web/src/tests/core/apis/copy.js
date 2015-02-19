var test0 = function() {
    var name = "createNewAPIVersion";
    var utils = require("/tests/utils.js");
    var copy = require("/core/apis/copy.js");


    var result = copy[name]("apiSample", "1.2.3","2.4.6");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};