<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="incaXml" uri="/WEB-INF/inca.tld" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>


<jsp:include page="header.jsp">
  <jsp:param name="title" value="Inca Weekly Status Report"/>
</jsp:include>

<%-- Check inputs and redirect to error if incorrect --%>
<c:set var="usage">  
Description:  display summary statistics by resource and suite

summary.jsp

</c:set>

<c:set var="legend" value="true"/>
<c:set var="legendanchor" value="north"/>
<c:set var="bgcolor" value="#FFFFFF"/>
<c:set var="height" value="800"/>
<c:set var="width" value="300"/>

<h1>Inca Weekly Status Report</h1>
<p>The graphs below shows the average series pass rate by resource and by suite
for the last 7 days (red bars).  The blue bars shows the average pass rate for
the previous 7 days.  The bar label shows the value of the average series pass
rate for the last 7 days and the difference in percentage from the previous 7
days.  The bar label text color is green if the average pass rate is better,
red if the average pass rate is worse, and gray if there was no change.  Click
on individual bars to show the percentages broken down further for each
individual resource or suite.</p>

<%-- get summary data --%>
<c:set var="xml">
  <inca:queryCache bean="${depotBean}" name='incaQueryStatus'/>
</c:set>

<%-- compute series pass percentage via a stylesheet --%>
<c:import var="computeSeriesAverages" url="/xsl/seriesAverages.xsl"/>
<c:set var="seriesAveragesXml">
  <incaXml:transform doc ="${xml}" xslt="${computeSeriesAverages}">
    <incaXml:param name="sort" value="descending"/>
    <incaXml:param name="ignoreErrs" value="${depotBean.ignoreErrorPattern}"/>
  </incaXml:transform>
</c:set>

<%-- compute pass percentage for suites and resources by week, etc. --%> 
<c:import var="periodAverages" url="/xsl/periodAverages.xsl"/>
<c:set var="periodAveragesResourcesXml">
  <incaXml:transform doc ="${seriesAveragesXml}" xslt="${periodAverages}">
    <incaXml:param name="resourceTotal" value="true"/>
  </incaXml:transform>
</c:set>
<c:set var="periodAveragesSuitesXml">
  <incaXml:transform doc ="${seriesAveragesXml}" xslt="${periodAverages}">
    <incaXml:param name="suiteTotal" value="true"/>
  </incaXml:transform>
</c:set>

<%-- plot averages --%>
<x:parse var="resourceDoc" doc="${periodAveragesResourcesXml}"/>
<x:parse var="suiteDoc" doc="${periodAveragesSuitesXml}"/>
<table><tr valign="top">
<td>
<inca:weeklyAveragesGraph doc="${resourceDoc}" title="Resource status history" xaxisLabel="Resource" 
                          filterResource="true">
  <inca:addValue dataset="averages" value="${pass}" 
                 row="${beginDate} - ${endDate}" col="${resource}"
                 tooltip="click for ${resource} details"
                 url="summaryDetails.jsp?resource=${resource}"/>
</inca:weeklyAveragesGraph>
</td>
<td valign="top">
<inca:weeklyAveragesGraph doc="${suiteDoc}" title="Suite status history" xaxisLabel="Suite" 
                          filterSuite="true">
  <inca:addValue dataset="averages" value="${pass}" 
                 row="${beginDate} - ${endDate}" col="${suite}"
                 tooltip="click for ${suite} details"
                 url="summaryDetails.jsp?guid=${guid}"/>
</inca:weeklyAveragesGraph>
</td>
</tr></table>

<jsp:include page="footer.jsp"/>
