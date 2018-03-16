<%@ tag import="de.laures.cewolf.ChartPostProcessor" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="org.jfree.chart.plot.CategoryPlot" %>
<%@ tag import="org.jfree.chart.JFreeChart" %>
<%@ tag import="org.jfree.chart.axis.NumberAxis" %>
<%@ tag import="org.jfree.chart.axis.CategoryAxis" %>
<%@ tag import="org.jfree.chart.axis.CategoryLabelPositions" %>
<%@ tag import="org.jfree.chart.renderer.category.LineAndShapeRenderer"%>

<%@ tag body-content="empty" %>
<%@ tag description="Creates a ChartPostProcessor instance that specifies integer tick units on the yaxis" %>

<%@ attribute name="xaxisHeight" rtexprvalue="true" required="true" type="java.lang.Integer" description="the height of the xaxis space"%>
<%@ attribute name="ymax" rtexprvalue="true" required="true" type="java.lang.Integer" description="the upper bound of the yaxis"%>
<%@ attribute name="ymin" rtexprvalue="true" required="true" type="java.lang.Integer" description="the lower bound of the yaxis"%>
<%@ attribute name="var" rtexprvalue="false" required="true" description="name of the exported scoped variable to hold the new de.laures.cewolf.ChartPostProcessor object"%>
<%@ variable name-from-attribute="var" 
             variable-class="de.laures.cewolf.ChartPostProcessor" alias="varName" 
             scope="AT_END" 
             description="an instance of de.laures.cewolf.ChartPostProcessor"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  ChartPostProcessor p = new ChartPostProcessor() {
    public void processChart(Object chart, Map params) {
      ((JFreeChart) chart).setBorderVisible(false);
      ((JFreeChart) chart).removeLegend();
      CategoryPlot plot = ((JFreeChart) chart).getCategoryPlot();
      LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
      renderer.setShapesVisible(true);
      renderer.setShapesFilled(true);
      CategoryAxis xaxis = (CategoryAxis)plot.getDomainAxis();
      xaxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
      xaxis.setFixedDimension( (Integer)jspContext.getAttribute("xaxisHeight"));
      xaxis.setTickMarksVisible( true );
      xaxis.setAxisLineVisible( true );
      NumberAxis yaxis = (NumberAxis)plot.getRangeAxis();
      yaxis.setLowerBound( (Integer)jspContext.getAttribute("ymin") );
      yaxis.setUpperBound( (Integer)jspContext.getAttribute("ymax") );
    }
  };
  jspContext.setAttribute( "varName", p );  
%>
