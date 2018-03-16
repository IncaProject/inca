function validate(form){
  var err = "";
  var checked = 0;
  if (form.series){
    for(i=0; i<form.series.length; i++){
      if(form.series[i].checked==true){
        checked++;
      }
    }
    if (!form.series.length && form.series.checked==true){
      checked++;
    }
  }

  if(checked < 1){
    err += "\nSelect series to graph\n";
  }

  var validdate  = /(^\d{6}$)/;
  var mindate = "";
  if (form.startDate){
    mindate = form.startDate.value;
  } else if (form.xmin){
    mindate = form.xmin.value;
  }
  if(!validdate.test(mindate) && mindate != ""){
    err += "\nStart date is invalid\n";
  }

  var maxdate = "";
  if (form.endDate){
    maxdate = form.endDate.value;
  } else if (form.xmax){
    maxdate = form.xmax.value;
  }
  if(!validdate.test(maxdate) && maxdate != ""){
    err += "\nEnd date is invalid\n";
  }

  var validcolor  = /(^#.{6}$)/;
  var color = form.bgcolor.value;
  if(!validcolor.test(color) && color != ""){
    err += "\nBackground color is invalid\n";
  }

  var validsize  = /(^(\d+|)$)/;
  var width = form.width.value;
  if(!validsize.test(width)){
    err += "\nWidth is invalid\n";
  }

  var height = form.height.value;
  if(!validsize.test(height)){
    err += "\nHeight is invalid\n";
  }

  if(err != ""){
    alert(err);
    return false;
  }
  return true;
}
