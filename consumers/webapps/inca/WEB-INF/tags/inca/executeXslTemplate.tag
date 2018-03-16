<%@ tag import="java.util.AbstractMap" %>

<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ tag description="Execute the specified xsl template" %>

<%@ attribute name="name" required="true" 
              description="name of xsl stylesheet (&lt;name&gt;.xsl should exist)"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<c:import var="xslt" url="/xsl/${name}.xsl"/>
<c:set var="xslt">
  <xsl:stylesheet version="2.0"
                  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                  xmlns="http://www.w3.org/1999/xhtml">

    <!-- override via a parameter -->
    <%
      String xslName = (String)jspContext.getAttribute( "name" );
      String paramOverride = "override" + xslName;
      if ( request.getParameter(paramOverride) != null ) {
        jspContext.setAttribute( "name", request.getParameter(paramOverride) );
      } 
    %>

    <xsl:include href="/xsl/${name}.xsl"/>
    <xsl:template match="/">
      <xsl:call-template name="${name}"/>
    </xsl:template>
  </xsl:stylesheet>
</c:set>
<x:transform xslt="${xslt}">
 <sometag/> <%-- any tag to run template over --%>
 <c:forEach items="${dynattrs}" var="xslParam">
   <x:param name="${xslParam.key}" value="${xslParam.value}"/>
 </c:forEach>
</x:transform>
