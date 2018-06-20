<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="xsl" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="header.jsp"/>
<br/>
<h1 class="body">View/Change Inca Configuration</h1>
<br/>

<c:set var="configFiles" value="${empty param.config ? 'runNow,kb,google,weekSummary,statusReport' : fn:split(param.config,',')}"/>
<c:choose>
  <c:when test="${! empty param.file && ! empty param.xml}">
    <inca:saveToFile content="${param.xml}" file="/xml/${param.file}.xml"/>
  </c:when>
  <c:when test="${! empty param.defaults}">
    <c:import var="xml" url="/xml/${param.defaults}Defaults.xml"/>
    <inca:saveToFile content="${xml}" file="/xml/${param.defaults}.xml"/>
  </c:when>
  <c:otherwise>
    <link rel="stylesheet" type="text/css" href="/inca/css/ext-all.css" />
    <script type="text/javascript" src="/inca/js/ext/ext-base-debug.js"></script>
    <script type="text/javascript" src="/inca/js/ext/ext-all-debug.js"></script>
    <script type="text/javascript" src="/inca/js/ext/ext-inca.js"></script>
    <p>This page requires javascript to be enabled and is best viewed in Firefox.  Edit configuration values by double clicking on them.  Add, delete or restore values using the toolbar buttons.  Expand or collapse all values using the + and - buttons.  Changes are saved automatically.</p><br/>
    <c:forEach items="${configFiles}" var="conf">
      <script type="text/javascript" src="/inca/js/ext/${conf}.js"></script>
      <p><div id="${conf}"></div><br/></p>
    </c:forEach>
  </c:otherwise>
</c:choose>

<jsp:include page="footer.jsp"/>
