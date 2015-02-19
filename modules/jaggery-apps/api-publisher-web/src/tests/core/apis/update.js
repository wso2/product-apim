var test0 = function() {
    var name = "updateAPI";
    var utils = require("/tests/utils.js");
    var update = require("/core/apis/update.js");
    var apiData={};

    apiData.apiName = "apiSample";
    apiData.version = "1.2.3";
    apiData.description = "hello";
    apiData.endpoint = "http://sample";
    apiData.wsdl = "http://sample?wsdl";
    apiData.imageUrl = "";
    apiData.tags = "jaggery,mashup";
    apiData.tier = "silver";
    apiData.status = "PUBLISHED";
    apiData.context = "/*";

    var uriTemplateArr = [];
    var uriMethodArr = [];

    uriTemplateArr.push("/*");
    uriMethodArr.push("GET");

    apiData.uriTemplateArr = uriTemplateArr;
    apiData.uriMethodArr = uriMethodArr;

    var result = update[name](apiData);
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};