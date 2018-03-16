<%@ tag import="de.laures.cewolf.ChartPostProcessor" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="org.jfree.chart.plot.CategoryPlot" %>
<%@ tag import="org.jfree.chart.JFreeChart" %>
<%@ tag import="org.jfree.chart.axis.NumberAxis" %>
<%@ tag import="org.jfree.chart.renderer.category.BarRenderer" %>

<%@ tag body-content="empty" %>
<%@ tag description="Creates a ChartPostProcessor instance that specifies integer tick units on the yaxis" %>

<%@ attribute name="var" rtexprvalue="false" required="true" description="name of the exported scoped variable to hold the new de.laures.cewolf.ChartPostProcessor object"%>
<%@ variable name-from-attribute="var" 
             variable-class="de.laures.cewolf.ChartPostProcessor" alias="varName" 
             scope="AT_END" 
             description="an instance of de.laures.cewolf.ChartPostProcessor"%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
  ChartPostProcessor p = new ChartPostProcessor() {
    public void processChart(Object chart, Map params) {
      CategoryPlot plot = ((JFreeChart) chart).getCategoryPlot();
      NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
      rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      BarRenderer renderer = (BarRenderer) plot.getRenderer();
      renderer.setDrawBarOutline(true);
    }
  };
  jspContext.setAttribute( "varName", p );  
%>
