<%@ tag body-content="empty" %>
<%@ tag description="Returns implicit request varable string as url or query string"  %>

<%@ attribute name="query" description="return query string instead of url (default=no)" %>
<%@ attribute name="var" rtexprvalue="false" required="true"
              description="name of the exported scoped variable to hold the request str" %>
<%@ variable name-from-attribute="var" variable-class="java.lang.Object"
             alias="varName" scope="AT_END"
             description="a string containing a request string" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="baseUrl">
  ${fn:replace(pageContext.request.requestURL, pageContext.request.servletPath, "")}
</c:set>
<c:set var="varName" value="${empty query ? baseUrl : pageContext.request.queryString}"/>
