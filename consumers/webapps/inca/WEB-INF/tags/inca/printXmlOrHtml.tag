<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ tag description="Prints xml or transforms to html if xsl supplied and not debug"  %>

<%@ attribute name="xml" required="true" description="xml to print or transform" %>
<%@ attribute name="xsl" required="true" description="xsl stylesheet" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="incaXml" uri="/WEB-INF/inca.tld" %>

<c:set var="escapeXml" value="${empty param.escapeXml ? 'true' : param.escapeXml}"/>
<c:choose>
  <c:when test="${!empty xsl and empty param.debug}">
    <incaXml:transform doc ="${xml}" xslt="${xsl}">
      <c:forEach items="${dynattrs}" var="xslParam">
        <incaXml:param name="${xslParam.key}" value="${xslParam.value}"/>
      </c:forEach>
    </incaXml:transform>
  </c:when>
  <c:otherwise>
    <c:choose><c:when test="${escapeXml == 'false'}">
      <c:out value="${xml}" escapeXml="false"/>
    </c:when><c:otherwise>
      <pre><c:out value="${xml}" escapeXml="true"/></pre>
    </c:otherwise></c:choose>
  </c:otherwise>
</c:choose>
