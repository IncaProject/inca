<%@ tag import="edu.sdsc.inca.consumer.CategoryBean" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<%@ tag description="Print a horizontal bar graph of the average series pass rate for the most recent week, the week before that, and the difference between them" %>

<%@ attribute name="bgcolor" type="java.lang.String" required="false"
              description="a hex value for for the graph background color" %>
<%@ attribute name="doc" required="true" type="java.lang.Object"
              description="a parsed xml document"%>
<%@ attribute name="filterResource" type="java.lang.String" required="false"
              description="specify if resource averages (using value 'true') or a specific resource average is desired" %>
<%@ attribute name="filterSuite" type="java.lang.String" required="false"
              description="specify if suite averages (using value 'true') or a specific suite average is desired" %>
<%@ attribute name="minHeight" type="java.lang.Integer" required="false" 
              description="the minimum minHeight to use for the graph" %>
<%@ attribute name="minWidth" type="java.lang.Integer" required="false" 
              description="the minimum width to use for the graph" %>
<%@ attribute name="title" type="java.lang.String" required="true"
              description="a title for the graph" %>
<%@ attribute name="xaxisLabel" type="java.lang.String" required="true"
              description="a label for the yaxis" %>

<%@ variable name-given="averages" variable-class="java.lang.Object" scope="NESTED" %>
<%@ variable name-given="beginDate" variable-class="java.lang.String" scope="NESTED" %>
<%@ variable name-given="endDate" variable-class="java.lang.String" scope="NESTED" %>
<%@ variable name-given="guid" variable-class="java.lang.String" scope="NESTED" %>
<%@ variable name-given="pass" variable-class="java.lang.String" scope="NESTED" %>
<%@ variable name-given="resource" variable-class="java.lang.String" scope="NESTED" %>
<%@ variable name-given="suite" variable-class="java.lang.String" scope="NESTED" %>

<%-- Make sure we have unique ids in case this tag is used multiple times --%>
<c:if test="${empty weeklyAveragesIdx}">
  <c:set var="weeklyAveragesIdx" value="0" scope="application"/>
</c:if>
<c:set var="weeklyAveragesIdx" value="${weeklyAveragesIdx+1}" scope="application"/>
<c:set var="idx" value="${weeklyAveragesIdx}"/>

<%-- Defaults --%>
<c:set var="bgcolor" value="${empty bgcolor ? '#FFFFFF' : bgcolor}" />
<c:set var="minHeight" 
       value="${empty minHeight || minHeight==0 ? 100 : minHeight}" />
<c:set var="minWidth" 
       value="${empty minWidth || minWidth==0 ? 300 : minWidth }" />

<%-- Read in averages to CategoryBean --%>
<c:set var="nameLength" value="0"/>
<inca:category var="averages">
<x:forEach select="$doc/seriesSummary/period[position() <= 2]" var="period">
  <%-- get start and end dates --%>
  <x:set var="begin" select="string($period/begin)"/>
  <x:set var="end" select="string($period/end)"/>
  <inca:time2date var="beginDate" millis="${begin}" dateFormat="MM/dd/yy"/> 
  <inca:time2date var="endDate" millis="${end}" dateFormat="MM/dd/yy"/> 

  <%-- filter for the right averages --%>
  <x:set var="periodAverages" select="$period"/>
  <c:choose><c:when test="${! empty filterResource && filterResource =='true'}">
    <x:set var="periodAverages" select="$periodAverages[resource]"/>
  </c:when><c:when test="${! empty filterResource && filterResource != 'true'}">
    <x:set var="periodAverages" 
           select="$periodAverages[resource = $filterResource]"/>
  </c:when></c:choose>
  <c:choose><c:when test="${! empty filterSuite && filterSuite == 'true'}">
    <x:set var="periodAverages" select="$periodAverages[guid]"/>
  </c:when><c:when test="${! empty filterSuite && filterSuite != 'true'}">
    <x:set var="periodAverages" select="$periodAverages[guid = $filterSuite]"/>
  </c:when></c:choose>

  <%-- read values --%>
  <x:forEach select="$periodAverages" var="stats">
    <x:set var="seriesCount" select="string($stats/seriesCount)"/>
    <c:if test="${seriesCount>0}">
      <x:set var="resource" select="string($stats/resource)"/>
      <x:set var="guid" select="string($stats/guid)"/>
      <c:if test="${! empty guid}">
        <c:set var="suite"><inca:guid2suite guid="${guid}"/></c:set>
      </c:if>
      <x:set var="pass" select="string($stats/passPercentage)"/>
      <fmt:setLocale value="en_US"/>
      <fmt:formatNumber var="pass" value="${pass}" maxFractionDigits="1"/>
      <c:if test="${fn:length(resource)>nameLength}">
        <c:set var="nameLength" value="${fn:length(resource)}"/>
      </c:if>
      <c:if test="${fn:length(suite)>nameLength}">
        <c:set var="nameLength" value="${fn:length(suite)}"/>
      </c:if>
      <jsp:doBody/>
    </c:if>
  </x:forEach>
</x:forEach>
</inca:category>

<%-- Plot as horizontal bars --%>
<cewolf:chart id="passGraph${idx}" type="horizontalbar"
    title="${title}" xaxislabel="${xaxisLabel}"
    yaxislabel="Average series pass rate"
    antialias="true"
    showlegend="true" legendanchor="north">
  <cewolf:colorpaint color="${bgcolor}"/>
  <cewolf:data>
    <cewolf:producer id="averages"/>
  </cewolf:data>
  <inca:averagesPostProcess var="catPP"/>
  <cewolf:chartpostprocessor id="catPP"/>
</cewolf:chart>
<cewolf:img chartid="passGraph${idx}" renderer="/cewolf" mime="image/png"
            width="${minWidth + (nameLength*13)}" 
            height="${minHeight + averages.columnCount*25}">
  <cewolf:map tooltipgeneratorid="averages"
              linkgeneratorid="averages"/>
</cewolf:img>
