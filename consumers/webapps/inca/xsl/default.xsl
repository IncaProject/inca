<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- default.xsl:  Prints table of suite(s) results.                      -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:rs="http://inca.sdsc.edu/queryResult/reportSummary_2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <xsl:param name="queryStr"/>

  <!-- ==================================================================== -->
  <!-- generateHTML                                                         -->
  <!--                                                                      -->
  <!-- Prints legend and calls printSuiteInfo.                              -->
  <!-- ==================================================================== -->
  <xsl:template name="generateHTML" match="/combo">
    <!-- inca-common.xsl -->
    <xsl:if test="not(contains($queryStr,'noDescription=true'))">
      <xsl:call-template name="printBodyTitle">
        <xsl:with-param name="title" select="'Inca reporter results'"/>
      </xsl:call-template>
      <p>Inca test results, version information, or performance results are shown below
         in one or more tables.  Each table displays related test results where the
         rows of the table will display the name of an Inca test, software version, or
         performance measurement.  The columns of the table display the resource where the
         test was executed.  Click on selected
         icons (described in the <a href="javascript:window.open('/inca/jsp/legend.jsp','incalegend','width=400,height=325,resizable=yes')">legend</a>) for more details about the
         collected Inca report.</p>
    </xsl:if>

    <!-- printSuiteInfo -->
    <xsl:apply-templates select="suites/suite|queries/query" />
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSuiteInfo                                                       -->
  <!--                                                                      -->
  <!-- Calls printSeriesNamesTable and printSeriesResultsTable              -->
  <!-- ==================================================================== -->
  <xsl:template name="printSuiteInfo" match="suite|query">
    <xsl:variable name="name" select="name"/>

    <xsl:variable name="defaultconfig" select="document('../xml/default.xml')/default"/>

    <!-- get all series names; we don't want cross-site names that aren't all2all series
         and don't want summary reporters -->
    <xsl:variable name="seriesNames"
         select="distinct-values(quer:object//rs:reportSummary[starts-with(nickname, 'all2all:') or ( not(contains(nickname, '_to_')) and not(matches(uri, '/summary\.successpct\.performance$')) )]/nickname)"/> 
    <xsl:variable name="csSeriesNamesString">
      <!-- needs to be crunched on one line to take out newlines in string -->
      <xsl:for-each select="quer:object//rs:reportSummary[not(ends-with(uri,'summary.successpct.performance'))]/nickname[not(starts-with(., 'all2all:')) and contains(., '_to_')]"><xsl:value-of select="substring-before(., '_to_')"/><xsl:if test="position() != last()">,</xsl:if></xsl:for-each>
    </xsl:variable>
    <xsl:variable name="csSeriesNames" select="distinct-values(tokenize($csSeriesNamesString,','))"/>

    <!-- Uncomment below if want to print a list of tests in the below table with links
    <xsl:call-template name="printSeriesNamesTable">
      <xsl:with-param name="seriesNames" select="$seriesNames"/>
    </xsl:call-template>
    -->
    <xsl:variable name="summaries" select="quer:object//rs:reportSummary[matches(uri,
     '/summary\.successpct\.performance$')]/body/performance/benchmark/statistics/statistic"/>
    <xsl:variable name="resources" select="/combo/resources/resource |
               /combo/suites/suite[matches(name, $name)]/resources/resource" />
    <xsl:variable name="resourceid" select="/combo/suites/suite[matches(name, $name)]/resourceId" />
    <table><tr>
      <td>
        <h1><xsl:value-of select="$name"/></h1>
      </td>
      <td align="right">
        <a href="/inca/HTML/kit-status-v1/{$name}/{$resourceid}"><span class="buttonGrey"><xsl:text disable-output-escaping="yes"><![CDATA[html &sect;]]></xsl:text></span></a>
        <a href="/inca/XML/kit-status-v1/{$name}/{$resourceid}"><span class="buttonGrey"><xsl:text disable-output-escaping="yes"><![CDATA[&lt; xml /&gt;]]></xsl:text></span></a>
        <a href="/inca/jsp/query.jsp?action=Refresh&amp;qname=incaQueryLatest%2B{encode-for-uri( replace( /combo/suites/suite[matches(name, $name)]/guid, '/', '_' ) )}"><span class="buttonGrey"><xsl:text disable-output-escaping="yes"><![CDATA[refresh &#x21bb;]]></xsl:text></span></a>
        <a href="javascript:window.open('/inca/jsp/legend.jsp','incalegend','width=400,height=325,resizable=yes')"><span class="buttonGrey"><xsl:text disable-output-escaping="yes"><![CDATA[legend &#10027;]]></xsl:text></span></a>
      </td>
    </tr><tr><td colspan="2">
      <xsl:call-template name="printSeriesResultsTable">
        <xsl:with-param name="seriesNames" 
                        select="distinct-values(insert-before($seriesNames,0,$csSeriesNames))"/>
        <xsl:with-param name="summaries" select="$summaries"/>
        <xsl:with-param name="resources" select="$resources[macros/macro[name='__equivalent__' and value='true']]"/>
        <xsl:with-param name="defaultconfig" select="$defaultconfig"/>
        <xsl:with-param name="localdefaultconfig" select="default"/>
      </xsl:call-template>
    </td></tr></table>
    <br/>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSeriesResultsTable                                              -->
  <!--                                                                      -->
  <!-- Prints a table with series results.                                  -->
  <!-- ==================================================================== -->
  <xsl:template name="printSeriesResultsTable">
    <xsl:param name="seriesNames"/>
    <xsl:param name="summaries"/>
    <xsl:param name="resources"/>
    <xsl:param name="defaultconfig"/>
    <xsl:param name="localdefaultconfig"/>
    <xsl:variable name="suite" select="."/>

    <xsl:variable name="groupTag" select="replace(substring-after($queryStr,'tag='), '&amp;.*', '')"/>
    <xsl:variable name="tags" select="distinct-values($suite//tag[starts-with(.,$groupTag)])"/>

    <xsl:variable name="untaggedseries" select="distinct-values($suite//rs:reportSummary[not($groupTag) or not(tags/tag[starts-with(.,$groupTag)])]/nickname)"/>

    <table class="subheader">
      <xsl:if test="$groupTag">
      <xsl:for-each select="$tags">
        <xsl:sort/>
        <xsl:variable name="tagValue" select="substring-after(.,concat($groupTag,'='))"/>
        <xsl:variable name="matchTag" select="concat($groupTag,'=', $tagValue)"/>
        <tr>
          <td class="subheader"><xsl:value-of select="$tagValue"/></td>
          <xsl:if test="$summaries"><td class="subheader">SUMMARY</td></xsl:if>
          <!-- inca-common.xsl printResourceNameCell -->
          <xsl:apply-templates select="$resources" mode="name">
            <xsl:sort/>
          </xsl:apply-templates>
        </tr>
        <xsl:for-each select="distinct-values($suite//rs:reportSummary[tags/tag=$matchTag]/nickname)">
          <xsl:sort select="replace(., '\d', '')" />
          <xsl:sort select="replace(.,'[^\d]', '')" data-type="number"/>

          <xsl:variable name="seriesName" select="."/>
          <xsl:if test="$seriesNames[.=$seriesName]">
          <xsl:call-template name="printSeriesResultsRow">
            <xsl:with-param name="resources" select="$resources"/>
            <xsl:with-param name="seriesName" select="$seriesName"/>
            <xsl:with-param name="printSeriesName" select="$seriesName"/>
            <xsl:with-param name="suite" select="$suite"/>
            <xsl:with-param name="summaries" select="$summaries"/>
            <xsl:with-param name="defaultconfig" select="$defaultconfig"/>
          </xsl:call-template>
          </xsl:if>
        </xsl:for-each>
      </xsl:for-each>
      </xsl:if>
      <xsl:for-each select="$untaggedseries">
        <!-- do text number sort -->
        <xsl:sort select="replace(., '\d', '')" />
        <xsl:sort select="replace(.,'[^\d]', '')" data-type="number"/>

        <xsl:if test="position() mod 20 = 1">
          <tr>
            <td class="subheader"/>
            <xsl:if test="$summaries"><td class="subheader">SUMMARY</td></xsl:if>
            <!-- inca-common.xsl printResourceNameCell -->
            <xsl:apply-templates select="$resources" mode="name">
              <xsl:sort/>
            </xsl:apply-templates>
          </tr>
        </xsl:if>
        <xsl:variable name="seriesName" select="."/>
        <xsl:call-template name="printSeriesResultsRow">
          <xsl:with-param name="resources" select="$resources"/>
          <xsl:with-param name="seriesName" select="$seriesName"/>
          <xsl:with-param name="printSeriesName" select="$seriesName"/>
          <xsl:with-param name="suite" select="$suite"/>
          <xsl:with-param name="summaries" select="$summaries"/>
          <xsl:with-param name="defaultconfig" select="$defaultconfig"/>
        </xsl:call-template>
      </xsl:for-each>
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printSeriesResultsRow -->
  <!--                                                                      -->
  <!-- Prints a row of series results    .                                  -->
  <!-- ==================================================================== -->
  <xsl:template name="printSeriesResultsRow">
    <xsl:param name="resources"/>
    <xsl:param name="seriesName"/>
    <xsl:param name="printSeriesName"/>
    <xsl:param name="suite"/>
    <xsl:param name="summaries"/>
    <xsl:param name="defaultconfig"/>
    <tr>
      <td class="clear"><a name="{$seriesName}">
        <xsl:value-of select="$printSeriesName" />
      </a></td>
      <xsl:if test="$summaries">
        <xsl:call-template name="printSummaryValue">
          <xsl:with-param name="test" select="$seriesName"/>
          <xsl:with-param name="summaries" select="$summaries"/>
          <xsl:with-param name="states" select="$defaultconfig/incaResult"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:for-each select="$resources">
        <xsl:sort/>

        <xsl:variable name="regexHost" select="concat(name, '$|',
          replace(macros/macro[name='__regexp__']/value, ' ','|'))"/>
        <xsl:variable name="csSeriesName" select="concat('^', encode-for-uri($seriesName),'_to_(', $regexHost, ')' )"/>
        <xsl:variable name="reports" select="$suite/quer:object//rs:reportSummary[nickname=$seriesName or matches(nickname, $csSeriesName)]"/>
        <!--  If this series tests a resource other than the one it ran on, use the target -->
        <!--  hostname to match the report for the displayed resource result. Otherwise,   -->
        <!--  use the host it ran on to match a report with the test results to display.   -->
        <xsl:variable name="result">
          <xsl:choose>
            <xsl:when test="count($reports[matches(targetHostname,$regexHost)])=1">
              <xsl:copy-of select="$reports[matches(targetHostname,$regexHost)]"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:copy-of select="$reports[matches(hostname,$regexHost)]"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="printResourceResultCell">
          <xsl:with-param name="result" select="$result/node()[normalize-space()]"/>
          <xsl:with-param name="defaultconfig" select="$defaultconfig"/>
        </xsl:call-template>

      </xsl:for-each>
    </tr>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printResourceResultCell                                              -->
  <!--                                                                      -->
  <!-- Prints a table cell with resource result.                            -->
  <!-- ==================================================================== -->
  <xsl:template name="printResourceResultCell">
    <xsl:param name="result"/>
    <xsl:param name="defaultconfig"/>
    <xsl:variable name="instance" select="$result/instanceId" />
    <xsl:variable name="foundVersion" select="$result/body/package/version|$result/body/package/subpackage"/>
    <xsl:variable name="errMsg" select="$result/errorMessage" />

    <xsl:choose><xsl:when test="count($result)=1">
        <!-- resource is not exempt -->
      <xsl:variable name="normRef">
        <xsl:choose><xsl:when test="$result/gmt">
          <xsl:value-of select="concat('/inca/jsp/instance.jsp?nickname=', encode-for-uri($result/nickname), '&amp;resource=', $result/hostname, '&amp;target=', $result/targetHostname, '&amp;collected=', $result/gmt)"/>
        </xsl:when><xsl:otherwise>
          <xsl:value-of select="concat('/inca/jsp/runNow.jsp?configId=', $result/seriesConfigId)"/>
        </xsl:otherwise></xsl:choose>
     </xsl:variable>
       <xsl:variable name="href"><xsl:call-template name="getLink">
           <xsl:with-param name="errMsg" select="$errMsg"/>
           <xsl:with-param name="normRef" select="$normRef"/>
           <xsl:with-param name="downtimeUrl" select="$defaultconfig/downtimeUrl"/>
       </xsl:call-template></xsl:variable>

       <!-- inca-common.xsl:  returns string of bgcolor|img.png -->
       <xsl:variable name="state"><xsl:call-template name="getStatus">
           <xsl:with-param name="result" select="$result"/>
           <xsl:with-param name="states" select="$defaultconfig/incaResult"/>
       </xsl:call-template></xsl:variable>

       <xsl:if test="$state!=''">
         <xsl:variable name="bgcolor" select="tokenize($state,'\|')[1]"/>
         <xsl:variable name="img" select="tokenize($state,'\|')[2]"/>
         <xsl:variable name="text" select="tokenize($state,'\|')[3]"/>
         <td bgcolor="{$bgcolor}" align="center">
           <a href="{$href}" title="{$errMsg}" id="statuscell" >
             <xsl:if test="$img!='' and (not(contains($state, 'pass')) or not($foundVersion))">
               <img src="{concat('/inca/img/', $img)}"/>
               <xsl:if test="$href != $normRef">
                 <a style="text-decoration:none; text-size: tiny" href="{$normRef}">*</a>
               </xsl:if>
               <br/>
             </xsl:if>
             <xsl:value-of select="$text"/>
             <xsl:choose>
               <xsl:when test="$result/body//statistics">
                 <table bgcolor="{$bgcolor}">
                 <xsl:call-template name="printBodyStats">
                   <xsl:with-param name="report" select="$result"/>
                 </xsl:call-template>
                 </table>
               </xsl:when>
               <xsl:when test="count($foundVersion)>1">
                 <xsl:for-each select="$result/body/package/subpackage">
                 <xsl:value-of select="ID"/>: <xsl:value-of select="version"/><br/>
                 </xsl:for-each>
               </xsl:when>
               <xsl:when test="string($foundVersion)!=''">
                 <xsl:value-of select="$foundVersion" />
               </xsl:when>
             </xsl:choose>
           </a>
         </td>
       </xsl:if>
     </xsl:when><xsl:otherwise>
	<!-- resource is exempt -->
       <xsl:variable name="naConfig" select="$defaultconfig/incaResult/secondaryState[@name='na']"/>
       <td bgcolor="{$naConfig/@bgcolor}" align="center"><xsl:value-of select="$naConfig/@text"/></td>
     </xsl:otherwise></xsl:choose>
  </xsl:template>

</xsl:stylesheet>
