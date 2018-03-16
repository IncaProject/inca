<?xml version="1.0" encoding="utf-8"?>

<!-- ==================================================================== -->
<!-- google.xsl:  Prints javascript to output a google map with markers   -->
<!--              representing resources and lines representing           -->
<!--              cross-site test status.                                 -->
<!-- ==================================================================== -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml"
		xmlns:quer="http://inca.sdsc.edu/dataModel/queryResults_2.0"
                xmlns:rs="http://inca.sdsc.edu/queryResult/reportSummary_2.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">

  <xsl:include href="../xsl/inca-common.xsl"/>
  <xsl:param name="ignoreErrs"/>
  <xsl:param name="hostnamePort"/>

  <!-- ==================================================================== -->
  <!-- Main template                                                        -->
  <!-- ==================================================================== -->
  <xsl:template match="/">
      <script>
      <xsl:text disable-output-escaping="yes"><![CDATA[
        function loadScript() {
          var script = document.createElement("script");
          script.type = "text/javascript";
          script.src = "http://maps.googleapis.com/maps/api/js?sensor=false&callback=load";
          document.body.appendChild(script);
        }
        window.onload = loadScript;
      ]]></xsl:text>
      </script>
      <script type="text/javascript">
        <xsl:call-template name="printGoogleJavascript"/>
      </script>
      <xsl:choose>
        <xsl:when test="count(error)>0">
          <!-- inca-common.xsl printErrors -->
          <xsl:apply-templates select="error" />
        </xsl:when>
        <xsl:otherwise>
          
          <div id="map"/>
          <br clear="all"/>
          <script>
          var testNames = new Array();
          <xsl:for-each select="/combo/google/crossSite/test">
            testNames.push( "<xsl:value-of select="name"/>" );
          </xsl:for-each>
          <xsl:text disable-output-escaping="yes"><![CDATA[
          for( var i = 0; i < testNames.length; i++ ) {
            var url = '<input type="button" value="Toggle ' + testNames[i] + 
                      ' status" onClick="toggleAll2All(\'' + testNames[i] +
                      '\')"/>';
            document.writeln( url );
          }
          ]]></xsl:text>
          </script>
          <br/>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <!-- ==================================================================== -->
  <!-- printGoogleJavascript                                                -->
  <!--                                                                      -->
  <!-- prints table with resources on left and % pass/fail on right         -->
  <!-- ==================================================================== -->
  <xsl:template name="printGoogleJavascript">
    <xsl:variable name="resources"
           select="/combo/resources/resource|/combo/suites/suite/resources/resource"/>
    <xsl:variable name="google" select="/combo/google"/>

    /* -------------------- GLOBAL VARIABLES -------------------------------- */
    var DEFAULT_MARKER_DIST = [ 0, 5,     2.25,  0.75, 0.75,  0.35, 
                                   0.2,   0.075, 0.05, 0.025, 0.01, 
                                   0.005, 0.0025 ];
    var DEGREES_PER_RADIAN = 180;
    var CS_TESTS = {}; // cross-site tests
    var LOGOS = new Array();
    var MAP;
    var RESOURCES = {}; 

    /* -------------------- GOOGLE  XML VARIABLES --------------------------- */
    var DEBUG = <xsl:value-of select='$google/debug'/>;
    var MAP_WIDTH = <xsl:value-of select='$google/width'/>;
    var MAP_HEIGHT  = <xsl:value-of select='$google/height'/>;
    var LINE_COLORS = {  
      pass: "<xsl:value-of select='$google/line/pass'/>", 
      fail: "<xsl:value-of select='$google/line/fail'/>" 
    };
    var MAGNIFICATION_LEVEL = 
      <xsl:value-of select='$google/magnificationLevel'/>;
    <xsl:text disable-output-escaping="yes"><![CDATA[
    if ( MAGNIFICATION_LEVEL > (DEFAULT_MARKER_DIST.length - 1) ) {
      MAGNIFICATION_LEVEL = DEFAULT_MARKER_DIST.length - 1;
    }
    ]]></xsl:text>
    var MAP_CENTER_COORDS = { 
      latitude: <xsl:value-of select='$google/center/latitude'/>, 
      longitude: <xsl:value-of select='$google/center/longitude'/> };
    var MARKER_DIST = DEFAULT_MARKER_DIST[MAGNIFICATION_LEVEL];
    <xsl:if test='$google/markerDist'>
      MARKER_DIST = <xsl:value-of select='$google/markerDist'/>;
    </xsl:if>
    var MARKER_ICON_PREFIX = 
      "<xsl:value-of select='$google/marker/iconUrlPrefix'/>";
    var MARKER_ICON_SUFFIX = 
      "<xsl:value-of select='$google/marker/iconUrlSuffix'/>";
    var MARKER_COLOR = { 
      fail: "<xsl:value-of select='$google/marker/iconStatus/fail'/>",
      pass: "<xsl:value-of select='$google/marker/iconStatus/pass'/>",
      warn: "<xsl:value-of select='$google/marker/iconStatus/warn'/>" 
    };
    var MARKER_ICON = {
      width: <xsl:value-of select='$google/marker/iconWidth'/>, 
      height: <xsl:value-of select='$google/marker/iconHeight'/> };
    var MARKER_ICON_SHADOW = {
      url: "<xsl:value-of select='$google/marker/shadowIconUrl'/>",
      width: <xsl:value-of select='$google/marker/shadowIconWidth'/>, 
      height: <xsl:value-of select='$google/marker/shadowIconHeight'/> 
    };
    var MAX_ERRORS = <xsl:value-of select='$google/maxErrors'/>;

    <!-- need to use javascript
         because you can't include tags inside another tag; width
         and height are attributes to the div tag -->
    <xsl:text disable-output-escaping="yes"><![CDATA[
      document.write( '<div id="map" style="width: ' +
        MAP_WIDTH + 'px; height: ' + MAP_HEIGHT + 'px">' +
      '<br/></div>');
    ]]></xsl:text>

    // setup default lat/long
    var defaultSite = {
      latitude: MAP_CENTER_COORDS.latitude,
      longitude: MAP_CENTER_COORDS.longitude,
      resourceIdx: 0
    };
    var SITES = {
      DEFAULT: defaultSite
    };

    /*
    * Pops up window to print debug messages.  From
    *
    * http://ajaxcookbook.org/javascript-debug-log/
    */
    function log(message) {
      if ( ! DEBUG ) { return };
      <xsl:text disable-output-escaping="yes"><![CDATA[
      if (!log.window_ || log.window_.closed) {
        var win = window.open("", null, "width=400,height=200," +
                              "scrollbars=yes,resizable=yes,status=no," +
                              "location=no,menubar=no,toolbar=no");
        if (!win) return;
        var doc = win.document;
        doc.write
          ( '<html><head><title>Debug Log</title></head><body></body></html> ');
        doc.close();
        log.window_ = win;
      }
      var logLine = log.window_.document.createElement("div");
      logLine.appendChild(log.window_.document.createTextNode(message));
      log.window_.document.body.appendChild(logLine);
      ]]></xsl:text>
    }

    /*
    * createMarker
    *
    * Creates a marker for a resource with an info window attached to it.
    * The info window will display the resource names, the availability,
    * and any failures.
    *
    * Arguments:
    *
    *   point: an GLatLng object describing where to place the marker
    *   resource:  a string containing the name of the resource which will
    *              appear as the title in the info window of the marker
    *   numUnknown:  the number of unknown tests for this resource
    *   numPassed:  the number of tests this resource passed
    *   errors:  an array of error objects (associative array) each containing 
    *            the nickname, hostname, and collected gmt time attributes.
    */
    function createMarker(point, resource, numUnknown, numPassed, errors ) {

      var MARKER_ICON_ANCHOR = new google.maps.Point
        ( <xsl:value-of select='$google/marker/iconAnchorCoord'/> );

      <xsl:text disable-output-escaping="yes"><![CDATA[
      var color;
      var perc;
      var numErrors = errors.length;
      var numTotal = numPassed + numErrors;
      perc = numPassed / numTotal;
      switch(perc)
      {
        case 0: color = MARKER_COLOR["fail"]; break    
        case 1: color = MARKER_COLOR["pass"]; break
        default: color = MARKER_COLOR["warn"]; break
      }

      var markerIcon = new google.maps.MarkerImage
        ( MARKER_ICON_PREFIX + color + MARKER_ICON_SUFFIX,
          new google.maps.Size( MARKER_ICON.width, MARKER_ICON.height ),
          new google.maps.Point(0,0),
          MARKER_ICON_ANCHOR );
      var marker = new google.maps.Marker({
          position: point,
          map: MAP,
          icon: markerIcon
        });
      var shadow = new google.maps.MarkerImage(
        MARKER_ICON_SHADOW.url,
        new google.maps.Size
          (MARKER_ICON_SHADOW.width, MARKER_ICON_SHADOW.height),
        new google.maps.Point(0,0) 
      );
      marker.setShadow( shadow );

      var errorMsg = "";
      if ( numErrors > 0 ) {
        errorMsg += "<p>Failed tests: " + numErrors;
        if ( numErrors > MAX_ERRORS ) {
          errorMsg += " (" + MAX_ERRORS + " displayed)";
        }
        errorMsg += "<br/>";
        for( var i = 0; i < numErrors && i < MAX_ERRORS; i++ ) {
          errorMsg += "&nbsp;&nbsp;" +
                      "<a href=../jsp/instance.jsp?" + 
                              "nickname=" + errors[i].nickname + 
                              "&collected=" + errors[i].gmt + 
                              "&resource=" + errors[i].hostname + ">" + 
                      errors[i].nickname + "</a><br/>";
        }
      }
      errorMsg += "</p>";
      var text = "<b style=\"font-family: sans-serif; font-size: small\">Resource:  " + resource + "</b>" + 
                 "<p style=\"font-family: sans-serif; font-size: small\">Availability: " + (Math.round(perc*1000)/10) + "%  (passed " + 
                 numPassed + "/" + numTotal + " known tests";
      if (numUnknown > 0) {
        text += " - " + numUnknown + " unknown";
      }
      text += ")</p>" + errorMsg;
      var infowindow = new google.maps.InfoWindow({
        content: text
      });
      google.maps.event.addListener(marker, 'click', function() {
         infowindow.open(MAP,marker);
       });

      return marker;
      ]]></xsl:text>
    }
   
    /*
    * toggleAll2All
    *
    * Remove or add lines from a specific all2all test
    *
    * Arguments:
    *
    *   testName:  The name of the all2all test
    */
    function toggleAll2All( testName ) {
      log( "toggling " + testName );
      <xsl:text disable-output-escaping="yes"><![CDATA[
        var test = CS_TESTS[testName];
        if ( test['displayed'] == 0 ) {
          test['displayed'] = 1;
          log( "Adding " + test['lines'].length + " lines" );
          for( var i = 0; i < test['lines'].length; i++ ) {
            test['lines'][i].setMap(MAP);
          }
        } else {
          test['displayed'] = 0;
          log( "Removing " + test['lines'].length + " lines" );
          for( var i = 0; i < test['lines'].length; i++ ) {
            test['lines'][i].setMap(null);
          }
        }
      ]]></xsl:text>
    }

    /*
    * load
    *
    * Called when page is loaded to generate google map
    */
    function load() {
      var latlng = new google.maps.LatLng(MAP_CENTER_COORDS.latitude, MAP_CENTER_COORDS.longitude);
      var myOptions = {
        zoom: MAGNIFICATION_LEVEL,
        mapTypeControl: true,
        streetViewControl: false,
        scaleControl: true,
        overviewMapControl: true,
        center: latlng,
        mapTypeId: <xsl:value-of select='$google/mapType'/>,
        size: new google.maps.Size( MAP_WIDTH, MAP_HEIGHT )
      };
      MAP = new google.maps.Map(document.getElementById('map'), myOptions);

      // get site info
      <xsl:apply-templates select="$google/sites/site" />

      // add logos onto map
      <xsl:for-each select="$google/sites/site/logo">
        var logoAngle = <xsl:value-of select="angle"/>;
        logoAngle = (logoAngle / DEGREES_PER_RADIAN) * Math.PI; // to radians
        var logoLongDiff = Math.cos(logoAngle) * 2 * MARKER_DIST; 
        var logoLatDiff = Math.sin(logoAngle) * 2 * MARKER_DIST; 
        LOGOS.push( 
          { url: "<xsl:value-of select="url"/>",
            width: <xsl:value-of select="width"/>,
            height: <xsl:value-of select="height"/>,
            anchorX: <xsl:value-of select="logoAnchorX"/>,
            anchorY: <xsl:value-of select="logoAnchorY"/>,
            latitude: <xsl:value-of select="../latitude"/> + logoLatDiff,
            longitude: <xsl:value-of select="../longitude"/> + logoLongDiff 
          } );
      </xsl:for-each>

      // add markers for each resource
      <xsl:variable name="reportSummaries" 
              select="/combo/suites/suite/quer:object//rs:reportSummary"/>
      <xsl:variable name="all2alls" select="$reportSummaries[targetHostame!='']"/>
      var curResourceName; 
      <!-- We want to display each resource in the resourceConfig file. 
           Each resource has equivalent hosts (where a RM can be run) in the
           __regexp__ macro so we want to treat all hosts as equivalent. -->
      <xsl:variable name="allResources" 
                    select="distinct-values($resources/name)"/>
      <xsl:for-each select="$allResources">
        curResourceName = "<xsl:value-of select="."/>";
        log("adding resource: " + curResourceName);
        <xsl:variable name="name" select="."/>
        var curResource = {
          regex: "^" + "<xsl:value-of select="distinct-values($resources[name=$name]/macros/macro[name='__regexp__']/value)"/>".replace( /\s+/g, "$|^" ) + "$"
        };
        RESOURCES[curResourceName] = curResource;
      </xsl:for-each>
      <!-- Get resource info so we can then do the magic in javascript -->
      SITES['DEFAULT']['numResources'] = <xsl:value-of select='count($allResources)'/>;
      <xsl:for-each select="$allResources">
        <xsl:sort/>
        <!-- pass in each resources tests -->
        <xsl:variable name="resourceName" select="."/>
        <xsl:call-template name="getResourceInfo">
          <xsl:with-param name="resource" select="$resourceName"/>
          <xsl:with-param name="site" 
            select="$google/sites/site/resources/resource[id=$resourceName]/../../name"/>
          <xsl:with-param name="reportSummaries" select="$reportSummaries[targetHostname=$resourceName or (hostname=$resourceName and string(targetHostname)='')]"/>
          <xsl:with-param 
            name="all2alls" select="$all2alls[hostname=$resourceName]"/>
          <xsl:with-param name="google" select="$google"/>
        </xsl:call-template> 
      </xsl:for-each>
      <xsl:text disable-output-escaping="yes"><![CDATA[

      // add logos
      for ( var i = 0; i < LOGOS.length; i++ ) {
        var image = new google.maps.MarkerImage(LOGOS[i].url,
          new google.maps.Size(LOGOS[i].width, LOGOS[i].height),
          new google.maps.Point(0,0),
          new google.maps.Point(LOGOS[i].anchorX, LOGOS[i].anchorY) );

        var myLatLng = new google.maps.LatLng(LOGOS[i].latitude, LOGOS[i].longitude);
        var marker = new google.maps.Marker({
          position: myLatLng,
          map: MAP,
          icon: image
        });
      }

      // add resource markers
      var resourceNames = Object.keys(RESOURCES);
      for( var i = 0; i < resourceNames.length; i++ ) { 
        var resource = RESOURCES[resourceNames[i]];
        var myLatLng = new google.maps.LatLng(resource['latitude'], resource['longitude']);
        createMarker( 
          myLatLng,
          resourceNames[i],
          resource['numUnknown'],
          resource['numPassed'],
          resource['errors']
        );
      }
      
      // add lines
      for( var i = 0; i < resourceNames.length; i++ ) {
        var destResource = RESOURCES[resourceNames[i]];
        log( "Adding lines for resource " + resourceNames[i] );
        var destTestNames = Object.keys( destResource['all2alls'] );
        for ( var j = 0; j < destTestNames.length; j++ ) {
          var test = destTestNames[j];
          if ( CS_TESTS[test] == null ) {
            CS_TESTS[test] = {
              displayed: 0,
              lines: new Array()
						};
          }
          var sourceResources = {};
          if ( destResource['all2alls'][test] != null ) {
            sourceResources = Object.keys( destResource['all2alls'][test] );
          }
          for( var k = 0; k < sourceResources.length; k++ ) {
            var sourceResource = RESOURCES[sourceResources[k]];
            if ( sourceResource == null ) { 
              log( "ERROR: source resource '" + sourceResources[k] + "' for cross site test " + test + " not found; please verify the resource group"  ); 
            }
            var sourcePt = new google.maps.LatLng( sourceResource['latitude'], sourceResource['longitude'] );
            var destPt = new google.maps.LatLng( destResource['latitude'], destResource['longitude'] );
            var color;
            if ( destResource['all2alls'][test][sourceResources[k]] == 1 ) {
              color = LINE_COLORS['pass'];
            } else {
              color = LINE_COLORS['fail'];
            }
            var polyline = new google.maps.Polyline({
               path: [sourcePt, destPt ],
               strokeColor: color,
               strokeOpacity: .5,
               strokeWeight: 3
             });
            log( "Pushing line for test " + test + " from " + sourceResources[k] + " to " + resourceNames[i] );
            CS_TESTS[test]['lines'].push( polyline );
          }
        }
      }
      ]]></xsl:text>
    }
  </xsl:template>

  <xsl:template name="getResourceInfo">
    <xsl:param name="resource"/>
    <xsl:param name="site"/>
    <xsl:param name="reportSummaries"/>
    <xsl:param name="all2alls"/>
    <xsl:param name="google"/>

    // ----- begin getResourceInfo( <xsl:value-of select="$resource"/> ) -----
    
    // add information about this resource
    curResourceName = "<xsl:value-of select="$resource"/>";
    if ( !RESOURCES[curResourceName] ) {
      RESOURCES[curResourceName] = {};
    }

    var curResource = RESOURCES[curResourceName];
    var siteName = "<xsl:value-of select="$site"/>";
    if ( siteName == "" ) {
      siteName = "DEFAULT";
    }
    // get marker info
    var site = SITES[siteName];
    if ( site['numResources'] == 1 ) {
      curResource['latitude'] =  site['latitude'];
      curResource['longitude'] = site['longitude'];
    } else { 
      var theta = ((2 * Math.PI) / site['numResources']) * 
                  site['resourceIdx'];
      var longDiff = Math.cos(theta) * MARKER_DIST;
      var latDiff = Math.sin(theta) * MARKER_DIST;
      curResource['latitude'] = site['latitude'] + latDiff;
      curResource['longitude'] = site['longitude'] + longDiff;
      site['resourceIdx'] = site['resourceIdx'] + 1;
    }
    <xsl:variable name="instanceReports" select='$reportSummaries[instanceId]'/>
    <xsl:variable name="totalReports" select="$instanceReports[not(matches(errorMessage, $ignoreErrs))][string($ignoreErrs)!='']|$instanceReports[string($ignoreErrs)='']"/>
    <xsl:variable name="unknownReports" select="$instanceReports[not(.=$totalReports)]"/>
    <xsl:variable name="passedReports" select="$totalReports[body!='' and not(matches(comparisonResult, '^Failure:.+$'))]"/>
    <xsl:variable name="failedReports" select="$totalReports[body='' or matches(comparisonResult, '^Failure:.+$')]"/>
    // get the number of unknown reports 
    curResource['numUnknown'] = <xsl:value-of select="count($unknownReports)"/>;
    // get the number of passed reports 
    curResource['numPassed'] = <xsl:value-of select="count($passedReports)"/>;
    // get the errors
    curResource['errors'] = new Array();
    <xsl:for-each select="$failedReports">
      curResource['errors'].push
        ( { hostname: '<xsl:value-of select="hostname"/>',
            gmt: '<xsl:value-of select="gmt"/>',
            nickname: '<xsl:value-of select="nickname"/>' } );
    </xsl:for-each>

    // get all2all tests
    curResource['all2alls'] = {};
    <xsl:for-each select="$reportSummaries[targetHostname=$resource]">
      <xsl:variable name="reportSummary" select="."/>
      <xsl:for-each select="$google/crossSite/test">
        <xsl:if test="matches($reportSummary/nickname, regex)">
          var testName = "<xsl:value-of select="name"/>";
          var srcName = "<xsl:value-of select="$reportSummary/hostname"/>";
          if ( curResource['all2alls'][testName] == null ) {
            curResource['all2alls'][testName] = {};
          }
          var tests = curResource['all2alls'][testName];
          <xsl:variable name="errorMessage"
                        select='replace($reportSummary/errorMessage,"\n"," ")'/>
          var errorMessage =
            "<xsl:value-of select="replace($errorMessage,'&quot;','\\&quot;')"/>";
          if ( errorMessage == "" ) {
            tests[srcName] = 1;
          } else {
            tests[srcName] = 0;
          }
      </xsl:if>
      </xsl:for-each>
    </xsl:for-each>
    
    // ------ end getResourceInfo( <xsl:value-of select="$resource"/> ) -----
  </xsl:template>

  <xsl:template name="getSiteInfo" match="site">

    // ----- begin getSiteInfo( <xsl:value-of select="name"/> ) -----
    var name = "<xsl:value-of select="name"/>";
    if ( name != "" ) {
      var newsite = {
        resourceIdx: 0,
        numResources: <xsl:value-of select='count(resources/resource)'/>,
        latitude: <xsl:value-of select='latitude'/>,
        longitude: <xsl:value-of select='longitude'/>
      };
      SITES[name] = newsite;
    } 
    // ------ end getSiteInfo( <xsl:value-of select="name"/> ) -----

  </xsl:template>
    
</xsl:stylesheet>
