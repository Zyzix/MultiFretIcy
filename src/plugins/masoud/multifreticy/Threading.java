package plugins.masoud.multifreticy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.math.BigDecimal;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import javassist.tools.framedump;
import plugins.adufour.ezplug.EzVarBoolean;
import poi.CreateWorkBook;

class Threading {
	   	private EzVarBoolean 		timeBool;
	   	private Sequence 			seq1;
	   	private Sequence 			seq2;
	   	private ROI2D 				roi;
	   	private XYSeries 			KVRatio;
	   	private XYSeries 			KVRatioGraph;
	   	private String[] 			XYinfo;
	   	private IcyFrame 			frame;
	   	private InterruptHandler 	iH; 
	   	XYSeriesCollection 			Ycol;
	   	public long 				count, timeD;
		double 						OldRatio;
		public MyWaitNotify 		mnw;
		public MyWaitNotify 		mnw2;
		private ROI2D 				bgROI;
		private String 				name;
	   
	   //setup vars based on checkboxes
	   Threading( 	EzVarIntRoi rei,
			   		Sequence numseq, 
			   		Sequence divseq, 
			   		EzVarBoolean tBool, 
			   		MyWaitNotify m, 
			   		MyWaitNotify m2, 
			   		InterruptHandler i, 
			   		ROI2D bg ) {
		   
     		iH = i;
     		mnw = m;
     		mnw2  = m2;
		   	timeBool = tBool;
     		roi = rei.EVIRoi;
     		seq1 = numseq;
     		seq2 = divseq;
     		bgROI = bg;
     		
     		KVRatio = new XYSeries("KV");
     		KVRatioGraph = new XYSeries("KV");
     		XYinfo = new String[3];
     		iH = new InterruptHandler(); 
     		Ycol = new XYSeriesCollection(KVRatio);
     		count = 0;
     		OldRatio = 0;
     		
     	    if (rei.EVIDiv != null && rei.EVINum != null && rei.EVIDiv != rei.EVINum) {
	     	    XYinfo[0] = numseq.getName() + " ÷ " + divseq.getName();
	     	    System.out.println("Creating " +  numseq.getName() + " versus " + divseq.getName());
	     	}
     	    else {
			   System.out.println("Minimum and maximum of one numerator and one divisor");
			   return; 
		   }
		   if (timeBool.getValue() == false) {XYinfo[1] = "Time";} else {XYinfo[1] = "Frame";}
		   XYinfo[2] = "Ratio";
		   
			//Run graph
			  SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				      // Here, we can safely update the GUI
				      // because we'll be called from the
				      // event dispatch thread
				    	Graph(Ycol);
				    }
			  });
	   }
	   //TODO: replace long currenttime with  java.math.BigDecimal
	   public void run(long currentTime,int x) {
		  //thread code
			if (seq1.getLastImage() == null) {MessageDialog.showDialog("seq1 empty"); return;}
			if (seq2.getLastImage() == null) {MessageDialog.showDialog("seq2 empty"); return;}
				double[] Kx = iterate(roi,seq1.getLastImage());
				double[] Vx = iterate(roi,seq2.getLastImage());
				double[] B1x = iterate(bgROI,seq1.getLastImage());
				double[] B2x = iterate(bgROI,seq2.getLastImage());
				
				double K = Kx[1];
				double V = Vx[1];
				double B1 = B1x[1];
				double B2 = B2x[1];
				
				K = K - B1;
				V = V - B2;
				
				timeD = (currentTime-Prestart.startTime)/1000000000;
				
				System.out.println("\nMean after BG correction for ROI " + seq1.getName() + " is: " + K);
				System.out.println("Mean after BG correction for ROI " + seq2.getName() + " is: " + V);
				System.out.println("The ratio for " + seq1.getName() + " ÷ " + seq2.getName() + " is: " + K/V + " at time: " + timeD + System.lineSeparator());
	          
				//Add the Ratio to the graph's dataset
				if (!Prestart.offlineBool) {
					KVRatio.addOrUpdate(timeD,K/V);
				} else { 
					KVRatio.addOrUpdate(x,K/V);
				}
				count = KVRatio.getItemCount();
				
		   }				
	   
	   public String getName() {
		   return name;
	   }
	   public void setName(String s) {
		   name = s;
	   }
	   
	   public void Stopping() {
				System.out.println(this.getName() + " interrupted, doing wait");
				mnw.doWait();
				System.out.println(this.getName() + " wait complete, sending data");
				SendData();
				System.out.println(this.getName() + " data sent");
				mnw.doNotify();
	   }
	   
	   
	   public double[] iterate (ROI2D Roi, IcyBufferedImage image) {
	        // Create a 2D mask out of the ROI
	        BooleanMask2D mask = Roi.getBooleanMask(false);
	        
	        //See points for testing:
/*	        Point[] arey  = mask.getContourPoints();
	        for (int i = 0; i < arey.length; i++) {
	        	System.out.println("X: " + arey[i].x + " " + "Y: " + arey[i].y);
	        }*/
	        // get every pixel and average it
	        double mean = 0;
	        double sample = 0;
	        System.out.println("Iterating " );
		        for (int x = 0;	x < image.getSizeX(); x++)
		        {
		            for (int y = 0; y < image.getSizeY(); y++)
		            {
		                if (mask.contains(x, y))
		                {
		                    mean += image.getData(x, y, 0);
		                    sample++;
		                }
		            }
		        }
		        System.out.println("pixel count: " + sample + " - mean intensity over ROI " + Roi.getName() + ": " + (mean / sample));
		   double[] result = new double[2];
		   result[0] = mean;
		   result[1] = mean/sample;
		   return (result);
		   
	   }
	   
		private JFreeChart createChart(XYDataset xydataset)
	    {

	        JFreeChart jfreechart = ChartFactory.createXYLineChart(
	        		XYinfo[0], 
	        		XYinfo[1],
	                XYinfo[2], 
	                xydataset,
	                PlotOrientation.VERTICAL,
	                true,
	                true,
	                false);
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
	        numberaxis.setAutoTickUnitSelection(true);
	        numberaxis.setVerticalTickLabels(true);
	        xyplot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

	      //  numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        return jfreechart;
	    }
	   
	    private void addIcyFrame(final IcyFrame f)
	    {
	    	frame = f;
	        frame.addToDesktopPane();
	    }
		
		private void Graph(XYDataset Ycol) {
			    JPanel mainPanel = GuiUtil.generatePanel("Graph");
			    IcyFrame mainFrame = GuiUtil.generateTitleFrame("Chart demo", mainPanel, new Dimension(300, 100), true, true, true,
			            true);
			
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
		public void SendData() {
			System.out.println("Writing to Excel from chart " + XYinfo[0]);
			Prestart.wbc.ApplyData(KVRatio, XYinfo[0]);
		}
		
		public void ExitThis() {
			System.out.println("Exiting a thread");
			SendData();
			//frame.removeFromMainDesktopPane();
			frame.close();
		}
		
	    /**
	     * Round to certain number of decimals
	     * 
	     * @param d
	     * @param decimalPlace
	     * @return
	     */
	    public static double round(double d, int decimalPlace) {
	        BigDecimal bd = new BigDecimal(Double.toString(d));
	        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
	        System.out.println(bd.doubleValue());
	        return bd.doubleValue();
	    }
	}