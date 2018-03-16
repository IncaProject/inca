<%@ tag import="edu.sdsc.inca.consumer.DepotBean" %>
<%@ tag import="edu.sdsc.inca.util.XmlWrapper" %>

<%@ tag body-content="empty" %>
<%@ tag description="Handles actions to the query cache" %>

<%@ attribute name="action" required="false" 
              description="If empty queries the cache, 'Add' to add a cached query, or 'Delete' to delete a cached query" %>
<%@ attribute name="bean" required="true" type="edu.sdsc.inca.consumer.DepotBean"
              description="a bean containing a query cache" %>
<%@ attribute name="command" required="false" 
              description="the depot query command (e.g., querySuite, queryHql, querySeries)"%>
<%@ attribute name="name" required="false" description="a cached query name" %>
<%@ attribute name="params" required="false" 
              description="a comma separated list of parameters to pass to the command" %>
<%@ attribute name="period" required="false" type="java.lang.Integer"
              description="the number of seconds to wait before refreshing cached query results" %>
<%@ attribute name="reloadAt" required="false" type="java.lang.String"
              description="the reload start time (WW:HH:MM)" %>
<%@ attribute name="suite" required="false" 
              description="a suite name which is being cached" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="reload" value="${empty reload || reload == '*:*:*' ? '*' : reload}"/>
<%
  String actionString = "";
  if ( jspContext.getAttribute("action") != null ) {
    actionString = (String)jspContext.getAttribute( "action" );
  }
  DepotBean depotBean = (DepotBean)jspContext.getAttribute( "bean" );
  String qName = (String)jspContext.getAttribute( "name" );

  // optional
  String paramString = (String)jspContext.getAttribute( "params" );
  String[] qParams = new String[0];
  if ( paramString != null ) qParams = paramString.split( ",\\s*" );
  int cachePeriod = 0;
  if ( jspContext.getAttribute ("period") != null ){
    cachePeriod = (Integer)jspContext.getAttribute ( "period" );
  }
  String reloadAt = (String)jspContext.getAttribute( "reloadAt" );
  String qType = (String)jspContext.getAttribute( "command" );
  if ( actionString.equals("Add") ) {
    if ( ! depotBean.hasQuery(qName) ) {
      depotBean.add( cachePeriod, reloadAt, qName, qType, qParams );
    }
  } else if ( actionString.equals("Delete") ) {
    depotBean.delete( qName );
  } else if ( actionString.equals("Refresh") ) {
    depotBean.refresh( qName );
  } else {
    if ( qName == null || qName.equals("") ) {
      qName = DepotBean.getCacheName
        ( (String)jspContext.getAttribute( "suite" ) ); 
    }
    String result = depotBean.getQueryResult(qName);
    Boolean prettyPrint = false;  
    if ( request.getParameter( "prettyprint" ) != null ) {
      prettyPrint = Boolean.parseBoolean(request.getParameter("prettyprint"));
    } 
    if ( prettyPrint && result != null ) {
      result = XmlWrapper.prettyPrint( result, "  " );
    } 
    out.println( result );
  }
%>
