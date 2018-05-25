package plugins.Zyzyx.theproj;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
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
import plugins.adufour.ezplug.EzVarBoolean;
import poi.CreateWorkBook;

class Threading extends Thread {
	   private EzVarBoolean timeBool;
	   private Sequence seq1;
	   private Sequence seq2;
	   private ROI2D roi;
	   private XYSeries KVRatio = new XYSeries("KV");
	   private String[] XYinfo = new String[3];
	   
	   private CreateWorkBook wbc;
	   private InterruptHandler iH = new InterruptHandler(); 

	   	XYSeriesCollection Ycol = new XYSeriesCollection(KVRatio);
	   	
	    long current = 0;
		double OldRatio = 0;
		public MyWaitNotify mnw;
		public MyWaitNotify mnw2;
		private ROI2D bgROI;
	   
	   //setup vars based on checkboxes
	   Threading( EzVarIntRoi rei, Sequence numseq, Sequence divseq, EzVarBoolean tBool, CreateWorkBook w, MyWaitNotify m, MyWaitNotify m2, InterruptHandler i, ROI2D bg ) {
     		iH = i;
		    wbc = w;
     		mnw = m;
     		mnw2  = m2;
		   	timeBool = tBool;
     		roi = rei.EVIRoi;
     		seq1 = numseq;
     		seq2 = divseq;
     		bgROI = bg;
    	    //Get start-time
     		current = System.nanoTime();
     	    System.out.println("Start time = " + current);
     	    
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
	   
	   public void run() {
		  //thread code
		   System.out.println(this.getName() + " runned");
		   while (!iH.interruptVar) {
			if (timeBool.getValue() == false ) {
				if (seq1.getLastImage() == null) {MessageDialog.showDialog("seq1 empty");}
				if (seq2.getLastImage() == null) {MessageDialog.showDialog("seq2 empty");}
				
				double K = iterate(roi,seq1.getLastImage());
				double V = iterate(roi,seq2.getLastImage());
				double B1 = iterate(bgROI,seq1.getLastImage());
				double B2 = iterate(bgROI,seq2.getLastImage());
				
				K = K - B1;
				V = V - B2;
				
				
				
				System.out.println("\nMean after BG correction for ROI " + seq1.getName() + " is: " + K);
				System.out.println("Mean after BG correction for ROI " + seq2.getName() + " is: " + V);
				System.out.println("The ratio for " + seq1.getName() + " ÷ " + seq2.getName() + " is: " + K/V + " at time: " + (System.nanoTime()-current) + System.lineSeparator());
	          
				//Add the Ratio to the graph's dataset
				if (timeBool.getValue() == false && OldRatio != K/V) {
					KVRatio.addOrUpdate((System.nanoTime()-current)/1000000000,K/V); 
				}
				//Register ratio for the next loop
				OldRatio = K/V;
				//wbc.ApplyData(KVRatio, XYinfo[0]);
				
				System.out.println(this.getName() + " waiting!");

				mnw2.doNormalWait();
				System.out.println(this.getName() + " done waiting!");
				}


				if (iH.interruptVar) {
					System.out.println(this.getName() + " interrupted!");
					Stopping();
					return;
				}
		   }				
	   }
	   
	   public void Stopping() {
				System.out.println(this.getName() + " interrupted, doing wait");
				mnw.doWait();
				System.out.println(this.getName() + " wait complete, sending data");
				SendData();
				System.out.println(this.getName() + " data sent");
				mnw.doNotify();
	   }
	   
	   
	   public double iterate (ROI2D Roi, IcyBufferedImage image) {
	        // consider first image only here
	        BooleanMask2D mask = Roi.getBooleanMask(false);
	        // consider first image only here
	        double mean = 0;
	        double sample = 0;
	        
		        for (int x = 0; x < Roi.getFirstSequence().getSizeX(); x++)
		        {
		            for (int y = 0; y < Roi.getFirstSequence().getSizeY(); y++)
		            {
		                if (mask.contains(x, y))
		                {
		                    mean += image.getData(x, y, 0);
		                    sample++;
		                }
		            }
		        }
		        System.out.println("mean intensity over ROI " + Roi.getName() + ": " + (mean / sample));
		   return (mean/sample);
		   
	   }
	   
		private JFreeChart createChart(XYDataset xydataset)
	    {

	        JFreeChart jfreechart = ChartFactory.createXYLineChart(XYinfo[0], XYinfo[1],
	                XYinfo[2], xydataset);
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
	        numberaxis.setVerticalTickLabels(true);
	        numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        return jfreechart;
	    }
	   
	    public void addIcyFrame(final IcyFrame frame)
	    {
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
			wbc.ApplyData(KVRatio, XYinfo[0]);
		}
	}