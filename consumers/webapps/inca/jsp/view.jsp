<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- Display pages from view.xml --%>
<c:set var="uri" value="${pageContext.request.requestURI}"/>
<c:set var="request" value="${fn:substringAfter(uri, '/inca/view/')}"/>

<c:import var="viewxml" url="/xml/views.xml"/>
<c:choose><c:when test="${! empty viewxml}">
  <x:parse var="viewobj" xml="${viewxml}"/>
  <x:choose><x:when select="$viewobj/views/view[@id=$pageScope:request]">
    <x:set var="view" select="$viewobj/views/view[@id=$pageScope:request]"/>
    <x:set var="jsp" select="$view/jsp"/>
    <c:set var="url">/jsp/<x:out select="$view/jsp"/>?<x:forEach var="param" select="$view/param"><x:out select="string($param/@id)"/>=<x:out select="string($param)"/>&</x:forEach></c:set>
    <jsp:forward page="${url}"/>
  </x:when><x:otherwise>
    <jsp:forward page="jsp/error.jsp">
      <jsp:param name="msg" value="Unknown view id '${request}'" />
      <jsp:param name="usage" value="" />
    </jsp:forward>
  </x:otherwise></x:choose>
</c:when><c:otherwise>
  <jsp:forward page="jsp/error.jsp">
    <jsp:param name="msg" value="Unable to find views.xml" />
    <jsp:param name="usage" value="" />
  </jsp:forward>
</c:otherwise></c:choose>

