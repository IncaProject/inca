<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- configDetailCSV.xsl:                                                 -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:output method="text"/>

  <xsl:template name="generateCSV" match="/quer:object/configs">
    <xsl:apply-templates select="config"/>
  </xsl:template>

  <xsl:template name="printSeries" match="config">"<xsl:value-of select="nickname"/>","<xsl:value-of select="suiteName"/>","<xsl:value-of select="resource"/>","<xsl:value-of select="target"/>","<xsl:value-of select="seriesName"/>","<xsl:value-of select="frequency"/>","<xsl:value-of select="notifier"/>","<xsl:value-of select="emailTarget"/>"
</xsl:template>

</xsl:stylesheet>
