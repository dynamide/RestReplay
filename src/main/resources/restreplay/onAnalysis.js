var dynamideJava2JavascriptReplacer = function(key, value) {
   var returnValue = value;
   try {
      if (value.getClass() !== null) { // If Java Object
         if (value instanceof java.lang.Number) {
            returnValue = 1 * value;
         } else if (value instanceof java.lang.Boolean) {
            returnValue = value.booleanValue();
         } else { // use Java toString()
            returnValue = '' + value;
         }
      }
   } catch (err) {
      // No worries... not a Java object
   }
   return returnValue;
};

var outstring = "";

var analytics = {};
    analytics.groups = [];
    analytics.totalTime = 0;
    analytics.failureTime = 0;
    analytics.successTime = 0;
    analytics.numSUCCESS = 0;
    analytics.numFAILURE = 0;
    analytics.env = ""+master.getEnvID();
    analytics.relativePathFromReportsDir = master.getRelativePathFromReportsDir();
    analytics.reportsDir = master.getReportsDir();
    analytics.testDir = master.getTestDir();
    analytics.protoHostPort = master.getProtoHostPort();
    analytics.startTime = master.startTime;
    analytics.endTime = master.endTime;


for (var i=0; i<serviceResultsListList.size(); i++) {
    var serviceResultsList = serviceResultsListList.get(i);

    var group = {};
    group.numFAILURE = 0;
    group.numSUCCESS = 0;
    group.failures = []; //{uri:"", testID:"", time:0}
    group.successes = [];
    group.successTime = 0;
    group.failureTime = 0;
    group.totalTime = 0;

    analytics.groups.push(group);

    var serviceResult = null;
    for (var j=0; j<serviceResultsList.size(); j++) {
        serviceResult = serviceResultsList.get(j);
        var isSUCCESS = serviceResult.isSUCCESS();
        group.id = serviceResult.testGroupID;
        group.uri = serviceResult.reportDetail.uri; //the last detail in each group wins, but the detail always has the correct group report URI.
        var objSR = {};
            objSR.uri = serviceResult.reportDetail.uri+'#'+serviceResult.testID;
            objSR.testID = serviceResult.testID;
            objSR.time = serviceResult.time;
        group.totalTime += objSR.time;
        if (isSUCCESS){
            analytics.numSUCCESS++;
            group.numSUCCESS++;
            group.successTime += (0+objSR.time);
            group.successes.push(objSR);
        } else {
            analytics.numFAILURE++;
            group.numFAILURE++;
            group.failureTime += (0+objSR.time);
            group.failures.push(objSR);
        }
    }

    analytics.totalTime += group.totalTime;
    analytics.failureTime += group.failureTime;
    analytics.successTime += group.successTime;
}

outstring = ""+JSON.stringify(analytics, dynamideJava2JavascriptReplacer, 4);
//kit.getOut().println("outstring:"+outstring);
outstring;
