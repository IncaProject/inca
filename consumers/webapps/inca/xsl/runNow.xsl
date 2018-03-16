<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- runNow.xsl:  result of run now request                               -->
<!-- ==================================================================== -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" >

  <xsl:include href="../xsl/inca-common.xsl"/>

  <xsl:param name="refreshPeriod"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
     <xsl:if test="error">
       <p><b>Error</b>: Unable to process run now request: 
          <xsl:value-of select="error"/>.  Please contact your Inca administrator.</p>
     </xsl:if>
     <xsl:if test="success">
       <p>Run now request successfully submitted for <b><xsl:value-of
       select="success"/></b> series.  New results may take up to <xsl:value-of select="$refreshPeriod"/> minutes to propagate to these web pages.</p>
     </xsl:if>
     <form method="POST" action="Javascript:history.go(-1)">
       <input type="submit" value="Go Back" name="Back"/>
     </form>
  </xsl:template>

</xsl:stylesheet>
