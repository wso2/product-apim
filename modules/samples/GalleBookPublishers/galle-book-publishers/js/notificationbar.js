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
var notificationBar = '.wr-notification-bar',
    navHeight = $('#nav').height(),
    headerHeight = $('header').height(),
    offset = (headerHeight + navHeight),
    toggleButton = 'a.wr-notification-toggle-btn';

/*
 * On window loaded functions.
 */
$(window).load(function(){
    setNotificationbarHeight();
    $(notificationBar).css('top' , offset);
    notificationbarPositionFix();
});

/*
 * On window resize functions.
 */
$(window).resize(function(){
    setNotificationbarHeight();
    notificationbarPositionFix();
});

/*
 * On main div.container resize functions.
 * @required  jquery.resize.js
 */
$('.container').resize(function(){
    setNotificationbarHeight();
    notificationbarPositionFix();
});

/*
 * On window scroll functions.
 */
$(function(){ // document ready
    if (!!$(notificationBar).offset()) { // make sure ".sticky" element exists
        //var stickyTop = $(notificationBar).offset().top; // returns number
        $(window).scroll(function(){ // scroll event
            notificationbarPositionFix();
        });
    }
});

/*
 * Notification panel fix positioning on window scrolling
 */
function notificationbarPositionFix(){
    var windowTop = $(window).scrollTop(); // returns number
    if (headerHeight < windowTop){
        $(notificationBar).css({ position: 'fixed', top: navHeight });
    }
    else {
        $(notificationBar).css('position','absolute');
        $(notificationBar).css('top' , offset);
    }
}

/*
 * Notification panel slide toggle
 */
function toggleNotificationbar(){
    $(notificationBar).toggleClass('toggled');
    $(toggleButton).toggleClass('selected');
}

/*
 * Set notification bar height to fill window height
 */
function setNotificationbarHeight(){
    var windowTop = $(window).scrollTop(); // returns number
    if (headerHeight < windowTop){
        $(notificationBar).height($('html').height() - (navHeight));
    }
    else {
        $(notificationBar).height($('html').height() - (offset+20));
    }
}