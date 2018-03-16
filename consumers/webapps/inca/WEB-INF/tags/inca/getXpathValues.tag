<%@ tag body-content="empty" %>
<%@ tag description="Retrieves an array of string values from an xpath" %>

<%@ attribute name="distinct" required="optional" 
              type="java.lang.Boolean"
              description="select only distinct values (default: false)" %>

<%@ attribute name="xpath" required="true" 
              type="java.util.Vector"
              description="an array of string nodes extracted from an XPATH expression" %>
<%@ attribute name="var" rtexprvalue="false" required="true" 
              description="name of the exported scoped variable to hold the string array" %>
<%@ variable name-from-attribute="var" variable-class="java.lang.Object"
             alias="varName" scope="AT_END" 
             description="a string array containing the xpath values" %>

<%@ tag import="java.util.HashMap" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<x:set var="count" select="count($xpath)" />
<%
  Double seriesCount = (Double)jspContext.getAttribute("count");
  String[] values = new String[seriesCount.intValue()];
  HashMap<String,String> distinct = new HashMap<String,String>();
%>
<x:forEach var="node" select="$xpath" varStatus="i">
  <x:set var="nodeValue" select="string($node)" />
  <c:set var="nodeValueString" value="${nodeValue}"/>
  <c:set var="loopCount" value="${i.count}"/>
  <%
    Integer i = (Integer)jspContext.getAttribute("loopCount");
    values[i-1] = (String)jspContext.getAttribute("nodeValueString");
    distinct.put( (String)jspContext.getAttribute("nodeValueString"), "" );
  %>
</x:forEach>
<%
  if ( jspContext.getAttribute("distinct") != null && 
       ((Boolean)jspContext.getAttribute("distinct")) ) {
    values = distinct.keySet().toArray( new String[ distinct.size() ] );
  }
  jspContext.setAttribute( "varName", values );
%>
