<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- seriesAverages.xsl: Computes series pass percentages.                -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <xsl:param name="sort"/>
  <xsl:param name="ignoreErrs"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:variable name="series" select="/quer:object/row/series"/>
    <xsl:variable name="endTimesAsc"
                  select="distinct-values($series/period/end)"/>
    <xsl:variable name="beginTimesAsc"
                  select="distinct-values($series/period/begin)"/>
    <xsl:variable name="endTimesDesc" select="reverse($endTimesAsc)"/>
    <xsl:variable name="beginTimesDesc" select="reverse($beginTimesAsc)"/>

    <seriesAverages>
      <xsl:choose><xsl:when test="$sort = 'descending'">

        <xsl:for-each select="$beginTimesDesc">
          <xsl:variable name="idx" select="position()"/>
          <xsl:call-template name ="printPeriod">
            <xsl:with-param name="begin" select="."/>
            <xsl:with-param name="end" select="$endTimesDesc[$idx]"/>
            <xsl:with-param name="series" select="$series"/>
          </xsl:call-template>
        </xsl:for-each>

      </xsl:when><xsl:otherwise>

        <xsl:for-each select="$beginTimesAsc">
          <xsl:variable name="idx" select="position()"/>
          <xsl:call-template name ="printPeriod">
            <xsl:with-param name="begin" select="."/>
            <xsl:with-param name="end" select="$endTimesAsc[$idx]"/>
            <xsl:with-param name="series" select="$series"/>
          </xsl:call-template>
        </xsl:for-each>

      </xsl:otherwise></xsl:choose>
    </seriesAverages>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printPeriod - print period details                                   -->
  <!-- ==================================================================== -->
  <xsl:template name="printPeriod" match="period">
    <xsl:param name="begin"/>
    <xsl:param name="end"/>
    <xsl:param name="series"/>

    <period>
      <begin><xsl:value-of select="$begin"/></begin>
      <end><xsl:value-of select="$end"/></end>
      <xsl:apply-templates select="$series">
        <xsl:with-param name="begin" select="$begin"/>
        <xsl:with-param name="end" select="$end"/>
      </xsl:apply-templates>
    </period>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printReport - print table with report details info                   -->
  <!-- ==================================================================== -->
  <xsl:template name="computeSeriesAverages" match="series">
    <xsl:param name="begin"/>
    <xsl:param name="end"/>

    <xsl:variable name="periods"
                  select="period[begin &gt;= $begin and end &lt;= $end]"/>
    <xsl:variable name="validErrors" select="$periods/failure[
                        not(matches(message, $ignoreErrs))]"/>
    <xsl:variable name="errorCount" select="sum($validErrors/count)"/>
    <xsl:variable name="successCount" select="sum($periods/success)"/>
    <xsl:variable name="total" select="$successCount + $errorCount"/>
    <xsl:if test="$total > 0">
      <series>
        <guid><xsl:value-of select="guid"/></guid>
        <resource><xsl:value-of select="resource"/></resource>
        <nickname><xsl:value-of select="nickname"/></nickname>
        <xsl:for-each select="$periods/failure">
          <numErrorMessages>
            <xsl:value-of select="count($periods/failure)"/>
          </numErrorMessages>
        </xsl:for-each>
        <passPercentage>
          <xsl:value-of select="($successCount div $total)*100"/>
        </passPercentage>
      </series>
    </xsl:if>

  </xsl:template>

</xsl:stylesheet>
