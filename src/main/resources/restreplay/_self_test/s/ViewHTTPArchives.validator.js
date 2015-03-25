kit.getOut().println("in validator: "+result.expectedContentExpanded.length());
var outstring = "";
var rr = JSON.parse(result.expectedContentExpanded);
if (rr && rr.log) {
    var entries = rr.log.entries;
    outstring += "<h2>Tests in session</h2>Omitting OPTIONS requests, including only JSON responses...<br />";
    var buff = [];
    for (var i=0; i<entries.length; i++) {
        var entry = entries[i];
        var postDataBlock = '';
        var responseBlock = '';
        if (entry.request.method ==  "OPTIONS"){
            continue;
        }
        if (entry.response){
            var mimet = entry.response.content.mimeType;
            if (mimet != "application/json" &&
                mimet != "text/json" ){
                    continue;
            }
        }
        if (entry.request.postData){
            var pd = entry.request.postData;
            var ps = JSON.parse(pd.text);
            postDataBlock += "<div><small><font color='green'>request ["+JSON.stringify(pd.mimeType)+"]<br /> <textarea rows='5' cols='80'>"+JSON.stringify(ps,null,4)+"</textarea></font></small></div>";
        }
        if (entry.response && entry.response.content.text){
            var ps = entry.response.content.text;
            var mt = entry.response.content.mimeType;
            if (mt == 'application/json'){
                ps = JSON.parse(ps);
                responseBlock += "<br /><small><font color='green'>response ["+mt+"]<br /><textarea rows='5' cols='80'>"+JSON.stringify(ps,null,4)+"</textarea></font></small>";
            } else {
                responseBlock = "<br /><small><font color='green'>response ["+mt+"]<br /></font></small>";
            }
        }
        buff.push( "<br /><hr /><b>"+entry.request.method+"</b>"
                  +" "+entry.request.url+""
                  +postDataBlock
                  +responseBlock
                  +"<br />" );
    }
    outstring += buff.join('');
} else {
    outstring += "INVALID";
}
outstring;