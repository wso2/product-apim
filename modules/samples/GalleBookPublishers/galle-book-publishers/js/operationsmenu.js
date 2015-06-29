/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Setting-up global variables.
 */
var operations = '.wr-operations',
    modalPopup = '.wr-modalpopup',
    modalPopupContainer = modalPopup + ' .modalpopup-container',
    modalPopupContent = modalPopup + ' .modalpopup-content',
    deviceCheckbox = '#ast-container .ctrl-wr-asset .itm-select input[type="checkbox"]',
    showOperationsBtn = '#showOperationsBtn',
    navHeight = $('#nav').height(),
    headerHeight = $('header').height(),
    offset = (headerHeight + navHeight),
    maxOperationsLimit = 15,
    hiddenOperation = '.wr-hidden-operations-content > div';

/*
 * DOM ready functions.
 */
$(document).ready(function(){
    /* collapse operations to a toggle menu, if exceeds max operations limit */
    if($(operations + "> a").length > maxOperationsLimit){
       $(showOperationsBtn).show();
    }
    else{
       $(operations).show();
    }

    toggleMoreOperationsHeight();
});

/*
 * On window loaded functions.
 */
$(window).load(function(){
    setPopupMaxHeight();
});

/*
 * On window resize functions.
 */
$(window).resize(function(){
    toggleMoreOperationsHeight();
    setPopupMaxHeight();
});

/*
 * On main div.container resize functions.
 * @required  jquery.resize.js
 */
$('.container').resize(function(){
    toggleMoreOperationsHeight();
});

/*
 * On operation click function.
 * @param selection: Selected operation
 */
function operationSelect(selection){
    $(modalPopupContent).html($(operations + ' .operation[data-operation='+selection+']').html());
    showPopup();
}

/*
 * On operation click function.
 * @param operation: Selected operation
 */
function runOperation(operation){
    console.log(operation);
}

/*
 * show popup function.
 */
function showPopup() {
    $(modalPopup).show();
    setPopupMaxHeight();
}

/*
 * hide popup function.
 */
function hidePopup() {
    $(modalPopupContent).html('');
    $(modalPopup).hide();
}

/*
 * set popup maximum height function.
 */
function setPopupMaxHeight() {
    $(modalPopupContent).css('max-height', ($('body').height() - ($('body').height()/100 * 30)));
    $(modalPopupContainer).css('margin-top', (-($(modalPopupContainer).height()/2)));
}

/*
 * Function to get selected devices ID's
 */
function getSelectedDeviceIds(){
    var deviceIdentifierList = [];
    $(deviceCheckbox).each(function(index){
        var device = $(this);
        var deviceId = device.data('deviceid');
        var deviceType = device.data('type');
        deviceIdentifierList.push({
            "id" : deviceId,
            "type" : deviceType
        });
    });
    return deviceIdentifierList;
}

/*
 * Function to open hidden device operations list
 */
function toggleMoreOperations(){
    $('.wr-hidden-operations, .wr-page-content').toggleClass('toggled');
    $(showOperationsBtn).toggleClass('selected');
    //$('.footer').toggleClass('wr-hidden-operations-toggled');
}

/*
 * Function to fit hidden device operation window height with the screen
 */
function toggleMoreOperationsHeight(){
    $('.wr-hidden-operations').css('min-height', $('html').height() - (offset+40));
}

/*
 * Advance operations sub categories show/hide toggle function
 */
function showAdvanceOperation(operation, button){
    $(button).addClass('selected');
    $(button).siblings().removeClass('selected');
    $(hiddenOperation + '[data-operation="' + operation + '"]').show();
    $(hiddenOperation + '[data-operation="' + operation + '"]').siblings().hide();
}

$(hiddenOperation + ' .wr-input-control.switch').click(function(){
    if($(this).hasClass('collapsed')){
        $(this).siblings('.fw-stack').addClass('collapsed');
    }
    else{
        $(this).siblings('.fw-stack').removeClass('collapsed');
    }
});
