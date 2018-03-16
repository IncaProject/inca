<%@ tag import="java.io.FileWriter" %>

<%@ tag body-content="empty" %>
<%@ tag description="Writes the content to disk" %>

<%@ attribute name="content" required="true" type="java.lang.String"
              description="string to write to disk" %>
<%@ attribute name="file" required="true" type="java.lang.String"
              description="path to file where content should be stored" %>

<% 
  String content = (String)jspContext.getAttribute( "content"   ); 
  String vpath = (String)jspContext.getAttribute( "file"   ); 
  String realPath = request.getRealPath(vpath);
  FileWriter writer = new FileWriter( realPath );
  writer.write( content );
  writer.close();
%>

