package plugins.MasoudR.multifreticy.Main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.InterruptHandler;
import plugins.MasoudR.multifreticy.DataObjects.MyWaitNotify;
import plugins.MasoudR.multifreticy.DataObjects.VarIntRoi;
import plugins.MasoudR.multifreticy.DataObjects.CcArgs;
import plugins.MasoudR.multifreticy.DataObjects.CustomCalc;
import plugins.adufour.ezplug.EzVarBoolean;

class Threading {
	   	private EzVarBoolean 		timeBool;
	   	private Sequence 			concS;
	   	private ArrayList<VarIntRoi>roi;
	   	private ArrayList<XYSeries>	KVRatio = new ArrayList<XYSeries>();
	   	private String[] 			XYinfo;
	   	private IcyFrame 			frame;
		@SuppressWarnings("unused")
		private InterruptHandler 	iH; 
	   	private XYSeriesCollection	Ycol = new XYSeriesCollection();
	   	public long 				count, timeD;
		double 						OldRatio;
		public MyWaitNotify 		mnw;
		public MyWaitNotify 		mnw2;
		private ROI2D 				bgROI;
		private String 				name;
		public String				pos;
		private JPanel				calcs;
		public ChartPanel			chartPanel;
		private Map<String,ArrayList<ArrayList<Double>>> rawDataPack = new HashMap<>(); 
		private Path logName;
		private CustomCalc 			customCalcs;
	    private ArrayList<ROI2D>	ccROIs;
		private Map<String,ArrayList<CcArgs>> calcArgs;
		public JPanel chartHolder = GuiUtil.generatePanel();
		
	   //setup vars based on checkboxes
	   Threading( 	Sequence s, 
			   		ArrayList<VarIntRoi> rei,
			   		EzVarBoolean tBool, 
			   		MyWaitNotify m, 
			   		MyWaitNotify m2, 
			   		InterruptHandler i, 
			   		ROI2D bg, 
			   		ArrayList<ROI2D> cr,  
			   		Map<String,ArrayList<CcArgs>> ca,
			   		CustomCalc cc, 
			   		JPanel bgSwitches, 
			   		Path ln ) {
		   	
     		iH		 	= i;
     		mnw		 	= m;
     		mnw2	 	= m2;
		   	timeBool 	= tBool;
     		concS	 	= s;
     		bgROI	 	= bg;
     		ccROIs		= cr;
     		pos 	 	= concS.getName();
     		this.setName(pos);
     		calcArgs    = ca;		
     		customCalcs	= cc;
     		calcs	 	= bgSwitches; 
     		logName	 	= ln;    
     		
     		roi = new ArrayList<VarIntRoi>();
     		chartHolder.setName(pos);
     	     
     		if (MultiFretIcy.PS.calcBool) {     		
	     		for (String e : calcArgs.keySet()) {
		     		KVRatio.add(new XYSeries(e));
		     		rawDataPack.put(e, new ArrayList<ArrayList<Double>>());
		     		rawDataPack.get(e).add(new ArrayList<Double>());
		     		rawDataPack.get(e).add(new ArrayList<Double>());
	     		}
	     		System.out.println("# of XY series: " + KVRatio.size());
	     		for (int l = 0; l < KVRatio.size(); l++) {
	     			System.out.println("adding to  ycol");
		     		Ycol.addSeries(KVRatio.get(l));
	     		}
     		}
     		     
     		if (!MultiFretIcy.PS.calcBool) {
     			for (VarIntRoi r : rei) {
     				if (r.position.equals(concS.getName()) && !r.getBgBool()) {
     					System.out.println("Initialising XY series for " + concS.getName());
     					roi.add(r);
     					String serieName =  r.Roi2D.getName() + ": " +r.getNumerator() + "/" + r.getDivisor();
     		     		KVRatio.add(new XYSeries(serieName));
    		     		rawDataPack.put(serieName, new ArrayList<ArrayList<Double>>());
    		     		rawDataPack.get(serieName).add(new ArrayList<Double>());
    		     		rawDataPack.get(serieName).add(new ArrayList<Double>());
     				}
     			}
	     		System.out.println("# of XY series: " + KVRatio.size());
	     		for (int l = 0; l < KVRatio.size(); l++) {
	     			System.out.println("adding to  ycol");
		     		Ycol.addSeries(KVRatio.get(l));
	     		}
     		}

		   rawDataPack.put("bg", new ArrayList<ArrayList<Double>>());
		   rawDataPack.get("bg").add(new ArrayList<Double>());
		   rawDataPack.get("bg").add(new ArrayList<Double>());
     		
     		// Graph info
     		XYinfo = new String[3];
     		iH = new InterruptHandler(); 

     		count = 1;
     		OldRatio = 0;
     		System.out.println("Concatenated sequence depth: " + concS.getSizeZ());
     		System.out.println("Concatenated sequence images: " + concS.getSizeT());
     		System.out.println("xyinfo0 = " + concS.getName());
     	    XYinfo[0] = concS.getName();
     		System.out.println("2 xyinfo0 = " + XYinfo[0]);



     	    
			if (timeBool.getValue() == false) {XYinfo[1] = "Time";} else {XYinfo[1] = "Frame";}
			XYinfo[2] = "Ratio";
		   
			//Run graph
//			SwingUtilities.invokeLater(new Runnable() {
//			    public void run() {
			      // Here, we can safely update the GUI
			      // because we'll be called from the
			      // event dispatch thread
			    	graph(KVRatio);
//			    }
//			  });
			System.out.println("Graph generated");
	   }	   
	   
