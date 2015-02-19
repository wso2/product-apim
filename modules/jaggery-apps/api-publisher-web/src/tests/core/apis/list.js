var test0 = function() {
    var name = "getAPI";
    var utils = require("/tests/utils.js");
    var get = require("/core/apis/list.js");

    var name1 = "getAPIsByProvider";



    var result = get[name]("apiSample", "1.2.3");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);


    var resultAll = get[name1]();
    if (resultAll.error) {
        utils.failure(name1, resultAll);
        return;
    }
    utils.success(name1, resultAll);

};