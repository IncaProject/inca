<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<jsp:useBean id="agentBean" scope="application" 
             class="edu.sdsc.inca.consumer.AgentBean" />
<jsp:useBean id="depotBean" scope="application" 
             class="edu.sdsc.inca.consumer.DepotBean" />

<jsp:include page="header.jsp"/>
<c:set var="usage">
  Description:  Queries depot for latest suite instances for provided
  suiteNames.  Wraps returned xml in &lt;combo&gt; tag along with selected
  resource group resources and optional xml file.  Then invokes chosen
  stylesheet on xml.  Use xsl=none to view xml.

  Usage:  current.jsp?queryNames=q1|suiteNames=s1,...&amp;resourceIds=r1,...[&amp;xsl=default.xsl][&amp;xml=x1.xml,...]

  where

  xsl = an optional xsl stylesheet [default: default.xsl]

  xml = a comma separated list of xml files to include.  Can be empty, contain
    one xml file or must match number of suiteNames and resourceIds if
    queryNames is empty.

  queryNames = a comma separated list of cached query names.  At least one name
    is required if suiteNames is empty.

  resourceIds = a comma separated list of resource group ids.  At least one
    resource id is required if suiteNames is not empty and the number of
    resourceIds should match the number of suiteNames.  A resource id is 
    optional if queryNames is provided.

  suiteNames = a comma separated list of suite names.  At least one suite name
    is required if queryNames is empty and the number of suite names
    should match the number of resourceIds.  A suite name is optional if 
    queryNames is provided.
</c:set>

<%-- Check input parameters and redirect error if incorrect --%>
<c:set var="xsltFile" value="${empty param.xsl ? 'default.xsl' : param.xsl}"/>

<c:set var="xmlFiles" value=
       "${empty param.xml ? null : fn:split(param.xml, ',')}"/>
<c:set var="queryNames" value=
       "${empty param.queryNames ? null : fn:split(param.queryNames, ',')}"/>
<c:set var="suiteNames" value=
       "${empty param.suiteNames ? null : fn:split(param.suiteNames, ',')}"/>
<c:set var="resourceIds" value=
       "${empty param.resourceIds ? null : fn:split(param.resourceIds, ',')}"/>
<c:set var="error"> 
  ${empty suiteNames && empty queryNames ?
     'Missing param suiteNames or queryNames' : '' }
  ${fn:length(resourceIds) > 1 && fn:length(resourceIds) != fn:length(suiteNames) ? 
     'Number of resourceIds must be 0 or 1 or match number of suiteNames' : ''} 
  ${! empty xmlFiles && fn:length(xmlFiles) > 1 && 
      fn:length(xmlFiles) != fn:length(suiteNames) ? 
     'Number of xml files must be 0 or 1 or match number of suiteNames' : ''} 
</c:set>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp"> 
    <jsp:param name="msg" value="${error}" /> 
    <jsp:param name="usage" value="${usage}" /> 
  </jsp:forward>
</c:if>

<%-- Wrap xml and run xslt over it if we have one; otherwise print xml --%>
<c:import var="xslt" url="/xsl/${xsltFile}"/>
<c:set var="xml">
  <combo>
    <c:choose>
      <c:when test="${!empty queryNames}">
        <queries>
          <c:forEach items="${queryNames}" var="queryName">
            <query>
              <c:set var="qName" value='${fn:replace(queryName, " ", "+")}'/>
              <name>${qName}</name>
              <resourceId>${resourceIds[i.count-1]}</resourceId>
              <inca:queryCache bean="${depotBean}" name="${qName}"/>
              <c:if test="${fn:length(resourceIds)>1}">
                <inca:resources name="${resourceIds[i.count-1]}" 
                  bean="${agentBean}" macros="__regexp__,__equivalent__" /> 
              </c:if>
              <c:if test="${fn:length(xmlFiles)>1}">
                <c:import var="xml" url="/xml/${xmlFiles[i.count-1]}"/>
                ${xml}
              </c:if>
            </query>
          </c:forEach>
        </queries>
      </c:when><c:otherwise>
        <suites>
          <c:forEach items="${suiteNames}" var="suiteName" varStatus="i">
            <suite>
              <guid>${agentBean.uri}/${suiteName}</guid>
              <name>${suiteName}</name>
              <resourceId>${resourceIds[i.count-1]}</resourceId>
              <inca:queryCache bean="${depotBean}" 
                               suite='${agentBean.uri}/${suiteName}'/>
              <c:if test="${fn:length(resourceIds)>1}">
                <inca:resources name="${resourceIds[i.count-1]}" 
                  bean="${agentBean}" macros="__regexp__,__equivalent__" /> 
              </c:if>
              <c:if test="${fn:length(xmlFiles)>1}">
                <c:import var="xml" url="/xml/${xmlFiles[i.count-1]}"/>
                ${xml}
              </c:if>
            </suite> <!-- ${suiteName} -->
          </c:forEach>
        </suites>
      </c:otherwise>
    </c:choose>
    <c:choose><c:when test="${fn:length(resourceIds)==1}">
      <inca:resources name="${resourceIds[0]}" bean="${agentBean}"
                      macros="__regexp__,__equivalent__"/> 
    </c:when><c:when test="${fn:length(resourceIds)==0}">
      <inca:resources bean="${agentBean}"
                      macros="__regexp__,__equivalent__" />
    </c:when></c:choose>
    <c:if test="${fn:length(xmlFiles)==1}">
      <c:import var="xml" url="/xml/${xmlFiles[0]}"/>${xml}</c:if>
    <c:if test="${xsltFile=='create-query.xsl'}">
        ${depotBean.queriesXml}
    </c:if>
  </combo>
</c:set>
<inca:date var="startDate" add="-14" />
<inca:getUrl var="queryStr" query="1"/>
<inca:printXmlOrHtml xsl="${xslt}" xml="${xml}" 
      startDate="${startDate}" queryStr="${queryStr}"
      resourceIds="${resourceIds}" suiteNames="${suiteNames}"
      topSeriesErrs="${param.topSeriesErrs}"
      startErrs="${param.startErrs}"
      endErrs="${param.endErrs}"
      hostnamePort="${pageContext.request.serverName}:${pageContext.request.serverPort}"
      ignoreErrs="${depotBean.ignoreErrorPattern}"/>
<jsp:include page="footer.jsp"/>
