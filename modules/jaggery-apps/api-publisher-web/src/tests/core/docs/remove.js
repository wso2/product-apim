var test0 = function() {
    var name = "removeDocumentation";
    var utils = require("/tests/utils.js");
    var remove = require("/core/docs/remove.js");


    var result = remove[name]("apiSample", "1.2.3", "doc1", "samples");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};