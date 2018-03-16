<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ page  import="java.net.URLDecoder" %>
<%@ page  import="java.net.URLEncoder" %>
<%@ page trimDirectiveWhitespaces="true"%>

<jsp:useBean id="agentBean" scope="application"
             class="edu.sdsc.inca.consumer.AgentBean" />
<jsp:useBean id="depotBean" scope="application"
             class="edu.sdsc.inca.consumer.DepotBean" />

<%-- Find out the rest prefix configured in web.xml and take it off the url --%>
<c:set var="uri" value="${pageContext.request.requestURI}"/>
<c:set var="request" value="${fn:substringAfter(uri, '/inca/')}"/>
<%--
<%
  pageContext.setAttribute( "request",
    URLDecoder.decode( (String)pageContext.getAttribute( "request" ) ) );
%>
--%>


<%-- Check inputs --%>
<c:set var="requestParts" value="${fn:split(request,'/')}"/>
<c:set var="nRequest" value="${fn:toUpperCase(request)}"/>
<c:set var="hasTimestamp">
  <inca:regexMatches pattern="\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3}-\d{2}:\d{2}" str="${requestParts[5]}"/>
</c:set>
<c:set var="hasStartDate">
  <inca:regexMatches pattern="\d{6}" str="${requestParts[5]}"/>
</c:set>
<c:set var="hasEndDate">
  <inca:regexMatches pattern="\d{6}" str="${requestParts[6]}"/>
