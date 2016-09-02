var outstring = "<h4>APIs called</h4>";
var arr = [];
for (var i=0; i<serviceResultsListList.size(); i++) {
    var serviceResultsList = serviceResultsListList.get(i);
    for (var j=0; j<serviceResultsList.size(); j++) {
       var serviceResult = serviceResultsList.get(j);
       arr.push(serviceResult.method+"    "+serviceResult.fullURL);
    }
 }
outstring += "<p>"+arr.join("<br />")+"</p>";
outstring;
