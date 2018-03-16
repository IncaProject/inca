<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<%@ tag body-content="empty" %>
<%@ tag description="Print a time series graph of the report exit status and a table of the pass/fail/unknown values; each series is printed in a subplot of the graph" %>

<%@ attribute name="bgcolor" type="java.lang.String" required="false"
              description="a hex value for for the graph background color" %>
<%@ attribute name="height" type="java.lang.Integer" required="false"
              description="the height to use for the subplot" %>
<%@ attribute name="legendanchor" type="java.lang.String" required="false"
              description="placement of the legend (north, east, south, west)" %>
<%@ attribute name="minHeight" type="java.lang.Integer" required="false"
              description="minimum height to use for the graph"%>
<%@ attribute name="series" type="java.lang.Object" required="true"
              description="an array of strings containing series nickname,resource pairs" %>
<%@ attribute name="showlegend" type="java.lang.Boolean" required="false"
              description="true to show the graph legend; false to not" %>
<%@ attribute name="showlinks" type="java.lang.Boolean" required="false"
              description="true to add links to the graph; false to not"%>
<%@ attribute name="showmouseovers" type="java.lang.Boolean" required="false"
              description="true to add mouseovers to the graph; false to not" %>
<%@ attribute name="width" type="java.lang.Integer" required="false"
              description="the width to use for the graph" %>
<%@ attribute name="xml" required="true"
              description="string containing xml exit status values will be extracted from" %>

<%-- Make sure we have unique ids in case this tag is used multiple times --%>
<c:if test="${empty incaHistoryIdx}">
  <c:set var="incaHistoryIdx" value="0" scope="application"/>
</c:if>
<c:set var="incaHistoryIdx" value="${incaHistoryIdx+1}" scope="application"/>
<c:set var="idx" value="${incaHistoryIdx}"/>

<%-- Defaults --%>
<c:set var="bgcolor" value="${empty bgcolor ? '#AAAAFF' : bgcolor}" />
<c:set var="legendanchor" value="${empty legendanchor ? '' : legendanchor}"/>
<c:set var="showlegend" value="${empty showlegend ? 'true' : showlegend}"/>
<c:set var="showlinks" value="${empty showlinks ? true : showlinks}" />
<c:set var="showmouseovers"
       value="${empty showmouseovers ? true : showmouseovers}" />
<c:set var="height" value="${empty height ? 75 : height}" />
<c:set var="minHeight" value="${empty minHeight ? 290 : minHeight}" />
<c:set var="width" value="${empty width || width==0 ? 500 : width }" />

<c:set var="height" value="${fn:length(series)*height}"/>
<c:set var="height" value="${height<minHeight ? minHeight : height}"/>

<%-- Create Jfree chart datasets for plotting a graph and a distribution to
     hold the counts of pass, fail, and unknown --%>

<c:forEach items="${series}" var="s" varStatus="i">
  <inca:split var="seriesParts" content="${s}" delim=","/>
  <c:set var="n" value="${seriesParts[0]}"/>
  <c:set var="r" value="${seriesParts[1]}"/>
  <c:set var="t" value="${seriesParts[2]}"/>
  <c:set var="l" value="${seriesParts[3]}"/>
  <c:set var="targetCond" value="and targetHostname='${t}'"/>
  <c:set var="targetClause" value="${empty t ? '' : targetCond}"/>
  <c:set var="node" value="/q:object/row/object[nickname='${n}' and resource='${r}' ${targetClause}]"/>
  <c:set var="label">${n}, ${r}</c:set>
  <c:set var="tlabel">${n}, ${r} to ${t}</c:set>
  <c:set var="label" value="${empty t ? rlabel : tlabel}"/>
  <c:set var="label" value="${empty l ? label : l}"/>
  <c:set var="tsCounter" value="${idx}${i.count}"/>
  <!-- For tooltip, exit_status is a processed value meaning that either exit_message
       or comparisonResult should be printed upon failure.  The expression below ensures
       that only one is every displayed (either or) -->
  <inca:timeSeries var="ts${tsCounter}" xml="${xml}" label="${label}"
                     xpath="${node}" timestampXpath="collected"
                     tooltipXpath="exit_message[../exit_status='Failure' and .!='']|comparisonResult[../exit_status='Failure' and ../exit_message='']"
                     linkXpath="concat('instance.jsp?nickname=', nickname,
                                '&amp;resource=', resource, '&amp;target=', targetHostname, '&amp;collected=', collected)"/>
</c:forEach>

<%-- Create time series graph using subplots for each series --%>
<table><tr><td>
<inca:timeSeriesPostProcess ytick="1.0" ymin="-1.1" ymax="1.1" var="pp"/>
<cewolf:combinedchart id="tsChart${idx}" title="Status History"
                      xaxislabel="Test result collection time"
                      layout="vertical" type="combinedxy" antialias="true"
                      showlegend="${legend}" legendanchor="${legendanchor}">
 <cewolf:colorpaint color="${bgcolor}"/>
 <fmt:formatNumber var="half"  pattern="0"
                   value="${fn:length(series) / 2}" />
 <c:forEach items="${series}" varStatus="i">
   <c:set var="ylabel"
          value="${i == half ? 'Result: pass=1, unknown=0, fail=-1' : ''}"/>
      <c:set var="tsCounter" value="${idx}${i.count}"/>
     <cewolf:plot type="xyline" yaxislabel="${ylabel}">
        <cewolf:data><cewolf:producer id="ts${tsCounter}" /></cewolf:data>
        <cewolf:chartpostprocessor id="pp"/>
      </cewolf:plot>
  </c:forEach>
</cewolf:combinedchart>
<cewolf:img chartid="tsChart${idx}" renderer="/cewolf" mime="image/png"
            width="${width}" height="${height}">
  <c:choose><c:when test="${showmouseovers && showlinks}">
    <cewolf:map tooltipgeneratorid="ts${idx}1" linkgeneratorid="ts${idx}1" />
  </c:when><c:when test="${showmouseovers}">
    <cewolf:map tooltipgeneratorid="ts${idx}1"/>
  </c:when><c:when test="${showlinks}">
    <cewolf:map linkgeneratorid="ts${idx}1" />
  </c:when></c:choose>
</cewolf:img>
</td></tr><tr><td width="${width}">
<inca:tableStatusSummary series="${series}" xml="${xml}"/>
</td></tr></table>
