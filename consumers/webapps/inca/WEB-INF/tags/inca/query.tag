<%@ tag import="edu.sdsc.inca.consumer.DepotQuery" %>
<%@ tag import="edu.sdsc.inca.util.XmlWrapper" %>
<%@ tag import="edu.sdsc.inca.util.StringMethods" %>
<%@ tag import="javax.xml.bind.DatatypeConverter" %>
<%@ tag import="java.util.Date" %>

<%@ tag body-content="empty" %>
<%@ tag description="Query the depot and return the results as a query results xml document" %>

<%@ attribute name="command" required="true"
              description="the depot query command (e.g., queryLatest, queryHql, queryStatusHistory)" %>
<%@ attribute name="params" required="false"
              description="a comma separated list of parameters to pass to the command"%>
<%@ attribute name="prettyprint" required="false" type="java.lang.Boolean"
              description="indent XML if true"  %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  String command = (String)jspContext.getAttribute( "command" );
  String paramString = (String)jspContext.getAttribute( "params" );
  String[] params = new String[0];
  if ( paramString != null ) params = paramString.split( ",\\s*" );
  Object[] paramObjects = new Object[params.length];
  String result = null;
  if ( command.equals("database") ) {
    result = DepotQuery.query( "queryDatabase" );
  } else if ( command.equals("guids") ) {
    result = DepotQuery.query( "queryGuids" );
  } else if ( command.equals("hql") ) {
    result = DepotQuery.query( "queryHql", paramString );
  } else if ( command.equals("instance") ) {
    Date collected = DatatypeConverter.parseDateTime(params[3]).getTime();
    result = DepotQuery.query( "queryInstance", params[0], params[1], params[2], collected );
  } else if ( command.equals("latest")) {
    result = DepotQuery.query( "queryLatest", params[0] );
  } else if ( command.equals("period")) {
    if ( params.length == 2 ) {
      int numDays= Integer.parseInt( params[0].trim() );
      result = DepotQuery.query
        ( "queryPeriod", numDays, params[1] );
    } else {
      Date startDate = StringMethods.convertDateString(params[0], "MMddyy");
      Date endDate = StringMethods.convertDateString(params[1], "MMddyy");
      result = DepotQuery.query
        ( "queryPeriod", startDate, endDate, params[2] );
    }
  } else if ( command.equals("statusHistory") ) {
    if ( params.length == 3 ) {
      int numDays= Integer.parseInt( params[1].trim() );
      result = DepotQuery.query
        ( "queryStatusHistory", params[0], numDays, params[2] );
    } else {
      Date startDate = StringMethods.convertDateString(params[1], "yyyy-MM-dd");
      Date endDate = StringMethods.convertDateString(params[2], "yyyy-MM-dd");
      result = DepotQuery.query
        ( "queryStatusHistory", params[0], startDate, endDate, params[3] );
    }
  } else if ( command.equals("insertKb") ) {
	result = DepotQuery.query( "insertKbArticle", paramString );
  } else if ( command.equals("deleteKb") ) {
	result = DepotQuery.query( "deleteKbArticle", paramString );
  } else {
    result = "<error>Unknown command '" + command + "'</error>";
  }
  Boolean prettyPrint = false;
  if ( jspContext.getAttribute( "prettyprint" ) != null ) {
    prettyPrint = (java.lang.Boolean)jspContext.getAttribute("prettyprint");
  }
  if ( request.getParameter( "prettyprint" ) != null ) {
    prettyPrint = Boolean.parseBoolean( request.getParameter("prettyprint") );
  }
  if ( prettyPrint && result != null ) {
    result = XmlWrapper.prettyPrint( result, "  " );
  }
  out.println( result );
%>
