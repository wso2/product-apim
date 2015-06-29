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
var deviceCheckbox = '#ast-container .ctrl-wr-asset .itm-select input[type="checkbox"]',
    assetContainer = '#ast-container';

/*
 * DOM ready functions.
 */
$(document).ready(function(){
    /* Adding selected class for selected devices */
    $(deviceCheckbox).each(function(){
        addDeviceSelectedClass(this);
    });
});

/*
 * On Select All Device button click function.
 * @param button: Select All Device button
 */
function selectAllDevices(button){
    if(!$(button).data('select')){
        $(deviceCheckbox).each(function(index){
            $(this).prop('checked', true);
            addDeviceSelectedClass(this);
        });
        $(button).data('select', true);
        $(button).html('Deselect All Devices');
    }else{
        $(deviceCheckbox).each(function(index){
            $(this).prop('checked', false);
            addDeviceSelectedClass(this);
        });
        $(button).data('select', false);
        $(button).html('Select All Devices');
    }
}

/*
 * On listing layout toggle buttons click function
 * @param view: Selected view type
 * @param selection: Selection button
 */
function changeDeviceView(view, selection){
    $('.view-toggle').each(function() {
        $(this).removeClass('selected');
    });
    $(selection).addClass('selected');

    if(view == 'list'){
        $(assetContainer).addClass('list-view');
    }
    else {
        $(assetContainer).removeClass('list-view');
    }
}

/*
 * Add selected style class to the parent element function
 * @param checkbox: Selected checkbox
 */
function addDeviceSelectedClass(checkbox){
    if($(checkbox).is(':checked')){
        $(checkbox).closest('.ctrl-wr-asset').addClass('selected');
    }
    else{
        $(checkbox).closest('.ctrl-wr-asset').removeClass('selected');
    }
}

/*
 * On device checkbox select add parent selected style class
 */
$(deviceCheckbox).click(function(){
    addDeviceSelectedClass(this);
});
