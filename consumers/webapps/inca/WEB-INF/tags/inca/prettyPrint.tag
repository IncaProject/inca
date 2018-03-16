<%@ tag import="edu.sdsc.inca.util.XmlWrapper" %>

<%@ tag body-content="empty" %>
<%@ tag description="Print the xml using pretty print indent" %>

<%@ attribute name="xml" required="true" type="java.lang.String"
              description="an xml document" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  String xml = (String)jspContext.getAttribute( "xml" );
  jspContext.setAttribute( "xml", XmlWrapper.prettyPrint( xml, "  " ) );
%>
<pre>
<c:out value="${xml}" escapeXml="true"/>
</pre>
