<%@ tag import="de.laures.cewolf.ChartPostProcessor" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="org.jfree.chart.plot.CombinedDomainXYPlot" %>
<%@ tag import="org.jfree.chart.JFreeChart" %>
<%@ tag import="java.util.List" %>
<%@ tag import="java.util.Vector" %>
<%@ tag import="org.jfree.chart.renderer.xy.XYLineAndShapeRenderer" %>
<%@ tag import="org.jfree.chart.plot.XYPlot" %>
<%@ tag import="org.jfree.chart.axis.DateAxis" %>
<%@ tag import="java.util.regex.Pattern" %>
<%@ tag import="edu.sdsc.inca.util.StringMethods" %>
<%@ tag import="java.util.Date" %>
<%@ tag import="java.util.Calendar" %>
<%@ tag import="org.jfree.chart.axis.NumberAxis" %>
<%@ tag import="org.jfree.chart.axis.NumberTickUnit" %>
<%@ tag import="edu.sdsc.inca.util.Constants" %>
<%@ tag import="java.text.SimpleDateFormat" %>
<%@ tag import="org.jfree.chart.axis.DateTickUnit" %>

<%@ tag body-content="empty" %>
<%@ tag description="Creates a ChartPostProcessor instance that specifies vertical date ticks, ymin=-1.1, ymax=1.1, and marks" %>

<%@ attribute name="ymin" required="false"  type="java.lang.Float"
              description="Specify a min Y axis value if not happy with defaults." %>
<%@ attribute name="ymax" required="false"  type="java.lang.Float"
              description="Specify a max Y axis value if not happy with defaults." %>
<%@ attribute name="ytick" required="false"  type="java.lang.Float"
              description="Specify a ytick value if not happy with defaults." %>
<%@ attribute name="var" rtexprvalue="false" required="true" %>
<%@ variable name-from-attribute="var"
             variable-class="de.laures.cewolf.ChartPostProcessor" 
             alias="varName" scope="AT_END" 
             description="name of the exported scoped variable to hold the new de.laures.cewolf.ChartPostProcessor object" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  ChartPostProcessor p = new ChartPostProcessor() {
    private static final int DAYS_FOR_AUTOTICK = 20;

    public void processChart(Object chart, Map params) {
      XYPlot xyplot = ((JFreeChart) chart).getXYPlot();
      List subplots;
      if ( xyplot instanceof CombinedDomainXYPlot ) {
        subplots = ((CombinedDomainXYPlot)xyplot).getSubplots();
      } else {
        subplots = new Vector();
        subplots.add( xyplot ); 
      }
      for ( int i = 0; i < subplots.size(); i++ ) {
        XYPlot subplot = (XYPlot)subplots.get(i);

        // show filled shapes at each data point.
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setShapesVisible(true);
        renderer.setShapesFilled(true);
        subplot.setRenderer( renderer );

        // format xaxis date 
        DateAxis xaxis = (DateAxis)subplot.getDomainAxis();
        xaxis.setVerticalTickLabels( true );
        long timeDiff =
          (xaxis.getMaximumDate().getTime() - xaxis.getMinimumDate().getTime())
            / (Constants.MILLIS_TO_HOUR * Constants.HOURS_TO_DAY);
        if ( timeDiff < 2 ){
          xaxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
          xaxis.setTickUnit(new DateTickUnit(DateTickUnit.HOUR, 2));
        } else {
          xaxis.setDateFormatOverride(new SimpleDateFormat("MM/dd/yy"));
          if ( xaxis.getTickUnit().getUnit() > DateTickUnit.DAY ||
            timeDiff < DAYS_FOR_AUTOTICK ) {
            xaxis.setTickUnit(new DateTickUnit(DateTickUnit.DAY, 1 ));
          }
        }

        // format yaxis max/min values
        NumberAxis yaxis = (NumberAxis)subplot.getRangeAxis();
        if ( jspContext.getAttribute("ymin") != null ) {
          Float ymin = (Float)jspContext.getAttribute("ymin");
          yaxis.setLowerBound(ymin);
        }
        if ( jspContext.getAttribute("ymax") != null ) {
          Float ymax= (Float)jspContext.getAttribute("ymax");
          yaxis.setUpperBound(ymax);
        }
        // set number between yaxis tick marks
        if ( jspContext.getAttribute("ytick") != null ) {
          Float ytick = (Float)jspContext.getAttribute("ytick");
          yaxis.setTickUnit( new NumberTickUnit( ytick ) );
        } 
      }
    }
  };
  jspContext.setAttribute( "varName", p );  
%>
