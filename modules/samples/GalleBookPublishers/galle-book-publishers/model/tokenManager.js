var getAndStoreUserToken, getAccessToken;

(function () {

  getAndStoreUserToken = function (tokenEndpoint, consumerKey, consumerSecret, username, password){

    var applicationToken = consumerKey + ":" + consumerSecret;
    var authHeader = "Basic " + require("../model/base64.js").encode(applicationToken);
    var payload = "grant_type=password&username="+username+"&password="+password;

    var result = post(tokenEndpoint, payload, {"Authorization" : authHeader}, 'json');

    new Log().info(result);

    session.put("oauthToken", result.data);

    return result.data;

  }

  getAccessToken = function (tokenEndpoint, consumerKey, consumerSecret){
    refresAndStoreUserToken(tokenEndpoint, consumerKey, consumerSecret);
    return session.get("oauthToken").access_token;
  }

  var refresAndStoreUserToken = function (tokenEndpoint, consumerKey, consumerSecret){

    var refreshToken = session.get("oauthToken").refresh_token;

    var applicationToken = consumerKey + ":" + consumerSecret;
    var authHeader = "Basic " + require("../model/base64.js").encode(applicationToken);

    var payload = "grant_type=refresh_token&refresh_token="+refreshToken;

    var result = post(tokenEndpoint, payload, {"Authorization" : authHeader}, 'json');

    session.put("oauthToken", result.data);
  }

}());