	   //TODO: replace long currenttime with  java.math.BigDecimal
	   public void run(long currentTime,int x,String prenotated) {
		   int t = concS.getSizeT()-1;
		   if (!MultiFretIcy.PS.calcBool) {
			   
			   //Normal calculations
			   int n = 0;
			   for (VarIntRoi r : roi) {
     				if (r.position.equals(concS.getName()) && !r.getBgBool() && n < KVRatio.size()) {
     					String serieName =  r.Roi2D.getName() + ": " +r.getNumerator() + "/" + r.getDivisor();

						int num = 0;
						int div = 0;
						for (int v = 0; v < MultiFretIcy.PS.S1.rois.size(); v++) {
							ROI roi = MultiFretIcy.PS.S1.rois.get(v);
							System.out.println(roi.getName() + " num: " + r.getNumerator() + " div: " + r.getDivisor());
							if (roi.getName().equals(r.getNumerator())) {
							num = v;
							System.out.println("found num");
							}
							if (roi.getName().equals(r.getDivisor())) {
							div = v;
							System.out.println("found div");	
							
							}
						}
     					
			if (concS.getImage(t, num) == null) {System.out.println("num empty at t,num: " + t + ", " + num); return;}
			if (concS.getImage(t, div) == null) {System.out.println("div empty at t,div: " + t + ", " + div); return;}
				double[] Kx = iterate(r.Roi2D,concS.getImage(t, num));
				double[] Vx = iterate(r.Roi2D,concS.getImage(t, div));

				double K = Kx[1];
				double V = Vx[1];
				//Register raw data
				rawDataPack.get(serieName).get(0).add(Kx[1]);
				rawDataPack.get(serieName).get(1).add(Vx[1]);
				
				writeLog("Raw Data for " + concS.getChannelName(num) + " is: " + K);
				writeLog("Raw Data for " + concS.getChannelName(div) + " is: " + V);

 				System.out.println(r.getNumerator() + "(numerator) signal mean: " + K);
 				System.out.println(r.getDivisor() + "(divisor) signal mean: " + V);

				//Check if BG correction is needed, apply calculation
	     		for (Component a : calcs.getComponents()) {
	     			JCheckBox b;
	     			if (a instanceof JCheckBox) {
		     			b = (JCheckBox) a;	  
		     			if (b.getName().equals(r.getNumerator() + "_bg") && b.isSelected()) {
		     				double[] B1x = iterate(bgROI,concS.getImage(t, num));
		     				double B1 = B1x[1];
		     				//Register Raw Data
		    				rawDataPack.get("bg").get(0).add(B1);
		    				//Calculate corrected numerator
		    				K = K - B1;
		     				System.out.println(r.getNumerator() + "(numerator) background: " + B1);
		    				writeLog("Raw Background for " + concS.getChannelName(num) + " is: " + B1);
		     			}
		     			if (b.getName().equals(r.getDivisor() + "_bg") && b.isSelected()) {
		     				double[] B2x = iterate(bgROI,concS.getImage(t, div));
		     				double B2 = B2x[1];
		     				//Register Raw Data
		    				rawDataPack.get("bg").get(1).add(B2);
		    				//Calculate corrected numerator
		    				V = V - B2;
		     				System.out.println(r.getDivisor() + "(divisor) background: " + B2);
		    				writeLog("Raw Background for " + concS.getChannelName(div) + " is: " + B2);
		     			}
	     			}
	     		}   
				timeD = (currentTime-MultiFretIcy.PS.startTime)/1000000000;
				
				System.out.println("\nMean after calculations (if any) for ROI " + concS.getChannelName(num) + " is: " + K);
				System.out.println("Mean after calculations (if any) for ROI " + concS.getChannelName(div) + " is: " + V);
				System.out.println("The ratio for " + concS.getChannelName(num) + " ÷ " + concS.getChannelName(div) + " is: " + K/V + " at time: " + timeD + System.lineSeparator());
	          	
				if (prenotated != null && !MultiFretIcy.PS.offlineBool) {
					preNotate(prenotated, timeD, K/V);
				} else if (prenotated != null && MultiFretIcy.PS.offlineBool) {
					preNotate(prenotated, count, K/V);
				}
				
				//Add the Ratio to the graph's dataset
				if (!MultiFretIcy.PS.offlineBool) {
					KVRatio.get(n).addOrUpdate(timeD,K/V);
				} else { 
					KVRatio.get(n).addOrUpdate(count,K/V);
				}
				count = KVRatio.get(n).getItemCount(); 
				n++;
     				}
    			}
		 } else if(MultiFretIcy.PS.calcBool) {
		     		/*
		     		 * Custom Calcs zone *
		     		 * for each calcname in map get the calcargs and
		     		 * compare calcargname to calcrois names and 
		     		 * then iterate calcroi in calcargchannel
		     		 * 
		     		 * then input into calcargformula
		     		 * 
		     		 */
	//TODO change all doubles to BigDecimal (doubles shouldn't be used for precise values
			 		int l = 0;
		     		for (Map.Entry<String,ArrayList<CcArgs>> e : calcArgs.entrySet()) {
		     			for (CcArgs arg : e.getValue()) {
			     			for (ROI2D r : ccROIs) {
			     				if(r.getName().equals(arg.getArgName())) {    		
		     						arg.setValue(iterate(r, concS.getImage(t, arg.getChannel()))[1]);			     					
			     				}
			     			}
		     			}
		     		
		     			double finalCalc = 0;
		     		
			 		   for (int n = 0; n < customCalcs.GetNames().size(); n++) {
			 			   finalCalc = customCalculate(n);
			 			   System.out.println("final calc: " + finalCalc);
			 		   }	
			 		   
						timeD = (currentTime-MultiFretIcy.PS.startTime)/1000000000;
	
						System.out.println("The final custom calculation for " 
								+ concS.getName()
								+ finalCalc + " at time: " 
								+ timeD 
								+ System.lineSeparator());
			          	
						//Add the Ratio to the graph's dataset
						if (!MultiFretIcy.PS.offlineBool) {
							KVRatio.get(l).addOrUpdate(timeD,finalCalc);
						} else { 
							KVRatio.get(l).addOrUpdate(count,finalCalc);
						}
						count = KVRatio.get(l).getItemCount();    
						l++;
		     		}
		 	}
	   }				
	   
