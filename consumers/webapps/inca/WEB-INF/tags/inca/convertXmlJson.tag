<%@ tag import="org.json.*" %>
<%@ tag import="edu.sdsc.inca.util.XmlWrapper" %>

<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ tag description="Converts XML string to JSON string or vice versa"  %>

<%@ attribute name="xml" required="false" description="XML to convert to JSON" %>
<%@ attribute name="json" required="false" description="JSON to convert to XML" %>
<%@ attribute name="prettyprint" required="false" type="java.lang.Boolean"
              description="indent XML if true"  %>

<%-- Javadocs at http://www.json.org/java/ --%>
<%
String xml = (String)jspContext.getAttribute( "xml" );
if ( xml != null ) {
  org.json.JSONObject j = org.json.XML.toJSONObject(xml);
  out.println( j );
}
String json = (String)jspContext.getAttribute( "json" );
if ( json != null ) {
  org.json.JSONObject j = new org.json.JSONObject(json);
  String x = org.json.XML.toString(j);
  Boolean prettyPrint = false;
  if ( jspContext.getAttribute( "prettyprint" ) != null ) {
    prettyPrint = (java.lang.Boolean)jspContext.getAttribute("prettyprint");
  }  
  if ( request.getParameter( "prettyprint" ) != null ) {
    prettyPrint = Boolean.parseBoolean( request.getParameter("prettyprint") );
  }
  if ( prettyPrint ) {
    x = XmlWrapper.prettyPrint( x, "  " );
  }
  out.println( x );
}
%>
