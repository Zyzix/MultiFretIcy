package plugins.Zyzyx.theproj;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.sequence.SequenceDataIterator;

class Threadingbackup implements Runnable {
	   private Thread t;
	   private ROI threadRoiK;
	   private ROI threadRoiV;
	   private Boolean stopFlag2;
	   
	   //setup vars based on checkboxes
	   Threadingbackup( Map.Entry<EzVarIntRoi, EzVarIntRoi> entry ) {
		  //TODO switch case here? Or does if-else look better in this case...	   
		   if (entry.getKey().EVIBool.getValue() && !entry.getValue().EVIBool.getValue() ) {
		      threadRoiK = entry.getKey().EVIRoi;
		      threadRoiV = entry.getValue().EVIRoi;
		      System.out.println("Creating " +  threadRoiK.getName() + "versus" + threadRoiV.getName());
	       }
		   else if (entry.getValue().EVIBool.getValue() && !entry.getKey().EVIBool.getValue()) {
			  threadRoiV = entry.getKey().EVIRoi;
			  threadRoiK = entry.getValue().EVIRoi; 
		      System.out.println("Creating " +  threadRoiK.getName() + "versus" + threadRoiV.getName());
		   }
		   else {
			   System.out.println("Minimum and maximum of one numerator");
			   return; 
		   }
	   }
	   
	   public void run() {
		  //thread code
		   Second current = new Second( );
			while (!stopFlag2)
				{
		      System.out.println("Running " +  threadRoiK.getName() + "versus" + threadRoiV.getName() );
		      
		      TimeSeries MeansK = new TimeSeries(threadRoiK.getName());
		      TimeSeries MeansV = new TimeSeries(threadRoiV.getName());
		      TimeSeries KVRatio = new TimeSeries(threadRoiK.getName() + " ÷ " + threadRoiV.getName());  
	    
		      try {		    	  
		    	  double K = iterate(threadRoiK);
		          double V = iterate(threadRoiV);
		      
		          System.out.println("Mean for ROI " + threadRoiK.getName() + " is: " + K);
		          System.out.println("Mean for ROI " + threadRoiV.getName() + " is: " + V);
		          System.out.println("The ratio for threadRoiK.getName() / threadRoiV.getName() is: " + K/V + "at time: " + current);
		          current = ( Second ) current.next( ); 
		          Thread.sleep(50);
		          
		      } catch (InterruptedException e) {
		         System.out.println("Thread " +  threadRoiK.getName() + " interrupted.");
		      }
		      System.out.println("Thread " +  threadRoiK.getName() + " exiting.");
		   }
	   }
	   
	   public double iterate (ROI Roi) {
		      //iterate over every pixel, calculate mean intensity
	          SequenceDataIterator iterator = new SequenceDataIterator(Roi.getFirstSequence(), Roi);
	          double mean = 0;
	          double sample = 0;
	          
	          while (!iterator.done())
	          {
	              mean += iterator.get();
	              iterator.next();
	              sample++;
	          }
	          return (mean/sample);
	   }
	   
	   public void start () {
		   //start threads
		   if (threadRoiK != null) {
		      System.out.println("Starting " +  threadRoiK.getName() );
		      if (t == null) {
		         t = new Thread (this, threadRoiK.getName());
		         t.start ();
		      }
		   }
		   else {
			   System.out.println("Minimum and maximum of one numerator");
		   }
	   }
	   
	   public void stop () {
		//stop threads
		   stopFlag2 = true;
	   }

	    
	}