<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>

<jsp:include page="header.jsp"/>

<c:set var="usage">
  Description:  Queries depot for knowlege base articles

  Usage: searchKnowledgeBase.jsp?[error=message&amp;nickname=vtk-nvgl_version&amp;reporter=viz.lib.vtk-nvgl.version&amp;xsl=searchKb.xsl]

  where

  error = an optional error message to search for
  nickname = an optional series nickname to search for
  reporter = an optional reporter name to search for
  xsl = an optional xsl stylesheet [default: searchKb.xsl]

</c:set>

<%-- Check input parameters and redirect error if incorrect --%>
<c:set var="xsltFile" value="${empty param.xsl ? 'searchKb.xsl' : param.xsl}"/>
<c:set var="error"> 
  ${empty param.error && empty param.nickname && empty param.reporter ? 
  'Must add param error, nickname, or reporter to search for' : ''}
</c:set>
<%-- Check permissions --%>
<c:import var="kbConfigString" url="/xml/kb.xml"/>
<x:parse var="kbConfig" xml="${kbConfigString}"/>
<x:set var="allowKb" 
       select="string($kbConfig/kb/enable)"/>
<c:if test="${allowKb != 'true'}">
  <c:set var="error" value="Knowledge base has been disabled."/>
</c:if>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp"> 
    <jsp:param name="msg" value="${error}" /> 
    <jsp:param name="usage" value="${usage}" /> 
  </jsp:forward>
</c:if>

<c:set var="select">SELECT id, entered, errorMsg, series, reporter, authorName, authorEmail, articleTitle, articleText FROM KbArticle WHERE</c:set>

<c:set var="where">series='${param.nickname}' OR reporter='${param.reporter}' <c:if test="${!empty param.error}"> OR errorMsg LIKE '\%${param.error}\%' OR articleText LIKE '\%${param.error}\%'</c:if><c:if test="${!empty param.nickname}"> OR articleText LIKE '\%${param.nickname}\%'</c:if><c:if test="${!empty param.reporter}"> OR articleText LIKE '\%${param.reporter}\%'</c:if> ORDER BY entered DESC</c:set>

<c:set var="hql">${select} ${where}</c:set>

<c:set var="kbxml">
  <articles>
    <hql>${where}</hql>
    <inca:query command="hql" params="${hql}" prettyprint="true"/>
  </articles>
</c:set>

<c:import var="xslt" url="../xsl/${xsltFile}"/>
<inca:printXmlOrHtml xsl="${xslt}" xml="${kbxml}"/>
<jsp:include page="footer.jsp"/>
