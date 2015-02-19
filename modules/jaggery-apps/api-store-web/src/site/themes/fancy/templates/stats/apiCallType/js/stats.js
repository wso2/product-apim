var t_on = {
    'apiChart':1,
    'subsChart':1,
    'userChart':1,
    'serviceTimeChart':1,
    'tempLoadingSpace':1,
    'subsChart':1
};
var currentLocation;

var chartColorScheme1 = ["#3da0ea","#bacf0b","#e7912a","#4ec9ce","#f377ab","#ec7337","#bacf0b","#f377ab","#3da0ea","#e7912a","#bacf0b"];
//fault colors || shades of red
var chartColorScheme2 = ["#ED2939","#E0115F","#E62020","#F2003C","#ED1C24","#CE2029","#B31B1B","#990000","#800000","#B22222","#DA2C43"];
//fault colors || shades of blue
var chartColorScheme3 = ["#0099CC","#436EEE","#82CFFD","#33A1C9","#8DB6CD","#60AFFE","#7AA9DD","#104E8B","#7EB6FF","#4981CE","#2E37FE"];
currentLocation=window.location.pathname;
var statsEnabled = isDataPublishingEnabled();

require(["dojo/dom", "dojo/domReady!"], function(dom){
    currentLocation=window.location.pathname;
    //Initiating the fake progress bar
    jagg.fillProgress('apiChart');jagg.fillProgress('userChart');jagg.fillProgress('subsChart');jagg.fillProgress('serviceTimeChart');jagg.fillProgress('tempLoadingSpace');

    jagg.post("/site/blocks/stats/apiCallType/ajax/stats.jag", { action:"getFirstAccessTime",currentLocation:currentLocation  },
        function (json) {            

            if (!json.error) {

                if( json.usage && json.usage.length > 0){
                    
                    var d = new Date();
                    var firstAccessDay = new Date(json.usage[0].year, json.usage[0].month-1, json.usage[0].day);
                    var currentDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());
                    if(firstAccessDay.valueOf() == currentDay.valueOf()){
                        currentDay = new Date(d.getFullYear(), d.getMonth(), d.getDate()+1);
                    }
                    var rangeSlider =  $("#rangeSlider");
                    //console.info(currentDay);
                    rangeSlider.dateRangeSlider({
                        "bounds":{
                            min: firstAccessDay,
                            max: currentDay
                        },
                        "defaultValues":{
                            min: firstAccessDay,
                            max: currentDay
                        }
                    });
                    rangeSlider.bind("valuesChanged", function(e, data){
                        var from = convertTimeString(data.values.min);
                        var to = convertTimeStringPlusDay(data.values.max);

                      
                        drawAppAPICallType(from,to); 
                        
                       

                        
                    });


                }

                else if (json.usage && json.usage.length == 0 && statsEnabled) {
                    $('#content').html("");
                    $('#content').append($('<div class="errorWrapper"><img src="../themes/fancy/templates/stats/images/statsEnabledThumb.png" alt="Stats Enabled"></div>'));
                }

                else{
                    $('#content').html("");
                    $('#content').append($('<div class="errorWrapper"><span class="label top-level-warning"><i class="icon-warning-sign icon-white"></i>'
                        +i18n.t('errorMsgs.checkBAMConnectivity')+'</span><br/><img src="../themes/fancy/templates/stats/apiCallType/images/statsThumb.png" alt="Smiley face"></div>'));
                }


            }
            else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content:json.message,type:"error"});
                }
            }
            t_on['apiChart'] = 0;
            t_on['userChart'] = 1;
        }, "json");

});

$(document).ready(function(){
    $(document).scroll(function(){
        var top=$(document).scrollTop();
        console.info(top);
        var width = $("#rangeSliderWrapper").width();
        if(top > 180){
            $("#rangeSliderWrapper").css("position","fixed").css("top","30px").width(width);
        
        }else{
          
           $("#rangeSliderWrapper").css({ "position": "relative", "top": "0px" }); 
        }

    })
})




var drawAppAPICallType = function(from,to){

    var fromDate = from;
    var toDate = to;
    jagg.post("/site/blocks/stats/apiCallType/ajax/stats.jag", { action:"getAppApiCallType",currentLocation:currentLocation,fromDate:fromDate,toDate:toDate  },
        function (json) {
            if (!json.error) {
                $('#AppApiCallTypeTable').find("tr:gt(0)").remove();
                var length = json.usage.length;
                $('#AppApiCallTypeTable').show();
              for (var i = 0; i < json.usage.length; i++) {
                    $('#AppApiCallTypeTable').append($('<tr><td>' + json.usage[i].appName + '</td><td>' + json.usage[i].apiCallTypeArray[0].apiName + '</td><td class="tdNumberCell">' + json.usage[i].apiCallTypeArray[0].callType[0] + '</td></tr>'));
                    if(json.usage[i].apiCallTypeArray[0].callType.length > 1){
                        for(var k =1 ; k < json.usage[i].apiCallTypeArray[0].callType.length ; k++){
                             $('#AppApiCallTypeTable').append($('<tr><td>' + "" + '</td><td>' +"" + '</td><td class="tdNumberCell">' + json.usage[i].apiCallTypeArray[0].callType[k] + '</td></tr>'));
                         }
                     }
                    if(json.usage[i].apiCallTypeArray.length > 1){
                        for(var j = 1 ; j< json.usage[i].apiCallTypeArray.length ; j++){
                            $('#AppApiCallTypeTable').append($('<tr><td>' + "" + '</td><td>' +json.usage[i].apiCallTypeArray[j].apiName + '</td><td class="tdNumberCell">' + json.usage[i].apiCallTypeArray[j].callType[0] + '</td></tr>'));
                            if(json.usage[i].apiCallTypeArray[j].callType.length > 1){
                                for(var k =1 ; k < json.usage[i].apiCallTypeArray[j].callType.length ; k++){
                                    $('#AppApiCallTypeTable').append($('<tr><td>' + "" + '</td><td>' +"" + '</td><td class="tdNumberCell">' + json.usage[i].apiCallTypeArray[j].callType[k] + '</td></tr>'));
                                }
                            }
                        }
                    }
                }
                if (length == 0) {
                    $('#AppApiCallTypeTable').hide();
                    $('#tempLoadingSpace').html('');
                    $('#tempLoadingSpace').append($('<span class="label label-info">'+i18n.t('errorMsgs.noData')+'</span>'));

                }else{
                    $('#tempLoadingSpace').hide();
                }

            } else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content:json.message,type:"error"});
                }
            }
            t_on['tempLoadingSpace'] = 0;
        }, "json");
}


function isDataPublishingEnabled(){
    jagg.post("/site/blocks/stats/apiCallType/ajax/stats.jag", { action: "isDataPublishingEnabled"},
        function (json) {
            if (!json.error) {
                statsEnabled = json.usage;                
                return statsEnabled;
            } else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content: json.message, type: "error"});
                }
            }
        }, "json");        
}


var convertTimeString = function(date){
    var d = new Date(date);
    var formattedDate = d.getFullYear() + "-" + formatTimeChunk((d.getMonth()+1)) + "-" + formatTimeChunk(d.getDate());
    return formattedDate;
};



var convertTimeStringPlusDay = function(date){
    var d = new Date(date);
    var formattedDate = d.getFullYear() + "-" + formatTimeChunk((d.getMonth()+1)) + "-" + formatTimeChunk(d.getDate()+1);
    return formattedDate;
};

var formatTimeChunk = function (t){
    if (t<10){
        t="0" + t;
    }
    return t;
};
