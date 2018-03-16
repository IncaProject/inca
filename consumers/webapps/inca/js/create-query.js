function validate(form){
  var checked = 0;
  var where = new Array();
  for(i=0; i<form.series.length; i++){
    var mySeries = form.series[i];
    if(mySeries.checked==true){
      checked++;
      if (/,/.test(mySeries.value)){
        temp = mySeries.value.split(",");
        // format for depot query is (config.nickname='n1' AND series.resource='r1')
	where.push(" (config.nickname='" + temp[0] + "' AND series.resource='" + temp[1] + "') ");
      }
    }
  }
  form.qparams.value = where.join('OR');

  var qname = form.qname.value;
  var err = "";
  if(!qname){
    err += "\nEnter a stored query name\n";
  }
  if(checked < 1){
    err += "\nSelect series to graph\n";
  }
  if(err != ""){
    alert(err);
    return false;
  }
  return true;
}
