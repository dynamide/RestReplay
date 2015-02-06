function toggleDiv(divid){
    if(document.getElementById(divid).style.display == 'none'){
      document.getElementById(divid).style.display = 'block';
      var linkStyle = document.getElementById(divid+'_link').style;
      linkStyle.backgroundColor = '#ffc';  //highlight when expanded
      linkStyle.border = '4px solid #ffc';
      linkStyle.borderBottomWidth = '1px';
      linkStyle.margin = '0px';
      linkStyle.marginBottom = '0px';
      linkStyle.marginTop = '5px';
      linkStyle.marginLeft = '5px';
    }else{
      document.getElementById(divid).style.display = 'none';
      var linkStyle = document.getElementById(divid+'_link').style;
      linkStyle.backgroundColor = '#e0ffff';  //basic background for unexpanded
      linkStyle.border = '1px solid gray';
      linkStyle.margin = '0px';
      linkStyle.marginBottom = '0px';
      linkStyle.marginLeft = '2px';
    }
}

// usage: <a href="javascript:openAll();">openAll</a>
function openAll( ) {
    var divs = document.getElementsByTagName("div");
    for ( t = 0; t < divs.length; ++t )    {
         var td = divs[t];
         if (td.className == "PAYLOAD"){
             td.style.display = "block";
         }
    }
}

// usage: <a href="javascript:openAll();">closeAll</a>
function closeAll( ){
    var divs = document.getElementsByTagName("div");
    for ( t = 0; t < divs.length; ++t )    {
        var td = divs[t];
        if (td.className == "PAYLOAD"){
            td.style.display = "none";
        }
    }
}

function closeAllHeaders( ){
    var divs = document.getElementsByTagName("span");
    for ( t = 0; t < divs.length; ++t )    {
        var td = divs[t];
        if (td.className == "HEADERBLOCK"){
            td.style.display = "none";
        }
    }
}

function openAllHeaders( ){
    var divs = document.getElementsByTagName("span");
    for ( t = 0; t < divs.length; ++t )    {
        var td = divs[t];
        if (td.className == "HEADERBLOCK"){
           td.style.display = "inline";
        }
    }
}

function closeAllMutations( ){
    var pluses = document.querySelectorAll("span.childstate");
    for ( c = 0; c < pluses.length; ++c ) {
        pluses[c].innerHTML = "show";
    }
    var tables = document.querySelectorAll(".mutation");
    for ( t = 0; t < tables.length; ++t ) {
          tables[t].style.display = 'none';
    }

    var tds = document.querySelectorAll(".mutationTR");
        for ( t = 0; t < tds.length; ++t ) {
              tds[t].style.display = 'none';
        }
    var divs = document.querySelectorAll("DIV.mutation-detail-block");
    for ( t = 0; t < divs.length; ++t ) {
          divs[t].style.display = 'none';
    }

}

function openAllMutations( ){
    var pluses = document.querySelectorAll("span.childstate");
    for ( c = 0; c < pluses.length; ++c ) {
        pluses[c].innerHTML = "hide";
    }

    var tables = document.querySelectorAll(".mutation");
    for ( t = 0; t < tables.length; ++t ) {
          var table = tables[t];
          if (table.tagName && table.tagName == "TABLE"){
              table.style.display = 'table';
          } else {
              table.style.display = 'block';
              //not checking for all of them, I just use tables and divs.
          }
    }

    var tds = document.querySelectorAll(".mutationTR");
    for ( t = 0; t < tds.length; ++t ) {
          tds[t].style.display = 'table-row';
    }

    var divs = document.querySelectorAll("DIV.mutation-detail-block");
    for ( t = 0; t < divs.length; ++t ) {
          divs[t].style.display = 'block';
    }
}

//this function actually hides/shows all mutations.
function hideresults(rowid, obj){
    var els = document.querySelectorAll("#"+rowid);
    for ( t = 0; t < els.length; ++t ) {
        var e = els[t];
        var expanding = true;
        var stateEl = obj.querySelectorAll("span.childstate");
        if (stateEl && stateEl[0]){
            if (stateEl[0].innerHTML == "hide"){
                stateEl[0].innerHTML = "show";
                expanding = false;
            } else {
                stateEl[0].innerHTML = "hide";
                expanding = true;
            }
        }
        var rowdown = e.parentElement.parentElement.parentElement.nextElementSibling;
        while (rowdown){
            if (rowdown && rowdown.querySelectorAll("TD.mutationTD").length>0){
                if (expanding){
                    rowdown.style.display = 'table-row';
                } else {
                    rowdown.style.display = 'none';
                }
                rowdown = rowdown.nextElementSibling;
            } else {
                break;
            }
        }
        var b = document.querySelectorAll("DIV#"+rowid);
        for ( var dt = 0; dt < b.length; ++dt ) {
           var div = b[dt];
            if (expanding){
                div.style.display = 'block';
            } else {
                div.style.display = 'none';
            }
        }
    }
}

function showMutator(sender, id){
    var els = document.querySelectorAll("#"+id);
     if (els && els.length>0){
        var el = els[0];
        el.style.display = 'block';
     }
}

function showBlock(selector){
    var els = document.querySelectorAll(selector);
    for (i = 0; i < els.length; ++i) {
      els[i].style.display = 'block';
    }
}

function hideBlock(selector){
    var els = document.querySelectorAll(selector);
    for (i = 0; i < els.length; ++i) {
      els[i].style.display = 'none';
    }
}

function showEvalReportDetails(){
    showBlock("#LIVE_SECTION .evalReport-level1");
}

function hideEvalReportDetails(){
    hideBlock("#LIVE_SECTION .evalReport-level1");
}

function showFloatingMenu(){
    var el = document.querySelector("#FloatingMenu");
    if (el){
        el.style.display = 'block';
    }
}

function hideFloatingMenu(){
    var el = document.querySelector("#FloatingMenu");
    if (el){
        el.style.display = 'none';
    }
}

