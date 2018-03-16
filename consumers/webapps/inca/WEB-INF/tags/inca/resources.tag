<%@ tag import="edu.sdsc.inca.consumer.AgentBean" %>
<%@ tag import="edu.sdsc.inca.util.XmlWrapper" %>

<%@ tag body-content="empty" %>
<%@ tag description="Get the agent's resources document" %>

<%@ attribute name="bean" required="true" type="edu.sdsc.inca.consumer.AgentBean"
              description="the agent bean for the application" %>
<%@ attribute name="macros" required="false" 
              description="a comma separated list of macro names to show in the resources" %>
<%@ attribute name="name" required="false" description="a resource group name"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  AgentBean resources = (AgentBean)jspContext.getAttribute( "bean" );
  if ( resources != null ) {
    String resource = (String)jspContext.getAttribute( "name" );
    String macroString = (String)jspContext.getAttribute( "macros" );
    String[] macros = new String[0];
    if ( macroString != null ) macros = macroString.split( ",\\s*" );
    out.println( XmlWrapper.prettyPrint
      (resources.filter(resource, macros).getResourceConfig().xmlText(), "  ") );
  }
%>
