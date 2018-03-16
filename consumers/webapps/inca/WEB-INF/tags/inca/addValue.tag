<%@ tag import="edu.sdsc.inca.consumer.CategoryBean" %>

<%@ tag body-content="empty" %>
<%@ tag description="Add a value to a category dataset object" %>

<%@ attribute name="dataset" required="true" 
              description="a category dataset name" %>
<%@ attribute name="value" required="true" type="java.lang.Double"
              description="the value to add to the dataset" %>
<%@ attribute name="row" required="true" type="java.lang.String"
              description="the row to add the value to" %>
<%@ attribute name="col" required="true" type="java.lang.String"
              description="the column to add the value to" %>
<%@ attribute name="tooltip" required="false" type="java.lang.String"
              description="the text for the element mouseover" %>
<%@ attribute name="url" required="false" type="java.lang.String"
              description="the url if the value is clicked" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<% 
  String datasetName = (String)jspContext.getAttribute( "dataset" ); 
  CategoryBean data = 
    (CategoryBean)request.getAttribute( datasetName ); 
  Double value = (Double)jspContext.getAttribute( "value" ); 
  String row = (String)jspContext.getAttribute( "row" ); 
  String column = (String)jspContext.getAttribute( "col" ); 
  data.addValue( value, row, column );
  Object tooltip = jspContext.getAttribute( "tooltip" ); 
  if ( tooltip != null ) {
    data.addTooltip
      ( (String)tooltip, data.getRowIndex(row), data.getColumnIndex(column) );
  }
  Object url = jspContext.getAttribute( "url" ); 
  if ( url!= null ) {
    data.addLink
      ( (String)url, data.getRowIndex(row), data.getColumnIndex(column) );
  }
%>
