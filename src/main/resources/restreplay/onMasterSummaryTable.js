var outstring = "";
var arrSummary = [];
var numSUCCESS = 0;
var numFAILURE = 0;
var isSuccess = false;
var arrFailures = [];
var avgTime = 0;
var totTime = 0;
var tot = 0;

arrSummary.push("<table border='1' class='TOC_TABLE'>");
arrSummary.push(" <tr><th>Group"
                        +"</th><th>Total"
                        +"</th><th>SUCCEEDED"
                        +"</th><th>FAILED"
                        +"</th><th>Time (ms)"
                        +"</th></tr>"
                       );

for (var i=0; i<serviceResultsListList.size(); i++) {
    var serviceResultsList = serviceResultsListList.get(i);
    numSUCCESS = 0;
    numFAILURE = 0;
    totTime = 0;
    tot = 0;

    var serviceResult = null;
    for (var j=0; j<serviceResultsList.size(); j++) {
        serviceResult = serviceResultsList.get(j);
        isSUCCESS = serviceResult.isSUCCESS();
        if (isSUCCESS){
            numSUCCESS++;
        } else {
            numFAILURE++;
        }
        tot++;
        totTime += serviceResult.time
    }

    avgTime = totTime/(numSUCCESS+numFAILURE);

    if (serviceResult!=null){
        arrSummary.push(" <tr><td><a href='"+serviceResult.reportDetail.uri+"'>"+serviceResult.testGroupID+"</a>"
                        +"</td><td>"+(0+numSUCCESS+numFAILURE)
                        +"</td><td>"+numSUCCESS
                        +"</td><td>"+(numFAILURE==0?" ":"<span class='ERROR'>"+numFAILURE+"</span>")
                        +"</td><td>"+totTime
                        +"</td></tr>"
                       );
    }

}
arrSummary.push("</table>");
outstring += ""+arrSummary.join(" ");
outstring;