<%@ page import="java.util.TreeMap" %>
<%@ page import="edu.sdsc.inca.consumer.CategoryBean" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="incaXml" uri="/WEB-INF/inca.tld" %>
<%@ taglib uri='/WEB-INF/cewolf.tld' prefix='cewolf' %>

<jsp:include page="header.jsp">
  <jsp:param name="title" value="Inca History"/>
</jsp:include>

<%-- Check inputs and redirect to error if incorrect --%>
<c:set var="usage">  
Description:  display average series pass rate by filter

summaryHistory.jsp?groupBy=suite|resource[&amp;lines=multiple|total][&amp;filterResource=resource1,resource2][&amp;filterSuite=guid1,guid2]

groupBy: if suite, suite graphs will be displayed; if resource, resource graphs
         will be displayed [default: resource]

lines: if total, a single line will be displayed summarizing the total; if multiple,
       multiple lines will be displayed [default: total]

filterResource: display resource info from specific resources

filterSuite: displays suite info from specific suites 
</c:set>

<c:if test="${ (! empty param.groupBy && param.groupBy != 'suite' && 
                                         param.groupBy != 'resource') ||
               (! empty param.lines && param.lines != 'multiple' && 
                                       param.lines != 'total') }">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" 
               value="Incorrect value for groupBy or lines" />
    <jsp:param name="usage" value="${usage}" />
  </jsp:forward>
</c:if>

<c:set var="bgcolor" value="#FFFFFF"/>
<c:set var="height" value="300"/>
<c:set var="width" value="500"/>
<c:set var="groupBy" 
       value="${empty param.groupBy ? 'resource' : param.groupBy}"/>
<c:set var="lines" 
       value="${empty param.lines ? 'multiple' : param.lines}"/>

<%-- get series history from cache --%>
<c:set var="xml">
  <inca:queryCache bean="${depotBean}" name='incaQueryStatus'/>
</c:set>
<c:if test="${empty xml or xml == 'null'}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="Missing query...wait 30 seconds and reload" />
  </jsp:forward>
</c:if>

<%-- compute series pass percentage via a stylesheet --%>
<c:import var="computeSeriesAverages" url="/xsl/seriesAverages.xsl"/>
<c:set var="seriesAveragesXml">
  <incaXml:transform doc ="${xml}" xslt="${computeSeriesAverages}">
    <incaXml:param name="sort" value="ascending"/>
    <incaXml:param name="ignoreErrs" value="${depotBean.ignoreErrorPattern}"/>
  </incaXml:transform>
</c:set>

<%-- compute pass percentage for suites and resources by week, etc. --%>
<c:import var="periodAverages" url="/xsl/periodAverages.xsl"/>
<c:set var="periodAveragesXml">
  <incaXml:transform doc ="${seriesAveragesXml}" xslt="${periodAverages}">
  <c:if test="${groupBy == 'resource' and lines == 'total'}">
    <incaXml:param name="resourceTotal" value="true"/>
  </c:if>
  <c:if test="${groupBy == 'suite' and lines == 'total'}">
    <incaXml:param name="suiteTotal" value="true"/>
  </c:if>
  <c:if test="${! empty param.filterResource}">
    <incaXml:param name="resource" value="${fn:join(paramValues.filterResource, ',')}"/>
  </c:if>
  <c:if test="${! empty param.filterSuite}">
    <incaXml:param name="guid" value="${fn:join(paramValues.filterSuite,',')}"/>
  </c:if>
  </incaXml:transform>
</c:set>

<%-- Read in averages to CategoryBeans --%>
<x:parse var="doc" doc="${periodAveragesXml}"/>
<% 
  TreeMap<String, CategoryBean> beans = new TreeMap<String, CategoryBean>(); 
  pageContext.setAttribute( "averages", beans );
