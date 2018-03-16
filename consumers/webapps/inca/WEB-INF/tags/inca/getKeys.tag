<%@ tag import="java.util.AbstractMap" %>

<%@ tag body-content="empty" %>
<%@ tag description="Retrieve an array of key values from an object if it is an instance of java.util.AbstractMap" %>

<%@ attribute name="map" required="true" type="java.lang.Object"
              description="Either an AbstractMap object or an array of strings" %>
<%@ attribute name="var" rtexprvalue="false" required="true" 
              description="name of the exported scoped variable to hold the array of strings" %>
<%@ variable name-from-attribute="var" variable-class="java.lang.Object"
             alias="varName" scope="AT_END" 
             description="An string array of key values or the original input" %>


<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  if ( AbstractMap.class.isInstance(jspContext.getAttribute("map")) ) {
    java.util.AbstractMap map = 
      (java.util.AbstractMap)jspContext.getAttribute("map");
    String[] keyArray = new String[map.size()];
    int i = 0;
    for( Object o : map.keySet() ) {
      keyArray[i] = (String)o;
      i++;
    }
    jspContext.setAttribute( "varName", keyArray );
  } else {
    jspContext.setAttribute( "varName", jspContext.getAttribute("map") );
  }
%>
