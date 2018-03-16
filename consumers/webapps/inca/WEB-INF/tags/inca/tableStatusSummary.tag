<%@ tag import="java.util.HashMap" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<%@ tag body-content="empty" %>
<%@ tag description="Print a table of the pass/fail/unknown values for specified series." %>  

<%@ attribute name="series" type="java.lang.Object" required="true" 
              description="an array of strings of format series nickname,resource[,label]" %>
<%@ attribute name="xml" required="true" 
              description="string containing xml where report statuses will be extracted from" %>

<%-- Print counts --%>
<inca:distribution var="status" xml="${xml}">
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
  <inca:distributionRow bean="${status}"  rowkey="${label}" 
                          xpath="${node}" statusAsFloat="true" />
</c:forEach>
</inca:distribution>

<table class="header">
<tr>
  <td class="header">Test name, resource</td>
  <td class="header" align="center"># Total Known Reports</td>
  <td class="header" align="center"># Passed Reports</td>
  <td class="header" align="center"># Failed Reports</td>
  <td class="header" align="center"># Total Unknown Reports</td>
  <td class="header" align="center">% Passed Reports</td>
</tr>
<c:forEach items="${status.rowKeys}" var="row" varStatus="i">
   <c:set var="rowIdx" value="${i.count-1}"/>
  <inca:distributionValue var="pass" bean="${status}" 
                              row="${rowIdx}" colValue="1.0"/>
  <inca:distributionValue var="fail" bean="${status}" 
                              row="${rowIdx}" colValue="-1.0"/>
  <inca:distributionValue var="unknown" bean="${status}" 
                              row="${rowIdx}" colValue="0.0"/>
  <tr>
    <td class="clear">${row}</td>
    <td class="clear" align="center">${pass + fail}</td>
    <td class="clear" align="center">${pass}</td>
    <td class="clear" align="center">${fail}</td>
    <td class="clear" align="center">${unknown}</td>
    <td class="clear" align="center"> 
      <fmt:formatNumber value="${(pass / (pass+fail)) * 100}" 
                        maxFractionDigits="0"/>%
    </td>
  </tr>
</c:forEach>
</table>

