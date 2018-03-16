<%@ tag import="java.util.Date" %>

<%@ tag body-content="empty" %>
<%@ tag description="Convert a timestamp in millis to a string date" %>

<%@ attribute name="millis" required="true" type="java.lang.Long"
              description="timestamp in milliseconds" %>
<%@ attribute name="dateFormat" required="false" type="java.lang.String"
              description="the format of the date string (default MMddyy)" %>
<%@ attribute name="var" rtexprvalue="false" required="true" 
              description="name of the exported scoped variable to hold the date" %>
<%@ variable name-from-attribute="var" variable-class="java.lang.Object"
             alias="varName" scope="AT_END" 
             description="a string contaiing a date" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="dateFormat" value="${empty dateFormat ? 'MMddyy' : dateFormat}"/>

<jsp:useBean id="date" class="java.util.Date" />
<jsp:setProperty name="date" property="time" value="${millis}"/>
<fmt:formatDate var="varName" value="${date}" pattern="${dateFormat}" />

