<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- seriesStats.xsl                                                      -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <xsl:param name="ignoreErrs"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:variable name="series" select="//quer:object/row/series"/>
    <xsl:variable name="endTimes"
                  select="reverse(distinct-values($series/period/end))"/>
    <xsl:variable name="beginTimes"
                  select="reverse(distinct-values($series/period/begin))"/>

    <seriesStatistics>
      <xsl:apply-templates select="$series">
        <xsl:with-param name="beginTimes" select="$beginTimes"/>
        <xsl:with-param name="endTimes" select="$endTimes"/>
      </xsl:apply-templates>
    </seriesStatistics>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printReport - print table with report details info                   -->
  <!-- ==================================================================== -->
  <xsl:template name="computeSeriesAverages" match="series">
    <xsl:param name="beginTimes"/>
    <xsl:param name="endTimes"/>

    <xsl:variable name="period" 
                  select="period[begin=$beginTimes[1] and end=$endTimes[1]]"/>

    <pre><series>
      <guid><xsl:value-of select="guid"/></guid>
      <resource><xsl:value-of select="resource"/></resource>
      <nickname><xsl:value-of select="nickname"/></nickname>
      <xsl:variable name="validErrors" 
       select="$period/failure[not(matches(message, $ignoreErrs))]"/>
      <xsl:variable name="errorCount" select="sum($validErrors/count)"/>
      <xsl:if test="$errorCount>0"> 
        <errorCount>
          <xsl:value-of select="$errorCount"/>
        </errorCount>
        <uniqErrorCount>
          <xsl:value-of select="count($period/failure)"/>
        </uniqErrorCount>
        <oldErrorCount>
          <xsl:value-of select="count($validErrors[index-of(../../period[end&lt;$endTimes[1]]/failure/message,message)>0])"/> 
        </oldErrorCount>
      </xsl:if> 
      <xsl:variable name="successCount" select="sum($period/success)"/>
      <xsl:variable name="total" select="$successCount + $errorCount"/>
      <xsl:if test="$total > 0">
        <passPercentage>
          <xsl:value-of select="round(($successCount div $total)*100)"/>
        </passPercentage>
      </xsl:if>
    </series></pre>

  </xsl:template>

</xsl:stylesheet>
