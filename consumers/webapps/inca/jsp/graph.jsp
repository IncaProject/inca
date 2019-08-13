<%@ page trimDirectiveWhitespaces="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="incaXml" uri="/WEB-INF/inca.tld" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>

<inca:date var="two" add="-14"/>
<inca:date var="now" add="1"/>

<%-- Check inputs and redirect to error if incorrect --%>
<c:set var="usage">  Description: Creates graphs for series history specified.

graph.jsp?series=testName,resourceName,target[,label][&amp;series=testName,resourceName,target[,label]...]

  where

  series = a series nickname and resource separated by a comma and an optional
           label
</c:set>
<c:set var="width" value="${empty param.width ? 500 : param.width}"/>
<c:set var="height" value="${empty param.height ? 400 : param.height}"/>
<c:set var="bgcolor" value="${empty param.bgcolor ? '#AAAAFF' : param.bgcolor}"/>
<c:set var="map" value="${empty param.map ? false : param.map}"/>
<c:set var="chart" value="${empty param.chart ? 'single' : param.chart}"/>

<c:out value="${series}"/>
<c:forEach items="${paramValues.series}" var="series">
  <inca:split var="seriesParts" content="${series}" delim="," limit="4"/>
  <c:if test="${fn:length(seriesParts) < 3 }">
    <jsp:forward page="error.jsp">
      <jsp:param name="msg" value="Incorrect series spec '${series}' ${fn:length(seriesParts)}" />
      <jsp:param name="usage" value="${usage}" />
    </jsp:forward>
  </c:if>
</c:forEach>

<c:set var="allmetrics" value="${fn:join(paramValues.metric, ',')}"/>

<%-- Query data and if not results, forward to error page --%>
<c:set var="query">
  <c:out value="${empty param.startDate ? two : param.startDate}"/>,
  <c:out value="${empty param.endDate ? now : param.endDate}"/>,
  <c:forEach items="${paramValues.series}" var="series">
    <inca:split var="seriesParts" content="${series}" delim="," limit="4"/>
    <c:set var="targetClause">AND config.series.targetHostname = '${seriesParts[2]}'</c:set>
    <c:set var="targetClause" value="${seriesParts[2] == '' ? '' : targetClause}"/>
    (config.nickname='<c:out value="${seriesParts[0]}"/>' AND 
    config.series.resource='<c:out value="${seriesParts[1]}"/>' <c:out value="${targetClause}" escapeXml="false"/> ) OR</c:forEach></c:set>
<c:set var="xml"><inca:query command="period" params="${fn:substring(query,0,fn:length(query)-3)}"/></c:set>
<c:if test="${empty xml}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="No data found for series ${fn:join(paramValues.series, ', ')})" />
  </jsp:forward>
</c:if>

<c:if test="${empty param.printXML and empty param.printCSV}">
  <jsp:include page="header.jsp"/>
</c:if>

<c:choose>
  <c:when test="${! empty param.printXML}">
    <c:set var="xmlResult">
      <graph xmlns="http://www.w3.org/1999/xhtml">${xml}</graph>
    </c:set>
<%
  response.setContentType("text/xml");
  response.setCharacterEncoding("ISO-8859-1");
%>
    ${xmlResult}
  </c:when>
  <c:when test="${! empty param.printCSV}">
    <c:import var="xslt" url="/xsl/restGraphCsv.xsl"/>
    <c:set var="csvResult">
      <incaXml:transform doc="${xml}" xslt="${xslt}"/>
    </c:set>
<%
  response.setContentType("text/csv");
  response.setCharacterEncoding("ISO-8859-1");
%>
    ${csvResult}
  </c:when>
  <c:otherwise>
<table cellpadding="10">
<tr><td valign="top">
  <c:choose><c:when test="${allmetrics != ''}">
    <inca:chartPlotMetric xml="${xml}" series="${paramValues.series}" 
                     showlinks="${map}" showmouseovers="${map}" 
                     width="${width}" height="${height}" bgcolor="${bgcolor}" 
                     metric="${allmetrics}"
                     showlegend="${param.legend}" chart="${chart}"
                     legendanchor="${legendAnchor}"/>
  </c:when><c:otherwise>
    <inca:historyGraph xml="${xml}" series="${paramValues.series}" 
                     showlinks="${map}" showmouseovers="${map}" 
                     width="${width}" bgcolor="${bgcolor}" 
                     showlegend="${param.legend}" 
                     legendanchor="${legendAnchor}"/>
  </c:otherwise></c:choose> 
