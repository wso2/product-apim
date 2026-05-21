// **GENERATED CODE** //

var responseCode = mc.getProperty('query.param.responseCode');
var responseCodeSC;
var responses = [];

if (!responses[200]) {
responses [200] = [];
}
responses[200]["application/json"] = {
  "id" : 10,
  "name" : "doggie",
  "category" : {
    "id" : 1,
    "name" : "Dogs"
  },
  "photoUrls" : [ "string" ],
  "tags" : [ {
    "id" : 0,
    "name" : "string"
  } ],
  "status" : "available"
};

// **MANUALLY ADDED CODE** //

if (mc.getProperty('uri.var.petId') == 1) {
  responses[200]["application/json"] = {
    "id" : 1,
    "category" : {
      "id" : 1,
      "name" : "Dog"
    },
    "name" : "doggie",
    "photoUrls" : [ "mock-pet-image-url" ],
    "tags" : [ {
      "id" : 1,
      "name" : "German Sheperd"
    } ],
    "status" : "available"
  }
}

if (!responses[400]) {
  responses[400] = [];
}
responses[400]["application/json"] = "";

if (!responses[404]) {
  responses[404] = [];
}
responses[404]["application/json"] = "";

responses[501] = [];
responses[501]["application/json"] = {
"code" : 501,
"description" : "Not Implemented"}

if (responseCode == null) {
responseCode = 200;
}

if (!responses[responseCode]) {
  if (responses["default"]) {
    responseCode = "default"
  } else {
    responseCode = 501;
  }
}
if (responseCode === "default") {
  responseCodeSC = mc.getProperty('query.param.responseCode');
} else {
  responseCodeSC = responseCode;
}

mc.setProperty('CONTENT_TYPE', 'application/json');
mc.setProperty('HTTP_SC', responseCodeSC + "");
mc.setPayloadJSON(responses[responseCode]["application/json"]);
