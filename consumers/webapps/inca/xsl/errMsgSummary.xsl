<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- errorSummary.xsl:  Print out a table of errs for individual series   -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:sdf="java.text.SimpleDateFormat"
                xmlns:rd="http://inca.sdsc.edu/dataModel/reportDetails_2.1"
                xmlns:xdt="http://www.w3.org/2004/07/xpath-datatypes">
  <!-- building a key-table where failures are 'grouped' by their message -->
  <xsl:key name="failures-by-message" match="failure" use="message" />

  <xsl:param name="ignoreErrs"/>
  <xsl:param name="startErrs"/>
  <xsl:param name="endErrs"/>
  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:text disable-output-escaping="yes"><![CDATA[
      <script type="text/javascript" src="/inca/js/sorttable.js"></script>
    ]]></xsl:text>
    
    <xsl:variable name="series" select="/combo/queries/query/quer:object/row/series"/>
    <xsl:variable name="failures" select="$series/period[begin&gt;=$startErrs 
                 and end&lt;=$endErrs]/failure[not(matches(message, $ignoreErrs))]"/>
    <xsl:variable name="firstFail">
      <xsl:call-template name="convertMillisToDate">
        <xsl:with-param name="millis" select="$startErrs"/>
        <xsl:with-param name="format" select="'MM/dd/yy'"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="lastFail">
      <xsl:call-template name="convertMillisToDate">
        <xsl:with-param name="millis" select="$endErrs"/>
        <xsl:with-param name="format" select="'MM/dd/yy'"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="histRef" select="concat('/inca/jsp/graph.jsp?startDate=',
         replace($firstFail,'/',''),'&amp;endDate=',replace($lastFail,'/',''))"/>
    <br/>
    <!-- inca-common.xsl -->
    <xsl:call-template name="printBodyTitle">
      <xsl:with-param name="title" 
        select="concat('Error Message Summary (',$firstFail,' - ',$lastFail,')')"/>
    </xsl:call-template>

    <xsl:variable name="href" 
     select="'status.jsp?xsl=seriesSummary.xsl&amp;xml=weekSummary.xml&amp;queryNames=incaQueryStatus'"/>
    <table class="sortableTop"><tr><td>
      <p>The table below shows the number of times an error message occurred during a given 
         time period.  The total number of times a message occurred is further broken down into
         the number of times it occurred for a resource's test.</p>

      <p>Return to <a href="{$href}">series error summary</a> or view the 
         <a href="{$href}&amp;topSeriesErrs=1">top series with errors</a>.</p>
       
      <p> Click on the date ranges in the legend to the right to see a summary of 
     error messages for each time period.  Click on the column headers in the table below to sort it.</p>

    </td>
    <td>
    <xsl:variable name="endTimes"
                  select="reverse(distinct-values($series/period/end))"/>
    <xsl:variable name="beginTimes"
                  select="reverse(distinct-values($series/period/begin))"/>
    <xsl:variable name="summaries" select="/combo/summaries/summary"/>
    <table class="sortableLegend">
      <!-- inca-common.xsl -->
      <xsl:apply-templates select="$summaries" mode="printRanges">
        <xsl:with-param name="beginTimes" select="$beginTimes"/>
        <xsl:with-param name="endTimes" select="$endTimes"/>
        <xsl:with-param name="href" select="replace($href, 'seriesSummary.xsl', 'errMsgSummary.xsl')"/>
      </xsl:apply-templates>
    </table></td></tr></table>
    <!-- Table of values -->
    <xsl:variable name="displayErrors" select="sum($failures/count)"/>
    <center>
      <table class="sortable">
      <xsl:choose><xsl:when test="$displayErrors and $displayErrors > 0">
        <thead>
          <tr class="left-align">
            <th class="sortable-left-break">Total Times Message Occured</th>
            <th class="sortable-left-break">Times Message Occured by Test</th>
            <th class="sortable-left-break">Error Message</th>
          </tr>
        </thead>
        <tbody>
   <xsl:for-each-group select="$failures" group-by="message">
     <xsl:sort select="sum($failures[message=current-grouping-key()]/count)" order="descending"/>
    <tr>
      <td class="sortable-left-break rt-align top-align">
        <xsl:value-of select="sum($failures[message=current-grouping-key()]/count)"/>
      </td>
      <td class="sortable-left-break">
        <table class="noborder">
        <xsl:for-each select="$series[period[begin&gt;=$startErrs 
                 and end&lt;=$endErrs and failure[message=current-grouping-key()]]]">
          <xsl:sort select="sum(period[begin&gt;=$startErrs 
                 and end&lt;=$endErrs]/failure[message=current-grouping-key()]/count)" order="descending"/>
          <tr>
          <td class="rt-align top-align"><xsl:value-of select="sum(period[begin&gt;=$startErrs 
                 and end&lt;=$endErrs]/failure[message=current-grouping-key()]/count)"/></td>
          <td class="top-align"><a href="{concat($histRef, '&amp;series=', nickname, ',', resource)}">
              <xsl:value-of select="resource"/>
          </a></td><td class="top-align"><xsl:value-of select="nickname"/></td>
          </tr>
        </xsl:for-each>
        </table>
      </td>
      <td valign="top" class="sortable-left-break"><li class="hang">
            <xsl:value-of select="current-grouping-key()"/>
      </li></td>
    </tr>
    </xsl:for-each-group>
        </tbody>
      </xsl:when>
        <xsl:otherwise>
          <tr><td>
            <h1 class="passText">No errors found.</h1>
          </td></tr>
        </xsl:otherwise>
      </xsl:choose>
    </table></center>
  </xsl:template>

</xsl:stylesheet>
