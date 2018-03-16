<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- periodStats.xsl                                                      -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:variable name="series"  select="seriesStatistics/series"/>
    <xsl:variable name="resources" select="distinct-values($series/resource)"/>
    <xsl:variable name="guids" select="distinct-values($series/guid)"/>
    <periodStatistics>
      <xsl:for-each select="$resources">
        <xsl:sort select="."/>
        <resourceStats>
          <xsl:variable name="resource" select="."/>
          <resource><xsl:value-of select="$resource"/></resource>
          <xsl:call-template name="computeAverage">
            <xsl:with-param name="series" select="$series[resource=$resource]"/>
          </xsl:call-template>
        </resourceStats>
      </xsl:for-each>          
    </periodStatistics>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printReport - print table with report details info                   -->
  <!-- ==================================================================== -->
  <xsl:template name="computeAverage">
    <xsl:param name="series"/>

    <xsl:variable name="sum" select="sum($series/passPercentage)"/>
    <xsl:variable name="count" select="count($series)"/>

    <seriesCount><xsl:value-of select="$count"/></seriesCount>
    <errorCount><xsl:value-of select="sum($series/errorCount)"/></errorCount>
    <uniqErrorCount><xsl:value-of select="sum($series/uniqErrorCount)"/></uniqErrorCount>
    <oldErrorCount><xsl:value-of select="sum($series/oldErrorCount)"/></oldErrorCount>
    <!--
    <passPercentage>
      <xsl:value-of select="round($sum div $count)"/>
    </passPercentage>
    -->
  </xsl:template>
</xsl:stylesheet>
