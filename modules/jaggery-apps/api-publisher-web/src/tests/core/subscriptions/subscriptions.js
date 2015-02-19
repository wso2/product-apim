var test0 = function() {

    var utils = require("/tests/utils.js");
    var subs = require("/core/subscriptions/subscriptions.js");

    var name = "getSubscribersOfAPI";
    var result = subs[name]("apiSample", "1.2.3");
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);


    var name1 = "getSubscribedAPIs";
    var result1 = subs[name1]("admin");
    if (result1.error) {
        utils.failure(name1, result1);
        return;
    }
    utils.success(name1, result1);


    var name2 = "getSubscribersOfProvider";
    var result2 = subs[name2]();
    if (result2.error) {
        utils.failure(name2, result2);
        return;
    }
    utils.success(name2, result2);
};