<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- inca-common.xsl:  Common templates for use in Inca stylesheets.      -->
<!-- ==================================================================== -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:sdf="java.text.SimpleDateFormat"
                xmlns:date="java.util.Date"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:rs="http://inca.sdsc.edu/queryResult/reportSummary_2.0">


  <!-- ==================================================================== -->
  <!-- printErrors - print errors in xml                                    -->
  <!-- ==================================================================== -->
  <xsl:template name="printErrors" match="error">
    <h3>Error:  <xsl:value-of select="." /></h3>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printBodyTitle:  print the title of a page along with a timestamp    -->
  <!-- ==================================================================== -->
  <xsl:template name="printBodyTitle">
    <xsl:param name="title"/>
    <xsl:variable name="datenow" select="date:new()" />
    <table width="100%" border="0">
      <tr align="left">
        <td><h1 class="body"><xsl:value-of select="$title"/></h1></td>
        <td align="right">
          <p class="footer">Page loaded:
            <xsl:call-template name="formatDate">
              <xsl:with-param name="date" select="$datenow"/>
            </xsl:call-template>
          </p>
        </td>
      </tr>
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- formatDate: format a date like "MM-dd-yyyy hh:mm a (z)"              -->
  <!-- ==================================================================== -->
  <xsl:template name="formatDate">
    <xsl:param name="date"/>
    <xsl:variable name="dateformat" select="sdf:new('MM-dd-yyyy hh:mm a (z)')"/>
    <xsl:value-of select="sdf:format($dateformat, $date)"/>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- formatAge: format a date like "X days X hours X mins" or "X secs"    -->
  <!-- ==================================================================== -->
  <xsl:template name="formatAge">
    <xsl:param name="age"/>
    <xsl:variable name="diff" select="current-dateTime()-$age"/>
    <xsl:variable name="format" select="replace(replace(replace(replace(
        $diff, 'P',''),'D',' days '),'T',''),'H',' hours ')"/>
    <xsl:choose>
      <xsl:when test="$format[not(matches(.,'^(\d|\.)+S$'))]">
        <xsl:value-of select="replace($format, 'M.*',' mins')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of
            select="replace($format, '\.\d+S',' secs')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- getStatus:  interpret the status of an Inca result                   -->
  <!-- ==================================================================== -->
  <xsl:template name="getStatus">
    <xsl:param name="result" />
    <xsl:param name="states" />

    <xsl:variable name="stale">
      <xsl:if test="$result/gmtExpires">
        <xsl:call-template name="markOld">
          <xsl:with-param name="gmtExpires" select="$result/gmtExpires" as="xs:dateTime" />
        </xsl:call-template>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="errMsg" select="$result/errorMessage" />
    <xsl:variable name="comparitor" select="$result/comparisonResult" />


    <xsl:variable name="primaryState"><xsl:choose>
      <xsl:when test="count($result/body)=0">missing</xsl:when> 
      <xsl:when test="$errMsg[matches(., '.*skipped due to high load.*')]">busy</xsl:when>
      <xsl:when test="$errMsg[matches(., '^NOT_AT_FAULT:')]">noFault</xsl:when>
      <xsl:when test="$result/body//statistics/statistic[ID='errors']/value>0">error</xsl:when>
      <xsl:when test="$result/body//statistics/@errors>0">error</xsl:when>
      <xsl:when test="($comparitor='Success' and not($errMsg[matches(., 'Unable to fetch proxy|Inca error')])) or
        (string($result/body)!=''
         and string($errMsg)=''
         and string($comparitor)='' )">pass</xsl:when>
      <xsl:otherwise>error</xsl:otherwise>
    </xsl:choose></xsl:variable>

    <xsl:variable name="secondaryState"><xsl:choose>
      <xsl:when test="$errMsg[matches(., '^DOWNTIME:.*: ')]">down</xsl:when>
      <xsl:when test="string($stale)!=''">stale</xsl:when>
      <xsl:when test="$result/body//statistics/statistic[ID='warnings']/value>0">warnings</xsl:when>
      <xsl:when test="$result/body//statistics/@warnings>0">warnings</xsl:when>
      <xsl:when test="$errMsg[matches(., 'Unable to fetch proxy')]">proxyError</xsl:when>
      <xsl:when test="$errMsg[matches(., 'Inca error')]">incaError</xsl:when>
    </xsl:choose></xsl:variable>

    <xsl:choose><xsl:when test="$secondaryState!=''">
      <xsl:value-of select="$states/secondaryState[@name=$secondaryState]/@bgcolor"/>
    </xsl:when><xsl:otherwise>
      <xsl:value-of select="$states/primaryState[@name=$primaryState]/@bgcolor"/>
    </xsl:otherwise></xsl:choose>|<xsl:value-of select="$states/primaryState[@name=$primaryState]/@img"/>|<xsl:value-of select="$states/secondaryState[@name=$secondaryState]/@text"/>

  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printCronDescription:  print description of Inca cron syntax         -->
  <!-- ==================================================================== -->
  <xsl:template name="printCronDescription">
    <p><a name="cron">(*)</a> Inca uses a modified version of Vixie cron syntax.
      The format is as follows:</p>
    <p><b>minute hour dayOfMonth month dayOfWeek</b></p>
    <table border="0">
      <tr>
        <td><b>minute</b></td>
        <td>The minute of the hour the reporter will be executed (range: 0-59)</td>
      </tr>
      <tr>
        <td><b>hour</b></td>
        <td>The hour of the day the reporter will be executed (range: 0-23)</td>
      </tr>
      <tr>
        <td><b>dayOfMonth</b></td>
        <td>The day of the month the reporter will be executed (range: 0-23)</td>
      </tr>
      <tr>
        <td><b>month</b></td>
        <td>The month the reporter will be executed (range: 1-12)</td>
      </tr>
      <tr>
        <td><b>dayOfWeek</b></td>
        <td>The day of the week the reporter will be executed (range: 0-6)</td>
      </tr>
    </table>
    <p>Ranges are allowed in any field.  For example, "0-4" in the minute field
      would mean to execute on the minutes 0, 1, 2, 3, and 4 only.  A step
      value can also be used with a range.  For example, "0-59/10" in the minute
      field would indicate to run every 10 minutes.  Finally, "?" in the field
      tells Inca to pick a random time within the specified range.  For example,
      "? * * * *" means to run every hour and let Inca choose which minute
      to run the reporter on.  Likewise, "?-59/10 * * * *" means to run every
      10 minutes and let Inca choose which minute to start on (e.g., if Inca
      chose "5", the reporter would execute at minutes 5, 15, 25, 35, 45, and 55.
    </p>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- markOld - print an ' *' if $gmt is older than $markAge hours         -->
  <!-- ==================================================================== -->
  <xsl:template name="markOld">
    <xsl:param name="gmtExpires" />
    <xsl:variable name="now" select="current-dateTime()" />
    <xsl:if test="$gmtExpires le $now">
      <xsl:value-of select="' *'" />
    </xsl:if>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSeriesNamesTable - prints a table with a list of series names   -->
  <!-- ==================================================================== -->
  <xsl:template name="printSeriesNamesTable">
    <xsl:param name="seriesNames"/>
    <table cellpadding="8">
      <tr valign="top">
        <td>
          <xsl:for-each select="$seriesNames">
            <xsl:sort/>
            <xsl:if test="position() mod 4 = 1">
              <td />
            </xsl:if>
            <li><a href="#{.}"><xsl:value-of select="." /></a></li>
          </xsl:for-each>
        </td>
      </tr>
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printResourceNameCell - prints a table cell with resource name.      -->
  <!-- ==================================================================== -->
  <xsl:template name="printResourceNameCell" match="resource" mode="name">
    <td class="subheader"><xsl:value-of select="name" /></td>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printStatisticsTable - prints a table with body statistics.  -->
  <!-- ==================================================================== -->
  <xsl:template name="printBodyStats">
    <xsl:param name="report"/>
    <xsl:if test="$report/body//statistics">
       <xsl:for-each select="$report/body//statistics/statistic">
         <xsl:if test="not(matches(ID, 'errors|warnings')) or value &gt; 0 ">
         <tr>
         <td align="left">&#160;<xsl:value-of select="ID"/></td>
         <td><xsl:value-of select="value"/>&#160; <xsl:value-of select="units"/></td></tr>
         </xsl:if>
       </xsl:for-each>
       <xsl:for-each select="$report/body//statistics/@*">
         <xsl:if test="not(matches(name(), 'errors|warnings')) or . &gt; 0 ">
         <tr>
         <td><xsl:value-of select="name()"/></td>
         <td><xsl:value-of select="."/></td>
         </tr>
         </xsl:if>
       </xsl:for-each>
     </xsl:if>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- getLink - get hyperlink to use for report summary downtimes          -->
  <!-- ==================================================================== -->
  <xsl:template name="getLink">
    <xsl:param name="errMsg" />
    <xsl:param name="normRef" />
    <xsl:param name="downtimeUrl" />
    <xsl:choose>
      <xsl:when test="$errMsg[matches(., '^DOWNTIME:\d+:')]">
        <xsl:value-of select="$downtimeUrl/@prefix" />
        <xsl:analyze-string select="$errMsg" regex="^DOWNTIME:(\d+):.*">
          <xsl:matching-substring>
            <xsl:value-of select="regex-group(1)"/>
          </xsl:matching-substring>
        </xsl:analyze-string>
        <xsl:value-of select="$downtimeUrl/@suffix" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$normRef" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- getDownErr - get error message to use for report details downtimes   -->
  <!-- ==================================================================== -->
  <xsl:template name="getDownErr">
    <xsl:param name="errMsg" />
    <xsl:param name="downtimeUrl" />
    <xsl:choose>
      <xsl:when test="$errMsg[matches(., '^DOWNTIME:\d+:')]">
        <xsl:variable name="item">
        <xsl:analyze-string select="$errMsg" regex="^DOWNTIME:(\d+):.*">
          <xsl:matching-substring>
            <xsl:value-of select="regex-group(1)"/>
          </xsl:matching-substring>
        </xsl:analyze-string>
        </xsl:variable>
        <xsl:variable name="href"
                      select="concat($downtimeUrl/@prefix, $item, $downtimeUrl/@suffix)" />
        <a href="{$href}">DOWNTIME:</a>
        <xsl:value-of select="replace( $errMsg, '^DOWNTIME:\d+:','')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$errMsg" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSuiteBoxes                                                      -->
  <!--                                                                      -->
  <!-- Calls printSeriesBoxTable                                            -->
  <!-- ==================================================================== -->
  <xsl:template name="printSuiteBoxes" match="suite" mode="box">
    <xsl:variable name="tableLabel" select="name"/>
    <xsl:variable name="seriesNames"
                  select="distinct-values(quer:object//rs:reportSummary/nickname)"/>
    <xsl:if test="position() = 1">
      <input type="button" name="CheckAll" value="check all"
             onClick="checkAll(form.series)"/>
      <input type="button" name="UnCheckAll" value="uncheck all"
             onClick="uncheckAll(form.series)"/>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="count(/combo/resources)=1">
        <!-- inca-common.xsl -->
        <xsl:call-template name="printCheckBoxTable">
          <xsl:with-param name="tableLabel" select="$tableLabel"/>
          <xsl:with-param name="rowLabels" select="$seriesNames"/>
          <xsl:with-param name="colLabels"
                          select="/combo/resources/resource[name]"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="printCheckBoxTable">
          <xsl:with-param name="tableLabel" select="$tableLabel"/>
          <xsl:with-param name="rowLabels" select="$seriesNames"/>
          <xsl:with-param name="colLabels"
                          select="resources/resource[name]"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printCheckBoxTable                                                   -->
  <!--                                                                      -->
  <!-- Prints a table with form check boxes                                 -->
  <!-- ==================================================================== -->
  <xsl:template name="printCheckBoxTable">
    <xsl:param name="tableLabel"/>
    <xsl:param name="rowLabels"/>
    <xsl:param name="colLabels"/>
    <xsl:variable name="tableNode" select="."/>
    <p><xsl:value-of select="$tableLabel"/></p>
    <table class="subheader">
      <xsl:for-each select="$rowLabels">
        <xsl:sort/>
        <xsl:variable name="rowLabel" select="."/>
        <xsl:if test="position() mod 20 = 1">
          <!-- header row of column names -->
          <tr>
            <td class="subheader"/>
            <td class="subheader"/>
            <!-- inca-common.xsl -->
            <xsl:apply-templates select="$colLabels" mode="name">
              <xsl:sort/>
            </xsl:apply-templates>
          </tr>
          <!-- header row of column checkboxes -->
          <tr>
            <td class="subheader" colspan="2">select row or column:</td>
            <!-- printColumnHeaderBoxCell -->
            <xsl:apply-templates select="$colLabels" mode="box">
              <xsl:sort/>
              <xsl:with-param name="tableId" select="$tableLabel"/>
            </xsl:apply-templates>
          </tr>
        </xsl:if>
        <!-- row for each series name -->
        <tr>
          <td class="clear">
            <xsl:value-of select="."/>
          </td>
          <td class="subheader">
            <input type="checkbox" name="fliprow" value="row"
                   onClick="flip(form, '{$tableLabel}', '{$rowLabel}',
                     'ROW', this.checked)"/>
          </td>
          <!-- printBoxCellValue -->
          <xsl:apply-templates select="$colLabels" mode="resultbox">
            <xsl:sort/>
            <xsl:with-param name="tableNode" select="$tableNode"/>
            <xsl:with-param name="tableId" select="$tableLabel"/>
            <xsl:with-param name="rowLabel" select="$rowLabel"/>
          </xsl:apply-templates>
        </tr>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printColumnHeaderBoxCell                                             -->
  <!--                                                                      -->
  <!-- Prints a table cell with resource form box.                          -->
  <!-- ==================================================================== -->
  <xsl:template name="printColumnHeaderBoxCell" match="resource" mode="box">
    <xsl:param name="tableId"/>
    <td class="subheader">
      <input type="checkbox" name="flipcol" value="col" onClick="flip(form,
              '{$tableId}', '{name}', 'COL', this.checked)"/>
    </td>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printBoxCellValue                                                    -->
  <!--                                                                      -->
  <!-- Prints a table cell with resource form box.                          -->
  <!-- ==================================================================== -->
  <xsl:template name="printBoxCellValue" match="resource" mode="resultbox">
    <xsl:param name="tableNode"/>
    <xsl:param name="tableId"/>
    <xsl:param name="rowLabel"/>
    <xsl:variable name="regexHost" select="concat('^', name, '$|',
        replace(macros/macro[name='__regexp__']/value, ' ','|'))"/>
    <xsl:variable name="result" select="$tableNode/quer:object//rs:reportSummary[
                  (matches(targetHostname,$regexHost) or
                  (matches(hostname,$regexHost) and string(targetHostname)=''))
                   and nickname=$rowLabel]" />
    <xsl:choose>
      <xsl:when test="string(macros)= '' or count($result)>0">
        <!-- resource is not exempt -->
        <td class="clear">
          <input type="checkbox" name="series" value="{concat($rowLabel, ',',
            name, ',', name, ' (', $rowLabel, '),', $tableId)}"
                 id="{concat('-TABLEID-',$tableId,'-COL-',name,'-ROW-',$rowLabel)}"/>
        </td>
      </xsl:when>
      <xsl:otherwise>
        <!-- resource is exempt -->
        <td class="na">
          <xsl:text>n/a</xsl:text>
        </td>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSummaryValue                                                    -->
  <!--                                                                      -->
  <!-- Prints a table cell with summary result.                             -->
  <!-- ==================================================================== -->
  <xsl:template name="printSummaryValue">
    <xsl:param name="test"/>
    <xsl:param name="summaries"/>
    <xsl:param name="states"/>
    <xsl:variable name="numPassRegex" select="concat('^', $test, '-success$')"/>
    <xsl:variable name="numPass" select="$summaries[ID[matches(., $numPassRegex)]]/value"/>
    <xsl:variable name="numFailRegex" select="concat('^', $test, '-fail$')"/>
    <xsl:variable name="numFail" select="$summaries[ID[matches(., $numFailRegex)]]/value"/>
    <xsl:variable name="numTotal" select="$numPass+$numFail"/>
    <xsl:variable name="sumReport" select="$numPass/../../../../../.."/>
    <xsl:variable name="sumLink"
      select="concat('/inca/jsp/instance.jsp?nickname=', encode-for-uri($sumReport/nickname),
           '&amp;resource=', $sumReport/hostname, '&amp;target=', $sumReport/targetHostname,
           '&amp;collected=', encode-for-uri($sumReport/gmt) )"/>
    <xsl:choose>
      <xsl:when test="$numTotal>1">
        <xsl:variable name="status">
          <xsl:choose><xsl:when test="$numPass>1">
              <xsl:value-of select="'pass'"/>
            </xsl:when><xsl:otherwise>
              <xsl:value-of select="'error'"/>
          </xsl:otherwise></xsl:choose>
        </xsl:variable>
        <td bgcolor="{$states/primaryState[@name=$status]/@bgcolor}"><a href="{$sumLink}">
          <img src="{concat('/inca/img/', $states/primaryState[@name=$status]/@img)}"/><br/>
          <xsl:value-of select="$numPass"/>/<xsl:value-of select="$numTotal"/>
        </a></td>
      </xsl:when>
      <xsl:when test="$numTotal=0 or $numTotal=1">
        <td class="clear"><xsl:value-of select="' '" /></td>
      </xsl:when>
      <xsl:otherwise>
        <td class="na">n/a</td>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- replaceBreak - replace symbol break with html break                  -->
  <!-- ==================================================================== -->
  <xsl:template name="replaceBreak">
    <xsl:param name="text" select="."/>
    <xsl:choose>
      <xsl:when test="contains($text, '&#xa;')">
        <xsl:copy-of select="substring-before($text, '&#xa;')"/> <br/>
        <xsl:call-template name="replaceBreak">
          <xsl:with-param name="text" select="substring-after($text, '&#xa;')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- convertMillisToDate - convert milliseconds to a date                         -->
  <!-- ==================================================================== -->
  <xsl:template name="convertMillisToDate">
    <xsl:param name="millis"/>
    <xsl:param name="format"/>
    <xsl:variable name="origin" select="xs:date('1970-01-01')"/>
    <xsl:variable name="one-ms" select="xs:dayTimeDuration('PT0.001S')"/>
    <xsl:variable name="in" select="xs:integer($millis)"/>
    <xsl:variable name="date" select="$origin + $in * $one-ms"/>
    <xsl:variable name="dateformat" select="sdf:new($format)"/>
    <xsl:value-of select="sdf:format($dateformat, $date)"/>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- convertDateToMillis - convert date to milliseconds                   -->
  <!-- ==================================================================== -->
  <xsl:template name="convertDateToMillis">
    <xsl:param name="date"/>
    <xsl:variable name="origin" select="xs:date('1970-01-01')"/>
    <xsl:variable name="one-ms" select="xs:dayTimeDuration('PT0.001S')"/>
    <xsl:variable name="in" select="xs:date($date)"/>
    <xsl:value-of select='xs:integer(($in - $origin) div $one-ms)'/>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printRanges - print date ranges                                      -->
  <!-- ==================================================================== -->
  <xsl:template name="printRanges" match="summary" mode="printRanges">
    <xsl:param name="beginTimes"/>
    <xsl:param name="endTimes"/>
    <xsl:param name="href"/>
    <xsl:variable name="beginIndex" select="xs:integer(beginIndex)"/>
    <xsl:variable name="endIndex" select="xs:integer(endIndex)"/>
    <xsl:variable name="begin" select="$beginTimes[$beginIndex]"/>
    <xsl:variable name="end" select="$endTimes[$endIndex]"/>
    <xsl:variable name="errLink"
        select="concat(replace($href, 'seriesSummary.xsl','errMsgSummary.xsl'),
        '&amp;startErrs=',$begin,'&amp;endErrs=',$end)"/>
    <tr><td> <a href="{$errLink}"><xsl:value-of select="title"/></a>: </td><td>
    <xsl:if test="$begin and $end">
      <xsl:call-template name="convertMillisToDate">
        <xsl:with-param name="millis" select="$begin"/>
        <xsl:with-param name="format" select="'MM/dd/yy'"/>
      </xsl:call-template> -
      <xsl:call-template name="convertMillisToDate">
        <xsl:with-param name="millis" select="$end"/>
        <xsl:with-param name="format" select="'MM/dd/yy'"/>
      </xsl:call-template>
    </xsl:if>
    </td></tr>
  </xsl:template>

</xsl:stylesheet>