	   public String getName() {
		   return name;
	   }
	   public void setName(String s) {
		   name = s;
	   }	   
   
//	   public void Stopping() {
//				System.out.println(this.getName() + " interrupted, doing wait");
//				mnw.doWait();
//				System.out.println(this.getName() + " wait complete, sending data");
//				SendData();
//				System.out.println(this.getName() + " data sent");
//				mnw.doNotify();
//	   }
	   
	   public double customCalculate(int n) {
			   System.out.println("Calculating " + customCalcs.GetName(n));		   
   
			   ArrayList<Argument> argayList = margsGetter(calcArgs.get(customCalcs.GetName(n)));
			   //can add new arguments from main/bg here

			   ArrayList<Argument> aList = new ArrayList<Argument>();
			   
			   for (Argument d : argayList) {
				   System.out.printf("%narg %s has a value of %s%n",d.getArgumentName(),d.getArgumentValue());
				   aList.add(new Argument(d.getArgumentName(),d.getArgumentValue()));
			   }
			   

			   
			   Expression e = new Expression(cleanFormula(customCalcs.GetFormula(n)), aList.toArray(new Argument[0]));
			   mXparser.consolePrintln(e.getExpressionString() + " = " + e.calculate());
			   return e.calculate();
	   }
	   
	   //Remove brackets from formula
	   public String cleanFormula(String s) {
		   System.out.println("Cleaning formula: " + s);
//		   s = s.split("(>>>)")[1];
		   
		   String regex = "(\\w*)\\[(\\w*?)\\]";
		   Pattern pattern = Pattern.compile(regex);
		   Matcher matcher = pattern.matcher(s);
 
		   int matchCount = 0;
		   while (matcher.find()) {
			   matchCount++;
			   System.out.printf("Match count: %s, Group Zero Text: '%s'%n", matchCount,
					   matcher.group());
			   for (int i = 1; i <= matcher.groupCount(); i++) {
				   System.out.printf("Capture Group Number: %s, Captured Text: '%s'%n", i, matcher.group(i));
   				}	
//			   argsList.add(new CcArgs(matcher.group(1),GetChannel(matcher.group(2))));		
			   s = s.replaceFirst("\\[" + matcher.group(2) + "\\]", String.valueOf(MultiFretIcy.PS.S1.SU1.GetChannel(matcher.group(2))));
			   
		   }
		   

		   
		   System.out.printf("Detected Formula: %n%s%n",s);   
		   return s;
	   }
	   
	   
	   
//	   public String cleanFormula(String s) {
//		   String regex = "([^>>>].*?)(\\[\\w+\\])([^A-Za-z\\n]*)";
//	       Pattern pattern = Pattern.compile(regex);
//	       Matcher matcher = pattern.matcher(s);
//	       
//	       String fom = "";
//	        while (matcher.find()) {
//	            for (int i = 1; i <= matcher.groupCount(); i=i+2) {
//	            	if (!matcher.group(i).contains("[")) {         		
//	            		fom = fom + matcher.group(i);	
//    	         	}    	
//	            }
//	        }       
//		   System.out.printf("Detected Formula: %n%s%n",fom);   
//		   return fom;
//	   }
	   
	   
		public ArrayList<Argument> margsGetter(ArrayList<CcArgs> arrayList) {
			ArrayList<Argument> mgl = new ArrayList<Argument>();
			
			for(int i = 0; i<arrayList.size(); i++) {
				System.out.println(concS.getName() + " " + arrayList.get(i).getArgName()+arrayList.get(i).getChannel() + ": " + arrayList.get(i).getValue());
				mgl.add(new Argument(arrayList.get(i).getArgName()+arrayList.get(i).getChannel(), arrayList.get(i).getValue()));
			}
			return mgl;
		}
	   
