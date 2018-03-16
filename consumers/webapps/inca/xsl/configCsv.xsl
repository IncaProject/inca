<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- configCSV.xsl:                                                       -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:output method="text"/>

  <xsl:template name="generateCSV" match="/combo">
    <xsl:apply-templates select="quer:object/configs/config"/>
  </xsl:template>

  <xsl:template name="printSeries" match="config"><xsl:variable name="repname" select="reportName"/>"<xsl:value-of select="nickname"/>","<xsl:value-of select="suiteName"/>","<xsl:value-of select="resources"/>","<xsl:value-of select="targets"/>","<xsl:value-of select="$repname"/>","<xsl:value-of select="frequencies"/>","<xsl:value-of select="notification"/>","<xsl:value-of select="deployed"/>","<xsl:value-of select="lastRun"/>","<xsl:value-of select="/combo/catalogs/catalog/reporter/property[matches(name, 'name') and matches(value, $repname)]/../property[name = 'description']/value"/>"
</xsl:template>

</xsl:stylesheet>
