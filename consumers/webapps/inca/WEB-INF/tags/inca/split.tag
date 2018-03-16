<%@ tag body-content="empty" %>
<%@ tag description="Replaces jstl's split in favor of native Java" %>

<%@ attribute name="content" required="true" type="java.lang.String"
              description="the string content to be split" %>
<%@ attribute name="delim" required="true" type="java.lang.String"
              description="the delimiting regular expression" %>
<%@ attribute name="limit" required="false" type="java.lang.Integer"
              description="maximum number of splits" %>
<%@ attribute name="var" rtexprvalue="false" required="true" 
              description="name of the exported scoped variable to hold the array " %>
<%@ variable name-from-attribute="var" variable-class="java.lang.Object"
             alias="varName" scope="AT_END" 
             description="an array of strings" %>

<%-- 
the native jstl split strips empty elements - Java does not and can be preferred at times
--%>

<%  
  String content = (String)jspContext.getAttribute("content");
  String regex = (String)jspContext.getAttribute("delim");
  if ( jspContext.getAttribute("limit") == null ) {
    jspContext.setAttribute("varName", content.split(regex) );
  } else {
    Integer limit = (Integer)jspContext.getAttribute("limit");
    jspContext.setAttribute("varName", content.split(regex, limit) );
  }
%>

