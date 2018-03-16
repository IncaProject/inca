<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>

<jsp:include page="header.jsp">
  <jsp:param name="title" value="Inca Weekly Status Report"/>
</jsp:include>

<%-- Check inputs and redirect to error if incorrect --%>
<c:set var="usage">  
Description:  display summary statistics by resource and suite

summaryDetails.jsp

</c:set>

<c:set var="legend" value="true"/>
<c:set var="legendanchor" value="north"/>
<c:set var="bgcolor" value="#FFFFFF"/>

<h1><a href="${url}/jsp/summary.jsp">Inca Weekly Status Report</a>
    <c:choose><c:when test="${! empty param.resource}">
      --> Resource ${param.resource}
    </c:when><c:when test="${! empty param.guid}">
      --> Suite <inca:guid2suite guid="${param.guid}"/>
    </c:when></c:choose>
</h1>
<p>The graph below shows the average series pass rate 
    <c:choose><c:when test="${! empty param.resource}">
      by suite for resource ${param.resource}
    </c:when><c:when test="${! empty param.guid}">
      by resource for suite '<inca:guid2suite guid="${param.guid}"/>'
    </c:when></c:choose>
for the last 7 days (red bars).  The blue bars shows the average series pass
rate for the previous 7 days.  The bar label shows the value of the average
series pass rate for the last 7 days and the difference in percentage from the
previous 7 days.  The bar label text color is green if the average series pass
rate is better, red if the average series pass rate is worse, and gray if
there was no change. </p>

<c:set var="xml">
  <inca:queryCache bean="${depotBean}" name='incaQueryStatus'/>
</c:set>

<%-- compute series pass percentage via a stylesheet --%>
<c:import var="computeSeriesAverages" url="/xsl/seriesAverages.xsl"/>
<c:set var="seriesAveragesXml">
  <x:transform doc ="${xml}" xslt="${computeSeriesAverages}">
    <x:param name="sort" value="descending"/>
    <x:param name="ignoreErrs" value="${depotBean.ignoreErrorPattern}"/>
  </x:transform>
</c:set>

<%-- compute pass percentage for suites and resources by week, etc. --%> 
<c:import var="periodAveragesDetails" url="/xsl/periodAverages.xsl"/>
<c:set var="periodAveragesXml">
  <x:transform doc ="${seriesAveragesXml}" xslt="${periodAveragesDetails}">
    <c:choose><c:when test="${! empty param.resource}">
      <x:param name="resource" value="${param.resource}"/>
    </c:when><c:when test="${! empty param.guid}">
      <x:param name="guid" value="${param.guid}"/>
    </c:when></c:choose>
  </x:transform>
</c:set>

<%-- Plot averages --%>
<x:parse var="doc" doc="${periodAveragesXml}"/>
<c:choose><c:when test="${! empty param.resource}">
  <inca:weeklyAveragesGraph doc="${doc}" xaxisLabel="Suite"
                           title="Status history for resource ${param.resource}">
    <inca:addValue dataset="averages" value="${pass}" 
                   row="${beginDate} - ${endDate}" col="${suite}"
                   tooltip="click for ${suite} error details"
                   url="status.jsp?resourceIds=${param.resource}&suiteNames=${suite}&xsl=seriesSummary.xsl&xml=weekSummary.xml&queryNames=incaQueryStatus" />
  </inca:weeklyAveragesGraph>
</c:when><c:when test="${! empty param.guid}">
  <c:set var="suite"><inca:guid2suite guid="${param.guid}"/></c:set>
  <inca:weeklyAveragesGraph doc="${doc}" xaxisLabel="Resource"
                            title="Status history for suite ${suite}"> 
    <inca:addValue dataset="averages" value="${pass}" 
                   row="${beginDate} - ${endDate}" col="${resource}"
                   tooltip="click for ${resource} details"
                   url="status.jsp?resourceIds=${resource}&suiteNames=${suite}&xsl=seriesSummary.xsl&xml=weekSummary.xml&queryNames=incaQueryStatus" />
  </inca:weeklyAveragesGraph>
</c:when></c:choose>

<jsp:include page="footer.jsp"/>
