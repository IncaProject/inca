<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- index.xsl:  Lists all configured suite and resource names in an      -->
<!--             HTML form whose action is to display results for the     -->
<!--             selected suite and resource.                             -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- generateHTML                                                         -->
  <!--                                                                      -->
  <!-- Prints out header page description and bottom cron table description -->
  <!-- Also prints out the table start and end tags and calls printSuite    -->
  <!-- to generate suite header and rows.                                   -->
  <!-- ==================================================================== -->
  <xsl:template name="generateHTML" match="/combo">
    <xsl:variable name="suites" select="suites"/>
    <xsl:variable name="numSuites" select="count($suites/suite)"/>
    <xsl:variable name="resources" select="resources/resource"/>
    <xsl:variable name="numResources" select="count($resources)"/>
    <form method="get" action="status.jsp" name="form" onsubmit="setParam(form);">
      <table border="0" align="center" cellpadding="10" width="400">
        <tr><td colspan="2">
          <h3 align="center">Welcome to the Inca Consumer web pages!</h3>
          <p>To display the status page for a suite, please select one suite
            and one resource below and click 'Submit'.</p>
        </td></tr>
        <tr align="center">
          <td>
            <xsl:if test="$numSuites=0">
              <p><i>No suites found</i></p>
            </xsl:if>
            <xsl:if test="$numSuites>0">
              <p>SUITE:</p>
              <p>
                <select name="suiteNames" size="10">
                  <xsl:for-each select="$suites/suite">
                    <xsl:sort select="." />
		    <xsl:choose>
		      <xsl:when test="position()=1">
                        <option value="{name}" selected=""><xsl:value-of select="name"/></option>
		      </xsl:when>
		      <xsl:otherwise>
                        <option value="{name}"><xsl:value-of select="name"/></option>
		      </xsl:otherwise>
		    </xsl:choose>
                  </xsl:for-each>
                </select>
              </p>
            </xsl:if>
          </td>
          <td>
            <xsl:if test="$numResources=0">
              <p><i>No resources found</i></p>
            </xsl:if>
            <xsl:if test="$numResources>0">
              <p>RESOURCE:</p>
              <p>
                <select name="resourceIds" size="10">
                  <xsl:for-each select="$resources/name">
                    <xsl:sort select="."/>
		    <xsl:choose>
		      <xsl:when test="position()=1">
                        <option value="{.}" selected=""><xsl:value-of select="."/></option>
		      </xsl:when>
		      <xsl:otherwise>
                        <option value="{.}"><xsl:value-of select="."/></option>
		      </xsl:otherwise>
		    </xsl:choose>
                  </xsl:for-each>
                </select>
              </p>
            </xsl:if>
          </td>
        </tr>
        <tr align="center">
          <td colspan="2">
            <input type="submit" name="Submit" value="Submit"/>
          </td>
        </tr>
        <tr align="center">
          <td colspan="2">
            <p>(To view suite configuration details,
              <a href="config.jsp?xsl=config.xsl">click here</a>.)</p>
          </td>
        </tr>
      </table>
    </form>
  </xsl:template>

</xsl:stylesheet>
