<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<jsp:useBean id="agentBean" scope="application" 
             class="edu.sdsc.inca.consumer.AgentBean" />

<jsp:include page="header.jsp"/>

<c:set var="usage">
  Description:  Queries depot for series config XML and then sends a
  run now request on the agent.

  Usage: runNow.jsp?configId=c1[&amp;xsl=runNow.xsl]

  where

  xsl = an optional xsl stylesheet [default: runNow.xsl]

  configId = the id of the series config to run now
</c:set>

<%-- Check input parameters and redirect error if incorrect --%>
<c:set var="xsltFile" value="${empty param.xsl ? 'runNow.xsl' : param.xsl}"/>
<c:set var="error"> 
  ${empty param.configId ? 'Missing param configId' : ''}
</c:set>
<%-- Check permissions --%>
<c:import var="adminConfigString" url="/xml/runNow.xml"/>
<x:parse var="adminConfig" xml="${adminConfigString}"/>
<x:set var="allowRunNow" 
       select="string($adminConfig/incaConsumerConfig/allowRunNow)"/>
<c:if test="${allowRunNow != 'true'}">
  <c:set var="error" value="Run now has been disabled."/>
</c:if>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp"> 
    <jsp:param name="msg" value="${error}" /> 
    <jsp:param name="usage" value="${usage}" /> 
  </jsp:forward>
</c:if>

<%-- get the series config xml by id --%>
<c:set var="xml"><inca:query command="hql" prettyprint="true"
              params='select sc from SeriesConfig sc where sc.id = ${param.configId}'/></c:set>
<c:choose><c:when test="${empty xml || ! fn:contains(xml, 'series')}">
  <c:set var="resultXml">
    <error>No series exists with id = ${param.configId}</error>
  </c:set>
</c:when><c:otherwise>
  <c:set var="nickname" value="${fn:substringBefore(fn:substringAfter(xml, '<nickname>'), '</nickname')}"/>
  <%-- Now send the request to the agent --%>
  <jsp:setProperty name="agentBean" property="log"
                   value="Run now request to execute series id=${param.configId} from host=${pageContext.request.remoteHost}"/>
  <c:catch var="agentError">
    <jsp:setProperty name="agentBean" property="runNow" value="${xml}"/>
  </c:catch>
  <c:set var="resultXml">
    <c:choose><c:when test="${empty agentError}">
      <success>${nickname}</success>
    </c:when><c:otherwise>
      <error>Error sending request to the agent ${agentBean.uri}</error>
    </c:otherwise></c:choose>
  </c:set>
</c:otherwise></c:choose>

<%-- Wrap xml and run xslt over it if we have one; otherwise print xml --%>
<c:import var="xslt" url="../xsl/${xsltFile}"/>
<inca:printXmlOrHtml xsl="${xslt}" xml="${resultXml}" 
                  refreshPeriod="${agentBean.cacheReloadPeriod / (60 * 1000)}"/>

<jsp:include page="footer.jsp"/>
