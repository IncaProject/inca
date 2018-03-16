<%@ tag import="edu.sdsc.inca.consumer.CategoryBean" %>

<%@ tag description="Creates a new CategoryBean object and exports it to the specified var name"%>

<%@ attribute name="var" rtexprvalue="true" required="true" 
              description="name of the exported scoped variable to hold the new CategoryBean object"%>

<%
  String varName = (String)jspContext.getAttribute( "var" );
  try {
    CategoryBean cat = new CategoryBean( varName );
    jspContext.setAttribute( varName, cat , PageContext.REQUEST_SCOPE );
  } catch ( Exception e ) {
    out.println( "Problem creating category bean: " + e );
  }
%>
<jsp:doBody/>
