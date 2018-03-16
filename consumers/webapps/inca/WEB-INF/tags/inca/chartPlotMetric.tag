<%@ tag import="java.util.Vector" %>
<%@ tag import="java.util.regex.Matcher" %>
<%@ tag import="java.util.regex.Pattern" %>
<%@ tag import="java.util.HashMap" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<%@ tag body-content="empty" dynamic-attributes="dynattrs" %>
<%@ tag description="Print a time series graph of the report metrics and a table of the pass/fail/unknown values. " %>

<%@ attribute name="bgcolor" type="java.lang.String" required="false"
              description="a hex value for for the graph background color" %>
<%@ attribute name="chart" type="java.lang.String" required="false"
              description="Describes how time series are displayed in graphs.  Default value is 'single' meaning all series and metrics will display on a single graph.  A value of 'series' means one graph containing all metrics will be printed per series.  A value of 'metric' means one graph containing all series will be printed per metric.  Finally a value of multiple means one graph will be printed per metric and series." %>
<%@ attribute name="height" type="java.lang.Integer" required="false"
              description="the height to use for the graph" %>
<%@ attribute name="legendanchor" type="java.lang.String" required="false"
              description="placement of the legend (north, east, south, west)" %>
<%@ attribute name="metric" type="java.lang.String" required="false"
              description="name(s) of metric(s) to show in the graph"%>
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
<%@ attribute name="showtablesummary" type="java.lang.Boolean" required="false" 
              description="true to report summary table to end; false to not" %>
<%@ attribute name="titlePrefix" type="java.lang.String" required="false"
              description="Specify a prefix for each graph title" %>
<%@ attribute name="titleSuffix" type="java.lang.String" required="false"
              description="Specify a suffix for each graph title" %>
<%@ attribute name="unitPattern" type="java.lang.String" required="false"
              description="Java pattern string to chop off units from a metric name and attach them to y axis label."%>
<%@ attribute name="width" type="java.lang.Integer" required="false"
              description="the width to use for the graph" %>
<%@ attribute name="xml" required="true"
              description="string containing xml where metric values will be extracted from" %>


<%-- Make sure we have unique ids in case this tag is used multiple times --%>
<c:if test="${empty incaHistoryIdx}">
  <c:set var="incaHistoryIdx" value="0" scope="application"/>
</c:if>

<%-- Defaults --%>
<c:set var="bgcolor" value="${empty bgcolor ? '#AAAAFF' : bgcolor}" />
<c:set var="chart" value="${empty chart  ? 'single' : chart}" />
<c:set var="legendanchor" value="${empty legendanchor ? '' : legendanchor}"/>
<c:set var="showlegend" value="${empty showlegend ? 'true' : showlegend}"/>
<c:set var="showlinks" value="${empty showlinks ? true : showlinks}" />
<c:set var="showmouseovers"
       value="${empty showmouseovers ? true : showmouseovers}" />
<c:set var="showtablesummary" 
       value="${empty showtablesummary ? true : showtablesummary}" />
<c:set var="height" value="${empty height ? 75 : height}" />
<c:set var="metrics" value="${empty metric ? null : fn:split(metric, ',')}" />
<c:set var="minHeight" value="${empty minHeight ? 290 : minHeight}" />
<c:set var="titlePrefix" value="${empty titlePrefix ? '' : titlePrefix}"/>
<c:set var="titleSuffix" value="${empty titleSuffix ? '' : titleSuffix}"/>
<c:set var="width" value="${empty width || width==0 ? 500 : width }" />

<%-- For ymin, ymax, and ytick --%>
<%
  HashMap<String,String> attributes = (HashMap<String,String>)jspContext.getAttribute("dynattrs");
  for( String name : attributes.keySet() ) {
    jspContext.setAttribute(name, attributes.get(name));
  }
%>

<%-- Create Jfree chart datasets for plotting time series for each metric --%>

<%
  Vector<String> tsNames = new Vector<String>();
  jspContext.setAttribute( "tsNames", tsNames );
%>
<c:set var="uniqTsIdx" value="${incaHistoryIdx}"/>
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

  <c:forEach items="${metrics}" var="m">
    <c:set var="m_" value = "${m}"/>
<%  // statistics in short mode need to be alphanumeric or _ or - otherwise it
		// the xpath will be invalid; long mode can be more flexible so we just
    // cleanse the short mode string so it will be ignored
    String m_clean = (String)jspContext.getAttribute( "m_" );
    m_clean = m_clean.replaceAll("[^\\w_-]", "");
    m_clean = m_clean.replaceAll("^\\d", "_");
    jspContext.setAttribute( "m_", m_clean );
