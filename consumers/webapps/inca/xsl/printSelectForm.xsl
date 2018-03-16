<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- printSelectForm.xsl:  Prints form to filter results.                 -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml" >

  <xsl:include href="/xsl/inca-common.xsl"/>

  <xsl:param name="filterResource"/>
  <xsl:param name="filterSuite"/>
  <xsl:param name="groupBy"/>
  <xsl:param name="lines"/>

  <!-- ==================================================================== -->
  <!-- generateHTML                                                         -->
  <!--                                                                      -->
  <!-- Prints form to filter results.                                       -->
  <!-- ==================================================================== -->
  <xsl:template name="generateHTML" match="/">
    <form method="get" action="/inca/jsp/summaryHistory.jsp"><br/>
    <table class="subheader" cellspacing="10">
      <tr valign="top">
        <td>
          <b><u>FILTER RESULTS</u></b><br/><br/>
          <table>
            <tr valign="top">
              <td align="right">Group by:</td>
              <td>
                <input type="radio" name="groupBy" value="resource">
                  <xsl:if test="$groupBy='resource' or $groupBy=''">
                    <xsl:attribute name="checked"/>
                  </xsl:if>resource
                </input><br/>
                <input type="radio" name="groupBy" value="suite">
                  <xsl:if test="$groupBy='suite'">
                    <xsl:attribute name="checked"/>
                  </xsl:if>suite
                </input><br/><br/>
              </td>
            </tr>
            <tr valign="top">
              <td align="right">Display:</td>
              <td>
                <input type="radio" name="lines" value="multiple">
                    <xsl:if test="$lines = '' or $lines='multiple'">
                      <xsl:attribute name="checked"/>
                    </xsl:if>
                  multiple lines
                </input><br/><input type="radio" name="lines" value="total">
                   <xsl:if test="$lines='total'">
                     <xsl:attribute name="checked"/>
                   </xsl:if>
                   total
                </input><br/><br/>
                <input type="submit" name="submit" value="Filter"/> 
              </td>
            </tr>
          </table>
        </td>
        <td>
          <xsl:variable name="guids" 
          select="distinct-values(/seriesAverages/period/series/guid)"/>
          suite:<br/>
          <select name="filterSuite" multiple="true" size="10">
              <option>
                <xsl:attribute name="value"/>
                <xsl:if test="$filterSuite = ''">
                  <xsl:attribute name="selected"/>
                </xsl:if>
                &lt;all&gt;
              </option>
            <xsl:for-each select="$guids">
              <xsl:sort/>
              <option>
                <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
                <xsl:if test="$filterSuite and contains($filterSuite, .)">
                  <xsl:attribute name="selected"/>
                </xsl:if>
                <xsl:value-of select="tokenize(.,'/')[4]"/>
              </option>
            </xsl:for-each>
          </select>
        </td>
        <td>
          <xsl:variable name="resources" 
            select="distinct-values(/seriesAverages/period/series/resource)"/>
            resource:<br/>
          <select name="filterResource" multiple="true" size="10">
            <option>
              <xsl:attribute name="value"/>
              <xsl:if test="$filterResource = ''">
                <xsl:attribute name="selected"/>
              </xsl:if>
              &lt;all&gt;
            </option>
            <xsl:for-each select="$resources">
              <xsl:sort/>
              <option>
                <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
                <xsl:if test="$filterResource and contains($filterResource, .)">
                  <xsl:attribute name="selected"/>
                </xsl:if>
                <xsl:value-of select="."/>
              </option>
            </xsl:for-each>
          </select>
        </td>
      </tr>
    </table>
    </form>
  </xsl:template>

</xsl:stylesheet>
