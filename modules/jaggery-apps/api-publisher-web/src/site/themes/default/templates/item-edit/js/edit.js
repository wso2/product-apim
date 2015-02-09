
var TIERS = [];

$(document).ready(function() {
    //js code to hide form options 
    //@todo: can change to a jquery plugin and move to base js
    $('.more-options').click(function(){
        var id = $(this).attr('ref');
        var div = $('#'+id);
        div.toggle('fast');
        console.log(div.css('display'));
        if(div.css('display') == 'none'){
        }
        else{
        }
        // return false to prevent page going up
        return false; 
    });


    $("select[name='editTier']").change(function() {
        // multipleValues will be an array
        var multipleValues = $(this).val() || [];
        var countLength = $('#tiersCollection').length;
        if (countLength == 0) {

            $('<input>').attr('type', 'hidden')
                    .attr('name', 'tiersCollection')
                    .attr('id', 'tiersCollection')
                    .attr('value', multipleValues)
                    .appendTo('#editAPIForm');
        } else {
            $('#tiersCollection').attr('value', multipleValues);

        }

    });

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

    $("#clearThumb").on("click", function () {
        $('#apiThumb-container').html('<input type="file" class="input-xlarge" name="apiThumb" />');
    });

    enableDisableButtons();
    $('#context').change(function() {
        getContextValue();
    });
    $('#uriTemplate').click(function() {
        $('#resourceTableError').hide('fast');
    });


            createHiddenForm();
    $('#resourceTable tr.resourceRow').each(function(){
        $('input',this).unbind('change');
        $('input:checkbox',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('input:text',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('select',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });
    });

    loadInSequences();
    loadOutSequences();
    loadFaultSequences();

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
});

function loadTiers() {
    var target = document.getElementById("editTier");
    jagg.post("/site/blocks/item-add/ajax/add.jag", { action:"getTiers" },
        function (result) {
            if (!result.error) {
                var arr = [];
                var row = '#resourceRow';
                for (var i = 0; i < result.tiers.length; i++) {
                    // arr.push(result.tiers[i].tierName);
                    var k = result.tiers.length - i -1;
                    $('.getThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                    $('.putThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                    $('.postThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                    $('.deleteThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                    $('.optionsThrottlingTier',row).append($('<option value="'+result.tiers[k].tierName+'" title="'+result.tiers[k].tierDescription+'">'+result.tiers[k].tierDisplayName+'</option>'));
                }

                TIERS = result.tiers;
                $("#resource_view").trigger("draw");

                for (var i = 0; i < result.tiers.length; i++) {
                    arr.push(result.tiers[i].tierName);
                }

                for (var j = 0; j < arr.length; j++) {
                    option = new Option(arr[j], arr[j]);
                    target.options[j] = option;
                    target.options[j].text = result.tiers[j].tierDisplayName;
                    target.options[j].title = result.tiers[j].tierDescription;
                }
                addSelectedTiers(target);

            }
            $('#saveMessage').hide(); $('#saveButtons').show();

        }, "json");
}


var addResourcesToApi = function () {
    $('#resourceRow').clone().addClass('resourceRow').insertAfter($('#resourceRow')).show();
    enableDisableButtons();
    $('#resourceTable tr.resourceRow').each(function(){
        $('input',this).unbind('change');
        $('input:checkbox',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('input:text',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });

        $('select',this).change(function(){
            createHiddenForm();
            validateResourceTable();
        });
    });
};
var enableDisableButtons = function(){
   $('#resourceTable tr').each(function(index){
        var allRows = $('#resourceTable tr');
        if(index > 0){
            if(index == 2){
                    $('.upButton',this).attr('disabled','disabled');
                    $('.downButton',this).removeAttr('disabled');
            }
            if(index > 2 && allRows.length-1 > index){
                    $('.downButton',this).removeAttr('disabled');
                    $('.upButton',this).removeAttr('disabled');
            }
            if(allRows.length-1 == index){
                $('.deleteButton',this).removeAttr('disabled','disabled');
                if(index != 2){
                    $('.upButton',this).removeAttr('disabled');
                }else {
                    $('.upButton',this).attr('disabled','disabled');
                    $('.deleteButton',this).attr('disabled','disabled');
                }
                $('.downButton',this).attr('disabled','disabled');
            }
        }
    });
};
var moveMe = function(moveButton){
    var action = "move-up";
    if($(moveButton).hasClass('downButton')){
        action = "move-down";
    }

    if(action == "move-up"){
        $(moveButton).parent().parent().insertBefore($(moveButton).parent().parent().prev());
    }
    if(action == "move-down"){
        $(moveButton).parent().parent().insertAfter($(moveButton).parent().parent().next());
    }

    enableDisableButtons();
    createHiddenForm();
    validateResourceTable();
};
var createHiddenForm = function(){
    $('#hiddenFormElements input').remove();

    $('#resourceTable tr').each(function(index){
        var resourcesCount = index - 2;
        var resourceMethodValues = "";
        var resourceMethodAuthValues = "";
        var resourceThrottlingTierValues = "";
        var tr = this;
        //read the checkbox values

        if($('.resource-get',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "GET"}else{resourceMethodValues += ",GET"}
            var selectedValue = $('.getAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
            <!--Throttling-fix-->
            var selectedValueThrottling = $('.getThrottlingTier',tr).val();
            if(resourceThrottlingTierValues == ""){resourceThrottlingTierValues += selectedValueThrottling }else{resourceThrottlingTierValues += ","+selectedValueThrottling}
            <!--Throttling-fix-->
        }
        if($('.resource-put',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "PUT"}else{resourceMethodValues += ",PUT"}
            var selectedValue = $('.putAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
            <!--Throttling-fix-->
            var selectedValueThrottling = $('.putThrottlingTier',tr).val();
            console.log(selectedValueThrottling);
            if(resourceThrottlingTierValues == ""){resourceThrottlingTierValues += selectedValueThrottling }else{resourceThrottlingTierValues += ","+selectedValueThrottling}
            <!--Throttling-fix-->
        }
        if($('.resource-post',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "POST"}else{resourceMethodValues += ",POST"}
            var selectedValue = $('.postAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue }
            <!--Throttling-fix-->
            var selectedValueThrottling = $('.postThrottlingTier',tr).val();
            console.log(selectedValueThrottling);
            if(resourceThrottlingTierValues == ""){resourceThrottlingTierValues += selectedValueThrottling }else{resourceThrottlingTierValues += ","+selectedValueThrottling}
            <!--Throttling-fix-->
        }
        if($('.resource-delete',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "DELETE"}else{resourceMethodValues += ",DELETE"}
            var selectedValue = $('.deleteAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
            <!--Throttling-fix-->
            var selectedValueThrottling = $('.deleteThrottlingTier',tr).val();
            console.log(selectedValueThrottling);
            if(resourceThrottlingTierValues == ""){resourceThrottlingTierValues += selectedValueThrottling }else{resourceThrottlingTierValues += ","+selectedValueThrottling}
            <!--Throttling-fix-->
        }
        if($('.resource-options',tr).is(':checked')){
            if(resourceMethodValues == ""){resourceMethodValues += "OPTIONS"}else{resourceMethodValues += ",OPTIONS"}
            var selectedValue = $('.optionsAuthType',tr).val();
            if(resourceMethodAuthValues == ""){resourceMethodAuthValues += selectedValue }else{resourceMethodAuthValues += ","+selectedValue}
            <!--Throttling-fix-->
            var selectedValueThrottling = $('.optionsThrottlingTier',tr).val();
            console.log(selectedValueThrottling);
            if(resourceThrottlingTierValues == ""){resourceThrottlingTierValues += selectedValueThrottling }else{resourceThrottlingTierValues += ","+selectedValueThrottling}
            <!--Throttling-fix-->
        }

        if(resourcesCount >= 0){
            $('<input>').attr('type', 'hidden')
                .attr('name', 'uriTemplate-' + resourcesCount).attr('id', 'uriTemplate-' + resourcesCount).attr('value', $('.resourceTemplate',tr).val())
                .appendTo('#hiddenFormElements');

            $('<input>').attr('type', 'hidden')
                .attr('name', 'resourceMethod-' + resourcesCount).attr('id', 'resourceMethod-' + resourcesCount).attr('value', resourceMethodValues)
                .appendTo('#hiddenFormElements');

            $('<input>').attr('type', 'hidden')
                .attr('name', 'resourceMethodAuthType-' + resourcesCount).attr('id', 'resourceMethodAuthType-' + resourcesCount).attr('value', resourceMethodAuthValues)
                .appendTo('#hiddenFormElements');
            <!--Throttling-fix-->
            $('<input>').attr('type', 'hidden')
                .attr('name', 'resourceMethodThrottlingTier-' + resourcesCount).attr('id', 'resourceMethodThrottlingTier-' + resourcesCount).attr('value', resourceThrottlingTierValues)
                .appendTo('#hiddenFormElements');
            <!--Throttling-fix-->
        }
    });
    $('#resourceCount').val($('#resourceTable tr').length-2);
};
var deleteResource = function (deleteButton) {
    var count=$('#resourceTable tr').length;
    //Check whether only one defined resource remains before delete operation
    if(count==3){
        $('#resourceTableError').show('fast');
        $('#resourceTableError').html( i18n.t('errorMsgs.apiResource')+'<br />');
        return;
    }
    $('#resourceTableError').hide('fast');
    $(deleteButton).parent().parent().remove();

    enableDisableButtons();
    createHiddenForm();
};

var validateResourceTable = function(){
    var errors = "";

    $('.resourceRow input.resourceTemplate').each(function(){
        var myVal = $(this).val();
        var foundMyVal = 0;
        $('.resourceRow input.resourceTemplate').each(function(){
            if($(this).val()==myVal){
                foundMyVal++;
            }
        });
        if(foundMyVal > 1){
            errors +=  i18n.t('errorMsgs.uniqueUrlPattern')+"<strong>" + myVal + "</strong>"+ i18n.t('errorMsgs.duplicateUrlPattern') +"<br/>";
        }
        if(myVal == ""){
            errors += i18n.t('errorMsgs.emptyUrlPattern')+"<br />";
        }
    });

    var allRowsHas_at_least_one_check = true;
    $('.resourceRow').each(function(){
        var tr = this;
        var noneChecked = true;
        $('input:checkbox',tr).each(function(){
            if($(this).is(":checked")){
                noneChecked = false;
            }
        });

        if(noneChecked){
            allRowsHas_at_least_one_check = false;
        }
    });



    if(!allRowsHas_at_least_one_check){
        errors += i18n.t('errorMsgs.emptyVerb')+"<br />";
    }
    if(errors != ""){
        $('#resourceTableError').show('fast');
        $('#resourceTableError').html(errors);
        $('#updateButton').attr('disabled','disabled');
    }else{
        $('#resourceTableError').hide('fast');
        $('#updateButton').removeAttr('disabled');
    }
    return errors;
};

function getContextValue() {
    var context = $('#context').val();
    var version = $('#apiVersion').val();

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
    if (visibility == "public" || visibility == "private"){
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
				    if(result.sequences[i] == insequence){
                        $('#inSequence').append('<option value="'+result.sequences[i]+'" selected="selected">'+result.sequences[i]+'</option>');
				    }else{
				        $('#inSequence').append('<option value="'+result.sequences[i]+'">'+result.sequences[i]+'</option>');
				    }
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
                            if(result.sequences[i] == outsequence){
                                $('#outSequence').append('<option value="'+result.sequences[i]+'" selected="selected">'+result.sequences[i]+'</option>');
                            }
                            else{
                                $('#outSequence').append('<option value="'+result.sequences[i]+'">'+result.sequences[i]+'</option>');
                            }
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
                            if(result.sequences[i] == faultsequence){
                                $('#faultSequence').append('<option value="'+result.sequences[i]+'" selected="selected">'+result.sequences[i]+'</option>');
                            }
                            else{
                                $('#faultSequence').append('<option value="'+result.sequences[i]+'">'+result.sequences[i]+'</option>');
                            }
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