</td><td align="left" valign="top" class="clear">
  <h1>Customize Graph</h1>
  <form method="get" action="graph.jsp" name="form"
        onsubmit="return validate(form);">
    <table class="subheader">
      <tr>
        <td>chart type:</td>
        <td><input name="chart" type="radio" value="single" 
                   ${chart == "single" ? 'checked="yes"' : ''}/> one chart<br/>
            <input name="chart" type="radio" value="series" 
                   ${chart == "series" ? 'checked="yes"' : ''}/> one chart per series<br/>
            <input name="chart" type="radio" value="metric" 
                   ${chart == "metric" ? 'checked="yes"' : ''}/> one chart per metric<br/>
            <input name="chart" type="radio" value="multiple" 
                   ${chart == "multiple" ? 'checked="yes"' : ''}/> one chart per series and metric<br/>
        </td>
      </tr>
      <tr>
        <td>show mouseovers/hyperlinks for datapoints:</td>
        <td><input name="map" type="radio" value="true" ${map ? 'checked="yes"' : ''}/> yes
            <input name="map" type="radio" value="false" ${map ? '' : 'checked="yes"'}/> no</td>
      </tr>
      <tr>
        <td>show legend:</td>
        <td><input name="legend" type="radio" value="true" checked=""/> yes
          <input name="legend" type="radio" value="false"/> no</td>
      </tr>
      <tr>
        <td>legend position:</td>
        <td>
          <input name="legendAnchor" type="radio"
                 value="south" checked=""/> south
          <input name="legendAnchor" type="radio" value="north"/> north
          <input name="legendAnchor" type="radio" value="east"/> east
          <input name="legendAnchor" type="radio" value="west"/> west
        </td>
      </tr>
      <tr>
        <td>width:</td>
        <td>
          <input name="width" type="text" size="5" value="${width}"/>
        </td>
      </tr>
      <tr>
        <td>height:</td>
        <td>
          <input name="height" type="text" size="5" value="${height}"/>
        </td>
      </tr>
      <tr>
        <td>background color:</td>
        <td><input name="bgcolor" type="text"
                   value="${bgcolor}"/> (e.g. #DDDDDD)</td>
      </tr>
      <tr>
        <td>start date:</td>
        <td>
          <c:set var="startDate" 
                 value="${empty param.startDate ? two : param.startDate}"/>
          <input name="startDate" type="text"
                   value="${startDate}"/> (MMddyy format, e.g. "093007")
        </td>
      </tr>
      <tr>
        <td>end date:</td>
        <td><input name="endDate" type="text"
                   value="${param.endDate}"/> (MMddyy format, e.g. "103007")</td>
      </tr>
      <tr><td valign="top"><table>
        <tr><td><h2>Series</h2></td><tr>
        <c:forEach items="${paramValues.series}" var="series">
        <inca:split var="seriesParts" content="${series}" delim="," limit="4"/>
        <tr>
          <td align="left">
            <input type="checkbox" name="series" value="${series}" checked=""/>
            ${seriesParts[3]}
          </td>
        </tr>
        </c:forEach>
        <tr>
          <td>
            <input type="button" name="CheckAll" value="check all"
                   onClick="checkAll(form.series)"/>
            <input type="button" name="UnCheckAll" value="uncheck all"
                   onClick="uncheckAll(form.series)"/>
            <input type="hidden" name="map" value="${map}"/>
          </td>
        </tr>
      </table></td><td valign="top">
        <c:if test="${! empty param.availMetrics && param.availMetrics != ''}"><table>
        <tr><td><h2>Metrics</h2></td><tr>
        <c:forEach items="${fn:split(param.availMetrics,',')}" var="availMetric">
        <tr>
          <td>
            <c:set var="addcheck">
              <c:if test="${fn:contains(allmetrics, availMetric)}">checked=""</c:if>
            </c:set>
            <input type="checkbox" name="metric" value="${availMetric}" ${addcheck}/>
            ${availMetric}
          </td>
        </tr>
        </c:forEach>
        <tr>
          <td>
            <input type="button" name="CheckAll" value="check all"
                   onClick="checkAll(form.metric)"/>
            <input type="button" name="UnCheckAll" value="uncheck all"
                   onClick="uncheckAll(form.metric)"/>
          </td>
        </tr>
      </table></c:if>
      </td></tr>
    </table>
    <br/>
    <input type="hidden" name="map" value="${map}"/>
    <input type="hidden" name="availMetrics" value="${param.availMetrics}"/>
    <input type="submit" name="submit" value="re-graph"/>
  </form>

</td></tr></table>
  <c:if test="${allmetrics == ''}">
    <inca:errorGraph xml="${xml}" series="${paramValues.series}" 
                 showmouseovers="${map}" height="${height}"
                 width="${width}" bgcolor="${bgcolor}"
                 showlegend="${param.legend}" legendanchor="${legendAnchor}"/>
  </c:if>
  <jsp:include page="footer.jsp"/>
  </c:otherwise>
</c:choose>
