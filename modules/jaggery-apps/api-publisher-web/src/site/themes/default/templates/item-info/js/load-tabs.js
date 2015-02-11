var chartColorScheme1 = ["#3da0ea","#bacf0b","#e7912a","#4ec9ce","#f377ab","#ec7337","#bacf0b","#f377ab","#3da0ea","#e7912a","#bacf0b"];
//fault colors || shades of red
var chartColorScheme2 = ["#ED2939","#E0115F","#E62020","#F2003C","#ED1C24","#CE2029","#B31B1B","#990000","#800000","#B22222","#DA2C43"];
//fault colors || shades of blue
var chartColorScheme3 = ["#0099CC","#436EEE","#82CFFD","#33A1C9","#8DB6CD","#60AFFE","#7AA9DD","#104E8B","#7EB6FF","#4981CE","#2E37FE"];

var t_on = {
            'versionChart':1,
            'versionUserChart':1,
            'userVersionChart':1,
            'userChart':1
            };

var getLastAccessTime = function(name) {
    var lastAccessTime = null;
    var provider = $("#item-info #spanProvider").text();
    jagg.syncPost("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIVersionUserLastAccess",provider:provider,mode:'browse' },
                  function (json) {
                      if (!json.error) {
                          var length = json.usage.length;
                          for (var i = 0; i < length; i++) {
                              if (json.usage[i].api_name == name) {
                                  lastAccessTime = json.usage[i].lastAccess + " (Accessed version: " + json.usage[i].api_version + ")";
                                  break;
                              }
                          }
                      } else {
                          if (json.message == "AuthenticateError") {
                              jagg.showLogin();
                          } else {
                              jagg.message({content:json.message,type:"error"});
                          }
                      }
                  });
    return lastAccessTime;
};

var getResponseTime = function(name) {
    var responseTime = null;
    var provider = $("#item-info #spanProvider").text();
    jagg.syncPost("/site/blocks/stats/ajax/stats.jag", { action:"getProviderAPIServiceTime",provider:provider,mode:'browse'},
                  function (json) {
                      if (!json.error) {
                          var length = json.usage.length;
                          for (var i = 0; i < length; i++) {
                              if (json.usage[i].apiName == name) {
                                  responseTime = json.usage[i].serviceTime + " ms";
                                  break;
                              }
                          }
                      } else {
                          if (json.message == "AuthenticateError") {
                              jagg.showLogin();
                          } else {
                              jagg.message({content:json.message,type:"error"});
                          }
                      }
                  });
    return responseTime;
};


$(document).ready(function() {

    // Converting dates from timestamp to date string
    jagg.printDate();

    if (($.cookie("selectedTab") != null)) {
        var tabLink = $.cookie("selectedTab");
        $('#' + tabLink + "Link").tab('show');
        //$.cookie("selectedTab", null);
        pushDataForTabs(tabLink);
    }

    $('a[data-toggle="tab"]').on('shown', function (e) {
        jagg.sessionAwareJS({callback:function(){
            var clickedTab = e.target.href.split('#')[1];
            ////////////// edit tab
            pushDataForTabs(clickedTab);
            $.cookie("selectedTab",clickedTab);
        }});

    });
    
});
var t_on = {
            'versionChart':1,
            'versionUserChart':1
            };
