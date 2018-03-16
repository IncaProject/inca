<%@ tag import="de.laures.cewolf.ChartPostProcessor" %>
<%@ tag import="edu.sdsc.inca.consumer.BarItemLabelColorRenderer" %>
<%@ tag import="edu.sdsc.inca.consumer.DiffCategoryItemLabelGenerator" %>
<%@ tag import="java.util.Map" %>
<%@ tag import="java.awt.Color" %>
<%@ tag import="java.awt.Font" %>
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
      ((JFreeChart) chart).setBorderVisible(false);
      CategoryPlot plot = ((JFreeChart) chart).getCategoryPlot();
      NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
      rangeAxis.setUpperMargin(0.30);
      rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
      BarRenderer renderer = (BarRenderer)plot.getRenderer();
      BarItemLabelColorRenderer myrenderer = new BarItemLabelColorRenderer
        ( renderer, new Color( 74, 160, 44 ), Color.RED, Color.DARK_GRAY );
      plot.setRenderer(myrenderer);
      myrenderer.setDrawBarOutline(true);
      myrenderer.setBaseItemLabelGenerator
        ( new DiffCategoryItemLabelGenerator() );
      myrenderer.setBaseItemLabelFont(new Font("Serif", Font.BOLD, 12));
      myrenderer.setBaseItemLabelsVisible(true);
      myrenderer.setItemMargin(0);
    }
  };
  jspContext.setAttribute( "varName", p );  
%>