	   public double[] iterate (ROI2D Roi, IcyBufferedImage image) {
	        // Create a 2D mask out of the ROI
	        BooleanMask2D mask = Roi.getBooleanMask(false);
	        
	        // get every pixel and average it
	        double sum = 0;
	        double sample = 0;
	        System.out.println("Iterating " );
		        for (int x = 0;	x < image.getSizeX(); x++)
		        {
		            for (int y = 0; y < image.getSizeY(); y++)
		            {
		                if (mask.contains(x, y))
		                {
		                    sum += image.getData(x, y, 0);
		                    sample++;
		                }
		            }
		        }
		       // System.out.println("pixel count: " + sample + " - mean intensity over ROI " + Roi.getName() + ": " + (mean / sample));
		        System.out.println("measurement pixel count: " + sample);
		   double[] result = new double[2];
		   result[0] = sum;
		   result[1] = sum/sample;
		   return (result);
		   
	   }
		   
	    private void addIcyFrame(final IcyFrame f)
	    {
	    	frame = f;
	        frame.addToDesktopPane();
	    }
	    
		private void graph(ArrayList<XYSeries> dataList) {
		    ArrayList<XYSeriesCollection> list = new ArrayList<XYSeriesCollection>();		    	
			for (XYSeries s : dataList) {
				list.add(new XYSeriesCollection(s));
			}			
	        
		    final NumberAxis domainAxis = new NumberAxis("Time");
		    final NumberAxis rangeAxis = new NumberAxis("Ratio");
		    
		    domainAxis.setTickLabelPaint(Color.WHITE);
		    rangeAxis.setTickLabelPaint(Color.WHITE);
		    
		    
		    // create plot ...
		    final IntervalXYDataset data0 = new XYSeriesCollection(dataList.get(0));
		    final XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer(true, false);
		    renderer0.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
		    final XYPlot plot = new XYPlot(data0, domainAxis, rangeAxis, renderer0);

		    // add other datasets and renderers ... 
			    for (int n = 1; n < dataList.size(); n++) {		    
			    final IntervalXYDataset data1 = new XYSeriesCollection(dataList.get(n));
			    final XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, false); 
			    plot.setDataset(n, data1);
			    plot.setRenderer(n, renderer1);
			    renderer1.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
		    }
		    
		    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		    domainAxis.setAutoRangeIncludesZero(false);
		    domainAxis.setAutoTickUnitSelection(true);
		    domainAxis.setVerticalTickLabels(true);
		    domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		    
		    rangeAxis.setAutoRangeIncludesZero(false);
		    rangeAxis.setAutoTickUnitSelection(true);
		    rangeAxis.setVerticalTickLabels(true);
		    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		    
		    // return a new chart containing the overlaid plot ...
		    JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);    
		    chart.setNotify(false);
		    
	        Font font = new Font(chart.getTitle().getFont().getName(),Font.BOLD,11);
