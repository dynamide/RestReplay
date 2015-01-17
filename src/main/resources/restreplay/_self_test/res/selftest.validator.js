var r = JSON.parse(serviceResult.result);
var theCount = r.req.theCount;
serviceResult.addExport("newCountExport", theCount);
var res = {"newCount": Number(theCount)};  //note that you need Number(prevCount) and not "prevCount": prevCount, because the latter will cause sun.org.mozilla.javascript.internal.EvaluatorException: Java class "[B" has no public instance field or method named "toJSON".

serviceResult.addAlertOK("Just some info", "some OK context");
serviceResult.addExport("exportFromValidator", "Hello from Validator: "+__FILE__);


var stringArray = kit.newStringArray(theCount);
stringArray[0] = "abc123";
stringArray[1] = "abc456";
serviceResult.addExport("ORDER_IDS", stringArray);

JSON.stringify(res);

