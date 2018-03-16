<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>


<jsp:include page="header.jsp"/>
<c:set var="usage">
  Description:  Adds/edits/deletes series comments in DB

  Usage: addKnowledgeBase.jsp?nickname=tgresid-version-4.2.0&amp;reporter=cluster.admin.tgresid.version[&amp;error=error message&amp;author=Kate Ericson&amp;email=kericson@sdsc.edu&amp;text=new content to add to knowledge base]

  where

  nickname = series nickname (e.g. tgresid-version-4.2.0)

  reporter = reporter name (e.g. cluster.admin.tgresid.version)

  error = error message for the series

  author = the person adding to the knowledge base

  email = email of the person adding to the knowledge base
  
  text = the text to add to the knowledge base
</c:set>
<c:set var="error">
  ${empty param.nickname ? 'Missing param nickname' : '' }
  ${empty param.reporter ? 'Missing param reporter' : '' }
</c:set>
<c:if test="${error != ''}">
  <jsp:forward page="error.jsp">
    <jsp:param name="msg" value="${error}" />
    <jsp:param name="usage" value="${usage}" />
  </jsp:forward>
</c:if>

<c:choose>
  <c:when test="${empty param.author or !empty param.id}">
    <script type="text/javascript">
      function validate(form){
        var err = "";
        var checked = 0;
        if (!form.email.value){ err += "\nAuthor's email address is required\n"; }
        if (!form.author.value){ err += "\nAuthor's name is required\n"; }
        if (!form.text.value){ err += "\nArticle text is required\n"; }
        if (!form.title.value){ err += "\nTitle is required\n"; }
        if(err != ""){ alert(err); return false; }
        return true;
      }
    </script>
    <table cellpadding="4">
      <tr><td>
        <h3>New knowledge base text for the "${param.nickname}" series</h3>
      </td></tr>
      <form method="post" action="addKnowledgeBase.jsp" name="form" onsubmit="return validate(form);">
        <tr><td class="header">Title:</td></tr>
        <tr>
          <td>
            <c:set var="titleName">
              ${empty param.title ? param.nickname : param.title }
            </c:set>
            <input name="title" type="text" size="50" value="${titleName}">
            <br/>
          </td>
        </tr>
        <tr><td class="header">Text:</td></tr>
        <tr>
          <td>
            <c:set var="printError">Error message:
------------
${param.error}
------------
            </c:set>
            <c:set var="errorMessage">${empty param.error ? '': printError}</c:set>
            <textarea name="text" cols="50" rows="20">${empty param.text ?  errorMessage : param.text}</textarea><br/>
          </td>
        </tr>
        <tr><td class="header">Name:</td></tr>
        <tr>
          <td>
            <input name="author" type="text" size="50" value="${param.author}">
            <br/>
          </td>
        </tr>
        <tr><td class="header">Email:</td></tr>
        <tr>
          <td>
            <input name="email" type="text" size="50" value="${param.email}">
            <br/>
          </td>
        </tr>
        <tr><td>
          <input type="hidden" name="nickname" value="${param.nickname}"/>
          <input type="hidden" name="reporter" value="${param.reporter}"/>
          <input type="hidden" name="error" value="${param.error}"/>
          <input type="hidden" name="edit" value="${param.id}"/>
          <input type="submit" value="add to knowledge base"/>
        </td></tr>
      </form>
    </table>
  </c:when>
  <c:otherwise>
    <inca:date var="date" dateFormat="MM-dd-yy, K:mm a (zz)"/>
    <c:set var="action"> 
      <c:choose>
        <c:when test="${!empty param.delete}">deleted</c:when>
        <c:when test="${!empty param.edit and empty param.delete}">edited</c:when>
        <c:otherwise>added</c:otherwise>
      </c:choose>
    </c:set> 
    <p>The following knowledge base text has been ${action}:
      <br><br><b>Date:</b> ${date}
      <br><br><b>Author:</b> ${param.author}
      <br><br><b>Email:</b> ${param.email}
      <br><br><b>Title:</b> ${param.title}
      <br><br><b>Text:</b> <pre> ${param.text} </pre></p>
      <br/><form method="POST" action="Javascript:history.go(-3)">
        <input type="submit" value="Go Back" name="Back"/> 
      </form>
    <c:import var="kbConfigString" url="/xml/kb.xml"/>
    <x:parse var="kbConfig" xml="${kbConfigString}"/>
    <c:set var="kbEmail"><x:out select="$kbConfig/kb/submitEmailNotification"/></c:set>
    <c:if test="${kbEmail != 'none'}">
      <c:set var="subject" 
           value="${action} knowledge base text for ${param.nickname} from Inca form"/>
      <c:set var="email" value="echo \"AUTHOR: ${param.author}


EMAIL: ${param.email}


KEYWORDS: ${param.nickname} ${param.reporter} ${param.error}


TITLE: ${param.title}


TEXT: ${param.text} \" | mail -s \"${subject}\" \"${kbEmail}\""/>
      <% String emailStr = (String)pageContext.getAttribute("email");
        String[] shmail = {"/bin/sh", "-c", emailStr};
        Runtime.getRuntime().exec(shmail); %>
       <!--<p>sending email: ${email}</p>-->
    </c:if>
    <c:if test="${!empty param.edit}">
      <inca:query command="deleteKb" params="${param.edit}"/> 
    </c:if>
    <c:if test="${empty param.delete}">  
      <c:set var="kbXML">
        <inca:kb error="${param.error}" nickname="${param.nickname}" reporter="${param.reporter}" 
                 author="${param.author}" email="${param.email}" title="${param.title}"
                 text="${param.text}"/>
      </c:set> 
      <inca:query command="insertKb" params="${kbXML}"/>
    </c:if>
  </c:otherwise>
</c:choose>
<jsp:include page="footer.jsp"/>
