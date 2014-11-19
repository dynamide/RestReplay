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
        pluses[c].innerHTML = "+";
    }
    var divs = document.getElementsByTagName("td");
    for ( t = 0; t < divs.length; ++t )    {
        var td = divs[t];
        if (td.className == "mutation"){
            td.parentNode.style.display = "none";
        }
    }
}

function openAllMutations( ){
    var pluses = document.querySelectorAll("span.childstate");
    for ( c = 0; c < pluses.length; ++c ) {
        pluses[c].innerHTML = "-";
    }

    var divs = document.getElementsByTagName("td");
    for ( t = 0; t < divs.length; ++t )    {
        var td = divs[t];
        if (td.className == "mutation"){
           td.parentNode.style.display = "table-row";
        }
    }
}

//a duplicate that searches by class.
function closeAllMutationElements(){
    var els = document.querySelectorAll(".child-results-summary");
    for ( t = 0; t < els.length; ++t ) {
      els[t].style.display = 'none';
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
            if (rowdown && rowdown.querySelectorAll("TD.mutation").length>0){
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

