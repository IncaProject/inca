<%@ page trimDirectiveWhitespaces="true" %>

<%@ page import="edu.sdsc.inca.consumer.DepotQuery" %>
<%@ page import="edu.sdsc.inca.util.XmlWrapper" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<c:set var="xml">
<%
  String xml = DepotQuery.query("queryReporterSeriesDetail", new Object[] { });

  out.print(XmlWrapper.prettyPrint(xml, "  "));
%>
</c:set>

<c:choose>
  <c:when test="${empty param.csv}">
    <c:set var="xsltFile" value="${empty param.xsl ? 'configDetailHtml.xsl' : param.xsl}"/>
    <c:import var="xslt" url="/xsl/${xsltFile}"/>
    <inca:getUrl var="url"/>
    <jsp:include page="header.jsp"/>
    <script type="text/javascript" src="${url}/js/hide-col.js"></script>
    <inca:printXmlOrHtml xsl="${xslt}" xml="${xml}"/>
    <jsp:include page="footer.jsp"/>
  </c:when>
  <c:otherwise>
    <c:import var="xslt" url="/xsl/configDetailCsv.xsl"/>
    <c:set var="csv">
      <x:transform doc="${xml}" xslt="${xslt}"/>
    </c:set>
<%
  String csv = (String)pageContext.getAttribute("csv", PageContext.PAGE_SCOPE);

  response.setContentType("text/csv");
  response.setHeader("Content-Length", String.valueOf(csv.length()));
  response.setHeader("Content-Disposition", "attachment; filename=reporters_detail.csv");

  out.print(csv);
%>
  </c:otherwise>
</c:choose>