%>
    <c:set var="metPath" value="body//statistics/@${m_}|body//statistics/statistic[ID='${m}']/value|body/*/@${m_}"/>
    <c:set var="tooltipPath" value="concat('${m}=', ${metPath})"/>
    <%-- since time series variables are per page, we need to make them unique in case this tag is used multiple times within a page --%>
    <c:set var="tsVar">
      <c:choose>
        <c:when test="${chart == 'single'}">${uniqTsIdx}</c:when>
        <c:when test="${chart == 'metric'}">${uniqTsIdx}${m}</c:when>
        <c:when test="${chart == 'series'}">${uniqTsIdx}${l}</c:when>
        <c:when test="${chart == 'multiple'}">${uniqTsIdx}${l}${m}</c:when>
      </c:choose>
    </c:set>
    <%
      String tsVarName = (String)jspContext.getAttribute( "tsVar" );
      if ( ! tsNames.contains( tsVarName ) ) {
        tsNames.add( tsVarName );
      }
    %>
    <inca:timeSeries var="ts${tsVar}" xml="${xml}" label="${label}, ${m}"
                     xpath="${node}" timestampXpath="collected"
                     valueXpath="${metPath}"
                     tooltipXpath="${tooltipPath}"
                     linkXpath="concat('instance.jsp?nickname=', nickname,
                               '&amp;resource=', resource, '&amp;target=', targetHostname,
                               '&amp;collected=', collected)"/>
  </c:forEach>
</c:forEach>

<%-- Create graph for each time series --%>
<table cellspacing="10">
<%
  String unitPatternString = null;
  Pattern unitPatternObj = null;
  if ( jspContext.getAttribute("unitPattern") != null ) {
    unitPatternString = (String)jspContext.getAttribute("unitPattern");
    unitPatternObj = Pattern.compile( unitPatternString );
  }
%>
  <inca:timeSeriesPostProcess var="pp"/>
  <c:if test="${! empty ymin}"><inca:timeSeriesPostProcess var="pp" ymin="${ymin}"/></c:if>
  <c:if test="${! empty ymax}"><inca:timeSeriesPostProcess var="pp" ymax="${ymax}"/></c:if>
  <c:if test="${! empty ytick}"><inca:timeSeriesPostProcess var="pp" ytick="${ytick}"/></c:if>
  <c:if test="${! empty ymin && ! empty ymax}"><inca:timeSeriesPostProcess var="pp" ymin="${ymin}" ymax="${ymax}"/></c:if>
  <c:if test="${! empty ymin && ! empty ytick}"><inca:timeSeriesPostProcess var="pp" ymin="${ymin}" ytick="${ytick}"/></c:if>
  <c:if test="${! empty ymax && ! empty ytick}"><inca:timeSeriesPostProcess var="pp" ymax="${ymax}" ytick="${ytick}"/></c:if>
  <c:if test="${!empty ymin && ! empty ymax && ! empty ytick}"><inca:timeSeriesPostProcess var="pp" ymin="${ymin}" ymax="${ymax}" ytick="${ytick}"/></c:if>

  <c:forEach items="${tsNames}" var="t">
    <tr><td style="height:${height}" valign="top">
    <c:set var="title"><c:choose>
        <c:when test="${t == ''}">Metric History</c:when>
        <c:otherwise>${fn:replace(t, uniqTsIdx, '')}</c:otherwise>
    </c:choose></c:set>
    <%
      if ( unitPatternString != null ) {
        String title = (String)jspContext.getAttribute("title");
        Matcher unitMatcher = unitPatternObj.matcher( title );
        if ( unitMatcher.find() ) {
          jspContext.setAttribute( "ylabel", unitMatcher.group() );
        } else {
          jspContext.setAttribute( "ylabel", "" );
        }
        title = title.replaceAll( unitPatternString, "" );
        title = title.replaceAll( "_", " " ).trim(); // clean up title
        jspContext.setAttribute( "title", title );
      }
    %>
    <c:set var="incaHistoryIdx" value="${incaHistoryIdx+1}"
           scope="application"/>
    <cewolf:chart id="tsChart${incaHistoryIdx}"
                  title="${titlePrefix}${title}${titleSuffix}"
                  xaxislabel="metric collection time"
                  type="timeseries" yaxislabel="${ylabel}" antialias="true"
                  showlegend="${legend}" legendanchor="${legendanchor}">
     <cewolf:colorpaint color="${bgcolor}"/>
     <cewolf:data><cewolf:producer id="ts${t}" /></cewolf:data>
     <cewolf:chartpostprocessor id="pp"/>
    </cewolf:chart>
    <cewolf:img chartid="tsChart${incaHistoryIdx}" renderer="/cewolf"
                mime="image/png" width="${width}" height="${height}">
      <c:choose><c:when test="${showmouseovers && showlinks}">
        <cewolf:map tooltipgeneratorid="ts${t}" linkgeneratorid="ts${t}" />
      </c:when><c:when test="${showmouseovers}">
        <cewolf:map tooltipgeneratorid="ts${t}"/>
      </c:when><c:when test="${showlinks}">
        <cewolf:map linkgeneratorid="ts${t}" />
      </c:when></c:choose>
    </cewolf:img>
    </td></tr>
  </c:forEach>
</td></tr><tr><td width="${width}">

<c:if test="${showtablesummary}">
  <inca:tableStatusSummary series="${series}" xml="${xml}"/>
</c:if>

</td></tr></table>
