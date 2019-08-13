<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="incaXml" uri="/WEB-INF/inca.tld" %>

<jsp:include page="header.jsp">
  <jsp:param name="title" value="Inca Error Page"/>
</jsp:include>
<c:import var="xslt" url="/xsl/error.xsl"/>
<incaXml:transform xslt="${xslt}">
<error>
  <message>${param.msg}</message>
  <usage>${param.usage}</usage>
</error>
</incaXml:transform>
<jsp:include page="footer.jsp"/>
