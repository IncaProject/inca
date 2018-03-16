<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- configDetailHtml.xsl: Prints details of the currently running series -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <!-- ==================================================================== -->
  <!-- generateHTML                                                         -->
  <!--                                                                      -->
  <!-- Prints legend and calls printSeries   .                              -->
  <!-- ==================================================================== -->

  <xsl:template name="generateHTML" match="/quer:object/configs">
    <!-- inca-common.xsl -->
    <xsl:call-template name="printBodyTitle">
      <xsl:with-param name="title" select="'Running Reporter Series - Detail'"/>
    </xsl:call-template>
     <p>This page lists all of the
      <b><xsl:value-of select="count(config)"/></b> reporter
      series currently running for this Inca deployment.  View <a href="javascript:window.open('/inca/jsp/descriptions.jsp','incalegend','width=400,height=325,resizable=yes')">column descriptions</a> for more information.  Click on the "Expand" radio button to show the reporter series by individual resource. Click on column headers to sort.  </p>

    <p align="right">export all as <a href="/inca/jsp/seriesConfig.jsp?csv=1">CSV</a></p>

    <p><table style="width:100%;border:0px;background-color:#ffeecc">
      <tr>
        <td>
          Show columns
        </td><td>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 0)"/>Name</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 1)"/>Suite</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 2)"/>Resource</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 3)"/>Target</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 4)"/>Script</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 5)"/>Frequency</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 6)"/>Notification</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('seriesTable', 7)"/>Email</span><xsl:text> </xsl:text>
        </td>
        <td style="text-align:right">
          <input type="radio" name="expandCollapseButton" checked="checked"/> Expand
          <input type="radio" name="expandCollapseButton" onclick="window.location.href = '/inca/jsp/config.jsp'"/> Collapse
        </td>
      </tr>
    </table></p>

    <!-- printSeries -->
    <table width="100%" class="sortable" id="seriesTable">
    <thead><tr>
      <th class="sortHead sortable-left-break"><p>Name</p></th>
      <th class="sortHead sortable-left-break"><p>Suite</p></th>
      <th class="sortHead sortable-left-break"><p>Resource</p></th>
      <th class="sortHead sortable-left-break"><p>Target</p></th>
      <th class="sortHead sortable-left-break"><p>Script</p></th>
      <th class="sortHead sortable-left-break"><p>Frequency</p></th>
      <th class="sortHead sortable-left-break"><p>Notification</p></th>
      <th class="sortHead sortable-left-break sortable-break"><p>Email</p></th>
    </tr></thead>
    <tbody>
      <xsl:apply-templates select="config" />
    </tbody>
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSeries                                                          -->
  <!--                                                                      -->
  <!-- Generates rows of tables containing series information               -->
  <!-- ==================================================================== -->
  <xsl:template name="printSeries" match="config">
    <xsl:variable name="uri" select="seriesUri"/>
    <xsl:variable name="cgi" select="replace($uri, '/bin/', '/cgi-bin/reporters.cgi?action=help&amp;reporter=')"/>
    <tr align="left">
      <td class="sortable-left-break"><xsl:value-of select="nickname"/></td>
      <td class="sortable-left-break"><xsl:value-of select="suiteName"/></td>
      <td class="sortable-left-break"><xsl:value-of select="resource"/></td>
      <td class="sortable-left-break"><xsl:value-of select="target"/></td>
      <td class="sortable-left-break">
        <a href="{$cgi}">
          <xsl:value-of select="seriesName"/>
        </a>
      </td>
      <td class="sortable-left-break"><xsl:value-of select="frequency"/></td>
      <td class="sortable-left-break"><xsl:value-of select="notifier"/></td>
      <td class="sortable-left-break sortable-break"><xsl:value-of select="emailTarget"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
