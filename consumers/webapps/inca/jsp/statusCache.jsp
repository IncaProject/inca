<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<jsp:useBean id="agentBean" scope="application" 
             class="edu.sdsc.inca.consumer.AgentBean" />
<jsp:useBean id="depotBean" scope="application" 
             class="edu.sdsc.inca.consumer.DepotBean" />

<jsp:include page="header.jsp"/>

<c:set var="usage">
  Description:  

  where

  xsl = an optional xsl stylesheet [default: default.xsl]

  cacheName = a name of a cached query
</c:set>

<%-- Check input parameters and redirect error if incorrect --%>
<c:set var="xsltFile" value="${empty param.xsl ? 'default.xsl' : param.xsl}"/>
<c:set var="cacheName" 
       value="${empty param.cacheName? 'incaQueryStatus' : param.cacheName}"/>

<%-- Wrap xml and run xslt over it if we have one; otherwise print xml --%>
<c:import var="xslt" url="/xsl/${xsltFile}"/>
<c:import var="periodXml" url="/xml/weekSummary.xml"/>
<c:set var="xml">
  <combo>
  ${periodXml}
  <inca:queryCache bean="${depotBean}" name='${cacheName}'/>
  </combo>
</c:set>
<inca:printXmlOrHtml xsl="${xslt}" xml="${xml}"/>
<jsp:include page="footer.jsp"/>
