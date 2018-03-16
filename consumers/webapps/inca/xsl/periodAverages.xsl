<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- periodAverages.xsl:  Computes pass percentage for suites and         -->
<!--			  resources by week, etc.                         -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <xsl:param name="resource" />
  <xsl:param name="guid" />
  <xsl:param name="resourceTotal" />
  <xsl:param name="suiteTotal" />

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <seriesSummary>
      <xsl:for-each select="seriesAverages/period">
        <period>
          <begin><xsl:value-of select="begin"/></begin>
          <end><xsl:value-of select="end"/></end>

          <!-- compute resource average (of series averages) -->
          <xsl:variable name="resourceList">
              <xsl:choose><xsl:when test="$resource">
                <xsl:value-of select="tokenize($resource, ',')"/>
              </xsl:when><xsl:otherwise>
                <xsl:value-of select="distinct-values(series/resource)"/>
              </xsl:otherwise></xsl:choose>
          </xsl:variable>
          <xsl:variable name="guidList">
            <xsl:choose><xsl:when test="$guid">
              <xsl:value-of select="tokenize($guid,',')"/>
            </xsl:when><xsl:otherwise>
              <xsl:value-of select="distinct-values(series/guid)"/>
            </xsl:otherwise></xsl:choose>
          </xsl:variable>
          <xsl:choose><xsl:when test="$resourceTotal and $resourceTotal = 'true'">
            <xsl:call-template name="printResourceStats">
              <xsl:with-param name="resources" select="tokenize($resourceList, ' ')"/>
              <xsl:with-param name="series" select="series"/>
            </xsl:call-template>
          </xsl:when><xsl:when test="$suiteTotal and $suiteTotal = 'true'">
            <xsl:call-template name="printSuiteStats">
              <xsl:with-param name="guids" select="tokenize($guidList,' ')"/>
              <xsl:with-param name="series" select="series"/>
            </xsl:call-template>
          </xsl:when><xsl:otherwise>
            <xsl:call-template name="printResourceSuiteStats">
              <xsl:with-param name="resources" select="tokenize($resourceList, ' ')"/>
              <xsl:with-param name="guids" select="tokenize($guidList,' ')"/>
              <xsl:with-param name="series" select="series"/>
            </xsl:call-template>
          </xsl:otherwise></xsl:choose>

        </period>
      </xsl:for-each>
    </seriesSummary>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printResourceStats - Print stats by resource                         -->
  <!-- ==================================================================== -->
  <xsl:template name="printResourceStats">
    <xsl:param name="resources"/>
    <xsl:param name="series"/>

    <xsl:for-each select="$resources">
      <xsl:sort select="."/>

      <xsl:variable name="resource" select="."/>
        <average>
          <resource><xsl:value-of select="$resource"/></resource>
        <xsl:call-template name="computeAverage">
          <xsl:with-param name="series" select="$series[resource=$resource]"/>
        </xsl:call-template>
        </average>
      </xsl:for-each> 
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSuiteStats - Print stats by suite                               -->
  <!-- ==================================================================== -->
  <xsl:template name="printSuiteStats">
    <xsl:param name="guids"/>
    <xsl:param name="series"/>

    <xsl:for-each select="$guids">
      <xsl:sort select="."/>

      <xsl:variable name="guid" select="."/>
        <average>
          <guid><xsl:value-of select="$guid"/></guid>
        <xsl:call-template name="computeAverage">
          <xsl:with-param name="series" select="$series[guid=$guid]"/>
        </xsl:call-template>
        </average>
      </xsl:for-each> 
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printResourceSuiteStats - print stats by resource, suite pair        -->
  <!-- ==================================================================== -->
  <xsl:template name="printResourceSuiteStats">
    <xsl:param name="resources"/>
    <xsl:param name="guids"/>
    <xsl:param name="series"/>

    <xsl:for-each select="$resources">
      <xsl:sort select="."/>

      <xsl:variable name="resource" select="."/>

      <xsl:for-each select="$guids">
        <xsl:sort select="."/>
        <xsl:variable name="guid" select="."/>

        <average>
          <resource><xsl:value-of select="$resource"/></resource>
          <guid><xsl:value-of select="$guid"/></guid>
          <xsl:call-template name="computeAverage">
            <xsl:with-param name="series" 
                            select="$series[resource=$resource and guid=$guid]"/>
          </xsl:call-template>
        </average>
      </xsl:for-each> 
    </xsl:for-each>          
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- computeAverage - compute average series pass rate for a group of     -->
  <!--                  series                                              -->
  <!-- ==================================================================== -->
  <xsl:template name="computeAverage">
    <xsl:param name="series"/>

    <xsl:variable name="sum" select="sum($series/passPercentage)"/>
    <xsl:variable name="count" select="count($series)"/>

    <xsl:if test="$count > 0">
    <seriesCount><xsl:value-of select="$count"/></seriesCount>
    <passPercentage>
      <xsl:value-of select="$sum div $count"/>
    </passPercentage>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
