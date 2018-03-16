function showHideColumn(tableName, colNum)
{
  var table = document.getElementById(tableName);
  var heads = table.getElementsByTagName('th');
  var rows = table.getElementsByTagName('tr');
  var style;

  if (heads[colNum].style.display == 'none')
    style = null;
  else
    style = 'none';

  heads[colNum].style.display = style;

  for (var row = 1 ; row < rows.length ; row++) {
    var cells = rows[row].getElementsByTagName('td');

    cells[colNum].style.display = style;
  }
}
