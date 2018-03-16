<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- query.xsl:  Creates form to create and store hql queries -->
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
          <h1>Query Management</h1><br/>
          <xsl:call-template name="printJavascript" />
          <xsl:apply-templates select="queryInfo/quer:queryStore" />
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printJavascript                                                      -->
  <!--                                                                      -->
  <!-- Output some Javascript functions and global variables.               -->
  <!-- ==================================================================== -->
  <xsl:template name="printJavascript">
    <script language="javascript" type="text/javascript">

      var DAYS_OF_WEEK = [ "sun", "mon", "tues", "wed", "thurs", "fri", "sat" ];

      /*
      * Store query information
      */
      var queries = new Array();
      <xsl:for-each select="/queryInfo/quer:queryStore/query">
        var queryName = "<xsl:value-of select="name"/>";
        queries[queryName] = {};
        queries[queryName].qtype = 
          "<xsl:value-of select="replace(type/command, 'query', '')"/>";
        queries[queryName].qparams= "<xsl:value-of select="string-join(type/params/param, ', ')"/>";
        var period = 0;
        <xsl:if test="cache">
          period = <xsl:value-of select="cache/reloadPeriod"/>;
        </xsl:if>
        queries[queryName].period = period;
        queries[queryName].wday = "*";
        queries[queryName].hour = "*";
        queries[queryName].min = "*";
        <xsl:if test="cache/reloadAt">
          var reloadAt = "<xsl:value-of select="cache/reloadAt"/>";
          if ( reloadAt != "*" ) {
            var reloadAtParts = reloadAt.split(":");
            queries[queryName].wday = reloadAtParts[0];
            queries[queryName].hour = reloadAtParts[1];
            queries[queryName].min = reloadAtParts[2];
          }
        </xsl:if>
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
        document.queryForm.qname.value = queryName; 
        document.queryForm.qtype.value=queries[queryName].qtype;
        document.queryForm.qparams.value=queries[queryName].qparams;
        document.queryForm.period.value=queries[queryName].period;
        document.queryForm.min.value=queries[queryName].min;
        document.queryForm.hour.value=queries[queryName].hour;
        if ( queries[queryName].wday == '*' ) {
          document.queryForm.wday.value=queries[queryName].wday;
        } else {
          document.queryForm.wday.value=DAYS_OF_WEEK[queries[queryName].wday];
        }
      }

      /*
      * Store template information
      */
      var templates = new Array();
      <xsl:for-each select="/queryInfo/quer:queryStore/template">
        var templateName = "<xsl:value-of select="name"/>";
        templates[queryName] = {};
        templates[queryName].qtype = "<xsl:value-of select="type/command"/>";
        templates[queryName].qparams = 
          "<xsl:value-of select="replace(type/params, '\n|\r', '\\n\\&#xa;')"/>";
        templates[templateName].param = new Array(1);
        <xsl:for-each select="param">
          var position = "<xsl:value-of select="position()"/>";
          templates[templateName].param[position]= "<xsl:value-of select="."/>";
          </xsl:for-each>
      </xsl:for-each>

      /*
      * Change HQL and template params
      * depending on which template selected
      */
      function changeTemplateValues( template, tbl ) {
        var tbl = document.getElementById(tbl);
        var rows = tbl.getElementsByTagName('tr').length;
        <xsl:text disable-output-escaping="yes"><![CDATA[
        /*document.queryForm.hql.value=templates[template].hql;
        for (i=1; i<=rows; i++){
          tbl.deleteRow(i);
        }*/
        for (i=0; i<=templates[template].param.length; i++){
          var newRow = tbl.insertRow(i);
          newRow.insertCell(0).innerHTML  = templates[template].param[i];
          newRow.insertCell(1).innerHTML = '<input type="text" name="param"/>' +
          templates[template].param.length;
        }
        ]]></xsl:text>
      }

      /*
      * Clear the value of a given form element.
      *
      * Arguments:
      *
      *   formElement - An input element in a form.
      */
      function clearValue( formElement ) {
        formElement.value = "";
      }

      /*
      * Clear if element is "Enter Name"
      */
      function clearEnter( formElement ) {
      if (formElement.value == "Enter Name")
        formElement.value = "";
      }

      /*
      * Confirm that user wants to change value
      */
      function confirmChange( value ) {
        if (qSelected(value)){
          var agree=confirm("Do you want to change " + value + "?");
        if (agree){
	  cleanParams();
          return true;
        }else{
          return false;
        }
        }
      }

      /*
      * Confirm that user wants to delete value
      */
      function confirmDelete( value ) {
        if (qSelected(value)){
          var agree=confirm("Do you want to delete " + value + "?");
        if (agree){
	  cleanParams();
          return true;
        }else{
          return false;
        }
        }
      }

      /*
      * Check whether stored query is selected
      * to perform exe/change/delete
      */
      function qSelected( query ) {
        if (query){
	  cleanParams();
          return true;
        }else{
          alert("Please select a stored HQL query.");
          return false;
      	}
      }

      /*
      * Replace line breaks in query params
      */
      function cleanParams( ) {
        document.queryForm.qparams.value=document.queryForm.qparams.value.replace(/\n|\r/, "");
      }
    </script>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printQueryForm                                                       -->
  <!--                                                                      -->
  <!-- Prints a HTML form that allows an user to add a stored query         -->
  <!-- ==================================================================== -->
  <xsl:template name="printQueryForm" match="quer:queryStore">
    <form method="post" action="query.jsp" name="queryForm">
      <table class="subheader" cellpadding="20" cellspacing="0">
        <tr valign="top">
          <td width="50%">
            <b>Stored Queries:</b><br/>
            <xsl:choose>
              <xsl:when test="count(query)>0">
                <select name ="selectQname" size="10"
                        onClick="changeFormValues(selectQname.value);">
                  <xsl:for-each select="query">
                    <xsl:sort select="." />
                    <option value="{name}">
                      <xsl:value-of select="name"/>
                    </option>
                  </xsl:for-each>
                </select>
                <br/>
              </xsl:when>
              <xsl:otherwise>
                <p><em>(no stored queries)</em></p>
              </xsl:otherwise>
            </xsl:choose>
      <br/>
      <input type="submit" value="De-select stored queries (refresh page)" />
          </td>
          <td>
            <p>query name:<br/>
              <input type="text" name="qname" value="Enter Name"
                     onClick="clearEnter(qname)" size="60"/>
            </p>
            <p>fetch every (seconds):<br/>
              <input type="text" name="period" size="60" value="0"/>
            </p>
            <p>first fetch at (WW:HH:MM):<br/>
              <select name="wday">
                <option selected="true">*</option>
                <script language="javascript" type="text/javascript">
                <xsl:text disable-output-escaping="yes"><![CDATA[
                  for (i=0; i<7; i++) {
                    document.write( "<option>" + DAYS_OF_WEEK[i] + "</option>" );
                  }
                ]]></xsl:text>
                </script>
              </select> : 
              <select name="hour">
                <option selected="true">*</option>
                <script language="javascript" type="text/javascript">
                <xsl:text disable-output-escaping="yes"><![CDATA[
                  for (i=0; i<=23; i++) {
                    document.write( "<option>" + i + "</option>" );
                  }
                ]]></xsl:text>
                </script>
              </select> :
              <select name="min">
                <option selected="true">*</option>
                <script language="javascript" type="text/javascript">
                <xsl:text disable-output-escaping="yes"><![CDATA[
                  for (i=0; i<60; i++) {
                    document.write( "<option>" + i + "</option>" );
                  }
                ]]></xsl:text>
                </script>
              </select>
            </p>
            <p>command:<br/>
              <select name="qtype">
                <option>database</option>
                <option>guids</option>
                <option default="true">hql</option>
                <option>instance</option>
                <option>latest</option>
                <option>period</option>
                <option>statusHistory</option>
              </select>
             </p>
             <p>params:<br/> <textarea name="qparams" rows="10" cols="80" wrap="soft"
                        onClick="clearEnter(qparams)">Enter Query Params
              </textarea>
              </p>
            <input name ="action" type="submit" value="View"
                   onClick="return qSelected(selectQname.value)"/>
            <input name ="action" type="submit" value="Execute"
                   onClick="cleanParams()"/>
            <input name ="action" type="submit" value="Refresh"
                   onClick="cleanParams()"/>
            <input name="action" type="submit" value="Add"
                   onClick="cleanParams()"/>
            <input name="action" type="submit" value="Change"
                   onClick="return confirmChange(selectQname.value)"/>
            <input name ="action" type="submit" value="Delete"
                   onClick="return confirmDelete(selectQname.value)"/>
          </td>
        </tr>
      </table>
    </form>
  </xsl:template>

</xsl:stylesheet>
