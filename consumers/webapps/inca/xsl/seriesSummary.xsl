<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- seriesSummary.xsl:  Print out a table of stats for individual series -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:sdf="java.text.SimpleDateFormat"
                xmlns:rd="http://inca.sdsc.edu/dataModel/reportDetails_2.1"
                xmlns:xdt="http://www.w3.org/2004/07/xpath-datatypes">
  <!-- building a key-table where <series> are 'grouped' by their nickname -->
  <xsl:key name="series-by-nickname" match="series" use="nickname" />

  <xsl:param name="resourceIds"/>
  <xsl:param name="suiteNames"/>
  <xsl:param name="ignoreErrs"/>
  <xsl:param name="topSeriesErrs"/>
  <xsl:include href="../xsl/inca-common.xsl"/>

  <xsl:variable name="href" 
     select="'status.jsp?xsl=seriesSummary.xsl&amp;xml=weekSummary.xml&amp;queryNames=incaQueryStatus'"/>
  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:text disable-output-escaping="yes"><![CDATA[
      <script type="text/javascript" src="/inca/js/sorttable.js"></script>
    ]]></xsl:text>
    <xsl:variable name="series"
                  select="/combo/queries/query/quer:object/row/series"/>
    <xsl:variable name="endTimes"
                  select="reverse(distinct-values($series/period/end))"/>
    <xsl:variable name="beginTimes"
                  select="reverse(distinct-values($series/period/begin))"/>

    <!-- find out how many periods are in the output and only display the 
         periods we have values for -->
    <xsl:variable name="periodCount" select="count($series[1]/period)"/>
    <xsl:variable name="summaries" select="/combo/summaries/summary[beginIndex &lt;= $periodCount]"/>
    <br/>
    <!-- inca-common.xsl -->
    <xsl:call-template name="printBodyTitle">
      <xsl:with-param name="title" select="'Series Error Summary'"/>
    </xsl:call-template>

    <table class="sortableTop"><tr><td>
      <p>The table below summarizes test series errors by time period.  A
        legend of the time periods and associated dates is located above the
        table on the right. Each time period includes the number of errors for
        the series during the time period, the number of unique or distinct
        errors during the period and the percentage of the total results that
        passed during the period. </p>

      <p>The change between the total number of errors in the most recent
        period and the total number of errors in the period before it is also
        given. If the number of errors in the most recent period is greater
        than the number of errors in the previous period (+), the number appears in
        red.  If the number of errors in the most recent period is less than the
        number in the previous period (-), the number is green.</p>
     
     <p>
     <xsl:choose>
       <xsl:when test="$topSeriesErrs!='1'">
         View the <a href="{$href}&amp;topSeriesErrs=1">top series with errors</a>.
       </xsl:when>
       <xsl:otherwise>
         Return to <a href="{$href}">series error summary</a>.
       </xsl:otherwise>
     </xsl:choose>
     Click on the date ranges in the legend to the right to see a summary of 
     error messages for each time period.</p>
      
     <p><b>Click on the column headers in the table to sort it.</b>
        Sorting is applied to the table in the order the column headers are
        clicked on (e.g. to sort by Resource and then Suite, click on the
        Suite column header then the Resource column header).  The column 
        header clicked on last will be the primary sort for the table.</p>

    </td><td>
      <table class="sortableLegend">
        <!-- inca-common.xsl -->
        <xsl:apply-templates select="$summaries" mode="printRanges">
          <xsl:with-param name="beginTimes" select="$beginTimes"/>
          <xsl:with-param name="endTimes" select="$endTimes"/>
          <xsl:with-param name="href" select="$href"/>
        </xsl:apply-templates>
      </table></td></tr></table>
    <!-- Table of values -->
    <xsl:variable name="resourceMatch">
      <xsl:choose>
        <xsl:when test="$resourceIds">
          <xsl:value-of select="concat('^', $resourceIds, '$')"/>
        </xsl:when>
        <xsl:otherwise><xsl:value-of select="'.*'"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="suiteMatch">
      <xsl:choose>
        <xsl:when test="$suiteNames">
          <xsl:value-of select="concat('^.*/', $suiteNames, '$')"/>
        </xsl:when>
        <xsl:otherwise><xsl:value-of select="'.*'"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="seriesMatch"
                  select="//series[resource[(matches(., $resourceMatch))]
                   and guid[(matches(., $suiteMatch))]]"/>
    <xsl:variable name="begin" 
                  select="xs:integer($summaries[position()=1]/beginIndex)"/>
    <xsl:variable name="end" 
                  select="xs:integer($summaries[position()=1]/endIndex)"/>
    <xsl:variable name="displayErrors" 
                  select="sum($seriesMatch/period[begin&gt;=$beginTimes[$begin] and
                  end&lt;=$endTimes[$end]]/failure[not(matches(message, $ignoreErrs))]/count)"/>
    <center>
     <xsl:if test="$resourceIds or $suiteNames">
      <h2><i>Errors for 
      <xsl:if test="$resourceIds">'<xsl:value-of select="$resourceIds"/>'</xsl:if>
      <xsl:if test="$resourceIds and $suiteNames"> and </xsl:if>
      <xsl:if test="$suiteNames">'<xsl:value-of select="$suiteNames"/>'</xsl:if>
      </i></h2>
       <p class="tableSubHead">
         View: <a href="{$href}">all errors</a>
         <xsl:if test="$resourceIds and $suiteNames">
           :: <a href="{$href}&amp;resourceIds={$resourceIds}">
                all for <xsl:value-of select="$resourceIds"/></a>
           :: <a href="{$href}&amp;suiteNames={$suiteNames}">
                all for <xsl:value-of select="$suiteNames"/></a>
         </xsl:if>
        </p>
     </xsl:if>
      <table class="sortable">
      <xsl:choose><xsl:when test="$displayErrors and $displayErrors > 0">
        <xsl:call-template name="printSortHeaderRow">
          <xsl:with-param name="summaries" select="$summaries"/>
        </xsl:call-template>
        <tbody>
        <xsl:choose>
          <xsl:when test="$topSeriesErrs='1'">
            <!-- apply template on the first node of every 'group' -->
            <xsl:apply-templates select="$seriesMatch[count(. | key('series-by-nickname', nickname)[1]) = 1]">
              <xsl:sort select="sum(key('series-by-nickname', nickname)/period[begin&gt;=$beginTimes[1] and end&lt;=$endTimes[1]]/failure[not(matches(message, $ignoreErrs))]/count)" order="descending"/>
              <xsl:with-param name="summaries" select="$summaries"/>
              <xsl:with-param name="beginTimes" select="$beginTimes"/>
              <xsl:with-param name="endTimes" select="$endTimes"/>
            </xsl:apply-templates>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="$seriesMatch">
              <xsl:sort select="sum(period[begin&gt;=$beginTimes[1] and end&lt;=$endTimes[1]]/failure[not(matches(message, $ignoreErrs))]/count)" order="descending"/>
              <xsl:with-param name="summaries" select="$summaries"/>
              <xsl:with-param name="beginTimes" select="$beginTimes"/>
              <xsl:with-param name="endTimes" select="$endTimes"/>
            </xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
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

  <!-- ==================================================================== -->
  <!-- printSortHeaderRow - print table header row for sortable table       -->
  <!-- ==================================================================== -->
  <xsl:template name="printSortHeaderRow">
    <xsl:param name="summaries"/>
    <xsl:variable name="title1" select="$summaries[position()=1]/title"/>
    <xsl:variable name="title2" select="$summaries[position()=2]/title"/>
    <thead>
      <tr>
        <xsl:if test="$topSeriesErrs!='1'">
          <th class="sortHead sortable-left-break">Suite</th>
          <th class="sortHead sortable-left-break">Resource</th>
          <th class="sortHead sortable-left-break">Target</th>
        </xsl:if>
        <th class="sortHead sortable-left-break">Nickname</th>
        <th class="sortable-left-break"><p class="sortLabel"><xsl:value-of select="$title1"/></p>
          <font class="sortHead"> #<br/>errs</font></th>
        <th class="sortHead">#<br/>unique<br/>errs</th>
        <th class="sortHead sortable-break">%<br/> pass</th>
        <th><p class="sortLabel">change between</p>
          <font class="sortHead"># errs<br/><xsl:value-of select="$title1"/></font>
            <br/><font class="sortlabel">and</font>
            <br/><font class="sortHead"># errs<br/>
            <xsl:value-of select="$title2"/></font></th>
        <th class="sortable-break">
          <font class="sortHead">% pass<br/><xsl:value-of select="$title1"/></font>
            <br/><font class="sortlabel">and</font>
            <br/><font class="sortHead">% pass<br/>
            <xsl:value-of select="$title2"/></font></th>
        <!-- header columns for periods -->
        <xsl:for-each select="$summaries[position()>1]">
          <th><p class="sortLabel"><xsl:value-of select="title"/></p>
            <font class="sortHead"> #<br/>errs</font></th>
          <th class="sortHead">#<br/>unique<br/>errs</th>
          <th class="sortHead sortable-break">%<br/> pass</th>
        </xsl:for-each>

      </tr>
    </thead>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printRow - print a series and its stats                              -->
  <!-- ==================================================================== -->
  <xsl:template name="printRow" match="series">
    <xsl:param name="summaries"/>
    <xsl:param name="beginTimes"/>
    <xsl:param name="endTimes"/>

    <xsl:variable name="recentErrors">
      <xsl:choose>
        <xsl:when test="$topSeriesErrs='1'">
          <xsl:apply-templates select="$summaries[position()=1]" mode="printStats">
            <xsl:with-param name="summaries" select="$summaries"/>
            <xsl:with-param name="beginTimes" select="$beginTimes"/>
            <xsl:with-param name="endTimes" select="$endTimes"/>
            <xsl:with-param name="periods" select="key('series-by-nickname', nickname)/period"/>
          </xsl:apply-templates>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="$summaries[position()=1]" mode="printStats">
            <xsl:with-param name="summaries" select="$summaries"/>
            <xsl:with-param name="beginTimes" select="$beginTimes"/>
            <xsl:with-param name="endTimes" select="$endTimes"/>
            <xsl:with-param name="periods" select="period"/>
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="$recentErrors[not(matches(., '^0|-.*'))]">
      <!-- get just the suite name - after the last '/' -->
      <xsl:variable name="pieceCount" select="count(tokenize(guid, '/'))"/>
      <xsl:variable name="suiteName" select="(tokenize(guid, '/'))[$pieceCount]"/>
      <tr>
        <xsl:if test="$topSeriesErrs!='1'">
          <td class="sortable-left-break"><xsl:value-of select="$suiteName"/> </td>
          <td class="sortable-left-break"><xsl:value-of select="resource"/></td>
          <td class="sortable-left-break"><xsl:value-of select="targetHostname"/></td>
        </xsl:if>
        <td class="sortable-left-break">
          <xsl:variable name="startDate">
            <xsl:call-template name="convertMillisToDate">
              <xsl:with-param name="millis" 
                select="$beginTimes[xs:integer($summaries[position()=1]/beginIndex)]"/>
              <xsl:with-param name="format" select="'MMddyy'"/>
            </xsl:call-template>
          </xsl:variable>
          <xsl:variable name="endDate">
            <xsl:call-template name="convertMillisToDate">
              <xsl:with-param name="millis" 
                select="$endTimes[xs:integer($summaries[position()=1]/endIndex)]"/>
              <xsl:with-param name="format" select="'MMddyy'"/>
            </xsl:call-template>
          </xsl:variable>
          <xsl:variable name="nameLink">
            <xsl:choose>
              <xsl:when test="$topSeriesErrs='1'">
                <xsl:value-of select="concat('/inca/jsp/summaryDetails.jsp?guid=',guid)"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:variable name="targetText"><xsl:if test="not(empty(targetHostname)) and targetHostname != ''"> to <xsl:value-of select="targetHostname"/> </xsl:if></xsl:variable>
                <xsl:variable name="label" select="concat(resource, $targetText, ' (',nickname,')' )"/>
                <xsl:value-of select="concat('/inca/jsp/graph.jsp?series=',nickname,',',resource,',',targetHostname,',',$label,'&amp;map=true&amp;startDate=',$startDate)"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <a href="{$nameLink}"><xsl:value-of select="nickname"/></a>
        </td>
        <xsl:choose>
          <xsl:when test="$topSeriesErrs='1'">
            <xsl:apply-templates select="$summaries" mode="printStats">
              <xsl:with-param name="summaries" select="$summaries"/>
              <xsl:with-param name="beginTimes" select="$beginTimes"/>
              <xsl:with-param name="endTimes" select="$endTimes"/>
              <xsl:with-param name="periods" select="key('series-by-nickname', nickname)/period"/>
              <xsl:with-param name="table" select="'true'"/>
            </xsl:apply-templates>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="$summaries" mode="printStats">
              <xsl:with-param name="summaries" select="$summaries"/>
              <xsl:with-param name="beginTimes" select="$beginTimes"/>
              <xsl:with-param name="endTimes" select="$endTimes"/>
              <xsl:with-param name="periods" select="period"/>
              <xsl:with-param name="table" select="'true'"/>
            </xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
      </tr>
    </xsl:if>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printStats - calculate num errors and error percentage               -->
  <!-- ==================================================================== -->
  <xsl:template name="printStats" match="summary" mode="printStats">
    <xsl:param name="summaries"/>
    <xsl:param name="beginTimes"/>
    <xsl:param name="endTimes"/>
    <xsl:param name="periods"/>
    <xsl:param name="table"/>

    <xsl:variable name="beginIndex" select="xs:integer(beginIndex)"/>
    <xsl:variable name="endIndex" select="xs:integer(endIndex)"/>
    <xsl:variable name="beginTime" select="$beginTimes[$beginIndex]"/>
    <xsl:variable name="endTime" select="$endTimes[$endIndex]"/>
      <xsl:variable name="thesePeriods"
                    select="$periods[begin&gt;=$beginTime and end&lt;=$endTime and (success > 0 or failure/count > 0)]"/>
      <xsl:variable name="fail" select="$thesePeriods/failure[not(matches(message, $ignoreErrs))]"/>
      <xsl:variable name="errorCount" select="sum($fail/count)"/>
      <xsl:variable name="successCount" select="sum($thesePeriods/success)"/>
      <xsl:variable name="total" select="$successCount + $errorCount"/>
      <xsl:choose>
        <!-- series executed during these periods and printing in table -->
        <xsl:when test="$total > 0 and $table">
          <xsl:variable name="errorPerc" select="round(($successCount div $total)*100)"/>
          <td class="sortable-left-break"><xsl:value-of select="$errorCount"/></td>
          <!-- unique errs in most recent period -->
          <td><xsl:value-of select="count($fail)"/></td>
          <td class="sortable-break"><xsl:value-of select="$errorPerc"/>%</td>
          <!-- compare most recent period to period before -->
          <xsl:if test="position()=1">
            <xsl:variable name="oldError">
              <xsl:apply-templates select="$summaries[position()=2]" mode="printStats">
                <xsl:with-param name="summaries" select="$summaries"/>
                <xsl:with-param name="beginTimes" select="$beginTimes"/>
                <xsl:with-param name="endTimes" select="$endTimes"/>
                <xsl:with-param name="periods" select="$periods"/>
              </xsl:apply-templates>
            </xsl:variable>
            <xsl:variable name="oldErrorCount">
              <xsl:analyze-string select="$oldError" regex="(.[^:]*):.*">
                <xsl:matching-substring><xsl:value-of select="regex-group(1)"/></xsl:matching-substring>
                <xsl:non-matching-substring><xsl:value-of select="'-'"/></xsl:non-matching-substring>
              </xsl:analyze-string>
            </xsl:variable>
            <xsl:variable name="oldErrorPerc">
              <xsl:analyze-string select="$oldError" regex="(.[^:]*):(.*)">
                <xsl:matching-substring><xsl:value-of select="regex-group(2)"/></xsl:matching-substring>
                <xsl:non-matching-substring><xsl:value-of select="'-'"/></xsl:non-matching-substring>
              </xsl:analyze-string>
            </xsl:variable>
            <xsl:variable name="comparison">
              <xsl:choose><xsl:when test="$oldErrorCount[matches(.,'^\d.*')]">
                <xsl:value-of
                    select="$errorCount - xs:integer($oldErrorCount)"/>
              </xsl:when><xsl:otherwise>-</xsl:otherwise></xsl:choose>
            </xsl:variable>
            <xsl:variable name="class">
              <xsl:if test="$comparison[matches(.,'\d.*')]">
                <xsl:if test="xs:integer($comparison) > 0">errorText</xsl:if>
                <xsl:if test="xs:integer($comparison) &lt; 0">passText</xsl:if>
              </xsl:if>
            </xsl:variable>
            <xsl:variable name="comparisonPerc">
              <xsl:choose><xsl:when test="$oldErrorPerc[matches(.,'^\d.*')]">
                <xsl:value-of
                    select="$errorPerc - xs:integer($oldErrorPerc)"/>
              </xsl:when><xsl:otherwise>-</xsl:otherwise></xsl:choose>
            </xsl:variable>
            <xsl:variable name="classPerc">
              <xsl:if test="$comparisonPerc[matches(.,'\d.*')]">
                <xsl:if test="xs:integer($comparisonPerc) > 0">passText</xsl:if>
                <xsl:if test="xs:integer($comparisonPerc) &lt; 0">errorText</xsl:if>
              </xsl:if>
            </xsl:variable>
            <td class="{$class}">
              <xsl:if test="$class='errorText'">+</xsl:if>
              <xsl:value-of select="$comparison"/>
            </td>
            <td class="{$classPerc} sortable-break">
              <xsl:if test="$classPerc='passText'">+</xsl:if>
              <xsl:value-of select="$comparisonPerc"/>
              <xsl:if test="$comparison[matches(.,'\d.*')]">%</xsl:if>
            </td>
          </xsl:if>
        </xsl:when>
        <!-- getting error count for non-table display -->
        <xsl:when test="$total > 0">
          <xsl:value-of select="$errorCount"/>
          <xsl:value-of select="':'"/>
          <xsl:value-of select="round(($successCount div $total)*100)"/>
        </xsl:when>
        <!-- series did not execute during these periods -->
        <xsl:otherwise>
          <td>-</td>
          <td>-</td>
          <td class="sortable-break">-</td>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
