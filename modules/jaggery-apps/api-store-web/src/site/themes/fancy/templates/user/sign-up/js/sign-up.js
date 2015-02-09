$(document).ready(function() {
    $.validator.addMethod("matchPasswords", function(value) {
		return value == $("#newPassword").val();
	}, "The passwords you entered do not match.");

    $.validator.addMethod('noSpace', function(value, element) {
            return !/\s/g.test(value);
    }, 'The Name contains white spaces.');


    $("#sign-up").validate({
     submitHandler: function(form) {
    	var fieldCount = document.getElementById('fieldCount').value;
	var allFieldsValues;
 	for(var i = 0; i < fieldCount; i++) {
		var value = document.getElementById( i + '.0cliamUri').value;
		if ( i == 0) {
			allFieldsValues = value;
		} else {
			allFieldsValues = allFieldsValues + "|" + value;
		}
	}
        var tenantDomain = document.getElementById('hiddenTenantDomain').value;
        var fullUserName;
        if(tenantDomain == "null" || tenantDomain == "carbon.super") {
            fullUserName = document.getElementById('newUsername').value;
        } else {
            fullUserName = document.getElementById('newUsername').value + "@" 
                    + tenantDomain;
        }

    	jagg.post("/site/blocks/user/sign-up/ajax/user-add.jag", {
            action:"addUser",
            username:fullUserName,
            password:$('#newPassword').val(),
            allFieldsValues:allFieldsValues
        }, function (result) {
            if (result.error == false) {
                if(result.showWorkflowTip){
                    jagg.message({content:"User account awaiting Administrator approval.",type:"info",
                        cbk:function() {
                            $('#signUpRedirectForm').submit();
                        }
                    });
                }else {
                    jagg.message({content:"User added successfully. You can now sign into the API store using the new user account.",type:"info",
                        cbk:function() {
                            $('#signUpRedirectForm').submit();
                        }
                    });
                }
            } else {
                jagg.message({content:result.message,type:"error"});
            }
        }, "json");
     }
    });
    $("#newPassword").keyup(function() {
        $(this).valid();
    });
    $('#newPassword').focus(function(){
        $('#password-help').show();
        $('.password-meter').show();
    });
    $('#newPassword').blur(function(){
        $('#password-help').hide();
        $('.password-meter').hide();
    });
});

var showMoreFields = function () {
	$('#moreFields').show();
	$('#moreFieldsLink').hide();
	$('#hideFieldsLink').show();
}
var hideMoreFields = function () {
	$('#moreFields').hide();
	$('#hideFieldsLink').hide();
	$('#moreFieldsLink').show();
}