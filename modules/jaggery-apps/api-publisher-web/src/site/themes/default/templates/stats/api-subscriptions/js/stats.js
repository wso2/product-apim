var t_on = {
    'apiChart': 1,
    'subsChart': 1,
    'serviceTimeChart': 1,
    'tempLoadingSpace': 1
};
var currentLocation;

var chartColorScheme1 = ["#3da0ea", "#bacf0b", "#e7912a", "#4ec9ce", "#f377ab", "#ec7337", "#bacf0b", "#f377ab", "#3da0ea", "#e7912a", "#bacf0b"];
//fault colors || shades of red
var chartColorScheme2 = ["#ED2939", "#E0115F", "#E62020", "#F2003C", "#ED1C24", "#CE2029", "#B31B1B", "#990000", "#800000", "#B22222", "#DA2C43"];
//fault colors || shades of blue
var chartColorScheme3 = ["#0099CC", "#436EEE", "#82CFFD", "#33A1C9", "#8DB6CD", "#60AFFE", "#7AA9DD", "#104E8B", "#7EB6FF", "#4981CE", "#2E37FE"];
currentLocation = window.location.pathname;
var statsEnabled = isDataPublishingEnabled();

require(["dojo/dom", "dojo/domReady!"], function (dom) {
    currentLocation = window.location.pathname;
    //Initiating the fake progress bar
    jagg.fillProgress('apiChart');
    jagg.fillProgress('subsChart');
    jagg.fillProgress('serviceTimeChart');
    jagg.fillProgress('tempLoadingSpace');

    jagg.post("/site/blocks/stats/api-subscriptions/ajax/stats.jag", { action: "getFirstAccessTime", currentLocation: currentLocation  },
        function (json) {

            if (!json.error) {

                if (json.usage && json.usage.length > 0) {
                    var d = new Date();
                    var firstAccessDay = new Date(json.usage[0].year, json.usage[0].month - 1, json.usage[0].day);
                    var currentDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());
                    if (firstAccessDay.valueOf() == currentDay.valueOf()) {
                        currentDay = new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1);
                    }
                    var rangeSlider = $("#rangeSlider");
                    //console.info(currentDay);
                    rangeSlider.dateRangeSlider({
                        "bounds": {
                            min: firstAccessDay,
                            max: currentDay
                        },
                        "defaultValues": {
                            min: firstAccessDay,
                            max: currentDay
                        }
                    });
                    rangeSlider.bind("valuesChanged", function (e, data) {
                        var from = convertTimeString(data.values.min);
                        var to = convertTimeStringPlusDay(data.values.max);
                        drawSubscriberCountByAPIs(from, to);

                    });
                    var width = $("#rangeSliderWrapper").width();
                    $("#rangeSliderWrapper").affix();
                    $("#rangeSliderWrapper").width(width);

                }

                else if (json.usage && json.usage.length == 0 && statsEnabled) {
                    $('#middle').html("");
                    $('#middle').append($('<div class="errorWrapper"><img src="../themes/default/templates/stats/images/statsEnabledThumb.png" alt="Stats Enabled"></div>'));
                }

                else {
                    $('#middle').html("");
                    $('#middle').append($('<div class="errorWrapper"><span class="label top-level-warning"><i class="icon-warning-sign icon-white"></i>'
                        + i18n.t('errorMsgs.checkBAMConnectivity') + '</span><br/><img src="../themes/default/templates/stats/api-subscriptions/images/statsThumb.png" alt="Smiley face"></div>'));
                }


            }
            else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content: json.message, type: "error"});
                }
            }
            t_on['apiChart'] = 0;
        }, "json");

});

var drawSubscriberCountByAPIs = function (from, to) {
    var fromDate = from;
    var toDate = to;
    jagg.post("/site/blocks/stats/api-subscriptions/ajax/stats.jag", { action: "getSubscriberCountByAPIs", currentLocation: currentLocation  },
        function (json) {
            if (!json.error) {
                var length = json.usage.length, data = [];
                if (length > 0) {
                    $('#subsChart').empty();
                    var easyPieChartDefaults = {
                        animate: 2000,
                        scaleColor: false,
                        lineWidth: 12,
                        lineCap: 'square',
                        size: 100,
                        trackColor: '#e5e5e5'
                    }
                    var allSubscriptionCount = 0;
                    for (var i = 0; i < length; i++) {
                        allSubscriptionCount += json.usage[i].count;
                    }
                    for (var i = 0; i < length; i++) {
                        var k = i + 1;
                        var chartId = "easyPieChart" + k;
                        var chartVal = Math.round((json.usage[i].count / allSubscriptionCount) * 100);
                        $('#subsChart').append("<div class='span2 easyPieWrapper'>" +
                            "<div style='color:" + chartColorScheme1[i] + "' data-percent='" + chartVal + "' id='" + chartId + "'>" + chartVal + "% </div>" +
                            "<b>" + json.usage[i].apiName + "</b><br /><i>" + json.usage[i].count + " subscription(s)</i></div>");
                        $('#' + chartId + '').easyPieChart({
                            animate: 2000,
                            scaleColor: false,
                            lineWidth: 12,
                            lineCap: 'square',
                            size: 110,
                            trackColor: '#e5e5e5',
                            barColor: chartColorScheme1[i]
                        });
                    }
                }


            } else {
                if (json.message == "AuthenticateError") {
                    jagg.showLogin();
                } else {
                    jagg.message({content: json.message, type: "error"});
                }
            }
            t_on['subsChart'] = 0;
        }, "json");
}

function isDataPublishingEnabled(){
    jagg.post("/site/blocks/stats/api-subscriptions/ajax/stats.jag", { action: "isDataPublishingEnabled"},
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

var convertTimeString = function (date) {
    var d = new Date(date);
    var formattedDate = d.getFullYear() + "-" + formatTimeChunk((d.getMonth() + 1)) + "-" + formatTimeChunk(d.getDate());
    return formattedDate;
};

var convertTimeStringPlusDay = function (date) {
    var d = new Date(date);
    var formattedDate = d.getFullYear() + "-" + formatTimeChunk((d.getMonth() + 1)) + "-" + formatTimeChunk(d.getDate() + 1);
    return formattedDate;
};

var formatTimeChunk = function (t) {
    if (t < 10) {
        t = "0" + t;
    }
    return t;
};

