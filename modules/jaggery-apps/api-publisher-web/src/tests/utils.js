var failure = function(method, result) {
    var log = new Log();
    log.error("FAILURE : " + method);
    log.info("=========================================================================================== : " + method+"   ERROR :"+result.error);
};

var success = function(method, result) {
    var log = new Log();
    log.info("SUCCESS : " + method);
};