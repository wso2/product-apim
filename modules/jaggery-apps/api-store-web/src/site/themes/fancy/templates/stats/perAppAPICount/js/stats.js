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

    jagg.post("/site/blocks/stats/perAppAPICount/ajax/stats.jag", { action:"getFirstAccessTime",currentLocation:currentLocation  },
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

             
                        drawGraphAPIUsage(from,to);


                        console.info("drawGraphAPIUsage");

                        
                    });
                    

                }

                else if (json.usage && json.usage.length == 0 && statsEnabled) {
                    $('#content').html("");
                    $('#content').append($('<div class="errorWrapper"><img src="../themes/fancy/templates/stats/images/statsEnabledThumb.png" alt="Stats Enabled"></div>'));
                }

                else{
                    $('#content').html("");
                    $('#content').append($('<div class="errorWrapper"><span class="label top-level-warning"><i class="icon-warning-sign icon-white"></i>'
                        +i18n.t('errorMsgs.checkBAMConnectivity')+'</span><br/><img src="../themes/fancy/templates/stats/perAppAPICount/images/statsThumb.png" alt="Smiley face"></div>'));


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
       // console.info(top);
        var width = $("#rangeSliderWrapper").width();
        if(top > 180){
            $("#rangeSliderWrapper").css("position","fixed").css("top","30px").width(width);
        
        }else{
          
           $("#rangeSliderWrapper").css({ "position": "relative", "top": "0px" }); 
        }

    })
})



var drawGraphAPIUsage = function(from,to){


    var fromDate = from;
    var toDate = to;
    jagg.post("/site/blocks/stats/perAppAPICount/ajax/stats.jag", { action:"getProviderAPIUsage",currentLocation:currentLocation,fromDate:fromDate,toDate:toDate  },
        function (json) {
            if (!json.error) {
                var lentth1 = json.usage.length;
                $('#apiUsage').empty();
                for(var k=0 ; k<lentth1 ;k++){

                     $('#apiUsage').append($('<div class="well"><div class="row-fluid"> <h3>Application Name:  '+json.usage[k].appName+'</h3><div class="span6" style="height:350px; width :350px"><div id="apiChart'+(k+1)+'" style="height:350px;"><div class="progress progress-striped active"><div class="bar" style="width: 10%;"></div></div></div> </div> <div class="span6"> <table class="table graphTable" id="apiTable'+(k+1)+'" style="display:none;"><tr> <th>'+ i18n.t("apiName")+'</th><th>'+ i18n.t("noOfAPICalls")+'</th></tr> </table> </div></div></div>'));
             } for(var k=0 ; k<lentth1 ;k++){
                var length = json.usage[k].apiCountArray.length,data = [];

                $('#apiTable'+(k+1)).find("tr:gt(0)").remove();
                $('#apiChart'+(k+1)).empty();
                for (var i = 0; i < length; i++) {
                    data[i] = [ json.usage[k].apiCountArray[i].apiName, parseInt( json.usage[k].apiCountArray[i].count )];
                    $('#apiTable'+(k+1)).append($('<tr><td>' +  json.usage[k].apiCountArray[i].apiName + '</td><td class="tdNumberCell">' +json.usage[k].apiCountArray[i].count + '</td></tr>'));

                }

                if (length > 0) {
                    $('#apiTable'+(k+1)).show();
                    require([
                        // Require the basic chart class
                        "dojox/charting/Chart",

                        // Require the theme of our choosing
                        "dojox/charting/themes/Claro",

                        // Charting plugins:

                        //  We want to plot a Pie chart
                        "dojox/charting/plot2d/Pie",

                        // Retrieve the Legend, Tooltip, and MoveSlice classes
                        "dojox/charting/action2d/Tooltip",
                        "dojox/charting/action2d/MoveSlice",

                        //  We want to use Markers
                        "dojox/charting/plot2d/Markers",

                        //  We'll use default x/y axes
                        "dojox/charting/axis2d/Default"
                    ], function(Chart, theme, Pie, Tooltip, MoveSlice) {

                        // Create the chart within it's "holding" node
                        var apiUsageChart = new Chart("apiChart"+(k+1));

                        // Set the theme
                        apiUsageChart.setTheme(theme);

                        // Add the only/default plot
                        apiUsageChart.addPlot("default", {
                            type: Pie,
                            markers: true,
                            radius:130
                        });

                        // Add axes
                        apiUsageChart.addAxis("x");
                        apiUsageChart.addAxis("y", { min: 5000, max: 30000, vertical: true, fixLower: "major", fixUpper: "major" });

                        // Define the data
                        var chartData; var color = -1;
                        require(["dojo/_base/array"], function(array){
                            chartData= array.map(data, function(d){
                                color++;
                                return {y: d[1], tooltip: "<b>"+d[0]+"</b><br /><i>"+d[1]+" call(s)</i>",fill:chartColorScheme1[color]};

                            });
                        });

                        apiUsageChart.addSeries("API Usage",chartData);



                        // Create the tooltip
                        var tip = new Tooltip(apiUsageChart,"default");

                        // Create the slice mover
                        var mag = new MoveSlice(apiUsageChart,"default");

                        // Render the chart!
                        apiUsageChart.render();



                    });

                } else {

                }


            }
            } else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content:json.message,type:"error"});
                }
            }
            t_on['apiChart'] = 0;
        }, "json");
}

function isDataPublishingEnabled(){
    jagg.post("/site/blocks/stats/perAppAPICount/ajax/stats.jag", { action: "isDataPublishingEnabled"},
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
