<xsl:stylesheet version="2.0"
            xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns="http://www.w3.org/1999/xhtml"
            xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
            xmlns:rs="http://inca.sdsc.edu/queryResult/reportSummary_2.0"
            xmlns:xs="http://www.w3.org/2001/XMLSchema">

  <xsl:param name="urlResource"/>

  <xsl:template match="/">
    <quer:object>
      <xsl:for-each select="config/resources/resource/name">
        <xsl:variable name="resource" select="."/>
        <xsl:for-each select="/config/quer:object/row/rs:reportSummary[
                    (hostname=$resource and string(targetHostname)='')
                 or targetHostname=$resource]">
          <reportSummary xmlns="http://inca.sdsc.edu/queryResult/reportSummary_2.0">
            <xsl:if test="$urlResource!=hostname and $urlResource!=targetHostname">
            <resourceAlias><xsl:value-of select="$urlResource"/></resourceAlias>
            </xsl:if>
            <xsl:copy-of select="hostname"/>
            <xsl:copy-of select="targetHostname"/>
            <xsl:copy-of select="uri"/>
            <xsl:copy-of select="nickname"/>
            <xsl:copy-of select="seriesConfigId"/>
            <xsl:copy-of select="instanceId"/>
            <xsl:copy-of select="gmt"/>
            <xsl:copy-of select="gmtExpires"/>
            <xsl:copy-of select="body"/>
            <xsl:copy-of select="errorMessage"/>
            <xsl:copy-of select="comparisonResult"/>
            <xsl:copy-of select="tags"/>
          </reportSummary>
        </xsl:for-each>
      </xsl:for-each>
    </quer:object>
  </xsl:template>
</xsl:stylesheet>
