<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="xsl" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<jsp:useBean id="agentBean" scope="application" 
             class="edu.sdsc.inca.consumer.AgentBean" />

<jsp:include page="header.jsp"/>
<c:set var="xsltFile" value="${empty param.xsl ? 'index.xsl' : param.xsl}"/>
<c:import var="xslt" url="/xsl/${xsltFile}"/>
<c:set var="xml">
  <combo>
    <inca:resources bean="${agentBean}"/>
    <jsp:getProperty name="agentBean" property="suiteNames"/>
  </combo>
</c:set>
<inca:printXmlOrHtml xsl="${xslt}" xml="${xml}"/>
<jsp:include page="footer.jsp"/>
