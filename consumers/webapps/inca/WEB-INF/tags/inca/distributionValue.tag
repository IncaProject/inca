<%@ tag import="edu.sdsc.inca.consumer.DistributionBean" %>
<%@ tag import="java.lang.Long" %>

<%@ tag body-content="empty" %>
<%@ tag description="Retrieve a count value from the distribution bean" %>

<%@ attribute name="bean" type="edu.sdsc.inca.consumer.DistributionBean" 
              required="true" 
              description="a bean containing a CategoryDataset representing a distribution" %>
<%@ attribute name="col" type="java.lang.Long" required="false" 
              description="the column index of the count value" %>
<%@ attribute name="colValue" required="false" 
              description="a value in the distribution" %>
<%@ attribute name="row" type="java.lang.Long" required="false" 
              description="the row index of the count value" %>
<%@ attribute name="rowValue" required="false" 
              description="the row description of the count value" %>
<%@ attribute name="var" rtexprvalue="false" required="true" 
              description="name of the exported scoped variable to hold the count value"%> 
<%@ variable name-from-attribute="var" variable-class="java.lang.Number" 
             alias="varName" scope="AT_END" description="the count value"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  DistributionBean distBean = (DistributionBean)jspContext.getAttribute( "bean" );
  long row, col; 
  if ( jspContext.getAttribute( "col" ) == null ) {
    String colValue = (String)jspContext.getAttribute( "colValue" );
    col = distBean.getColKeyIndex( colValue );
  } else {
    col = (Long)jspContext.getAttribute( "col" );
  }
  if ( jspContext.getAttribute( "row" ) == null ) {
    String rowValue = (String)jspContext.getAttribute( "rowValue" );
    row = distBean.getRowKeyIndex( rowValue );
  } else {
    row = (Long)jspContext.getAttribute( "row" );
  }
  java.lang.Number count = 0;
  if ( col >= 0 ) count = distBean.getValue( (int)row, (int)col );
  jspContext.setAttribute( "varName", count );
%>

