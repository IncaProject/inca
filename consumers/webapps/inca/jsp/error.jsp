<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<jsp:include page="header.jsp">
  <jsp:param name="title" value="Inca Error Page"/>
</jsp:include>
<c:import var="xslt" url="/xsl/error.xsl"/>
<x:transform xslt="${xslt}">
<error>
  <message>${param.msg}</message>
  <usage>${param.usage}</usage>
</error>
</x:transform>
<jsp:include page="footer.jsp"/>
