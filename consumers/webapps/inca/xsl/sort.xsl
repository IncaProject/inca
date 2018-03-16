<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- sort.xsl: Prints table sortable by clicking on column headers        -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:text disable-output-escaping="yes"><![CDATA[
      <script type="text/javascript" src="/inca/js/sorttable.js"></script>
    ]]></xsl:text>
      <xsl:choose>
        <xsl:when test="count(error)>0">
          <!-- inca-common.xsl printErrors -->
          <xsl:apply-templates select="error" />
        </xsl:when>
        <xsl:otherwise>
          <p>
            <!-- inca-common.xsl -->
            <xsl:call-template name="printBodyTitle">
              <xsl:with-param name="title" select="'Sortable Table'"/>
            </xsl:call-template>
            Click on the column headers in the table below to sort it. <br/>
            Sorting is applied to the table in the order the column headers
            are clicked on <br/>
            (e.g. to sort by col 1 and then col 2, click on column
            header 1 then column header 2).</p>
          <table class="sortable">
            <thead>
              <tr>
                <th>Name</th>
                <th>Major</th>
                <th>Sex</th>
                <th>English</th>
                <th>Japanese</th>
                <th>Calculus</th>
                <th>Geometry</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Student01</td>
                <td>Languages</td>
                <td>male</td>
                <td>80</td>
                <td>70</td>
                <td>75</td>
                <td>80</td>
              </tr>
              <tr>
                <td>Student02</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>90</td>
                <td>88</td>
                <td>100</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student03</td>
                <td>Languages</td>
                <td>female</td>
                <td>85</td>
                <td>95</td>
                <td>80</td>
                <td>85</td>
              </tr>
              <tr>
                <td>Student04</td>
                <td>Languages</td>
                <td>male</td>
                <td>60</td>
                <td>55</td>
                <td>100</td>
                <td>100</td>
              </tr>
              <tr>
                <td>Student05</td>
                <td>Languages</td>
                <td>female</td>
                <td>68</td>
                <td>80</td>
                <td>95</td>
                <td>80</td>
              </tr>
              <tr>
                <td>Student06</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>100</td>
                <td>99</td>
                <td>100</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student07</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>85</td>
                <td>68</td>
                <td>90</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student08</td>
                <td>Languages</td>
                <td>male</td>
                <td>100</td>
                <td>90</td>
                <td>90</td>
                <td>85</td>
              </tr>
              <tr>
                <td>Student09</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>80</td>
                <td>50</td>
                <td>65</td>
                <td>75</td>
              </tr>
              <tr>
                <td>Student10</td>
                <td>Languages</td>
                <td>male</td>
                <td>85</td>
                <td>100</td>
                <td>100</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student11</td>
                <td>Languages</td>
                <td>male</td>
                <td>86</td>
                <td>85</td>
                <td>100</td>
                <td>100</td>
              </tr>
              <tr>
                <td>Student12</td>
                <td>Mathematics</td>
                <td>female</td>
                <td>100</td>
                <td>75</td>
                <td>70</td>
                <td>85</td>
              </tr>
              <tr>
                <td>Student13</td>
                <td>Languages</td>
                <td>female</td>
                <td>100</td>
                <td>80</td>
                <td>100</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student14</td>
                <td>Languages</td>
                <td>female</td>
                <td>50</td>
                <td>45</td>
                <td>55</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student15</td>
                <td>Languages</td>
                <td>male</td>
                <td>95</td>
                <td>35</td>
                <td>100</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student16</td>
                <td>Languages</td>
                <td>female</td>
                <td>100</td>
                <td>50</td>
                <td>30</td>
                <td>70</td>
              </tr>
              <tr>
                <td>Student17</td>
                <td>Languages</td>
                <td>female</td>
                <td>80</td>
                <td>100</td>
                <td>55</td>
                <td>65</td>
              </tr>
              <tr>
                <td>Student18</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>30</td>
                <td>49</td>
                <td>55</td>
                <td>75</td>
              </tr>
              <tr>
                <td>Student19</td>
                <td>Languages</td>
                <td>male</td>
                <td>68</td>
                <td>90</td>
                <td>88</td>
                <td>70</td>
              </tr>
              <tr>
                <td>Student20</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>40</td>
                <td>45</td>
                <td>40</td>
                <td>80</td>
              </tr>
              <tr>
                <td>Student21</td>
                <td>Languages</td>
                <td>male</td>
                <td>50</td>
                <td>45</td>
                <td>100</td>
                <td>100</td>
              </tr>
              <tr>
                <td>Student22</td>
                <td>Mathematics</td>
                <td>male</td>
                <td>100</td>
                <td>99</td>
                <td>100</td>
                <td>90</td>
              </tr>
              <tr>
                <td>Student23</td>
                <td>Languages</td>
                <td>female</td>
                <td>85</td>
                <td>80</td>
                <td>80</td>
                <td>80</td>
              </tr>
            </tbody>
          </table>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
