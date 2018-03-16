<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- graph.xsl:  Prints form to select series to graph                    -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <xsl:param name="startDate" />

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:text disable-output-escaping="yes"><![CDATA[
      <script type="text/javascript" src="/inca/js/graph.js"></script>
      <script type="text/javascript" src="/inca/js/check-box.js"></script>
    ]]></xsl:text>
          <form method="get" action="/inca/jsp/graph.jsp" name="form"
                onsubmit="return validate(form);">
            <xsl:call-template name="printGraphForm"/>
            <!-- inca-common.xsl printSuiteBoxes -->
            <xsl:apply-templates select="combo/suites/suite" mode="box"/>
          </form>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printGraphForm                                                       -->
  <!--                                                                      -->
  <!-- Prints form fields for graph.jsp                                     -->
  <!-- ==================================================================== -->
  <xsl:template name="printGraphForm">
    <p>
      <!-- inca-common.xsl -->
      <xsl:call-template name="printBodyTitle">
        <xsl:with-param name="title" select="'Graph Result History'"/>
      </xsl:call-template>
      Graph the result history of any Inca report series on a single graph.
      <br/>Use the input parameters below to customize the appearance of the graph,
      <br/>check the boxes for each Inca report series to be graphed,
      and click 'GRAPH'.</p>
      <p>Adjust the graph size or remove the legend if selecting many series
      <br/>as the legend is included as part of the graph. For example, if
      <br/>graphing 20 series increase graph height to 600.</p>
    <table class="subheader">
      <tr>
        <td>show mouseovers/hyperlinks<br/> for datapoints:</td>
        <td><input name="map" type="radio" value="true"/> yes
          <input name="map" type="radio" value="false" checked=""/> no</td>
      </tr>
      <tr>
        <td>show legend:</td>
        <td><input name="legend" type="radio" value="true" checked=""/> yes
          <input name="legend" type="radio" value="false"/> no</td>
      </tr>
      <tr>
        <td>legend position:</td>
        <td>
          <input name="legendAnchor" type="radio"
                 value="south" checked=""/> south
          <input name="legendAnchor" type="radio" value="north"/> north
          <input name="legendAnchor" type="radio" value="east"/> east
          <input name="legendAnchor" type="radio" value="west"/> west
        </td>
      </tr>
      <tr>
        <td>width:</td>
        <td><input name="width" type="text" size="5" value="500"/></td>
      </tr>
      <tr>
        <td>height:</td>
        <td><input name="height" type="text" size="5" value="300"/></td>
      </tr>
      <tr>
        <td>background color:</td>
        <td><input name="bgcolor" type="text"/> (e.g. #ACB4BB)</td>
      </tr>
      <tr>
        <td>start date:</td>
        <td><script>
          var date = "<xsl:value-of select="$startDate"/>";
          <xsl:text disable-output-escaping="yes"><![CDATA[
            document.write('<input name="startDate" type="text" value="'+ date+'"/>'); 
          ]]></xsl:text>
        </script>(MMddyy format, e.g. "093007")</td>
      </tr>
      <tr>
        <td>end date:</td>
        <td><input name="endDate"
                   type="text"/> (MMddyy format, e.g. "103007")</td>
      </tr>
    </table>
    <br/><br/>
    <input type="submit" name="submit" value="GRAPH"/> <br/><br/>
  </xsl:template>

</xsl:stylesheet>
