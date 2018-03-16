<%@ tag import="edu.sdsc.inca.consumer.DistributionBean" %>

<%@ tag description="Creates a new DistributionBean object and exports it to the specified var name"%>
<%@ tag body-content="scriptless" %>

<%@ attribute name="xml" required="true" description="a string containing the xml the bean should search for data in"%>
<%@ attribute name="var" rtexprvalue="true" required="true" description="name of the exported scoped variable to hold the new DistributionBean object"%>

<%
  String varName = (String)jspContext.getAttribute( "var" );
  String xml = (String)jspContext.getAttribute( "xml" );
  try {
    DistributionBean dist = new DistributionBean( varName, xml );
    jspContext.setAttribute( varName, dist , PageContext.REQUEST_SCOPE );
  } catch ( Exception e ) {
    out.println( "Problem creating distribution bean: " + e );
  }
%>
<jsp:doBody/>
