
function loadDefaultSettings(){

	var defaultSettings = require("model/defaultSettings.json");
	var settingsManager = require("model/settingsManager.js");

	for (var key in defaultSettings) {
		settingsManager.addSetting(key, defaultSettings[key]);
	}

}

loadDefaultSettings();
