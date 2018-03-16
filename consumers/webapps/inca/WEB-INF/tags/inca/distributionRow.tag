<%@ tag import="edu.sdsc.inca.consumer.DistributionBean" %>

<%@ tag body-content="empty" %>
<%@ tag description="Adds a row to the specified DistributionBean" %>

<%@ attribute name="bean" required="true" 
              type="edu.sdsc.inca.consumer.DistributionBean"
              description="a bean containing a CategoryDataset representing a distribution"%>
<%@ attribute name="xpath" required="true" description="a string containing an xpath to row values that will be added to the specified bean" %>
<%@ attribute name="rowkey" required="true" description="a string containing a description for the row"%>

<%@ attribute name="statusAsFloat" required="false" type="java.lang.Boolean" description="if true assumes xpath points to a graph instance and we want the status of the instance returned as a float"%>

<%
  String xpath = (String)jspContext.getAttribute( "xpath" );
  String rowKey = (String)jspContext.getAttribute( "rowkey" );
  boolean statusAsFloat = false;
  if ( jspContext.getAttribute( "statusAsFloat" ) != null ) {
    statusAsFloat = (Boolean)jspContext.getAttribute( "statusAsFloat" );
  }
  DistributionBean dist = (DistributionBean)jspContext.getAttribute( "bean" );
  dist.add( xpath, rowKey, statusAsFloat );
%>
