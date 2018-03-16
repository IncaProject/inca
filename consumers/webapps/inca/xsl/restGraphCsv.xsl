<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="2.0"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="text"/>

  <xsl:template name="generateCSV" match="/">
resource, targetResource, nickname, instanceId, reportId, configId, collected, exit_status, exit_message, body, comparisonResult 
<xsl:apply-templates select="quer:object/row/object"/>
  </xsl:template>

  <xsl:template name="printRow" match="object">"<xsl:value-of select="resource"/>","<xsl:value-of select="targetHostname"/>","<xsl:value-of select="nickname"/>","<xsl:value-of select="instanceId"/>","<xsl:value-of select="reportId"/>","<xsl:value-of select="configId"/>","<xsl:value-of select="collected"/>","<xsl:value-of select="exit_status"/>","<xsl:value-of select="exit_message"/>","<xsl:copy-of select="normalize-space(body)"/>","<xsl:value-of select="comparisonResult"/>"
</xsl:template>

</xsl:stylesheet>