%>
<c:set var="statusStartDate" value=""/>
<c:set var="statusEndDate" value=""/>
<x:forEach select="$doc/seriesSummary/period" var="period" varStatus="i">
  <%-- get start and end dates --%>
  <x:set var="begin" select="string($period/begin)"/>
  <x:set var="end" select="string($period/end)"/>
  <inca:time2date var="beginDate" millis="${begin}" dateFormat="MM/dd/yy"/>
  <inca:time2date var="endDate" millis="${end}" dateFormat="MM/dd/yy"/>
  <c:if test="${i.count == 1}">
    <c:set var="statusStartDate" value="${beginDate}"/></c:if>
  <c:if test="${i.last}">
    <c:set var="statusEndDate" value="${endDate}"/></c:if>

  <x:set var="periodAverages" select="$period"/>

  <%-- read in values to category datasets --%>
  <x:forEach select="$periodAverages" var="avg">
    <x:set var="seriesCount" select="string($avg/seriesCount)"/>
    <c:if test="${seriesCount>0}">
      <x:set var="resource" select="string($avg/resource)"/>
      <x:set var="guid" select="string($avg/guid)"/>
      <c:if test="${! empty guid}">
        <c:set var="suite"><inca:guid2suite guid="${guid}"/></c:set>
      </c:if>
      <c:choose><c:when test="${groupBy == 'resource'}">
        <c:set var="id" value="${resource}"/>
      </c:when><c:otherwise>
        <c:set var="id" value="${suite}"/>
      </c:otherwise></c:choose>
      <x:set var="pass" select="string($avg/passPercentage)"/>
      <fmt:setLocale value="en_US"/>
      <fmt:formatNumber var="pass" value="${pass}" maxFractionDigits="1"/>
      <% String id = (String)pageContext.getAttribute( "id" ); 
         if ( ! beans.containsKey(id) ) { 
           CategoryBean bean = new CategoryBean( id );
           beans.put( id,  bean ); 
           pageContext.setAttribute("avg"+id, bean, PageContext.REQUEST_SCOPE);
         } 
      %>
      <c:if test="fn:contains(pass, ',')"><c:set var="pass" value="0"/></c:if> 
      <c:choose><c:when test="${groupBy == 'resource' && lines == 'total' }"> 
        <inca:addValue dataset="avg${resource}" value="${pass}" row="${resource}" 
                       col="${beginDate} - ${endDate}" tooltip="${pass}%"
                       url="/inca/jsp/summaryHistory.jsp?groupBy=suite&lines=multiple&filterResource=${resource}"/>
      </c:when><c:when test="${groupBy == 'resource' && lines == 'multiple' }">
        <inca:addValue dataset="avg${resource}" value="${pass}" row="${suite}" 
                       col="${beginDate} - ${endDate}" tooltip="${pass}%"
                       url="/inca/jsp/summaryHistory.jsp?groupBy=suite&lines=multiple&filterResource=${resource}"/>
      </c:when><c:when test="${groupBy == 'suite' && lines == 'total' }"> 
        <inca:addValue dataset="avg${suite}" value="${pass}" row="${suite}" 
                       col="${beginDate} - ${endDate}" tooltip="${pass}%"
                       url="/inca/jsp/summaryHistory.jsp?groupBy=resource&lines=multiple&filterSuite=${guid}"/>
      </c:when><c:when test="${groupBy == 'suite' && lines == 'multiple' }">
        <inca:addValue dataset="avg${suite}" value="${pass}" row="${resource}" 
                       col="${beginDate} - ${endDate}" tooltip="${pass}%"
                       url="/inca/jsp/summaryHistory.jsp?groupBy=resource&lines=multiple&filterSuite=${guid}"/>
      </c:when></c:choose>
    </c:if>
  </x:forEach>
</x:forEach>

<%-- print header description --%>
<a name="top"/>
<table width="100%">
<tr valign="top"><td>
<h1>Average series pass rate history ( ${statusStartDate} - ${statusEndDate} )</h1>
<c:if test="${empty statusStartDate or empty statusEndDate}">
  <h1 class="passText">Query data not available yet. Please come back later or refresh the cache</h1>
</c:if>
<%-- Print shortcuts --%>
<table><tr>
<c:forEach items="${averages}" var="avg" varStatus="i">
<c:if test="${(i.count % 4) == 1}">
<td><ul>
</c:if>
<li><a href="#${avg.key}">${avg.key}</a></li>
<c:if test="${(i.count % 4) == 0 || i.last}">
</ul></td>
</c:if>
</c:forEach>
</tr></table>
</td>
<td align="right">
<c:import var="printSelectForm" url="/xsl/printSelectForm.xsl"/>
<inca:printXmlOrHtml xml="${seriesAveragesXml}" xsl="${printSelectForm}" 
                     filterSuite="${fn:join(paramValues.filterSuite, ',')}" 
                     filterResource="${fn:join(paramValues.filterResource, ',')}"
                     groupBy="${param.groupBy}" lines="${param.lines}"/>
</td></tr></table>


<%-- Plot as line graphs --%>
<table>
<c:forEach items="${averages}" var="avg" varStatus="i">
<c:if test="${(i.count % 2) == 1}">
<tr> 
</c:if>
<td align="center">
<a name="${avg.key}">
  <h1>Average series pass rate for '${avg.key}'</h1></a>
[<a href="#top">top of page</a>]
<br/>
<br/>
<cewolf:chart id="hist${idx}${i.count}" type="line" xaxislabel="Time"
              yaxislabel="%"
                        antialias="true" showlegend="true"
                        legendanchor="north">
  <cewolf:colorpaint color="${bgcolor}"/>
      <cewolf:data>
        <cewolf:producer id="avg${avg.key}"/>
      </cewolf:data>
      <inca:categoryPostProcess var="catPP" xaxisHeight="125" ymin="-5" 
                                ymax="105"/>
      <cewolf:chartpostprocessor id="catPP"/>
</cewolf:chart>
  <cewolf:img chartid="hist${idx}${i.count}" renderer="/cewolf" mime="image/png"
              width="${width}" height="${height}">
    <cewolf:map tooltipgeneratorid="avg${avg.key}"
                linkgeneratorid="avg${avg.key}"/>
  </cewolf:img>
</td>
<c:if test="${(i.count % 2) == 0 || i.last}">
</tr>
</c:if>
</c:forEach>
</table>

<jsp:include page="footer.jsp"/>
