$(document).ready(function () {

    populateUsageData($("#year").val() + '-' + $("#month").val());

    $("#generate").click(function () {
        var year = $("#year");
        var month = $("#month");
        var selectedYear = year.val();
        var selectedMonth = month.val();
        deleteTableRows("usageSummaryBody");
        deleteTableRows("billingBody");
        populateUsageData(selectedYear + "-" + selectedMonth);

    });

});

var populateUsageData = function(period) {
    jagg.post("/site/blocks/billing/ajax/billing.jag", {
        action:"getProviderAPIUsage",
        period:period
    }, function (result) {
        if (!result.error) {
            var usage = result.usage;
            var i;
            var tbody = document.getElementById("usageSummaryBody");
            if(usage.length==0) {
                $('#showMsg').show();
                $('#usageDiv').hide();
            } else {
                $('#usageDiv').show();
                $('#showMsg').hide();

                var usageSummery = categoryBasedOnAPIVersion(usage);

                for(var k = 0; k < usageSummery.length; k++)    {
                    var row = document.createElement("tr");
                    var cell1 = document.createElement("td");

                    cell1.innerHTML = usageSummery[k].apiName;
                    var cell2 = document.createElement("td");
                    cell2.innerHTML = usageSummery[k].version;
                    var cell3 = document.createElement("td");
                    var tmpSpan = document.createElement('span');
                    tmpSpan.className = "pull-right";
                    tmpSpan.innerHTML = usageSummery[k].count;
                    cell3.appendChild(tmpSpan);
                    row.appendChild(cell1);
                    row.appendChild(cell2);
                    row.appendChild(cell3);
                    tbody.appendChild(row);

                }

                for (i = 0; i < usage.length; i++) {
                    var tbillBody = document.getElementById("billingBody");
                    var rowBil = document.createElement("tr");
                    var cell12 = document.createElement("td");

                    cell12.innerHTML = usage[i].apiName + " : " + usage[i].version;
                    var cellBil1 = document.createElement("td");
                    var tmpSpan1 = document.createElement('span');
                    tmpSpan1.className = "pull-right";
                    tmpSpan1.innerHTML = usage[i].count;
                    cellBil1.appendChild(tmpSpan1);
                    var cellBil2 = document.createElement("td");
                    if (parseFloat(usage[i].cost) === 0.0) {
                        cellBil2.innerHTML = "FREE";
                    } else{
                        cellBil2.innerHTML = "$"+usage[i].costPerAPI+" per API call";
                    }

                    var cellBil3 = document.createElement("td");
                    var tmpSpan2 = document.createElement('span');
                    tmpSpan2.className = "pull-right";
                    if (usage[i].cost=='0.00') {
                        tmpSpan2.innerHTML = "FREE";
                    } else {
                        tmpSpan2.innerHTML = usage[i].cost; }
                    cellBil3.appendChild(tmpSpan2);

                    rowBil.appendChild(cell12);
                    rowBil.appendChild(cellBil1);
                    rowBil.appendChild(cellBil2);
                    rowBil.appendChild(cellBil3);
                    tbillBody.appendChild(rowBil);

                }
                //if(usage.length>=1 && usage[0].cost!='0.00') {
                var tbillBody1 = document.getElementById("billingBody");
                var rowBill = document.createElement("tr");
                var cellBill = document.createElement("td");
                cellBill.colSpan = "3";
                rowBill.appendChild(cellBill);

                var cellBill2 = document.createElement("td");
                cellBill2.style = "border-top:solid #ccc 3px;";
                cellBill2.innerHTML = '<strong>Total</strong>';
                var tmpSpan3 = document.createElement('span');
                tmpSpan3.className = "pull-right";
                tmpSpan3.innerHTML = findTotalCharge(usage);
                cellBill2.appendChild(tmpSpan3);
                rowBill.appendChild(cellBill2);
                tbillBody1.appendChild(rowBill);
                //}
            }
        } else {
            jagg.message({content:result.message,type:"error"});
        }
    }, "json");
};

var deleteTableRows = function(tbodyId) {
    $("#"+tbodyId+"").empty();
};


var categoryBasedOnAPIVersion = function(usage)     {
    var list = [];

    for(var i = 0; i < usage.length; i++)    {
        var apiName = usage[i].apiName;
        var version = usage[i].version;
        var temp = $.grep(list, function(e) {return e.apiName === apiName && e.version === version});
        if(temp.length > 0)    {
            temp[0].count += usage[i].count;
        } else  {
            var entry = {apiName:usage[i].apiName, version:usage[i].version, count:usage[i].count};
            list.push(entry);
        }
    }
    return list;
};

var findTotalCharge = function(usage)     {

    var totalCharge = 0.0;

    for(var i = 0; i < usage.length;i++)    {
        totalCharge += parseFloat(usage[i].cost);
    }

    return totalCharge;
};