</c:set>
<c:if test='${!(fn:startsWith(nRequest,"CSV") or fn:startsWith(nRequest,"XML") or fn:startsWith(nRequest,"HTML") or fn:startsWith(nRequest,"JSON"))
              or fn:length(requestParts)<3 or fn:length(requestParts)>7
              or requestParts[1] != initParam.restId
              or (!empty requestParts[5] and requestParts[5]!="week"
                  and requestParts[5]!="month" and requestParts[5]!="quarter"
                  and requestParts[5]!="year" and !hasTimestamp and !hasStartDate)
              or (!empty requestParts[6] and (!hasStartDate or !hasEndDate))}'>

  <c:set var="error"><pre>${uri} </pre>

  <p>does not follow the following pattern:</p>

  <pre>CSV|XML|JSON|HTML/${initParam.restId}/&lt;suite&gt;/[&lt;resource&gt;[/&lt;seriesNickname&gt;[/&lt;timestamp&gt;|week|month|quarter|year|&lt;MMDDYY&gt;[/&lt;MMDDYY&gt;]]]</pre>
  </c:set>
  <jsp:forward page="/jsp/error.jsp">
    <jsp:param name="msg" value="incorrect rest url" />
    <jsp:param name="usage" value="${error}" />
  </jsp:forward>
</c:if>

<%-- find resource and suite name --%>
<c:set var="outType" value="${fn:toUpperCase(requestParts[0])}"/>
<%-- next field is the rest id used from web.xml --%>
<c:set var="suiteOrQuery" value="${requestParts[2]}"/>
<x:parse xml="${agentBean.suiteNames}" var="suiteNames"/>
<x:choose><x:when select="$suiteNames//suite[name=$suiteOrQuery]">
  <x:set var="suite" select="string($suiteOrQuery)"/>
</x:when><x:otherwise>
  <x:set var="query" select="string($suiteOrQuery)"/>
</x:otherwise></x:choose>
<c:set var="resource" value="${requestParts[3]}"/>
<c:set var="resourcePair" value="${fn:split(resource,',')}"/>
<c:set var="sourceResource" value="${resourcePair[0]}"/>
<c:set var="targetResource" value="${fn:length(resourcePair) > 1 ? resourcePair[1] : ''}"/>
<c:set var="nickname" value="${requestParts[4]}"/>
<c:set var="startHistory" value="${requestParts[5]}"/>
<c:set var="endHistory" value="${requestParts[6]}"/>

<c:set var="forwardUrl" value="/jsp/status.jsp"/>

<%------------------------------------------------------------------------
if request whole suite
------------------------------------------------------------------------ --%>
<c:choose><c:when test="${(! empty suite or ! empty query) and empty resource}">
  <c:choose><c:when test="${outType == 'HTML'}">
    <jsp:forward page="${forwardUrl}">
      <jsp:param name="suiteNames" value="${suite}" />
      <jsp:param name="queryNames" value="${query}" />
    </jsp:forward>
  </c:when><c:when test="${outType == 'XML'}">
    <%-- get the suite xml and filter out reports for resource --%>
    <% response.setContentType("text/xml;charset=ISO-8859-1"); %>
    <inca:queryCache bean="${depotBean}" name="${query}" suite='${agentBean.uri}/${suite}'/>
  </c:when><c:when test="${outType == 'JSON'}">
    <%-- get the suite xml and filter out reports for resource --%>
    <% response.setContentType("application/json"); %>
    <c:set var="suitexml">
      <inca:queryCache bean="${depotBean}" name="${query}" suite='${agentBean.uri}/${suite}'/>
    </c:set>
    <inca:convertXmlJson xml="${suitexml}"/>
  </c:when><c:otherwise>
    <jsp:forward page="/jsp/error.jsp">
      <jsp:param name="msg" value="unsupported output type" />
      <jsp:param name="usage" value="Output type ${outType} is not supported for this operation" />
    </jsp:forward>
  </c:otherwise></c:choose>
</c:when>

<%------------------------------------------------------------------------
otherwise they want a resources results for a suite
------------------------------------------------------------------------ --%>
<c:when test="${(! empty suite or ! empty query) and ! empty resource and empty nickname}">
<c:choose><c:when test="${outType == 'HTML'}">
  <jsp:forward page="${forwardUrl}">
    <jsp:param name="resourceIds" value="${resource}" />
    <jsp:param name="suiteNames" value="${suite}" />
    <jsp:param name="queryNames" value="${query}" />
  </jsp:forward>
</c:when><c:when test="${outType == 'XML' or outType == 'JSON'}">
  <%-- get the suite xml and filter out reports for resource --%>
  <c:set var="xml">
    <config>
    <inca:resources name="${resource}" bean="${agentBean}"/>
    <inca:queryCache bean="${depotBean}" name="${query}" suite='${agentBean.uri}/${suite}'/>
    </config>
  </c:set>
  <c:import var="xslt" url="/xsl/rest.xsl"/>
  <c:set var="results">
    <x:transform doc="${xml}" xslt="${xslt}">
      <x:param name="urlResource" value="${resource}"/>
    </x:transform>
  </c:set>
  <%-- carve off xml declaration --%>
  <c:set var="results" value="${fn:substring(results, 38, -1)}"/>
  <c:choose><c:when test="${outType == 'XML'}">
    <% response.setContentType("text/xml;charset=ISO-8859-1"); %>
    ${results}
  </c:when><c:otherwise>
    <% response.setContentType("application/json"); %>
    <inca:convertXmlJson xml="${results}"/>
  </c:otherwise></c:choose>
</c:when><c:otherwise>
  <jsp:forward page="/jsp/error.jsp">
    <jsp:param name="msg" value="unsupported output type" />
    <jsp:param name="usage" value="Output type ${outType} is not supported for this operation" />
  </jsp:forward>
</c:otherwise></c:choose>
</c:when>

<%------------------------------------------------------------------------
otherwise they want a report
------------------------------------------------------------------------ --%>
<c:when test="${(! empty suite or ! empty query) and ! empty resource and ! empty nickname and empty startHistory}">
  <%-- parse suite for test result --%>
  <x:parse var="xml">
    <config>
    <inca:resources name="${resource}" bean="${agentBean}" />
    <inca:queryCache bean="${depotBean}" name="${query}" suite='${agentBean.uri}/${suite}'/>
    </config>
  </x:parse>
  <x:set var="testResult" select="$xml//nickname[.=$nickname and
       (../targetHostname=$targetResource and ../hostname=$sourceResource)]/.."/>
  <x:set var="resource" select="string($testResult/hostname)"/>
  <x:set var="target" select="string($testResult/targetHostname)"/>
  <x:set var="gmt" select="string($testResult/gmt)"/>
  <c:if test="${empty gmt}">
    <jsp:forward page="/jsp/error.jsp">
      <jsp:param name="msg" value="report for ${nickname} not found" />
      <jsp:param name="usage" value="" />
    </jsp:forward>
  </c:if>
  <%-- then forward to instance.jsp page -- either xml or html --%>
  <c:choose><c:when test="${outType == 'HTML'}">
    <jsp:forward page="/jsp/instance.jsp">
      <jsp:param name="nickname" value="${nickname}" />
      <jsp:param name="resource" value="${resource}" />
      <jsp:param name="target" value="${target}" />
      <jsp:param name="collected" value="${gmt}" />
    </jsp:forward>
  </c:when><c:when test="${outType == 'XML'}">
    <jsp:forward page="/jsp/instance.jsp">
      <jsp:param name="nickname" value="${nickname}" />
      <jsp:param name="resource" value="${resource}" />
      <jsp:param name="target" value="${target}" />
      <jsp:param name="collected" value="${gmt}" />
      <jsp:param name="printXML" value="true" />
      <jsp:param name="escapeXml" value="false" />
      <jsp:param name="debug" value="true" />
      <jsp:param name="noHeader" value="true" />
      <jsp:param name="noFooter" value="true" />
    </jsp:forward>
  </c:when><c:when test="${outType == 'JSON'}">
    <c:set var="instancexml">
      <inca:query command="instance" params="${nickname},${resource},${target},${gmt}"/>
    </c:set>
    <inca:convertXmlJson xml="${instancexml}"/>
   
  </c:when><c:otherwise>
    <jsp:forward page="/jsp/error.jsp">
      <jsp:param name="msg" value="unsupported output type" />
      <jsp:param name="usage" value="Output type ${outType} is not supported for this operation" />
    </jsp:forward>
  </c:otherwise></c:choose>
