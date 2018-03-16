<%@ tag import="edu.sdsc.inca.dataModel.article.KbArticleDocument" %>
<%@ tag import="edu.sdsc.inca.util.XmlWrapper" %>
<%@ tag import="java.util.Calendar" %>

<%@ tag body-content="empty" %>
<%@ tag description="Return a KbArticle XML document" %>

<%@ attribute name="error" required="false" description="error message"%>
<%@ attribute name="nickname" required="true" description="series nickname"%>
<%@ attribute name="reporter" required="true" description="reporter name"%>
<%@ attribute name="author" required="true" description="author name"%>
<%@ attribute name="email" required="true" description="author email"%>
<%@ attribute name="title" required="true" description="article title"%>
<%@ attribute name="text" required="true" description="article text"%>
<%@ attribute name="prettyprint" required="false" type="java.lang.Boolean"
              description="indent XML if true"  %>

<%
  String error = (String)jspContext.getAttribute( "error" );
  String nickname = (String)jspContext.getAttribute( "nickname" );
  String reporter = (String)jspContext.getAttribute( "reporter" );
  String author = (String)jspContext.getAttribute( "author" );
  String email = (String)jspContext.getAttribute( "email" );
  String title = (String)jspContext.getAttribute( "title" );
  String text = (String)jspContext.getAttribute( "text" );
  edu.sdsc.inca.dataModel.article.KbArticle kb = 
      edu.sdsc.inca.dataModel.article.KbArticle.Factory.newInstance(); 
  Calendar entered = Calendar.getInstance();   
  kb.setEntered(entered);
  kb.setErrorMsg(error);
  kb.setSeries(nickname);
  kb.setReporter(reporter);
  kb.setAuthorName(author);
  kb.setAuthorEmail(email);
  kb.setArticleTitle(title);
  kb.setArticleText(text);
  KbArticleDocument doc = KbArticleDocument.Factory.newInstance();
  doc.setKbArticle(kb);
  String result = doc.toString().replace("\r", "");
  Boolean prettyPrint = false; 
  if ( jspContext.getAttribute( "prettyprint" ) != null ) {
    prettyPrint = (java.lang.Boolean)jspContext.getAttribute("prettyprint");
  }   
  if ( request.getParameter( "prettyprint" ) != null ) {
    prettyPrint = Boolean.parseBoolean( request.getParameter("prettyprint") );
  }
  if ( prettyPrint && result != null ) {
    result = XmlWrapper.prettyPrint( result, "  " ); 
  }
  out.println( result );
%>
