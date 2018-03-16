<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- error.xsl:  Displays JSP error message and usage information         -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:template match="/">
      <p>
        <b><font color="red">An error occurred while loading page:</font></b>
        &#160;<xsl:value-of select="error/message"/></p>
      <xsl:if test="error/usage!=''">
        <p><b>JSP Usage Information:</b></p>
        <pre><xsl:value-of select="error/usage"/></pre>
      </xsl:if>
  </xsl:template>

</xsl:stylesheet>
