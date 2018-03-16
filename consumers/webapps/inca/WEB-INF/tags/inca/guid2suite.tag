<%@ tag import="java.util.Date" %>

<%@ tag body-content="empty" %>
<%@ tag description="Extract the Inca suite name from a Inca guid" %>

<%@ attribute name="guid" required="true" type="java.lang.String"
              description="an Inca guid" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="guidParts" value="${fn:split(guid,'/')}"/>
<c:out value="${guidParts[fn:length(guidParts)-1]}"/>
