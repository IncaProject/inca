<%@ tag import="java.util.Calendar" %>

<%@ tag body-content="empty" %>
<%@ tag description="Returns the current date or from a number of days ago" %>

<%@ attribute name="add" required="false" type="java.lang.Integer"
              description="number of days to add to now(default 0)" %>
<%@ attribute name="dateFormat" required="false" type="java.lang.String"
              description="the format of the date string (default MMddyy)" %>
<%@ attribute name="var" rtexprvalue="false" required="true" 
              description="name of the exported scoped variable to hold the date" %>
<%@ variable name-from-attribute="var" variable-class="java.lang.Object"
             alias="varName" scope="AT_END" 
             description="a string contaiing a date" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="add" value="${empty add ? 0 : add}"/>
<c:set var="dateFormat" value="${empty dateFormat ? 'MMddyy' : dateFormat}"/>

<jsp:useBean id="now" class="java.util.GregorianCalendar" />
<c:if test="${add != 0}">
  <% 
    Integer add = (Integer)jspContext.getAttribute( "add"   ); 
    ((Calendar)jspContext.getAttribute( "now" )).add( Calendar.DATE, add ); 
  %>
</c:if>
<fmt:formatDate var="varName" value="${now.time}" pattern="${dateFormat}" />

