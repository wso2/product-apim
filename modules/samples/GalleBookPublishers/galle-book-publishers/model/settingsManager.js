
var addSetting, getSetting, printSettings;

(function () {

  addSetting =  function(name, value){

    var settings = application.get("settings");

  	if(!settings){
  		settings = new Object();
  	}

  	settings[name] = value;
  	application.put("settings", settings);
  }

  getSetting = function(name){

    var settings = application.get("settings");

    if(settings){
      return settings[name];
    }

    return null;

  }

  printSettings = function(){
    var log = new Log();
    log.info(application.get("settings"));
  }

}());
