/*
 * jQuery validate.password plug-in 1.0
 *
 * http://bassistance.de/jquery-plugins/jquery-plugin-validate.password/
 *
 * Copyright (c) 2009 Jörn Zaefferer
 *
 * $Id$
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 */
(function($) {
	

	function rating(rate, message) {
		return {
			rate: rate,
			messageKey: message
		};
	}
	
	function uncapitalize(str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
	
	$.validator.passwordRating = function(password, username) {
            var minLength = 6;
            var passwordStrength   = 0;

			if ((password.length >0) && (password.length <=5)) passwordStrength=1;

			if (password.length >= minLength) passwordStrength++;

			if ((password.match(/[a-z]/)) && (password.match(/[A-Z]/)) ) passwordStrength++;

			if (password.match(/\d+/)) passwordStrength++;

			if (password.match(/.[!,@,#,$,%,^,&,*,?,_,~,-,(,)]/))	passwordStrength++;

			if (password.length > 12) passwordStrength++;

            if (username && password.toLowerCase()== username.toLowerCase()){
			    passwordStrength = 0;
            }

			$('#pwdMeter').removeClass();
			$('#pwdMeter').addClass('neutral');

			switch(passwordStrength){
            case 1:
                return rating(1, "very-weak");
			  break;
			case 2:
                return rating(2, "weak");
			  break;
			case 3:
                return rating(3, "medium");
			  break;
			case 4:
                return rating(4, "strong");
			  break;
			case 5:
                 return rating(5, "vstrong");
			  break;
			default:
                return rating(1, "very-weak");
			}
	}

	$.validator.passwordRating.messages = {
		"similar-to-username": "Too similar to username",
		"very-weak": "Very weak",
		"weak": "Weak",
		"medium": "Medium",
		"strong": "Strong",
		"vstrong": "Very Strong"
	}
	
	$.validator.addMethod("password", function(value, element, usernameField) {
		// use untrimmed value
		var password = element.value,
		// get username for comparison, if specified
			username = $(typeof usernameField != "boolean" ? usernameField : []);
			
		var rating = $.validator.passwordRating(password, username.val());
		// update message for this field
		
		var meter = $(".password-meter", element.form);
		
		meter.find(".password-meter-bar").removeClass().addClass("password-meter-bar").addClass("password-meter-" + rating.messageKey);
		meter.find(".password-meter-message")
		.removeClass()
		.addClass("password-meter-message")
		.addClass("password-meter-message-" + rating.messageKey)
		.text($.validator.passwordRating.messages[rating.messageKey]);
		// display process bar instead of error message
		return rating.rate > 2;
	}, "Minimum system requirements not met");
	// manually add class rule, to make username param optional
	$.validator.classRuleSettings.password = { password: true };
	
})(jQuery);
