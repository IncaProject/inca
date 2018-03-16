<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>


<c:set var="usage">
  DESCRIPTION: Page for managing stored queries in the Consumer.  Queries
  can be added, deleted, changed, viewed or executed (and return XML).
  This jsp page manages the query manipulation via jsp tags and leaves the
  display of the current queries to a stylesheet.

  USAGE: query.jsp?[action=[Add,Change,Delete,View,Execute]&lt;actionParams&gt;][&amp;xsl=query.xsl]

  where

  action: An action to take on the query (see below for more information)
  xsl:    An optional stylesheet name to apply to the query store XML for the
          display of queries and manipulation options [default: query.xsl]

  Actions:

  Add:      add a query to the Consumer query store
    Required params:
      - qname:    Name of query to add
      - qtype:    A string containing the type of depot query (e.g. "latest")
      - qparams:  Where clause(s) for query (e.g. config.nickname='n1')
    Optional params:
      - period:   An integer containing the frequency of fetches
                  from the depot (seconds) or 0 for no prefetching.
      - wday: An integer indicating the day of week to fetch query
      - hour: An integer indicating the hour of the day to fetch query
      - min: An integer indicating the min the hour to fetch query

  Change:   change the details of a query
    Params are same as "Add" action

  Delete:  delete a query from the Consumer query store
    Required params:
      - qname: A string containing the name of the query to delete

  View:    retrieve a stored query and return the results in XML
    Required params:
      - qname: A string containing the name of the query to retrieve

  Execute: execute a query and return the results in XML
    Required params:
      - qtype:  A string containing the type of depot query (e.g. "latest")
      - qparams: Where clause(s) for query (e.g. config.nickname='n1')
</c:set>


<jsp:useBean id="depotBean" scope="application"
             class="edu.sdsc.inca.consumer.DepotBean" />

<%-- ------------------------------------------------------------------- --%>
<%-- Verify parameters.                                                  --%>
<%-- ------------------------------------------------------------------- --%>
<c:set var="xsltFile" value="${empty param.xsl ? 'query.xsl' : param.xsl}"/>
<c:set var='action' value="${empty param.action ? null : param.action}"/>
<c:set var='qtype' value="${empty param.qtype ? null : param.qtype}"/>
<c:set var='qparams' value="${empty param.qparams ? null : param.qparams}"/>
<c:set var='qname' value="${empty param.qname ? null : param.qname}"/>
<c:set var='period' value="${empty param.period ? 0 : param.period}"/>
<c:set var='wday' value="${empty param.wday ? '*': param.wday}"/>
<c:set var='hour' value="${empty param.hour ? '*' : param.hour}"/>
<c:set var='min' value="${empty param.min ? '*' : param.min}"/>
<c:set var='escapeXml' value="${empty param.escapeXml ? true : param.escapeXml}"/>
<c:import var="xslt" url="/xsl/${xsltFile}"/>

<c:if test="${escapeXml}">
  <jsp:include page="header.jsp">
    <jsp:param name="title" value="Inca Query Page"/>
  </jsp:include>
</c:if>

<% pageContext.setAttribute("cr","\n"); %>
<% pageContext.setAttribute("slashr","\r"); %>
<c:set var="qparams" value='${fn:replace(qparams, cr, " ")}'/>
<c:set var="qparams" value='${fn:replace(qparams, slashr, " ")}'/>

<c:set var="error">
  ${action!='Add' and action!='Change' and action!='Execute' and action!='View'
      and action!='Delete' and action!='Refresh' and 
      !empty action ? 'Action param is invalid' : '' }
  ${(action=='Add' or action=='Change') && (empty qname or empty qtype
      or empty qparams) ? 'Params qname, qtype and qparams required' : '' }
  ${action=='Execute' && (empty qtype or empty qparams)
      ? 'Params qtype and qparams require to execute query' : '' }
  ${(action=='View' or action=='Delete' or action=='Refresh')&& empty qname
      ? 'Param qname required' : '' }
</c:set>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="${error}" />
    <jsp:param name="usage" value="${usage}" />
  </jsp:forward>
</c:if>

<c:choose>
  <%-- ------------------------------------------------------------------- --%>
  <%-- Service a straight query request for the depot                      --%>
  <%-- ------------------------------------------------------------------- --%>
  <c:when test="${action=='Execute'}">
    <c:set var="xml">
      <inca:query command="${qtype}" params="${qparams}"/>
    </c:set>
    <c:if test="${escapeXml}"><pre></c:if>
      <c:out value="${xml}" escapeXml="true"/>
    <c:if test="${escapeXml}"></pre></c:if>
  </c:when>

  <%-- ------------------------------------------------------------------- --%>
  <%-- Service a stored query request                                      --%>
  <%-- ------------------------------------------------------------------- --%>
  <c:when test="${action=='View'}">
    <c:set var="xml">
      <inca:queryCache bean="${depotBean}" name="${qname}"/>
    </c:set>
    <c:if test="${escapeXml}"><pre></c:if>
      <c:out value="${xml}" escapeXml="${escapeXml}"/>
    <c:if test="${escapeXml}"></pre></c:if>
  </c:when>

  <%-- ------------------------------------------------------------------- --%>
  <%-- Service an Add, Change, or Delete, and/or display queries and       --%>
  <%-- manipulation options                                                --%>
  <%-- ------------------------------------------------------------------- --%>
  <c:when test="${action=='Delete'}">
    <inca:queryCache bean="${depotBean}" action="Delete" name="${qname}"/>
  </c:when>
  <c:when test="${action=='Add'}">
    <inca:queryCache bean="${depotBean}" action="Add" name="${qname}"
                     period="${period}" reloadAt="${wday}:${hour}:${min}"
                     command="${qtype}" params="${qparams}"/>
  </c:when>
  <c:when test="${action=='Change'}">
    <inca:queryCache bean="${depotBean}" action="Delete" name="${qname}"/>
    <inca:queryCache bean="${depotBean}" action="Add" name="${qname}"
                     period="${period}" reloadAt="${wday}:${hour}:${min}"
                     command="${qtype}" params="${qparams}"/>
  </c:when>
  <c:when test="${action=='Refresh'}">
    <inca:queryCache bean="${depotBean}" action="Refresh" name="${qname}"/>
  </c:when>
  <c:otherwise/>
</c:choose>

<%-- Display forms using xsl --%>
<c:set var="xml">
  <queryInfo>
      ${depotBean.queriesXml}
  </queryInfo>
</c:set>

<c:if test="${escapeXml}">
<inca:printXmlOrHtml xsl="${xslt}" xml="${xml}" />
<jsp:include page="footer.jsp"/>
</c:if>
