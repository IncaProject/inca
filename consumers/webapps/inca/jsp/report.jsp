<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>

<%-- Check inputs and redirect to error if incorrect --%>
<c:set var="usage">  Description: Creates graphs for series history specified.

report.jsp?xml=statusReport.xml&amp;startDate=050710&amp;endDate=051210

  where

  xml = an xml file describing graphs to be printed
  startDate = a date in mmddyy format
  endDate = a date in mmddyy format
</c:set>

<inca:date var="two" add="-14"/>
<inca:date var="now" add="1"/>

<c:set var="xmlFile" value= "${empty param.xml ? 'statusReport.xml' : param.xml}"/>
<c:import var="xml" url="/xml/${xmlFile}"/>
<c:if test="${empty xml}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="No xml document found in ${xmlFile}" />
  </jsp:forward>
</c:if>

<jsp:include page="header.jsp"/>

<x:parse var="statusReport" xml="${xml}"/>
<x:set var="height" select="string($statusReport/*/height)"/>
<x:set var="width" select="string($statusReport/*/width)"/>
<x:set var="pageDescription" select="string($statusReport/*/pageDescription)"/>
<x:set var="pageTitle" select="string($statusReport/*/pageTitle)"/>
<x:set var="showmouseovers" select="string($statusReport/*/mouseovers)"/>
<x:set var="showlinks" select="string($statusReport/*/links)"/>

<h1>${pageTitle}</h1>
<p>${pageDescription}</p>

<c:if test="${empty param.noRegraph || param.noRegraph == 'false'}">
<center>
<form method="get" action="report.jsp" name="form"
        onsubmit="return validate(form);">

  <table class="subheader"
    <tr><td>start date:</td><td>
      <input name="startDate" type="text" size="6"
                   value="${param.startDate}"/> (MMddyy format, e.g. "093007")
    </td></tr>
    <tr><td>end date:</td><td>
      <input name="endDate" type="text" size="6"
                   value="${param.endDate}"/> (MMddyy format, e.g. "093007")
    </td></tr>
    <tr><td colspan="2" align="center">
      <input name="xml" type="hidden" value="${param.xml}"/>
      <input type="submit" name="submit" value="re-graph"/>
    </td></tr>
  </table>
</form>
</center>
</c:if>

<table>
<tr valign="top">
<x:forEach var="graph" select="$statusReport//graph" > 

  <x:set var="seriesNode" select="$graph/series"/>
  <inca:getXpathValues xpath="${seriesNode}" var="series"/>
  <x:set var="metric" select="string($graph/metric)"/>
  <x:set var="chart" select="string($graph/chart)"/>

  <%-- Query data and if not results, forward to error page --%>
  <c:set var="query">
    <c:out value="${empty param.startDate ? two : param.startDate}"/>,
    <c:out value="${empty param.endDate ? now : param.endDate}"/>,
    <c:forEach items="${series}" var="seriesDesc">
      <inca:split var="seriesParts" content="${seriesDesc}" delim=","/>
      <c:set var="targetClause">AND config.series.targetHostname = '${seriesParts[2]}'</c:set>
      <c:set var="targetClause" value="${seriesParts[2] == '' ? '' : targetClause}"/>
      (config.nickname='<c:out value="${seriesParts[0]}"/>' AND 
      config.series.resource='<c:out value="${seriesParts[1]}"/>' <c:out value="${targetClause}" escapeXml="false"/> ) OR</c:forEach></c:set>

  <c:set var="xmlResults"><inca:query command="period" params="${fn:substring(query,0,fn:length(query)-3)}"/></c:set>

  <c:if test="${xmlResults !=''}">
      <c:choose><c:when test="${empty metric}">
        <x:set var="title" select="string($graph/title)"/>
        <c:if test="${title !=''}">
          <td colspan="3" class="header"><x:out select="$title" /></td>
        </c:if>
        </tr><tr><td>
        <table cellpadding="10">
        <tr><td valign="top">
          <inca:historyGraph xml="${xmlResults}" series="${series}" 
                         showlinks="${showlinks}" 
                         showmouseovers="${showmouseovers}" 
                         width="${width}" bgcolor="${param.bgcolor}" 
                         showlegend="${param.legend}" 
                         legendanchor="${legendAnchor}"/>
        </td><td align="left" valign="top" class="clear">
          <inca:errorGraph xml="${xmlResults}" series="${series}" 
                     showmouseovers="${showmouseovers}" height="${height}"
                     width="${width}" bgcolor="${param.bgcolor}"
                     showlegend="${param.legend}" legendanchor="${legendAnchor}"/>
        </td></tr></table>
        </td></tr>
      </c:when><c:otherwise>
        <x:set var="unitPattern" select="string($statusReport/*/unitPattern)"/>
        <td>
          <x:set var="titlePrefix" select="string($graph/titlePrefix)"/>
          <x:set var="titleSuffix" select="string($graph/titleSuffix)"/>
          <inca:chartPlotMetric xml="${xmlResults}" series="${series}" 
                         titlePrefix="${titlePrefix}" 
                         titleSuffix="${titleSuffix}" 
                         showlinks="${showlinks}"                          
                         showmouseovers="${showmouseovers}" 
                         width="${width}" height="${height}"
                         bgcolor="${param.bgcolor}" chart="${chart}"
                         metric="${metric}" unitPattern="${unitPattern}"
                         showlegend="${param.legend}" 
                         legendanchor="${legendAnchor}"/>
        </td>
      </c:otherwise></c:choose> 
  </c:if>
</x:forEach>
</tr></table>

<jsp:include page="footer.jsp"/>
