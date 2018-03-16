<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>


<jsp:include page="header.jsp"/>
<c:set var="usage">
  Description:  Adds series comments to DB

  Usage: addDbCommentsForm.jsp?series=s1&amp;host=h1&amp;nickname=n1[&amp;author=a1&amp;comment=c1]

  where

  series = the configId for the series (e.g. 4900779)

  host = the hostname for the series (e.g. honest1.ncsa.uiuc.edu)

  nickname = the series nickname (e.g. ant-unit)

  author = the person adding the comment

  comment = the comment text about the series
</c:set>
<c:set var="error">
  ${pageContext.request.scheme != "https" ? 'This page requires SSL.' : '' }
  ${empty param.series ? 'Missing param series' : '' }
  ${empty param.host ? 'Missing param host' : '' }
  ${empty param.nickname ? 'Missing param nickname' : '' }
</c:set>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="${error}" />
    <jsp:param name="usage" value="${usage}" />
  </jsp:forward>
</c:if>

<c:choose>
  <c:when test="${empty param.author or empty param.comment or
		(param.author == '' and param.comment == '')}">
    <table cellpadding="4">
      <tr><td>
        <h3>Comment for the "${param.nickname}" series on ${param.host}</h3>
        <p>Please email ${initParam.dbEmail} with problems using this form.</p>
      </td></tr>
      <form method="post" action="addDbCommentsForm.jsp">
        <tr><td class="header">Comment:</td></tr>
        <tr>
          <td>
            <textarea name="comment"
                      cols="50" rows="10">${param.comment}</textarea>
            <br/>
          </td>
        </tr>
        <tr><td class="header">Name or email:</td></tr>
        <tr>
          <td>
            <input name="author" type="text" size="50" value="${param.author}">
            <br/>
          </td>
        </tr>
        <tr><td>
          <input type="hidden" name="series" value="${param.series}"/>
          <input type="hidden" name="host" value="${param.host}"/>
          <input type="hidden" name="nickname" value="${param.nickname}"/>
          <input type="submit" name="Submit" value="add comment"/>
        </td></tr>
      </form>
    </table>
  </c:when>
  <c:otherwise>
    <c:if test="${!empty initParam.dbDriver}">
      <inca:date var="date" dateFormat="MM-dd-yy, K:mm a (zz)"/>
      <p>The following comment has been added.  Please email
          ${initParam.dbEmail} with any problems.
        <br><br><b>Date:</b> ${date}
        <br><br><b>Author:</b> ${param.author}
        <br><br><b>Comment:</b> <pre> ${param.comment} </pre></p>
      <c:set var="subject" 
             value="New Inca Comment for ${param.nickname} on ${param.host}"/>
      <c:set var="email" value="echo \"Author: ${param.author}

Comment: ${param.comment}\" | mail -s \"${subject}\" ${initParam.dbEmail}"/>
      <% String emailStr = (String)pageContext.getAttribute("email");
        String[] shmail = {"/bin/sh", "-c", emailStr};
        Runtime.getRuntime().exec(shmail); %>
      <sql:setDataSource var="db" driver="${initParam.dbDriver}"
                         url="${initParam.dbUrl}" user="${initParam.dbUser}"
                         password="${initParam.dbPw}"/>
      <sql:update var="insertcomments" dataSource="${db}">
        INSERT INTO incaseriesconfigcomments (incaentered,
        incaseriesconfigid, incaauthor, incacomment)
        VALUES ('${date}', '${param.series}', ?, ?);
        <sql:param value="${param.author}"/>
        <sql:param value="${param.comment}"/>
      </sql:update>
    </c:if>
  </c:otherwise>
</c:choose>
<jsp:include page="footer.jsp"/>
