var inSequencesLoaded = false;
var outSequencesLoaded = false;
var faultSequencesLoaded = false;

var TIERS = [];

function loadTiers(row) {

    jagg.post("/site/blocks/item-add/ajax/add.jag", { action:"getTiers" },
              function (result) {
                  if (!result.error) {
                      var arr = [];
                      $('.getThrottlingTier',row).html('');
                      $('.postThrottlingTier',row).html('');
                      $('.putThrottlingTier',row).html('');
                      $('.deleteThrottlingTier',row).html('');
                      $('.optionsThrottlingTier',row).html('');

                      TIERS = result.tiers;
                      $("#resource_view").trigger("draw");

                      for (var i = 0; i < result.tiers.length; i++) {
                          var k = result.tiers.length - i -1;

                          $('.getThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                          $('.putThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                          $('.postThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                          $('.deleteThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                          $('.optionsThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                      }

                  }
              }, "json");
}



$(document).ready(function() {
    //js code to hide form options 
    //@todo: can change to a jquery plugin and move to base js

    $('.js_hidden_section_title').click(function(){
        var $next = $(this).next();
        var $i = $('i',this);
        if($next.is(":visible")){
            $next.hide();
            $i.removeClass('icon-chevron-down');
            $i.addClass('icon-chevron-right');
        }else{
            $next.show();
            $i.removeClass('icon-chevron-right');
            $i.addClass('icon-chevron-down');
        }
    });

    var target = document.getElementById("tier");

    jagg.post("/site/blocks/item-add/ajax/add.jag", { action:"getTiers" },
              function (result) {
                  if (!result.error) {
                      var arr = [];

                      for (var i = 0; i < result.tiers.length; i++) {
                          arr.push(result.tiers[i].tierName);

                      }
                      for (var j = 0; j < arr.length; j++) {
                          option = new Option(arr[j], arr[j]);
                          target.options[j] = option;
                          target.options[j].text = result.tiers[j].tierDisplayName;
                          target.options[j].title = result.tiers[j].tierDescription;

                          if (j == 0) {
                              target.options[j].selected = 'selected';
                              $("#tiersHelp").html(result.tiers[0].tierDescription);
                              var tierArr = [];
                              tierArr.push(target.options[j].value);
                              $('<input>').attr('type', 'hidden')
                                      .attr('name', 'tiersCollection')
                                      .attr('id', 'tiersCollection')
                                      .attr('value', tierArr)
                                      .appendTo('#addAPIForm');
                          }
                      }
                  }
              }, "json");

    $('.default_version_check').change(function(){
        if($(this).is(":checked")){
            $(default_version_checked).val($(this).val());
        }else{
            $(default_version_checked).val("");
        }
    });


    $('.transports_check_http').change(function(){
        if($(this).is(":checked")){
            $(http_checked).val($(this).val());
        }else{
            $(http_checked).val("");
        }
    });

    $('.transports_check_https').change(function(){
        if($(this).is(":checked")){
            $(https_checked).val($(this).val());
        }else{
            $(https_checked).val("");
        }
    });

    $('.storeCheck').change(function () {
        var checkedStores = $('#externalAPIStores').val();
        if (checkedStores == "REMOVEALL") {
            checkedStores = "";
        }
        if ($(this).is(":checked")) {
            $('#externalAPIStores').val(checkedStores + "::" + $(this).val());
        } else {
            var storeValsWithoutUnchecked = "";
            var checkStoresArray = checkedStores.split("::");
            for (var k = 0; k < checkStoresArray.length; k++) {
                if (!(checkStoresArray[k] == $(this).val())) {
                    storeValsWithoutUnchecked += checkStoresArray[k] + "::";
                }
            }
            if (storeValsWithoutUnchecked == "") {
                storeValsWithoutUnchecked = "REMOVEALL";
            }
            $('#externalAPIStores').val(storeValsWithoutUnchecked);
        }
    });


    $("select[name='tier']").change(function() {
        // multipleValues will be an array
        var multipleValues = $(this).val() || [];
        var countLength = $('#tiersCollection').length;
        if (countLength == 0) {

            $('<input>').attr('type', 'hidden')
                    .attr('name', 'tiersCollection')
                    .attr('id', 'tiersCollection')
                    .attr('value', multipleValues)
                    .appendTo('#addAPIForm');
        } else {
            $('#tiersCollection').attr('value', multipleValues);

        }

    });

    $("#clearThumb").on("click", function () {
        $('#apiThumb-container').html('<input type="file" class="input-xlarge" name="apiThumb" />');
    });

    var v = $("#addAPIForm").validate({
                                          submitHandler: function(form) {
                                              // Adding custom validation for
												// the resource url UI
                                              /*if(validateResourceTable() != ""){
                                                  return;
                                              }*/


                                              $('#saveMessage').show();
                                              $('#saveButtons').hide();

                                              $(form).ajaxSubmit({
                                                                     success:function(responseText, statusText, xhr, $form) {
                                                                         if (!responseText.error) {
                                                                             var current = window.location.pathname;
                                                                             if (current.indexOf(".jag") >= 0) {
                                                                                 location.href = "index.jag";
                                                                             } else {
                                                                                 location.href = 'site/pages/index.jag';
                                                                             }
                                                                         } else {
                                                                             if (responseText.message == "timeout") {
                                                                                 if (ssoEnabled) {
                                                                                     var currentLoc = window.location.pathname;
                                                                                     if (currentLoc.indexOf(".jag") >= 0) {
                                                                                         location.href = "add.jag";
                                                                                     } else {
                                                                                         location.href = 'site/pages/add.jag';
                                                                                     }
                                                                                 } else {
                                                                                     jagg.showLogin();
                                                                                 }
                                                                             } else {
                                                                                 jagg.message({content:responseText.message,type:"error"});
                                                                             }
                                                                             $('#saveMessage').hide();
                                                                             $('#saveButtons').show();
                                                                         }
                                                                     }, dataType: 'json'
                                                                 });
                                          }
                                      });


});

function getContextValue() {
    var context = $('#context').val();
    var version = $('#version').val();

    if (context == "" && version != "") {
        $('#contextForUrl').html("/{context}/" + version);
        $('#contextForUrlDefault').html("/{context}/" + version);
    }
    if (context != "" && version == "") {
        if (context.charAt(0) != "/") {
            context = "/" + context;
        }
        $('#contextForUrl').html(context + "/{version}");
        $('#contextForUrlDefault').html(context + "/{version}");
    }
    if (context != "" && version != "") {
        if (context.charAt(0) != "/") {
            context = "/" + context;
        }
        $('.contextForUrl').html(context + "/" + version);
    }
}

function showHideRoles(){
    var visibility = $('#visibility').find(":selected").val();

    if (visibility == "public" || visibility == "private" || visibility == "controlled"){
        $('#rolesDiv').hide();
    } else{
        $('#rolesDiv').show();
    }
}

function showHideTennats(){
    var subscription = $('#subscriptions').find(":selected").val();

    if (subscription == "current_tenant" || subscription == "all_tenants"){
        $('#tennatsDiv').hide();
    } else{
        $('#tennatsDiv').show();
    }
}

function showUTProductionURL(){
    var endpointType = $('#endpointType').find(":selected").val();
    if(endpointType == "secured"){
        $('#credentials').show();
    }
    else{
        $('#credentials').hide();
    }

}

function showCacheTimeout(){
    var cache = $('#responseCache').find(":selected").val();
    if(cache == "enabled"){
        $('#cacheTimeout').show();
    }
    else{
        $('#cacheTimeout').hide();
    }

}

function loadInSequences() {

    if(inSequencesLoaded){
        return;
    }

	jagg.post("/site/blocks/item-add/ajax/add.jag", {
		action : "getCustomInSequences"
	},
 	function(result) {
		if (!result.error) {
			var arr = [];
			if (result.sequences.length == 0) {
				var msg = "No defined sequences";
				$('<input>').
				attr('type', 'hidden').
				attr('name', 'inSeq').
				attr('id', 'inSeq').
				attr('value', msg).
				appendTo('#addAPIForm');
			} else {
				for ( var j = 0; j < result.sequences.length; j++) {
					arr.push(result.sequences[j]);
				}
				for ( var i = 0; i < arr.length; i++) {
				    $('#inSequence').append('<option value="'+result.sequences[i]+'">'+result.sequences[i]+'</option>');
					$('<input>').
					attr('type', 'hidden').
					attr('name', 'inSeq').
					attr('id', 'inSeq').
					attr('value', result.sequences[i]).
					appendTo('#addAPIForm');

				}
			}
			inSequencesLoaded = true;
		}
	}, "json");
}

function loadOutSequences() {

	if(outSequencesLoaded){
        return;
	}

	jagg.post("/site/blocks/item-add/ajax/add.jag", {
		action : "getCustomOutSequences"
	},
			function(result) {
				if (!result.error) {
					var arr = [];
					if (result.sequences.length == 0) {
						var msg = "No defined sequences";
						$('<input>').
						attr('type', 'hidden').
						attr('name', 'outSeq').
						attr('id', 'outSeq').
						attr('value', msg).
						appendTo('#addAPIForm');
					}else {
						for ( var j = 0; j < result.sequences.length; j++) {
							arr.push(result.sequences[j]);
						}
						for(var i=0; i<arr.length; i++){						
							$('#outSequence').append('<option value="'+result.sequences[i]+'">'+result.sequences[i]+'</option>');
							$('<input>').
							attr('type', 'hidden').
							attr('name', 'outSeq').
							attr('id', 'outSeq').
							attr('value', result.sequences[i]).
							appendTo('#addAPIForm');

						}
					}
					outSequencesLoaded = true;
				}
			}, "json");
}

function loadFaultSequences() {

	if(faultSequencesLoaded){
        return;
	}

	jagg.post("/site/blocks/item-add/ajax/add.jag", {
		action : "getCustomFaultSequences"
	},
			function(result) {
				if (!result.error) {
					var arr = [];
					if (result.sequences.length == 0) {
						var msg = "No defined sequences";
						$('<input>').
						attr('type', 'hidden').
						attr('name', 'faultSeq').
						attr('id', 'faultSeq').
						attr('value', msg).
						appendTo('#addAPIForm');
					}else {
						for ( var j = 0; j < result.sequences.length; j++) {
							arr.push(result.sequences[j]);
						}
						for(var i=0; i<arr.length; i++){
							$('#faultSequence').append('<option value="'+result.sequences[i]+'">'+result.sequences[i]+'</option>');
							$('<input>').
							attr('type', 'hidden').
							attr('name', 'faultSeq').
							attr('id', 'faultSeq').
							attr('value', result.sequences[i]).
							appendTo('#addAPIForm');

						}
					}
					faultSequencesLoaded = true;
				}
			}, "json");
}

function toggleSequence(checkbox){
	if($(checkbox).is(":checked")){
		$(checkbox).parent().next().show();
		loadInSequences();
		loadOutSequences();
		loadFaultSequences();
	}else{
		$(checkbox).parent().next().hide();
	}

}
