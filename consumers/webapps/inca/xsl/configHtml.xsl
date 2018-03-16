<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- configHtml.xsl:                                                      -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- generateHTML                                                         -->
  <!--                                                                      -->
  <!-- Prints legend and calls printSeries.                                 -->
  <!-- ==================================================================== -->
  <xsl:template name="generateHTML" match="/combo">
    <!-- inca-common.xsl -->
    <xsl:call-template name="printBodyTitle">
      <xsl:with-param name="title" select="'Running Reporter Series'"/>
    </xsl:call-template>

    <p>This page lists all of the <b><xsl:value-of select="count(quer:object/configs/config)"/></b> reporter series currently running
      for this Inca deployment.  View <a href="javascript:window.open('/inca/jsp/descriptions.jsp','incalegend','width=400,height=325,resizable=yes')">column descriptions</a> for more information.  Click on the "Expand" radio button to show the reporter series by individual resource. Click on column headers to sort.  </p>

    <p align="right">export all as <a href="/inca/jsp/config.jsp?csv=1">CSV</a></p>

    <p><table style="width:100%;border:0px;background-color:#ffeecc">
      <tr>
        <td width="100px" align="left">
          Show columns
        </td><td>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 0)"/>Name</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 1)"/>Suite</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 2)"/>Resources</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 3)"/>Targets</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 4)"/>Script</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 5)"/>Frequency</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 6)"/>Notification</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 7)"/>Deployed</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 8)"/>Last Run</span><xsl:text> </xsl:text>
          <span style="white-space: nowrap"><input type="checkbox" checked="checked" onclick="showHideColumn('reporterTable', 9)"/>Description</span>
        </td>
        <td width="160px" style="text-align:right">
          <input type="radio" name="expandCollapseButton" onclick="window.location.href = '/inca/jsp/seriesConfig.jsp'"/> Expand
          <input type="radio" name="expandCollapseButton" checked="checked"/> Collapse
        </td>
      </tr>
    </table></p>



    <!-- printSeries -->
    <table width="100%" class="sortable" id="reporterTable">
    <thead><tr>
      <th class="sortHead sortable-left-break"><p>Name</p></th>
      <th class="sortHead sortable-left-break"><p>Suite</p></th>
      <th class="sorttable_nosort sortable-left-break" style="font-weight:normal"><p>Resources</p></th>
      <th class="sorttable_nosort sortable-left-break" style="font-weight:normal"><p>Targets</p></th>
      <th class="sortHead sortable-left-break"><p>Script</p></th>
      <th class="sortHead sortable-left-break"><p>Frequency</p></th>
      <th class="sortHead sortable-left-break" style="font-weight:normal"><p>Notification</p></th>
      <th class="sortHead sortable-left-break"><p>Deployed</p></th>
      <th class="sortHead sortable-left-break"><p>Last Run</p></th>
      <th class="sorttable_nosort sortable-left-break sortable-break" style="font-weight:normal"><p>Description</p></th>
    </tr></thead>
    <tbody>
      <xsl:apply-templates select="quer:object/configs/config"/>
    </tbody>
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSeries                                                          -->
  <!--                                                                      -->
  <!-- Generates rows of tables containing series information               -->
  <!-- ==================================================================== -->
  <xsl:template name="printSeries" match="config">
    <tr align="left">
      <xsl:variable name="repname" select="reportName"/>

      <td class="sortable-left-break"><xsl:value-of select="nickname"/></td>
      <td class="sortable-left-break"><xsl:value-of select="suiteName"/></td>
      <td class="sortable-left-break"><xsl:value-of select="resources"/></td>
      <td class="sortable-left-break"><xsl:value-of select="targets"/></td>
      <td class="sortable-left-break">
        <a href="{substring-before(seriesUri, $repname)}../cgi-bin/reporters.cgi?reporter={$repname}&amp;action=view">
          <xsl:value-of select="$repname"/>
        </a>
      </td>
      <td class="sortable-left-break"><xsl:value-of select="frequencies"/></td>
      <td class="sortable-left-break"><xsl:value-of select="notification"/></td>
      <td class="sortable-left-break"><xsl:value-of select="deployed"/></td>
      <td class="sortable-left-break"><xsl:value-of select="lastRun"/></td>
      <td class="sortable-left-break sortable-break">
        <!-- get the reporter description from the catalog which is a list
 of reporter entries each containing a property list; this
 says get the reporter entry whose name matches the reporter
 name and then get the description  -->
        <xsl:value-of select="/combo/catalogs/catalog/reporter/property[matches(name, 'name') and matches(value, $repname)]/../property[name = 'description']/value"/>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
