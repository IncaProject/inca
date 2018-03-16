<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- create-query.xsl: Prints form to select series and resources to query-->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
		xmlns:quer="http://inca.sdsc.edu/dataModel/queryStore_2.0">

  <xsl:include href="../xsl/inca-common.xsl"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
    <xsl:text disable-output-escaping="yes"><![CDATA[
      <script type="text/javascript" src="/inca/js/create-query.js"></script>
      <script type="text/javascript" src="/inca/js/check-box.js"></script>
    ]]></xsl:text>
      <xsl:choose>
        <xsl:when test="count(error)>0">
          <!-- inca-common.xsl printErrors -->
          <xsl:apply-templates select="error" />
        </xsl:when>
        <xsl:otherwise>
          <form method="post" action="/inca/jsp/query.jsp" name="form"
                onsubmit="return validate(form);">
            <p>
              <!-- inca-common.xsl -->
              <xsl:call-template name="printBodyTitle">
                <xsl:with-param name="title" select="'Create Stored Query'"/>
              </xsl:call-template>
              Create a stored query for Inca report series on
              specified resources. <br/>Enter a name to store this query under,
              check the boxes for each Inca report series to be queried,
              and click 'STORE QUERY'.</p>
            <table class="subheader" cellpadding="6">
              <tr>
                <td>Stored query name: </td>
                <td><input name="qname" type="text"/></td>
                <td>Fetch every (secs): </td>
                <td><input name="period" type="text"/></td>
              </tr>
              <tr>
                <td>
                  <input type="submit" name="submit" value="STORE QUERY"/>
                </td>
                <xsl:variable name="param"
                select="combo/quer:queryStore/query/type/params/param[matches(.,
                    '^\(config.nickname=.* AND series.resource=.*\).*')]"/>
                <xsl:if test="count($param) > 0">
                  <xsl:variable name="queries" select="$param/../../.."/>
                  <xsl:call-template name="printJavascript">
                    <xsl:with-param name="queries" select="$queries"/>
                  </xsl:call-template>
                  <td><select name ="qedit"
                              onChange="changeFormValues(qedit.value);">
                    <option value="select"> - Select Query to Edit -</option>
                    <xsl:for-each select="$queries">
                      <xsl:sort select="." />
                      <option value="{name}">
                        <xsl:value-of select="name"/>
                      </option>
                    </xsl:for-each>
                  </select>
                  </td>
                </xsl:if>
              </tr>
            </table><br/>
            <!-- inca-common.xsl printSuiteBoxes -->
            <xsl:apply-templates select="combo/suites/suite" mode="box"/>
            <input type="hidden" name="action" value="Add"/>
            <input type="hidden" name="qtype" value="latest"/>
            <input type="hidden" name="qparams" value=""/>
          </form>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printJavascript                                                      -->
  <!--                                                                      -->
  <!-- Output some Javascript functions and global variables.               -->
  <!-- ==================================================================== -->
  <xsl:template name="printJavascript">
    <xsl:param name="queries"/>
    <script language="javascript" type="text/javascript">
      /*
      * Store query information
      */
      var queries = new Array();
      <xsl:for-each select="$queries">
        var queryName = "<xsl:value-of select="name"/>";
        queries[queryName] = {};
        queries[queryName].qtype =
        "<xsl:value-of select="replace(type/command, 'query', '')"/>";
        queries[queryName].qparams=
        "<xsl:value-of select="string-join(type/params/param, ', ')"/>";
        var period = 0;
        <xsl:if test="cache">
          period = <xsl:value-of select="cache/reloadPeriod"/>;
        </xsl:if>
        queries[queryName].period = period;
      </xsl:for-each>

      /*
      * Change the values of the query name, type, params, and period based on
      * the selected query name.
      *
      * Arguments:
      *
      *   queryName - A string containing the name of a query.
      */
      function changeFormValues( queryName ) {
        document.form.qname.value = queryName;
        document.form.qtype.value=queries[queryName].qtype;
        document.form.period.value=queries[queryName].period;
        document.form.action.value='Change';
        var sql = queries[queryName].qparams.split(/ OR /g);
        var matchSeries = new Array();
        <xsl:text disable-output-escaping="yes"><![CDATA[
        for(i=0; i<sql.length; i++){
        ]]></xsl:text>
          var match = /\(config.nickname='(.[^']*)' AND series.resource='(.[^']*)'\)/i.exec(sql[i]);
          var series = match[1];
          series = series.replace(/\(/, "\\(");
          series = series.replace(/\)/, "\\)");
          var resource = match[2];
          matchSeries.push('COL-' + resource + '-ROW-' + series);
        }
        uncheckAll(form.series);
        var match = matchSeries.join('|');
        re = new RegExp('^-TABLEID-.*-(' + match + ')$');
        length = form.elements.length;
        <xsl:text disable-output-escaping="yes"><![CDATA[
        for(i = 0; i < length; i++){
    	    elm = form.elements[i];
    	    if (elm.type == 'checkbox' && re.test(elm.id)){
      	    elm.checked = true;
    	    }
      	}
        ]]></xsl:text>
      }
    </script>
  </xsl:template>

</xsl:stylesheet>
