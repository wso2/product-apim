var test0 = function() {
    var name = "getAllDocumentation";
    var utils = require("/tests/utils.js");
    var add = require("/core/docs/list.js");


    var result = add[name]("apiSample", "1.2.3");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};