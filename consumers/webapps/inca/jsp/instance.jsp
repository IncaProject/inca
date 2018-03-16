<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<c:if test="${empty param.printXML}">
  <jsp:include page="header.jsp"/>
</c:if>

<c:set var="usage">
  Description:  Queries depot for report instance and invokes the specified
  xsl stylesheet on it.

  Usage: instance.jsp?nickname=n1&amp;resource=r1&amp;target=t1&amp;collected=c1[&amp;xsl=instance.xsl]

  where

  xsl = an optional xsl stylesheet [default: instance.xsl]

  nickname = the series nickname for the report instance

  resource = the hostname the instance was executed on

  target = the hostname of the execution target

  collected = the GMT timestamp for the instance execution time
</c:set>

<%-- Check input parameters and redirect error if incorrect --%>
<c:set var="xsltFile" value="${empty param.xsl ? 'instance.xsl' : param.xsl}"/>
<c:set var="error">
  ${empty param.nickname ? 'Missing param nickname' : '' }
  ${empty param.resource ? 'Missing param resource' : ''}
  ${empty param.collected ? 'Missing param collected' : '' }
</c:set>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="${error}" />
    <jsp:param name="usage" value="${usage}" />
  </jsp:forward>
</c:if>

<%-- Wrap xml and run xslt over it if we have one; otherwise print xml --%>
<c:import var="xslt" url="../xsl/${xsltFile}"/>
<c:set var="xml">
  <instance xmlns="http://www.w3.org/1999/xhtml">
  <inca:query command="instance"
              params='${param.nickname},${param.resource},${param.target},${param.collected}'/>
  </instance>
</c:set>
<inca:date var="week" add="-7"/>
<inca:date var="month" add="-30"/>
<inca:date var="quarter" add="-90"/>
<inca:date var="year" add="-365"/>
<c:import var="runNowConfigString" url="/xml/runNow.xml"/>
<x:parse var="runNowConfig" xml="${runNowConfigString}"/>
<x:set var="allowRunNow" select="string($runNowConfig/incaConsumerConfig/allowRunNow)"/>
<c:import var="kbConfigString" url="/xml/kb.xml"/>
<x:parse var="kbConfig" xml="${kbConfigString}"/>
<x:set var="allowKb" select="$kbConfig/kb/enable"/>
<x:set var="kbSearch" select="$kbConfig/kb/searchString"/>
<x:set var="kbSubmit" select="$kbConfig/kb/submitString"/>
<c:if test="${empty param.printXML}">
  <inca:printXmlOrHtml xsl="${xslt}" xml="${xml}"
      printRunNow="${allowRunNow}"
      printKb="${allowKb}"
      kbSearch="${kbSearch}"
      kbSubmit="${kbSubmit}"
      week="${week}"
      month="${month}"
      quarter="${quarter}"
      year="${year}"/>

  <jsp:include page="footer.jsp"/>
</c:if>
<c:if test="${!empty param.printXML}">${xml}</c:if>
