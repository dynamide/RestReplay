var outstring = "";
var arr = [];

var arrSummary = [];
var numSUCCESS = 0;
var numFAILURE = 0;
var arrFailures = [];
var totalFailures = 0;

var analytics = {};


arrSummary.push("<table border='1' class='TOC_TABLE'>");
arrSummary.push(" <tr><th>testGroupID"
                        +"</th><th>FAILED"
                        +"</th><th>Links"
                        +"</th></tr>"
                       );

for (var i=0; i<serviceResultsListList.size(); i++) {
    var serviceResultsList = serviceResultsListList.get(i);
    numSUCCESS = 0;
    numFAILURE = 0;
    arrFailures = [];

    var serviceResult = null;
    for (var j=0; j<serviceResultsList.size(); j++) {
        serviceResult = serviceResultsList.get(j);
        var isSUCCESS = serviceResult.isSUCCESS();
        if (isSUCCESS){
            numSUCCESS++;
        } else {
            numFAILURE++;
            arrFailures.push(" <a href='"+serviceResult.reportDetail.uri+'#'+serviceResult.testID+"'>"+serviceResult.testID+"</a>");
        }
    }
    //   arrSummary.push(" reportMaster: "+serviceResult.reportMaster.directory+" :: "+serviceResult.reportMaster.relname);
    //              +" fullname: "+serviceResult.reportMaster.fullname
    //                +" <br />URI: "+serviceResult.reportDetail.uri



    if (numFAILURE>0 && serviceResult!=null){
        totalFailures++;
        var sFail = '';
        if (numFAILURE>5){
            sFail = ""+numFAILURE +" <a href='"+serviceResult.reportDetail.uri+"'> More ...</a>";
        } else {
            sFail = arrFailures.join(" ");
        }
        arrSummary.push(" <tr><td><a href='"+serviceResult.reportDetail.uri+"'>"+serviceResult.testGroupID+"</a>"
                            +"</td><td>"+(numFAILURE==0?" ":"<span class='ERROR'>"+numFAILURE+"</span>")
                            +"</td><td>"+sFail
                            +"</td></tr>"
                           );

    }

}
arrSummary.push("</table>");
if (totalFailures>0){
    outstring += " VERSION 1 "+arrSummary.join(" ");
} else {
    outstring = " VERSION 1 ";
}
outstring;