function pushDataForTabs(clickedTab){
     if (clickedTab == "versions") {

            jagg.fillProgress('versionChart');jagg.fillProgress('versionUserChart');
            var apiName = $("#infoAPIName").val();
            var version = $("#infoAPIVersion").val();
            var provider = $("#item-info #spanProvider").text();
            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getProviderAPIVersionUsage", provider:provider,apiName:apiName },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#versionChart').empty();
                              $('#versionTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].version, parseInt(json.usage[i].count)];
                                  $('#versionTable').append($('<tr><td>' + json.usage[i].version + '</td><td>' + json.usage[i].count + '</td></tr>'));

                              }

                              if (length > 0) {
                                  $('#versionTable').show();
                                  /*var plot1 = jQuery.jqplot('versionChart', [data],
                                                            {
                                                                seriesDefaults:{
                                                                    // Make this a pie chart.
                                                                    renderer:jQuery.jqplot.PieRenderer,
                                                                    rendererOptions:{
                                                                        // Put data labels on the pie slices.
                                                                        // By default, labels show the percentage of the slice.
                                                                        showDataLabels:true
                                                                    }
                                                                },
                                                                seriesColors: [ "#ed3c3c", "#ffe03e", "#48ca48", "#49baff","#7d7dff", "#ff468b", "#de621d", "#cb68c9"],
                                                                legend:{ show:true, location:'e' }
                                                            }
                                          );*/
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
                                      var versionChart = new Chart("versionChart");

                                      // Set the theme
                                      versionChart.setTheme(theme);

                                      // Add the only/default plot
                                      versionChart.addPlot("default", {
                                          type: Pie,
                                          markers: true,
                                          radius:130
                                      });

                                      // Add axes
                                      versionChart.addAxis("x");
                                      versionChart.addAxis("y", { min: 5000, max: 30000, vertical: true, fixLower: "major", fixUpper: "major" });

                                      // Define the data
                                      var chartData; var color = -1;
                                      require(["dojo/_base/array"], function(array){
                                          chartData= array.map(data, function(d){
                                              color++;
                                              return {y: d[1], tooltip: "<b>"+d[0]+"</b><br /><i>"+d[1]+" call(s)</i>",fill:chartColorScheme1[color]};

                                          });
                                      });

                                      versionChart.addSeries("Version",chartData);


                                      // Create the tooltip
                                      var tip = new Tooltip(versionChart,"default");

                                      // Create the slice mover
                                      var mag = new MoveSlice(versionChart,"default");

                                      // Render the chart!
                                      versionChart.render();

                                  });
                              } else {
                                  $('#versionTable').hide();
                                  $('#versionChart').css("fontSize", 14);
                                  $('#versionChart').append($('<span class="label label-info">' + i18n.t('errorMsgs.noData') + '</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                          t_on['versionChart'] = 0;
                      }, "json");


            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getSubscriberCountByAPIVersions", provider:provider,apiName:apiName },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#versionUserChart').empty();
                              $('#versionUserTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].apiVersion, parseInt(json.usage[i].count)];
                                  $('#versionUserTable').append($('<tr><td>' + json.usage[i].apiVersion + '</td><td>' + json.usage[i].count + '</td></tr>'));
                              }
                              if (length > 0) {
                                  $('#versionUserTable').show();
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
                                      var versionUserChart = new Chart("versionUserChart");

                                      // Set the theme
                                      versionUserChart.setTheme(theme);

                                      // Add the only/default plot
                                      versionUserChart.addPlot("default", {
                                          type: Pie,
                                          markers: true,
                                          radius:130
                                      });

                                      // Add axes
                                      versionUserChart.addAxis("x");
                                      versionUserChart.addAxis("y", { min: 5000, max: 30000, vertical: true, fixLower: "major", fixUpper: "major" });

                                      // Define the data
                                      var chartData; var color = -1;
                                      require(["dojo/_base/array"], function(array){
                                          chartData= array.map(data, function(d){
                                              color++;
                                              return {y: d[1], tooltip: "<b>"+d[0]+"</b><br /><i>"+d[1]+" subscription(s)</i>",fill:chartColorScheme1[color]};

                                          });
                                      });

                                      versionUserChart.addSeries("Version",chartData);


                                      // Create the tooltip
                                      var tip = new Tooltip(versionUserChart,"default");

                                      // Create the slice mover
                                      var mag = new MoveSlice(versionUserChart,"default");

                                      // Render the chart!
                                      versionUserChart.render();

                                  });
                              } else {
                                  $('#versionUserTable').hide();
                                  $('#versionUserChart').css("fontSize", 14);
                                  $('#versionUserChart').append($('<span class="label label-info">' + i18n.t('errorMsgs.noData') + '</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                          t_on['versionUserChart'] = 0;
                      }, "json");

        }

        if (clickedTab == "users") {
            jagg.fillProgress('userVersionChart');jagg.fillProgress('userChart');
            var name = $("#infoAPIName").val();
            var version = $("#infoAPIVersion").val();
            var provider = $("#item-info #spanProvider").text();
            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getProviderAPIUserUsage", provider:provider,apiName:name },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#userChart').empty();
                              $('#userTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].user, parseInt(json.usage[i].count)];
                                  $('#userTable').append($('<tr><td>' + json.usage[i].user + '</td><td>' + json.usage[i].count + '</td></tr>'));

                              }

                              if (length > 0) {
                                  $('#userTable').show();
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
                                      var userChart = new Chart("userChart");

                                      // Set the theme
                                      userChart.setTheme(theme);

                                      // Add the only/default plot
                                      userChart.addPlot("default", {
                                          type: Pie,
                                          markers: true,
                                          radius:130
                                      });

                                      // Add axes
                                      userChart.addAxis("x");
                                      userChart.addAxis("y", { min: 5000, max: 30000, vertical: true, fixLower: "major", fixUpper: "major" });

                                      // Define the data
                                      var chartData; var color = -1;
                                      require(["dojo/_base/array"], function(array){
                                          chartData= array.map(data, function(d){
                                              color++;
                                              return {y: d[1], tooltip: "<b>"+d[0]+"</b><br /><i>"+d[1]+" call(s)</i>",fill:chartColorScheme1[color]};

                                          });
                                      });

                                      userChart.addSeries("Version",chartData);


                                      // Create the tooltip
                                      var tip = new Tooltip(userChart,"default");

                                      // Create the slice mover
                                      var mag = new MoveSlice(userChart,"default");

                                      // Render the chart!
                                      userChart.render();

                                  });
                              } else {
                                  $('#userTable').hide();
                                  $('#userChart').css("fontSize", 14);
                                  $('#userChart').append($('<span class="label label-info">' + i18n.t('errorMsgs.noData') + '</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                      }, "json");

            jagg.post("/site/blocks/usage/ajax/usage.jag", { action:"getProviderAPIVersionUserUsage", provider:provider,apiName:name,version:version, server:"https://localhost:9444/" },
                      function (json) {
                          if (!json.error) {
                              var length = json.usage.length,data = [];
                              $('#userVersionChart').empty();
                              $('#userVersionTable').find("tr:gt(0)").remove();
                              for (var i = 0; i < length; i++) {
                                  data[i] = [json.usage[i].user, parseInt(json.usage[i].count)];
                                  $('#userVersionTable').append($('<tr><td>' + json.usage[i].user + '</td><td>' + json.usage[i].count + '</td></tr>'));

                              }

                              if (length > 0) {
                                  $('#userVersionTable').show();
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
                                      var userVersionChart = new Chart("userVersionChart");

                                      // Set the theme
                                      userVersionChart.setTheme(theme);

                                      // Add the only/default plot
                                      userVersionChart.addPlot("default", {
                                          type: Pie,
                                          markers: true,
                                          radius:130
                                      });

                                      // Add axes
                                      userVersionChart.addAxis("x");
                                      userVersionChart.addAxis("y", { min: 5000, max: 30000, vertical: true, fixLower: "major", fixUpper: "major" });

                                      // Define the data
                                      var chartData; var color = -1;
                                      require(["dojo/_base/array"], function(array){
                                          chartData= array.map(data, function(d){
                                              color++;
                                              return {y: d[1], tooltip: "<b>"+d[0]+"</b><br /><i>"+d[1]+" call(s)</i>",fill:chartColorScheme1[color]};

                                          });
                                      });

                                      userVersionChart.addSeries("Version",chartData);


                                      // Create the tooltip
                                      var tip = new Tooltip(userVersionChart,"default");

                                      // Create the slice mover
                                      var mag = new MoveSlice(userVersionChart,"default");

                                      // Render the chart!
                                      userVersionChart.render();

                                  });
                              } else {
                                  $('#userVersionTable').hide();
                                  $('#userVersionChart').css("fontSize", 14);
                                  $('#userVersionChart').append($('<span class="label label-info">' + i18n.t('errorMsgs.noData') + '</span>'));
                              }

                          } else {
                              if (json.message == "AuthenticateError") {
                                  jagg.showLogin();
                              } else {
                                  jagg.message({content:json.message,type:"error"});
                              }
                          }
                      }, "json");

            var responseTime = getResponseTime(name);
            var lastAccessTime = getLastAccessTime(name);

            if (responseTime != null && lastAccessTime != null) {
                $("#usageSummary").show();
                var doc = document;
                var tabBody = doc.getElementById("usageTable");

                var row1 = doc.createElement("tr");
                var cell1 = doc.createElement("td");
                cell1.setAttribute("class", "span4");
                cell1.innerHTML = i18n.t('titles.responseTimeGraph');
                var cell2 = doc.createElement("td");
                cell2.innerHTML = responseTime != null ? responseTime : i18n.t('errorMsgs.unavailableData');
                row1.appendChild(cell1);
                row1.appendChild(cell2);

                var row2 = doc.createElement("tr");
                var cell3 = doc.createElement("td");
                cell3.setAttribute("class", "span4");
                cell3.innerHTML = i18n.t('titles.lastAccessTimeGraph');
                var cell4 = doc.createElement("td");
                cell4.innerHTML = lastAccessTime != null ? lastAccessTime : i18n.t('errorMsgs.unavailableData');
                row2.appendChild(cell3);
                row2.appendChild(cell4);

                tabBody.appendChild(row1);
                tabBody.appendChild(row2);

            }

        }
}

Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) {
            size++;
        }
    }
    return size;
};



