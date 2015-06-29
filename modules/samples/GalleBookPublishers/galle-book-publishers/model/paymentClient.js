
var pay;

(function () {

  pay = function(payment, paymentApiUrl, accessToken, userIp){

    // Call payment API
    tokenHeaderValue = "Bearer " + accessToken;

    var xhr = new XMLHttpRequest();
    xhr.open("POST", paymentApiUrl, false);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.setRequestHeader("Authorization", tokenHeaderValue);
    xhr.setRequestHeader("Fraud-Detection-Mock-Client-IP", userIp);
    xhr.send(stringify(payment));

    var response = xhr.responseText;

    return parse(response);
  }

}());
