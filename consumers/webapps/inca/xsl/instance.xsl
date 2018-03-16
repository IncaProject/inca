<?xml version="1.0" encoding="UTF-8"?>

<!-- ==================================================================== -->
<!-- instance.xsl:  HTML table with report details.                       -->
<!-- ==================================================================== -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:sdf="java.text.SimpleDateFormat"
                xmlns:rd="http://inca.sdsc.edu/dataModel/reportDetails_2.1"
                xmlns:xdt="http://www.w3.org/2004/07/xpath-datatypes">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <xsl:param name="printRunNow"/>
  <xsl:param name="printKb"/>
  <xsl:param name="kbSearch"/>
  <xsl:param name="kbSubmit"/>
  <xsl:param name="week"/>
  <xsl:param name="month"/>
  <xsl:param name="quarter"/>
  <xsl:param name="year"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
      <xsl:apply-templates select="//rd:reportDetails/report" />
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printReport - print table with report details info                   -->
  <!-- ==================================================================== -->
  <xsl:template name="printReport" match="report">
    <xsl:variable name="config" select="../seriesConfig"/>
    <xsl:variable name="configId" select="../seriesConfigId"/>
    <xsl:variable
        name="cgi"
        select="concat(substring-before($config/series/uri, name),
        '../cgi-bin/reporters.cgi?reporter=', name, '&amp;action=help')"/>
    <xsl:variable name="comp" select="../comparisonResult"/>
    <xsl:variable name="used" select="../sysusage"/>
    <xsl:variable name="gmt" select="gmt" as="xs:dateTime" />
    <xsl:variable name="complete" select="exitStatus/completed"/>
    <xsl:variable name="errMsg" select="exitStatus/errorMessage" />
    <xsl:variable name="package" select="body/package"/>
    <xsl:variable name="resource" select="$config/resourceHostname"/>
    <xsl:variable name="target" select="$config/targetHostname"/>

    <xsl:variable name="defaultconfig" select="document('../xml/default.xml')/default"/>

    <xsl:variable name="resultText">
      <xsl:choose>
        <xsl:when test="count($comp)>0">
          <xsl:value-of select="$comp"/>
        </xsl:when>
        <xsl:when test="$complete='true'">
          <xsl:value-of select="'completed'"/>
        </xsl:when>
        <xsl:when test="$complete='false'">
          <xsl:value-of select="'did not complete'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'unknown'"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="nickName">
      <xsl:choose>
        <xsl:when test="$config/nickname!=''">
          <xsl:value-of select="$config/nickname"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- inca-common.xsl -->
    <xsl:call-template name="printBodyTitle">
      <xsl:with-param name="title" select="concat('Details for ',
      $nickName, ' series')" />
    </xsl:call-template>
    <xsl:variable name="metrics">
      <xsl:for-each select="body//statistics/statistic/ID">
        <xsl:value-of select="concat(., ',')"/>
      </xsl:for-each>
      <xsl:for-each select="body//statistics/@*">
        <xsl:value-of select="concat(name(), ',')"/>
      </xsl:for-each>
      <xsl:if test="body/*/@errors">errors,</xsl:if>
      <xsl:if test="body/*/@warnings">warnings,</xsl:if>
    </xsl:variable>
    <table width="600" cellpadding="4">
      <tr><td colspan="2" class="header"><xsl:text>Result:</xsl:text></td></tr>
      <tr>
        <td><p><xsl:value-of select="$resultText"/></p></td>
        <td>
          <xsl:variable name="targetText"><xsl:if test="not(empty($target)) and $target != ''"> to <xsl:value-of select="$target"/> </xsl:if></xsl:variable>
          <xsl:variable name="label" select="concat($resource, $targetText, ' (',$nickName,')')"/>
          <xsl:variable name="graphUrl"
                        select="concat('/inca/jsp/graph.jsp?series=', $nickName, ',', $resource, ',', $target, ',', $label, '&amp;availMetrics=', $metrics, '&amp;startDate=')"/>
          <table>
            <tr>
              <td>view results for past: </td>
              <td>    <a href="{$graphUrl}{$week}"><img src="/inca/img/week.gif"/></a>  </td>
              <td>    <a href="{$graphUrl}{$month}"><img src="/inca/img/month.gif"/></a> </td>
              <td>    <a href="{$graphUrl}{$quarter}"><img src="/inca/img/quarter.gif"/></a> </td>
              <td>    <a href="{$graphUrl}{$year}"><img src="/inca/img/year.gif"/></a> </td>
            </tr>
          </table>
        </td>
      </tr>
      <tr><td colspan="2">
        <xsl:if test="$resultText=$comp">
          <p class="code"><xsl:value-of select="concat('Expecting: ',
            $config/acceptedOutput/comparison)"/></p>
        </xsl:if>
        <xsl:if test="string($package/version)!=''">
          <p class="code"><xsl:value-of select="concat('Found: ',
            $package/version)"/></p>
        </xsl:if>
        <xsl:if test="string($package/subpackage/version)!=''">
          <p class="code"><xsl:text>Found: </xsl:text>
            <xsl:for-each select="$package/subpackage">
              <xsl:value-of select="concat(ID, ': ', version)"/><br/>
            </xsl:for-each>
          </p>
        </xsl:if>
        <xsl:if test="$resultText='did not complete' or $resultText='unknown'
          or $resultText=$comp">
          <xsl:variable name="downErr">
            <!-- inca-common.xsl -->
            <xsl:call-template name="getDownErr">
              <xsl:with-param name="errMsg" select="$errMsg"/>
              <xsl:with-param name="downtimeUrl" select="$defaultconfig/downtimeUrl"/>
            </xsl:call-template>
          </xsl:variable>
          <p class="code">
          <!-- inca-common.xsl -->
          <xsl:call-template name="replaceBreak">
            <xsl:with-param name="text" select="$downErr"/>
          </xsl:call-template>
          </p>
          <xsl:if test="$errMsg=''">
            <p class="code">
            <!-- inca-common.xsl -->
            <xsl:call-template name="replaceBreak">
              <xsl:with-param name="text" select="../stderr"/>
            </xsl:call-template>
            </p>
          </xsl:if>
        </xsl:if>
        <xsl:if test="$printKb='true'">
          <tr><td>
          <xsl:variable name="kb1" select="replace($kbSearch,'@nickname@', $nickName)"/>
          <xsl:variable name="kb2" select="replace($kb1,'@error@', encode-for-uri(substring($errMsg,0,60)))"/>
          <xsl:variable name="kb3" select="replace($kb2,'@reporter@', name)"/>
          <form method="post" action="{$kb3}">
          <input type="submit" value="search knowledge base"/>
          </form></td><td>
          <xsl:variable name="ab1" select="replace($kbSubmit,'@nickname@', $nickName)"/>
          <xsl:variable name="ab2" select="replace($ab1,'@error@', encode-for-uri(substring($errMsg,0,60)))"/>
          <xsl:variable name="ab3" select="replace($ab2,'@reporter@', name)"/>
          <form method="post" action="{$ab3}">
            <input type="submit" value="add to knowledge base"/>
          </form></td></tr>
        </xsl:if>
      </td>
      </tr>
      <xsl:if test="count(log/warn/message|log/error/message)>0">
        <tr>
          <td colspan="2"><xsl:text>Errors or warnings:</xsl:text>
            <xsl:apply-templates select="log/warn|log/error"/>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="$metrics != ''">
      <tr><td colspan="2" class="header">
        Metrics collected:
      </td></tr>
          <xsl:call-template name="printBodyStats">
            <xsl:with-param name="report" select="."/>
          </xsl:call-template>
      </xsl:if>
      <tr><td colspan="2" class="header">
        <xsl:text>Reporter details:</xsl:text>
      </td></tr>
      <tr>
        <td><xsl:text>reporter name</xsl:text></td>
        <td><a href="{$cgi}"><xsl:value-of select="name"/></a><br/>
          <xsl:text> (click name for more info)</xsl:text></td>
      </tr>
      <tr>
        <td><xsl:text>reporter version</xsl:text></td>
        <td><xsl:value-of select="$config/series/version"/></td>
      </tr>
      <tr>
        <td colspan="2" class="header">
          <xsl:text>Execution information:</xsl:text>
        </td>
      </tr>
      <tr>
        <td><xsl:text>ran at</xsl:text></td>
        <td>
          <xsl:call-template name="formatDate">
            <xsl:with-param name="date" select="$gmt"/>
          </xsl:call-template>
        </td>
      </tr>
      <tr>
        <td><xsl:text>age</xsl:text></td>
        <td>
          <xsl:call-template name="formatAge">
            <xsl:with-param name="age" select="$gmt"/>
          </xsl:call-template>
        </td>
      </tr>
      <tr>
        <td><xsl:text>cron</xsl:text></td>
        <td>
          <xsl:for-each select="$config/schedule/cron/*[not(self::suspended)
          and not(self::numOccurs)]">
            <xsl:value-of select="."/><xsl:text> </xsl:text>
          </xsl:for-each>
        </td>
      </tr>
      <tr>
        <td><xsl:text>ran on (hostname)</xsl:text></td>
        <td><xsl:value-of select="hostname"/></td>
      </tr>
      <tr>
        <td><xsl:text>memory usage (MB)</xsl:text></td>
        <td><xsl:value-of select="$used/memory"/></td>
      </tr>
      <tr>
        <td><xsl:text>cpu time (secs)</xsl:text></td>
        <td><xsl:value-of select="$used/cpuTime"/></td>
      </tr>
      <tr>
        <td><xsl:text>wall clock time (secs)</xsl:text></td>
        <td><xsl:value-of select="$used/wallClockTime"/></td>
      </tr>
      <tr><td colspan="2" class="header">
        <xsl:text>Input parameters:</xsl:text>
      </td></tr>
      <xsl:for-each select="$config/series/args/arg">
        <xsl:sort/>
        <tr><td><xsl:value-of select="name"/></td>
          <td><xsl:value-of select="value"/></td></tr>
      </xsl:for-each>
      <tr><td colspan="2" class="header">
        <xsl:text>Command used to execute the reporter:</xsl:text>
      </td></tr>
      <tr><td colspan="2"><p class="code">
        <xsl:value-of select="concat('% ',
        replace($config/series/context, $config/series/name, reporterPath))"/>
      </p></td></tr>
      <xsl:if test="$printRunNow='true'">
      <tr><td colspan="2" align="left">
        <form method="POST" action="/inca/jsp/runNow.jsp?configId={$configId}">
          <input type="submit" value="Run Now" name="Run Now"/> (requires authentication)
        </form>
      </td></tr>
      </xsl:if>
      <xsl:if test="count(log/system/message)>0">
        <tr><td colspan="2" class="header">
          <xsl:text>System commands executed by the reporter:</xsl:text>
        </td></tr>
        <tr><td colspan="2">
          <xsl:text>Note that the reporter may execute other actions in between
            system commands (e.g., change directories).</xsl:text>
          <xsl:apply-templates select="log/system"/>
        </td></tr>
      </xsl:if>
      <xsl:if test="count(log/info/message|log/debug/message)>0">
        <tr>
          <td colspan="2"><xsl:text>Debug or informational output:</xsl:text>
            <xsl:apply-templates select="log/info|log/debug"/>
          </td>
        </tr>
      </xsl:if>
      <!-- instance-extra.xsl for run-now and comment rows -->
      <!--<xsl:call-template name="instanceExtra">
        <xsl:with-param name="nickName" select="$nickName"/>
        <xsl:with-param name="config" select="$config"/>
      </xsl:call-template>-->
    </table>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printComment - print paragraph with comment                          -->
  <!-- ==================================================================== -->
  <xsl:template name="printComment" match="row">
    <p class="code">
      <xsl:value-of select="comment"/><br/>
      (<xsl:value-of select="author"/>, <xsl:value-of select="date"/>)
    </p>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printLog - print paragraph with log message                          -->
  <!-- ==================================================================== -->
  <xsl:template name="printLog" match="info|system">
    <p class="code">
      <xsl:if test="self::system">
        <xsl:text>% </xsl:text>
      </xsl:if>
      <xsl:value-of select="message"/>
    </p>
  </xsl:template>
  <xsl:template name="printDebug" match="debug|warn|error">
    <pre><p class="code"><xsl:value-of select="message"/></p></pre>
  </xsl:template>

</xsl:stylesheet>
