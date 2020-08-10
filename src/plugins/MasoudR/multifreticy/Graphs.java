package plugins.Zyzyx.theproj;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;

public class Graphs extends PluginActionable {

	private XYDataset Ycol;
	
    TimeSeries MeansK = new TimeSeries("K");
    TimeSeries MeansV = new TimeSeries("V");
    TimeSeries KVRatio = new TimeSeries("KV");  
	
    JPanel mainPanel = GuiUtil.generatePanel("Graph");
    IcyFrame mainFrame = GuiUtil.generateTitleFrame("Chart demo", mainPanel, new Dimension(300, 100), true, true, true,
            true);


	Graphs (double K, double V, double KV,Second time) {
		
		MeansK.add(time,K);
		MeansV.add(time,K);  
		KVRatio.add(time,K);
		
		TimeSeriesCollection Ycol = new TimeSeriesCollection();
		
//        value = value + Math.random( ) - 0.5;                 
//        series.add(current, new Double( value ) );                 
		
		Ycol.addSeries(MeansK);
		Ycol.addSeries(MeansV);
		Ycol.addSeries(KVRatio);
	}
	
	private static JFreeChart createChart(XYDataset xydataset)
    {

        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("TestChart", "Time",
                "Value", xydataset, true, true, false);
        jfreechart.setBackgroundPaint(Color.white);
        XYPlot xyplot = (XYPlot) jfreechart.getPlot();
        xyplot.setInsets(new RectangleInsets(5D, 5D, 5D, 20D));
        xyplot.setBackgroundPaint(Color.lightGray);
        xyplot.setAxisOffset(new RectangleInsets(5D, 5D, 5D, 5D));
        xyplot.setDomainGridlinePaint(Color.white);
        xyplot.setRangeGridlinePaint(Color.white);
        DeviationRenderer deviationrenderer = new DeviationRenderer(true, false);
        deviationrenderer.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
        deviationrenderer.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
        deviationrenderer.setSeriesStroke(1, new BasicStroke(3F, 1, 1));
        deviationrenderer.setSeriesFillPaint(0, new Color(255, 200, 200));
        deviationrenderer.setSeriesFillPaint(1, new Color(200, 200, 255));
        xyplot.setRenderer(deviationrenderer);
        NumberAxis numberaxis = (NumberAxis) xyplot.getRangeAxis();
        numberaxis.setAutoRangeIncludesZero(false);
        numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        return jfreechart;
    }

	@Override
	public void run() {
		
        JFreeChart chart = createChart(Ycol);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setFillZoomRectangle(true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setPreferredSize(new Dimension(500, 270));
        mainPanel.add(chartPanel);

        mainFrame.pack();
        addIcyFrame(mainFrame);

        mainFrame.setVisible(true);
        mainFrame.center();
        mainFrame.requestFocus();
	}
	
	
	
}
