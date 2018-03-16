function checkAll(field){
  for (i = 0; i < field.length; i++)
    field[i].checked = true ;
  if (!field.length)
    field.checked = true;
}

function uncheckAll(field){
  for (i = 0; i < field.length; i++)
    field[i].checked = false ;
  if (!field.length)
    field.checked = false;
}

function flip(form, tableId, match, cells, checkVal){
  match = match.replace(/\(/, "\\(");
  match = match.replace(/\)/, "\\)");
  end = '$';
  if (cells == 'COL'){
    end = '-ROW-.*$';
  }
  length = form.elements.length;
  re = new RegExp('^-TABLEID-' + tableId + '.*-' + cells + '-' + match + end);
  for(i = 0; i < length; i++){
    elm = form.elements[i];
    if (elm.type == 'checkbox' && re.test(elm.id)){
      elm.checked = checkVal;
    }
  }
}
