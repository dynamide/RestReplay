var r = JSON.parse(serviceResult.result);
var theCount = r.req.theCount;
serviceResult.addExport("newCountExport", theCount);
var res = {"newCount": Number(theCount)};  //note that you need Number(prevCount) and not "prevCount": prevCount, because the latter will cause sun.org.mozilla.javascript.internal.EvaluatorException: Java class "[B" has no public instance field or method named "toJSON".

var stringArray = kit.newStringArray(theCount);
for (var i=0; i<theCount; i++){
  stringArray[i] = "abc"+i;
}
serviceResult.addExport("ORDER_IDS", stringArray);

JSON.stringify(res);

