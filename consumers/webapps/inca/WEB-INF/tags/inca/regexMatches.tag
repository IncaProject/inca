<%@ tag import="java.util.regex.Pattern" %>

<%@ tag body-content="empty" %>
<%@ tag description="Test whether a string matches a regular expression." %>

<%@ attribute name="pattern" required="true" description="pattern to match"%>
<%@ attribute name="str" required="true" description="string to search"%>

<%
  String pattern = (String)jspContext.getAttribute( "pattern" );
  String str = (String)jspContext.getAttribute( "str" );
  out.println(Pattern.compile(pattern).matcher(str).matches());
%>
