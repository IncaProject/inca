<%@ tag import="edu.sdsc.inca.consumer.TimeSeriesBean" %>

<%@ tag body-content="empty" %>
<%@ tag description="Create a TimeSeriesCollection using the xpath values from the XML document" %>

<%@ attribute name="failedValue" required="false"  type="java.lang.Float"
              description="Specify value for metric when report fails.  By default failures are not plotted." %>
<%@ attribute name="label" required="false" 
              description="A label for the time series" %>
<%@ attribute name="linkXpath" required="true" 
              description="a relative xpath to use for the link" %>
<%@ attribute name="timestampXpath" required="true" 
              description="a realtive xpath to use for the timestamp" %>
<%@ attribute name="tooltipXpath" required="true" 
              description="a relative xpath to use for the tooltip" %>
<%@ attribute name="valueXpath" required="false" 
              description="a relative xpath to use for the value" %>
<%@ attribute name="var" rtexprvalue="true" required="true" 
              description="name of the exported scoped variable to hold the TimeSeriesCollection object" %>
<%@ attribute name="xpath" required="true" description="absolute xpath to datapoint" %>
<%@ attribute name="xml" required="true" 
              description="string containing xml exit status values will be extracted from"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  String varName = (String)jspContext.getAttribute("var");
  TimeSeriesBean ts; 
  if ( request.getAttribute(varName) == null ) {
    ts = new TimeSeriesBean( varName );
  } else {
    ts = (TimeSeriesBean)request.getAttribute( varName );
  }
  if ( jspContext.getAttribute( "failedValue" ) != null ) {
    ts.setFailedValue( (Float)jspContext.getAttribute("failedValue") );
  }
  ts.addSeries( (String)jspContext.getAttribute("xml"),
                (String)jspContext.getAttribute("xpath"),
                (String)jspContext.getAttribute("timestampXpath"),
                (String)jspContext.getAttribute("valueXpath"),
                (String)jspContext.getAttribute("linkXpath"), 
                (String)jspContext.getAttribute("tooltipXpath"),
                (String)jspContext.getAttribute("label") );
  jspContext.setAttribute( varName, ts, PageContext.REQUEST_SCOPE );
%>
