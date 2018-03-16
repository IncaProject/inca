<%@ tag body-content="empty" %>
<%@ attribute name="fileName" required="true" %>
<%@ attribute name="mode" required="false" %>
<%@ attribute name="var" rtexprvalue="false" required="true" %>
<%@ variable name-from-attribute="var" variable-class="java.lang.String" 
             alias="varName" scope="AT_END" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ tag import="edu.sdsc.inca.util.StringMethods" %>

<c:choose><c:when test="${empty mode || mode == 'string' }">
  <%
    String contents = StringMethods.fileContentsFromClasspath
      ( (String)jspContext.getAttribute( "fileName" ) );
    jspContext.setAttribute( "varName", contents );
  %>
</c:when><c:otherwise>
  <%
    String filePath = StringMethods.findInClasspath
      ( (String)jspContext.getAttribute( "fileName" ) );
    jspContext.setAttribute( "varName", filePath );
  %>
</c:otherwise></c:choose>
