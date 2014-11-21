function toggleDiv(divid){
    if(document.getElementById(divid).style.display == 'none'){
      document.getElementById(divid).style.display = 'block';
    }else{
      document.getElementById(divid).style.display = 'none';
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
}

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
    }
}

function showMutator(sender, id){
    var els = document.querySelectorAll("#"+id);
     if (els && els.length>0){
        var el = els[0];
        el.style.display = 'block';
     }

}

