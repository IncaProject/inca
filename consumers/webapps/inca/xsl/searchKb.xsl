<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- instance.xsl:  HTML table with report details.                       -->
<!-- ==================================================================== -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
		xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    
    <h1 class="body">Knowledge Base Search Results</h1>
    <p>You searched for:</p>
    <p class="code"><xsl:value-of select="articles/hql"/></p>
    <xsl:variable name="articles" select="articles/quer:object//object"/>
    <xsl:choose>
      <xsl:when test="$articles">
        <p>The following articles were returned:</p>
        <!-- inca-common.xsl -->
        <xsl:call-template name="printSeriesNamesTable">
          <xsl:with-param name="seriesNames" select="$articles/articleTitle"/>
        </xsl:call-template>
        <xsl:for-each select="$articles">
          <hr/><p><a name="{articleTitle}"></a></p>
          <h2><xsl:value-of select="articleTitle"/></h2>
          <p class="footer">Contributed by <xsl:value-of select="authorName"/> on 
            <xsl:value-of select="replace(entered,' .*','')"/> </p>
          <p><xsl:call-template name="replaceBreak">
            <xsl:with-param name="text" select="articleText"/>
          </xsl:call-template></p>
          <br/><table><tr><td>
          <form method="get" action="addKnowledgeBase.jsp">
            <input type="hidden" name="id" value="{id}"/>
            <input type="hidden" name="entered" value="{entered}"/>
            <input type="hidden" name="error" value="{errorMsg}"/>
            <input type="hidden" name="nickname" value="{series}"/>
            <input type="hidden" name="reporter" value="{reporter}"/>
            <input type="hidden" name="author" value="{authorName}"/>
            <input type="hidden" name="email" value="{authorEmail}"/>
            <input type="hidden" name="title" value="{articleTitle}"/>
            <input type="hidden" name="text" value="{articleText}"/>
            <input type="submit" value="edit"/>
          </form></td><td>
          <xsl:call-template name="printJavascript"/>
          <form method="get" action="addKnowledgeBase.jsp">
            <input type="submit" value="delete" onClick="return confirmDelete('{articleTitle}')"/>
            <input type="hidden" name="delete" value="true"/>
            <input type="hidden" name="edit" value="{id}"/>
            <input type="hidden" name="entered" value="{entered}"/>
            <input type="hidden" name="error" value="{errorMsg}"/>
            <input type="hidden" name="nickname" value="{series}"/>
            <input type="hidden" name="reporter" value="{reporter}"/>
            <input type="hidden" name="author" value="{authorName}"/>
            <input type="hidden" name="email" value="{authorEmail}"/>
            <input type="hidden" name="title" value="{articleTitle}"/>
            <input type="hidden" name="text" value="{articleText}"/>
          </form></td></tr></table>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <p class="errorText">No articles were found for this search.</p>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template name="printJavascript">
    <script language="javascript" type="text/javascript">
      function confirmDelete(title) {
        var agree=confirm("Are you sure you wish to delete the article '"+title+"'?");
        if (agree){ 
          return true ; 
        } else { 
          return false ;
        }
      }
    </script>
  </xsl:template>


</xsl:stylesheet>
