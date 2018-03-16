<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<%@ tag body-content="empty" %>
<%@ tag description="Print a stacked bar graph of a error distribution and a table of the corresponding values" %>

<%@ attribute name="bgcolor" type="java.lang.String" required="false"
              description="a hex value for for the graph background color" %>
<%@ attribute name="height" type="java.lang.Integer" required="false" 
              description="the height to use for the graph" %>
<%@ attribute name="legendanchor" type="java.lang.String" required="false" 
              description="placement of the legend (north, east, south, west)" %>
<%@ attribute name="series" type="java.lang.Object" required="true" 
              description="an array of strings containing series nickname,resource pairs" %>
<%@ attribute name="showlegend" type="java.lang.Boolean" required="false" 
              description="true to show the graph legend; false to not"%>
<%@ attribute name="showmouseovers" type="java.lang.Boolean" required="false" 
              description="true to add mouseovers to the graph; false to not"%>
<%@ attribute name="width" type="java.lang.Integer" required="false" 
              description="the width to use for the graph" %>
<%@ attribute name="xml" required="true" 
              description="a string containing xml error messages will be extracted from"%>

<%-- Make sure we have unique ids in case this tag is used multiple times --%>
<c:if test="${empty incaErrorIdx}">
  <c:set var="incaErrorIdx" value="0" scope="application"/>
</c:if>
<c:set var="incaErrorIdx" value="${incaErrorIdx+1}" scope="application"/>
<c:set var="idx" value="${incaErrorIdx}"/>

<%-- Defaults --%>
<c:set var="bgcolor" value="${empty bgcolor ? '#AAAAFF' : bgcolor}" />
<c:set var="height" value="${empty height || height==0 ? 400 : height}" />
<c:set var="legendanchor" value="${empty legendanchor ? '' : legendanchor}"/>
<c:set var="showlegend" value="${empty showlegend ? 'true' : showlegend}"/>
<c:set var="showmouseovers" 
       value="${empty showmouseovers ? 'true' : showmouseovers}" />
<c:set var="width" value="${empty width || width==0 ? 500 : width }" />

<%-- Create Jfree chart datasets for plotting --%>
<inca:distribution var="err" xml="${xml}">
<c:forEach items="${series}" var="s" varStatus="i">
  <inca:split var="seriesParts" content="${s}" delim=","/>
  <c:set var="n" value="${seriesParts[0]}"/>
  <c:set var="r" value="${seriesParts[1]}"/>
  <c:set var="t" value="${seriesParts[2]}"/>
  <c:set var="l" value="${seriesParts[3]}"/>
  <c:set var="label">${n}, ${r}</c:set>
  <c:set var="tlabel">${n}, ${r} to ${t}</c:set>
  <c:set var="label" value="${empty t ? rlabel : tlabel}"/>
  <c:set var="label" value="${empty l ? label : l}"/>

  <c:set var="targetCond" value="and targetHostname='${t}'"/>
  <c:set var="targetClause" value="${empty t ? '' : targetCond}"/>
  <c:set var="node" value="/q:object/row/object[nickname='${n}' and resource='${r}' ${targetClause}]"/>

  <inca:distributionRow bean="${err}" rowkey="${label}"
                        xpath="${node}[exit_status='Failure' and exit_message!='']/exit_message|${node}[exit_status='Failure' and exit_message='']/comparisonResult" />
</c:forEach>
</inca:distribution>

<%-- Create error distribution graph --%>
<c:if test="${fn:length(err.colKeys) > 0}">
<table cellpadding="10"><tr valign="top"><td>
<cewolf:chart id="dist${idx}" type="stackedverticalbar"
    title="Error Message Distribution" xaxislabel="Error Type"
    yaxislabel="Error Count"
    antialias="true"
    showlegend="${legend}" legendanchor="${legendanchor}">
  <cewolf:colorpaint color="${bgcolor}"/>
  <cewolf:data>
    <cewolf:producer id="err"/>
  </cewolf:data>
  <inca:distributionPostProcess var="distPP"/>
  <cewolf:chartpostprocessor id="distPP"/>
</cewolf:chart>
<cewolf:img chartid="dist${idx}" renderer="/cewolf" mime="image/png"
            width="${width}" height="${height}"> 
  <c:if test="${showmouseovers}"><cewolf:map tooltipgeneratorid="err"/></c:if>
</cewolf:img>
</td><td>

<%-- Print counts --%>
<table class="header">
<c:forEach items="${err.rowKeys}" var="row" varStatus="i"> 
  <tr>
    <td class="header" colspan="3"><b>${row}</b></td>
  </tr><tr>
    <td class="subheader">Error Type</td>
    <td class="subheader">Error Count</td>
    <td class="subheader">Error Message</td>
  </tr>
  <c:set var="hasErrors" value="false"/>
  <c:forEach items="${err.colKeys}" var="col" varStatus="j"> 
    <c:set var="rowIdx" value="${i.count-1}"/>
    <c:set var="colIdx" value="${j.count-1}"/>
    <inca:distributionValue var="value" bean="${err}" 
                            row="${rowIdx}" col="${colIdx}"/>
    <c:if test="${! empty value && value > 0 }">
      <c:set var="hasErrors" value="true"/>
      <tr>
        <td class="clear">${j.count - 1}</td>
        <td class="clear">${value}</td>
        <td class="clear"><pre>${col}</pre></td>
      </tr>
    </c:if>
  </c:forEach>
  <c:if test="${hasErrors=='false'}">
    <tr><td class="clear" colspan="3"><i>no errors</i></td></tr>
  </c:if>
</c:forEach>
</table>

</td></tr></table>

</c:if>

