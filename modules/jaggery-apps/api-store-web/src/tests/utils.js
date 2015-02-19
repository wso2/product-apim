var failure = function(method, result) {
    var log = new Log();
    log.error("FAILURE : " + method);
    log.info("=========================================================================================== : " + method);
};

var success = function(method, result) {
    var log = new Log();
    log.info("SUCCESS : " + method);
};