//	        chart.removeLegend();
//	        chart.clearSubtitles(); //these remove the legend if needed TODO: make it remove when only 1 trace
	        chart.getTitle().setFont(font);
	        chartPanel = new ChartPanel(chart, 
	        		300,
	        		162,
	        		100, 
	        		50, 
	        		3000,
	        		1500, 
	        		true,
	        		true,
	        		true, 
	        		true,
	        		true, 
	        		true,
	        		true);
	        chartPanel.setFillZoomRectangle(true);
	        chartPanel.setMouseWheelEnabled(true);
	        chartPanel.setBackground(Color.white);
	        
		    System.out.println("Rendering Graph");      

		    if (MultiFretIcy.PS.wsBool) {
		    	chartHolder.add(chartPanel);
		    } else {
			    JPanel mainPanel = GuiUtil.generatePanel();
			    mainPanel.add(chartPanel);
			    IcyFrame mainFrame = GuiUtil.generateTitleFrame(XYinfo[0] + " graph", mainPanel, 
			    		new Dimension(300, 100), true, true, true, true);	
			    
		        mainFrame.pack();
		        addIcyFrame(mainFrame);
		        mainFrame.setVisible(true);
		        mainFrame.center();
		        mainFrame.requestFocus();
			    mainFrame.getIcyInternalFrame().setName(XYinfo[0] + " graph");
		    }

		    listFrames();
		}
		
		public void listFrames() {
			JInternalFrame[] allframes = Icy.getMainInterface().getDesktopPane().getAllFrames();
			ArrayList<JInternalFrame> framesList = new ArrayList<JInternalFrame>(Arrays.asList(allframes));
			for (JInternalFrame f : framesList) {
				System.out.println("framnamlister: " + f.getName());
			}
		}
		
		public void sendData() {
			System.out.println("Writing to Excel from chart " + XYinfo[0]);	
//			try {
			MultiFretIcy.PS.wbc.ApplyData(	KVRatio, 
											rawDataPack, 
											XYinfo[0], 
											roi);
			System.out.println("Data applied");
//			} catch (NullPointerException e) {
//				MessageDialog.showDialog("DATA CORRUPTION", 
//						"Warning\nExcel file corrupted due to improper closure."
//						+ "\nRecover or create a new DataSheet.xlsx,"
//						+ " then press OK to continue.", MessageDialog.WARNING_MESSAGE);
//			}
		}
		
		
		
		
		public void exitThis() {
			//TODO temporary troubleshooting outs
			System.out.println("T1");
			System.out.println("\nExiting a thread");
			sendData();
			//TODO temporary troubleshooting outs
			System.out.println("T2");
			//frame.removeFromMainDesktopPane();
			try {
			frame.close();
			} catch (Exception e) {
				System.out.println("Tried to close an already closed Frame.");
			}
			//TODO temporary troubleshooting outs
			System.out.println("T3");
		}
		
	    /**
	     * Round to certain number of decimals
	     * 
	     * @param d
	     * @param decimalPlace
	     * @return
	     */
	    @SuppressWarnings("deprecation")
		public static double round(double d, int decimalPlace) {
	        BigDecimal bd = new BigDecimal(Double.toString(d));
	        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
	        System.out.println(bd.doubleValue());
	        return bd.doubleValue();
	    }
	    
	    public void annotate(String pointer) {
	    	XYPointerAnnotation pointe = new XYPointerAnnotation(pointer, KVRatio.get(0).getX(KVRatio.get(0).getItemCount()-1).doubleValue(),
	    			KVRatio.get(0).getY(KVRatio.get(0).getItemCount()-1).doubleValue(), -45);
	        chartPanel.getChart().getXYPlot().addAnnotation(pointe);
	    }
	    
	    public void preNotate(String pointer, double x, double y) {
	    	XYPointerAnnotation pointe = new XYPointerAnnotation(pointer, KVRatio.get(0).getX(KVRatio.get(0).getItemCount()-1).doubleValue(),
	    			KVRatio.get(0).getY(KVRatio.get(0).getItemCount()-1).doubleValue(), -45);
	        chartPanel.getChart().getXYPlot().addAnnotation(pointe);
	    }
	    
	    public void writeLog(String text) {
	    	System.out.println("Printing to Log: " + logName + "\\" + pos + "\\MFIDataLog.txt"); 

	    	File logFile = new File(logName + "\\" + pos + "\\MFIDataLog.txt");
	    	logFile.getParentFile().mkdirs();
	    	
	    	try(FileWriter fw = new FileWriter(logFile, true);
	    		    BufferedWriter bw = new BufferedWriter(fw);
	    		    PrintWriter out = new PrintWriter(bw))
	    		{
	    		    out.println(text);
	    		} catch (IOException e) {
	    		    e.printStackTrace();
	    		    return;
	    		}
	    }
	}