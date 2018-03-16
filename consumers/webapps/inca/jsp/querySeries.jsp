<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="header.jsp">
  <jsp:param name="title" value="Inca Query Series Page"/>
</jsp:include>

<c:set var="usage">
  Description:  Queries depot for latest instances of specified series.  
  Invokes chosen stylesheet on xml.  Use xsl=none to view xml.

  Usage:  querySeries.jsp?where=config.nickname='n1' AND series.resource='r1',where=config.nickname='n1' AND series.resource='r2'...[&amp;xsl=default.xsl]

  where

  where = WHERE sql clauses for depot's getLatestInstances function.  At least one where
    is required, but the jsp takes multiple where arguments (e.g. results from a checkbox form).

  xsl = an optional xsl stylesheet 
</c:set>

<c:set var="xsl" value="${param.xsl}"/>
<c:set var="where" value="${paramValues.where}"/>
<c:choose>
  <c:when test="${! empty where}">
    <c:set var="xml">
      <inca:query command="latest" params="${fn:join(where, ' OR ')}"/>
    </c:set>
    <inca:printXmlOrHtml xsl="${xsl}" xml="${xml}"/>
  </c:when>
  <c:otherwise>
    <pre><c:out value="${usage}"/></pre>
  </c:otherwise>
</c:choose>

<jsp:include page="footer.jsp"/>
