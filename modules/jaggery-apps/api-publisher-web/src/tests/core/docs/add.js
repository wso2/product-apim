var test0 = function() {
    var name = "addDocumentation";
    var utils = require("/tests/utils.js");
    var add = require("/core/docs/add.js");


    var result = add[name]("apiSample", "1.2.3", "doc1", "samples", "This is a sample", "url", "www.google.com");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};