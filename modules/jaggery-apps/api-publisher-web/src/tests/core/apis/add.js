var test0 = function() {
    var name = "saveAPI";
    var utils = require("/tests/utils.js");
    var add = require("/core/apis/add.js");

    var apiData = {};
    apiData.apiName = "apiSample";
    apiData.version = "1.2.3";
    apiData.description = "hello";
    apiData.endpoint = "http://ab";
    apiData.wsdl = "http://ab";
    apiData.imageUrl = "";
    apiData.tags = "jaggery,mashup";
    apiData.tier = "silver";
    apiData.context = "/*";

    var uriTemplateArr = [];
    var uriMethodArr = [];

    uriTemplateArr.push("/*");
    uriMethodArr.push("GET");

    apiData.uriTemplateArr = uriTemplateArr;
    apiData.uriMethodArr = uriMethodArr;

    var result = add[name](apiData);
    if (result.error) {
        utils.failure(name, result);
        return;
    }
    utils.success(name, result);
};