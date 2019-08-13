<%@ page trimDirectiveWhitespaces="true" %>

<%@ page import="edu.sdsc.inca.consumer.DepotQuery" %>
<%@ page import="edu.sdsc.inca.util.XmlWrapper" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="incaXml" uri="/WEB-INF/inca.tld" %>

<jsp:useBean id="agentBean" scope="application" class="edu.sdsc.inca.consumer.AgentBean" />

<c:set var="xml">
  <combo>
<%
  String xml = DepotQuery.query("queryReporterSeries", new Object[] { });

  out.print(XmlWrapper.prettyPrint(xml, "  "));
%>
    <jsp:getProperty name="agentBean" property="catalog"/>
  </combo>
</c:set>

<c:choose>
  <c:when test="${empty param.csv}">
    <c:set var="xsltFile" value="${empty param.xsl ? 'configHtml.xsl' : param.xsl}"/>
    <c:import var="xslt" url="/xsl/${xsltFile}"/>
    <inca:getUrl var="url"/>
    <jsp:include page="header.jsp"/>
    <script type="text/javascript" src="${url}/js/hide-col.js"></script>
    <inca:printXmlOrHtml xsl="${xslt}" xml="${xml}"/>
    <jsp:include page="footer.jsp"/>
  </c:when>
  <c:otherwise>
    <c:import var="xslt" url="/xsl/configCsv.xsl"/>
    <c:set var="csv">
      <incaXml:transform doc="${xml}" xslt="${xslt}"/>
    </c:set>
<%
  String csv = (String)pageContext.getAttribute("csv", PageContext.PAGE_SCOPE);

  response.setContentType("text/csv");
  response.setHeader("Content-Length", String.valueOf(csv.length()));
  response.setHeader("Content-Disposition", "attachment; filename=reporters.csv");

  out.print(csv);
%>
  </c:otherwise>
</c:choose>
