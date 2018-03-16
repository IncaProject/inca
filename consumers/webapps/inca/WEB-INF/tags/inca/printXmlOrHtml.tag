<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ tag description="Prints xml or transforms to html if xsl supplied and not debug"  %>

<%@ attribute name="xml" required="true" description="xml to print or transform" %>
<%@ attribute name="xsl" required="true" description="xsl stylesheet" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<c:set var="escapeXml" value="${empty param.escapeXml ? 'true' : param.escapeXml}"/>
<c:choose>
  <c:when test="${!empty xsl and empty param.debug}">
    <x:transform doc ="${xml}" xslt="${xsl}">
      <c:forEach items="${dynattrs}" var="xslParam">
        <x:param name="${xslParam.key}" value="${xslParam.value}"/>
      </c:forEach>
    </x:transform>
  </c:when>
  <c:otherwise>
    <c:choose><c:when test="${escapeXml == 'false'}">
      <c:out value="${xml}" escapeXml="false"/>
    </c:when><c:otherwise>
      <pre><c:out value="${xml}" escapeXml="true"/></pre>
    </c:otherwise></c:choose>
  </c:otherwise>
</c:choose>