</c:when>

<%------------------------------------------------------------------------
otherwise they want a report history
------------------------------------------------------------------------ --%>
<c:when test="${(! empty suite or ! empty query) and ! empty resource and ! empty nickname and ! empty startHistory}">
  <%-- parse suite for test result --%>
  <x:parse var="xml">
    <config>
    <inca:resources name="${resource}" bean="${agentBean}" />
    <inca:queryCache bean="${depotBean}" name="${query}" suite='${agentBean.uri}/${suite}'/>
    </config>
  </x:parse>
  <x:set var="testResult" select="$xml//nickname[.=$nickname and
       (../targetHostname=$targetResource and ../hostname=$sourceResource)]/.."/>
  <x:set var="instanceId" select="string($testResult/instanceId)"/>
  <x:set var="configId" select="string($testResult/seriesConfigId)"/>
  <c:if test="${empty instanceId or empty configId}">
    <jsp:forward page="/jsp/error.jsp">
      <jsp:param name="msg" value="report for ${nickname} not found" />
      <jsp:param name="usage" value="" />
    </jsp:forward>
  </c:if>
  <inca:date var="week" add="-7" dateFormat="yyyy-MM-dd"/>
  <inca:date var="month" add="-30"/>
  <inca:date var="quarter" add="-90"/>
  <inca:date var="year" add="-365"/>
  <c:choose>
    <c:when test="${startHistory == 'week'}">
      <x:set var="start" select="$week"/>
    </c:when>
    <c:when test="${startHistory == 'month'}">
      <x:set var="start" select="$month"/>
    </c:when>
    <c:when test="${startHistory == 'quarter'}">
      <x:set var="start" select="$quarter"/>
    </c:when>
    <c:when test="${startHistory == 'year'}">
      <x:set var="start" select="$year"/>
    </c:when>
    <c:otherwise>
      <x:set var="start" select="$startHistory"/>
    </c:otherwise>
  </c:choose>

  <%-- then forward to graph.jsp page -- either xml or html --%>
  <c:choose>
    <c:when test="${outType == 'HTML' and !hasTimestamp}">
    <jsp:forward page="/jsp/graph.jsp">
      <jsp:param name="series" value="${nickname},${sourceResource},${targetResource}" />
      <jsp:param name="startDate" value="${start}" />
      <jsp:param name="endDate" value="${empty endHistory ? '' : endHistory}" />
    </jsp:forward>
    </c:when>
    <c:when test="${outType == 'HTML' and hasTimestamp}">
    <jsp:forward page="/jsp/instance.jsp">
      <jsp:param name="collected" value="${startHistory}" />
      <jsp:param name="nickname" value="${nickname}" />
      <jsp:param name="resource" value="${sourceResource}" />
      <jsp:param name="target" value="${target}" />
    </jsp:forward>
    </c:when>
    <c:when test="${outType == 'XML' and hasTimestamp}">
    <jsp:forward page="/jsp/instance.jsp">
      <jsp:param name="collected" value="${startHistory}" />
      <jsp:param name="nickname" value="${nickname}" />
      <jsp:param name="resource" value="${sourceResource}" />
      <jsp:param name="target" value="${targetResource}" />
      <jsp:param name="printXML" value="true" />
      <jsp:param name="escapeXml" value="false" />
      <jsp:param name="debug" value="true" />
      <jsp:param name="noHeader" value="true" />
      <jsp:param name="noFooter" value="true" />
    </jsp:forward>
    </c:when>
    <c:when test="${outType == 'XML' and !hasTimestamp}">
    <jsp:forward page="/jsp/graph.jsp">
      <jsp:param name="series" value="${nickname},${sourceResource},${targetResource}" />
      <jsp:param name="startDate" value="${start}" />
      <jsp:param name="endDate" value="${empty endHistory ? '' : endHistory}" />
      <jsp:param name="printXML" value="true" />
      <jsp:param name="escapeXml" value="false" />
      <jsp:param name="noHeader" value="true" />
      <jsp:param name="noFooter" value="true" />
    </jsp:forward>
    </c:when>
    <c:when test="${outType == 'CSV' and !hasTimestamp}">
    <jsp:forward page="/jsp/graph.jsp">
      <jsp:param name="series" value="${nickname},${sourceResource},${targetResource}" />
      <jsp:param name="startDate" value="${start}" />
      <jsp:param name="endDate" value="${empty endHistory ? '' : endHistory}" />
      <jsp:param name="printCSV" value="true" />
      <jsp:param name="noHeader" value="true" />
      <jsp:param name="noFooter" value="true" />
    </jsp:forward>
    </c:when>
    <c:when test="${outType == 'JSON'}">
      <c:set var="targetClause">AND config.series.targetHostname = '${targetResource}'</c:set>
      <c:set var="targetClause" value="${targetResource == '' ? '' : targetClause}"/>
      <c:set var="query">
        062017,
        062617,
    (config.nickname='<c:out value="${nickname}"/>' AND
    config.series.resource='<c:out value="${sourceResource}"/>' <c:out value="${targetClause}" escapeXml="false"/> ) </c:set>
      <c:out escapeXml="false" value="${query}"/>
      <inca:query command="period" params="${query}"/>
    </c:when>
    <c:otherwise>
      <jsp:forward page="/jsp/error.jsp">
        <jsp:param name="msg" value="unsupported output type" />
        <jsp:param name="usage" value="Output type ${outType} is not supported for this operation" />
      </jsp:forward>
    </c:otherwise>
  </c:choose>
</c:when>

</c:choose>
