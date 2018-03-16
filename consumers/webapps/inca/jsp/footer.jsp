<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>
<c:if test="${empty param.noFooter}">
  <inca:executeXslTemplate name="footer"/>
</body>
</html>
</c:if>
