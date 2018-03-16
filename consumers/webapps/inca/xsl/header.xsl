<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- header.xsl:  Prints HTML page header.                                -->
<!--              Style controlled by nav.css (e.g. top level label width)-->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml">
  <xsl:param name="url" />

  <xsl:template name="header">
    <xsl:variable name="jsp" select="concat($url, '/jsp/')"/>
    <xsl:variable name="sample" select="'suiteNames=sampleSuite&amp;resourceIds=defaultGrid'"/>
    <table width="100%" class="subheader">
      <tr>
        <td><b><a href="/" style="text-decoration: none">INCA STATUS PAGES</a></b></td>
        <td>
          <div id="menu">
            <ul>
              <li><h2>Admin</h2>
                <ul>
                  <li>
                    <a href="{concat($jsp, 'config.jsp')}">
                      List running reporters
                    </a>
                  </li>
                  <li>
                    <a href="{concat($jsp, 'seriesConfig.jsp')}">
                      List running reporters - detail
                    </a>
                  </li>
                  <li>
                    <a href="{concat($jsp, 'admin.jsp')}">
                      View/Change configuration
                    </a>
                  </li>
                </ul>
              </li>
            </ul>
            <ul>
              <li><h2>Query</h2>
                <ul>
                  <li>
                    <a href="{concat($jsp, 'status.jsp?xsl=graph.xsl&amp;', $sample)}">
                      Create sampleSuite graph
                    </a>
                  </li>
                  <li>
                    <a href="{concat($jsp, 'status-auth.jsp?xsl=create-query.xsl&amp;', $sample)}">
                      Create sampleSuite stored query
                    </a>
                  </li>
                  <li>
                    <a href="{concat($jsp, 'query.jsp')}">
                      Manage stored queries
                    </a>
                  </li>
                </ul>
              </li>
            </ul>
            <ul>
              <li><h2>Reports</h2>
                <ul>
                  <li>
                  <xsl:variable name="startDate" select="current-date() - xs:dayTimeDuration('P7D')" />
                  <xsl:variable name="date" select='concat (format-date($startDate, "[M,2][D,2]"), substring(format-date($startDate, "[Y]"),3,2))'/>
                  <a href="{concat($jsp, 'report.jsp?startDate=', $date)}">Past week: sampleSuite pass/fail graphs and err msgs</a>
                  <a href="{concat($jsp, 'summary.jsp')}">Past week: avg pass rate by resource/suite</a>
                  <a href="{concat($jsp, 'status.jsp?xsl=seriesSummary.xsl&amp;xml=weekSummary.xml&amp;queryNames=incaQueryStatus')}">Past 4 weeks: series error summary</a>
                  <a href="{concat($jsp, 'summaryHistory.jsp?groupBy=resource')}">Resource avg pass history</a>
                  <a href="{concat($jsp, 'summaryHistory.jsp?groupBy=suite')}">Suite avg pass history</a>
                  </li>
                </ul>
              </li>
            </ul>
            <ul>
              <li><h2>Current Data</h2>
                <ul>
                  <li>
                    <a href="{concat($jsp, 'status.jsp?', $sample)}">
                      Table of sampleSuite results
                    </a>
                  </li>
                  <li>
                    <a href="{concat($jsp, 'status.jsp?xsl=google.xsl&amp;xml=google.xml&amp;', $sample)}">
                      Map of sampleSuite results
                    </a>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
        </td></tr></table>
  </xsl:template>

</xsl:stylesheet>
