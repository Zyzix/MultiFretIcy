package plugins.MasoudR.multifreticy.Main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.roi.ROI2DRectangle;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.sequence.SequenceListener;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.AcquiredObject;
import plugins.MasoudR.multifreticy.DataObjects.EzVarIntRoi;
import plugins.MasoudR.multifreticy.DataObjects.InterruptHandler;
import plugins.MasoudR.multifreticy.DataObjects.Milestone;
import plugins.MasoudR.multifreticy.DataObjects.MyWaitNotify;
import plugins.MasoudR.multifreticy.DataObjects.VarIntRoi;
import plugins.MasoudR.multifreticy.DataObjects.CcArgs;
import plugins.MasoudR.multifreticy.DataObjects.CustomCalc;
import plugins.adufour.activecontours.ActiveContours;
import plugins.adufour.activecontours.ActiveContours.ExportROI;
import plugins.adufour.activecontours.ActiveContours.ROIType;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.vars.lang.VarROIArray;

@SuppressWarnings("deprecation")
public class Startup implements SequenceListener, ActionListener, ItemListener, ListSelectionListener {
	
	private IcyFrame			theFrame;
	private	JScrollPane			scrollROI; 
	private JScrollPane			scrollPos;
	protected VarROIArray 		outputROIs;
	EzVarBoolean				timeBool;
	EzVarBoolean				unlinked;
	ArrayList<JCheckBox>		calcs;
	public MyWaitNotify 		mnw;
	
	ArrayList<VarIntRoi> 		RoiNums;
	ArrayList<EzVarIntRoi> 		RoiNums2;
	ArrayList<Threading> 		threads;
	ArrayList<Sequence> 		concSeqList;
	public ArrayList<Milestone>	milestones = new ArrayList<Milestone>();
	
	private int 				inty;	

	//Create Arraylists
	boolean						stopFlag, threadsBooted;
	private ArrayList<Integer> 	royList;
	public InterruptHandler 	iH;
	public MyWaitNotify 		mnw2, mnw3;
	public String[] 			choices;
	ArrayList<String>			mpLabels;
	public int 					offlineNumber = 0;
	public boolean				startReady = false;
	
	public JButton 				msButton, startButton, stopButton, saveButton, detectButton, BGdetectButton;
	private JTextField 			msField, maxIterations;
	private JRadioButton		detRadioMP;
	private JRadioButton		detRadioWF;
	
	private int 				msInt;
	JList<String> 				posL;
	private JPanel 				RPanel;
	private JPanel 				bgSwitches;
	private JPanel				ccSwitches;
	
	String						lastchoiceNum;
	String						lastchoiceDiv;
	Boolean						lastchoiceBool;
	ArrayList<JComboBox<String>>numListList;
	ArrayList<JComboBox<String>>divListList;
	ArrayList<JCheckBox>		RoiBoolList;
	
	public Workspace 			ws;
	
    private Properties			cp;
    public	CustomCalc			cc;
	private Object answer = JOptionPane.CANCEL_OPTION;
	public ArrayList<AcquiredObject> lastBatch;
	
	//Get Rois from split sequences and add to Rois list
	public Startup(ArrayList<Sequence> CSL, String[] possibilities, File calcsFile) throws IOException {			
		cp = new Properties();
		FileInputStream in;
			in = new FileInputStream(System.getProperty("user.home") + "\\MFIoptions.cfg");
			cp.load(in);
			in.close();
		
			
		//Loading in calcsFile
		if (MultiFretIcy.PS.calcBool) {
			cc = new CustomCalc(calcsFile);
		}
			
		theFrame = new IcyFrame("MultiFret", true, true);
		theFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
		theFrame.setMaximisable(true);
		theFrame.setAlwaysOnTop(true);
		theFrame.getContentPane().setLayout(new BoxLayout(theFrame.getContentPane(), BoxLayout.Y_AXIS));
		System.out.println("count frame comp: " + theFrame.getContentPane().getComponentCount());

		concSeqList = CSL;
		choices = possibilities;
		lastchoiceNum = cp.getProperty("DefaultNum", choices[0]);
		lastchoiceDiv = cp.getProperty("DefaultDiv", choices[1]);
		lastchoiceBool = false;

		outputROIs = new VarROIArray("list of ROI");
		mnw = new MyWaitNotify();
		RoiNums = new ArrayList<VarIntRoi>();
		RoiNums2 = new ArrayList<EzVarIntRoi>();
		threads = new ArrayList<Threading>();
		calcs = new ArrayList<JCheckBox>();
		mpLabels = new ArrayList<String>();
		numListList = new ArrayList<JComboBox<String>>();
		divListList = new ArrayList<JComboBox<String>>();
		RoiBoolList = new ArrayList<JCheckBox>();
		
		inty = 0;	
		//Create Arraylists
		
		stopFlag = false;
		threadsBooted = false;
		royList = new ArrayList<Integer>();
		iH = new InterruptHandler();
		mnw2 = new MyWaitNotify();
		mnw3 = new MyWaitNotify();	
	}

	protected void initialize() throws IOException {	//This creates the GUI and sets up RoiNums (containing EzVarIntRois)
		System.out.println("###Initialising####################");
		System.out.println("Custom Calculations: " + MultiFretIcy.PS.calcBool);
		timeBool = new EzVarBoolean("Offline", false);
		// Create corrections frame components
		bgSwitches = new JPanel();
		bgSwitches.setName("Corrections");
		ccSwitches = new JPanel();
		ccSwitches.setName("Corrections");
		
		
		JLabel calcsLabel = new JLabel("Background deduction for channel:");	
		calcsLabel.setName("Corrections");	

		saveButton = new JButton("Save corrections and last ROI channel selection");
		saveButton.setName("Corrections");
		saveButton.addActionListener(this);
				
		// Create detection options panel component
		JPanel detSettings = new JPanel();
		detSettings.setName("Detection");

		// Instructions above the main bit
		JPanel topLevel = new JPanel();
		JLabel textinfo = new JLabel("Draw atleast one ROI and a background ROI");
		topLevel.add(textinfo);

		// Here we create a splitpane for selection of positions, and showing their respective ROI settings
		mpLabels.add("Workspace");
		mpLabels.add("Corrections");			
		mpLabels.add("Detection");			


		if (!MultiFretIcy.PS.offlineBool) {
			for (int i = 0; i < MultiFretIcy.PS.posList.getNumberOfPositions(); i++) {
				mpLabels.add(MultiFretIcy.PS.posList.getPosition(i).getLabel());
			}		
		} else {
			for (Sequence sequence : MultiFretIcy.PS.sequences) {
				mpLabels.add(sequence.getName());
			}
		}
		String[] mpLabelsAr = new String[mpLabels.size()];
		posL = new JList<String>(mpLabels.toArray(mpLabels.toArray(mpLabelsAr)));
        posL.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        posL.addListSelectionListener(this);
        
		RPanel = new JPanel();
		RPanel.setLayout(new BoxLayout(RPanel, BoxLayout.Y_AXIS));
		
		scrollPos = new JScrollPane(posL);
		scrollROI = new JScrollPane(RPanel);
		scrollROI.getVerticalScrollBar().setUnitIncrement(50);  
		scrollPos.getVerticalScrollBar().setUnitIncrement(50);  
		
		JSplitPane posPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,scrollPos,scrollROI);
		posPanel.setOneTouchExpandable(false);
		posPanel.setDividerLocation(75);
		
		System.out.println("Enabling Listeners");
		for (Sequence seq : concSeqList) {
			seq.addListener(this);
			System.out.print(seq.getName() + " activated | ");
		}
		System.out.println("\nListeners Active");

		//Milestone, start, stop button		
		JLabel msLabel = new JLabel("Milestone Name");
		msInt = 1;
		msField = new JTextField("Milestone#" + msInt);
		
		JLabel mitLabel = new JLabel("Max. iterations");
		maxIterations = new JTextField("10000");
		
		msButton = new JButton("Milestone");
		msButton.addActionListener(this);
		
		detectButton = new JButton("Detect");
		detectButton.addActionListener(this);
		
		BGdetectButton = new JButton("BGDetect");
		BGdetectButton.addActionListener(this);
		
		stopButton = new JButton("Stop");
		stopButton.addActionListener(this);
		stopButton.setEnabled(false);

		startButton = new JButton("Start");
		startButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(msLabel);
		buttonPanel.add(msField);		
		buttonPanel.add(msButton);
		buttonPanel.add(mitLabel);
		buttonPanel.add(maxIterations);		
		buttonPanel.add(detectButton);
//		buttonPanel.add(BGdetectButton);
		buttonPanel.add(stopButton);
		buttonPanel.add(startButton);
		
		//Setup and add the panels
		if (!MultiFretIcy.PS.calcBool) {
			for (String s : choices) {
				JCheckBox e = new JCheckBox(s, Boolean.parseBoolean(cp.getProperty(s, "false")));
				e.setName(s + "_bg");
				e.setAlignmentX(Component.LEFT_ALIGNMENT);
				bgSwitches.add(e);
			}
		calcsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		calcsLabel.setVisible(false);
		RPanel.add(calcsLabel);		
		
		bgSwitches.setAlignmentX(Component.LEFT_ALIGNMENT);
		bgSwitches.setVisible(false);
		RPanel.add(bgSwitches);
		} else {		
			for (int x = 0; x < cc.GetNames().size(); x++) {
				String s = cc.GetName(x);
				JCheckBox e = new JCheckBox(s, Boolean.parseBoolean(cp.getProperty(s, "false")));
				e.setName(s);
				cc.SetChoice(x, e);
				System.out.println("Detected CC " + s);
				ccSwitches.add(e);
			}
			
			JLabel ccLabel = new JLabel("Custom corrections:");	
			ccLabel.setName("Corrections");		
			ccLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			ccLabel.setVisible(false);
			RPanel.add(ccLabel);
					
			ccSwitches.setAlignmentX(Component.LEFT_ALIGNMENT);
			ccSwitches.setVisible(false);
			RPanel.add(ccSwitches);					
		}
		
		saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		saveButton.setVisible(false);
		RPanel.add(saveButton);
		
		//Detector settings components	
		JLabel detLabel = new JLabel("Detector settings:");	
		detLabel.setName("Detection");
		detLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		detLabel.setVisible(false);
		RPanel.add(detLabel);
		
		detSettings.add(detRadioMP = new JRadioButton("Max Pixel", true));
		detSettings.add(detRadioWF = new JRadioButton("Whole Frame", false));
		ButtonGroup bg = new ButtonGroup();
		bg.add(detRadioMP);bg.add(detRadioWF);
		detSettings.add(detRadioMP);detSettings.add(detRadioWF);
		
		detSettings.setAlignmentX(Component.LEFT_ALIGNMENT);
		detSettings.setVisible(false);
		RPanel.add(detSettings);
		
		// Workspace builder
		if (MultiFretIcy.PS.wsBool) {
			ws = new Workspace();
			ws.setName("Workspace");
			ws.setVisible(false);
			ws.setAlignmentX(Component.LEFT_ALIGNMENT);
			RPanel.add(ws);		
			
			for (int i = 0; i < MultiFretIcy.PS.S1.concSeqList.size(); i++) {
				MultiFretIcy.PS.S1.concSeqList.get(i).getFirstViewer().getIcyInternalFrame().setVisible(true);
				ws.addComponent(0, i, MultiFretIcy.PS.S1.concSeqList.get(i).getFirstViewer().getIcyInternalFrame());
			}
		}

		theFrame.repaint();
		scrollROI.repaint();
		
		topLevel.setAlignmentX(Component.LEFT_ALIGNMENT);
		theFrame.add(topLevel);
		
		posPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		posPanel.setResizeWeight(0);
		theFrame.add(posPanel);
		
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		theFrame.add(buttonPanel);

		theFrame.addToDesktopPane();
		theFrame.pack();
		theFrame.setVisible(true);
			
		// Tile all frames
		//Tile();
	}

	@Override
	public void sequenceChanged(SequenceEvent event)
	{
		    if (event.getType() == SequenceEventType.ADDED)
		    {
		        // ROI change ?
		        if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI)
		        {
//		        	SwingUtilities.invokeLater(new Runnable(){
//		        		public void run(){
				            ROI2D roy = (ROI2D) event.getSource();
				        	if (!royList.contains(roy.getId())) {
				            String Roiname = "ROI" + String.valueOf(inty);
				            roy.setName(Roiname);
				            roy.setShowName(true);
				            inty += 1;
				            // test roi properties here
				            System.out.println("Roi " + Roiname + " made in " + roy.getFirstSequence()); //this gives all the sequence info, including channel in which it was made
				            
				    		//Generate Roi integer components
				            JPanel roiPanel = new JPanel();
				            roiPanel.setVisible(false);
				            roiPanel.setName(roy.getFirstSequence().getName());
				            	            			            
	         	            JCheckBox RoiBool = new JCheckBox("Background", lastchoiceBool);
	         	            RoiBool.setName("bg541a364");
	         	            RoiBool.addItemListener(this);
	         	            
	         	            Set<JCheckBox> calcChecks = new LinkedHashSet<JCheckBox>();	     
	         	           if (!MultiFretIcy.PS.calcBool) {
	         	        	   calcChecks.add(RoiBool);
	         	           } else {
	         	        	   System.out.println("args number: " + cc.GetArgs().size());
		         	            for (int i = 0; i < cc.GetArgs().size(); i++) {		
		         	            	String[] a = cc.GetArg(i);
		         	            	for (int x = 0; x < a.length; x++) {  
		         	            		System.out.println("Generating CC arg checkbox: " + cc.GetName(i));
		         	            		JCheckBox b = new JCheckBox(a[x], false);
		         	            		b.setName(cc.GetName(i));
		         	            		b.addItemListener(this);
			        	            	calcChecks.add(b);		         	            	
		         	            	}
		         	            }
	         	           }
	         	            
	         	            // Package the vars for this ROI
	         	           	JLabel jl = new JLabel(Roiname);
	         	            roiPanel.add(jl);
	         	            
				            JComboBox<String> numList = null;
				            JComboBox<String> divList = null;
	         	            
				            // Set up ratio selection controls
				            if (!MultiFretIcy.PS.calcBool) {
				            numList = new JComboBox<String>(choices);
				            divList = new JComboBox<String>(choices);
				            numList.setSelectedItem(lastchoiceNum);
				            divList.setSelectedItem(lastchoiceDiv);

				            numList.addItemListener(this);
				            divList.addItemListener(this);
				            numListList.add(numList);
				            divListList.add(divList);
				            
			    			roiPanel.add(numList);
			    			roiPanel.add(divList);
				            }
				            
			    			for (JCheckBox j : calcChecks) {
			    				roiPanel.add(j);
			    			}
			    			
			    			roiPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			    			
			    			VarIntRoi RoiNum = new VarIntRoi(roy, calcChecks, numList, divList, roy.getFirstSequence().getName());
			    			
			    			RPanel.add(roiPanel);
			    			roiPanel.setMaximumSize(roiPanel.getPreferredSize());
			    			
			    			//switch selection
			    			if (!MultiFretIcy.PS.wsBool) {
				    			for (int n = 0; n < mpLabels.size(); n++) {
				    				if (mpLabels.get(n).equals(roy.getFirstSequence().getName())) {
				    					posL.setSelectedIndex(n);
				    				}
				    			}
				    			updateRoiSelection(roy.getFirstSequence().getName());
			    			}
			    			
			    			BGdisabler(); //includes repaint
			    			
			    			//Update variables
			    			RoiNums.add(RoiNum);
			    			royList.add(roy.getId());
				        	}
//		        		}
//		        	});

	          }
	        }

		    if (event.getType() == SequenceEventType.REMOVED)
		    	{
		        if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI)
		        	{
		        	ROI2D roy = (ROI2D) event.getSource();
		        	String Roiname = roy.getName();
		            System.out.println("Deleting Roi " + Roiname);
		            removeROI(Roiname);
		            
	            	for (int i = 0; i < royList.size(); i++) {
	            		if (royList.get(i) == roy.getId()){
	            			royList.remove(i);
	            			System.out.println("Removed ROI ID: " + roy.getId());
	            		}
	            	}
	            	for (VarIntRoi rn : RoiNums) {
	            		if (roy.getName().equals(rn.Roi2D.getName())){
	            			RoiNums.remove(rn);
	            			System.out.println("Removed ROInum: " + roy.getName());
	            			break;
	            		}
	            	}
		        }
		    }
	}		    
	
	// Run the program
	protected void Execute() throws FileNotFoundException {
		// Set console output to log
		//PrintStream out = new PrintStream(
		//        new FileOutputStream(GetLogPath("log").toString(), true), true);
		//System.setOut(out);
		//System.setErr(out); //TODO:
		System.out.println("###Executing####################");
		
		// Check enabled custom calculations and extract formula information
		Map<String,ArrayList<CcArgs>> calcArgs = new HashMap<>(); //Key = calcName, Value = formula/args
		
		if (MultiFretIcy.PS.calcBool) {
	 		for (Component a : ccSwitches.getComponents()) {
	 			JCheckBox b;
	 			if (a instanceof JCheckBox) {
	 				b = (JCheckBox) a;
	 			
		 			for (int c = 0; c < cc.GetNames().size(); c++) {
		 				if (cc.GetName(c).equals(b.getName()) && b.isSelected()){
		 					System.out.println("Mapping " + cc.GetName(c));
		 					calcArgs.put(cc.GetName(c), GetArgs(cc.GetFormula(c)));
		 				}
		 			}
	 			}
	 		}
		}
		
		// Detect background ROI selection
		for (Sequence sequence : concSeqList) {		
			
			ArrayList<ROI2D> calcROIS = new ArrayList<ROI2D>();
			System.out.println("Processing ROI for " + sequence.getName());
			ROI2D bgROI = null;
			// Name bgROIs
			for (VarIntRoi rei : RoiNums) {
				if (rei.position.equals(sequence.getName())) {	
					for (JCheckBox j : rei.getCheckBoxList()) {
						if (j.isSelected() == true && j.getName().equals("bg541a364") == false) {
							sequence.removeListener(this);	
							rei.Roi2D.setName(j.getText());
							calcROIS.add(rei.Roi2D);
							sequence.addListener(this);	
						} else if (j.isSelected() == true && j.getName().equals("bg541a364") == true) {
							if (!MultiFretIcy.PS.calcBool && bgROI != null) {
								MessageDialog.showDialog("Multiple backgrounds selected!"); 
								startButton.setEnabled(true);
								stopButton.setEnabled(false);
								return;
							}
							sequence.removeListener(this);	
							rei.Roi2D.setName("bgROI");
							bgROI = rei.Roi2D;
							sequence.addListener(this);	
						}
					}
				}
			}
			//Ready Check
			if (!MultiFretIcy.PS.calcBool && bgROI == null) {
				MessageDialog.showDialog("no background selected"); 
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				return;
			}
			if (MultiFretIcy.PS.calcBool) {
				boolean ready = false;
				for (JCheckBox jc : cc.GetChoices()) {
		        	if (jc.isSelected()) {
		        		ready = true;
		        	}
				}
				if (!ready) {
					MessageDialog.showDialog("no calculation selected"); 
					startButton.setEnabled(true);
					stopButton.setEnabled(false);
					return;
				}
			}
			
			
			sequence.removeListener(this);	
			
			// Get path for unused log folder
			Path logName = GetLogPath("data");
			
			// Get designated channels and create threads
			
			//Open threads
			Threading R1 = new Threading (	sequence,
											RoiNums,
										 	timeBool, 
										 	mnw,
										 	mnw2,
										 	iH,
										 	bgROI,
										 	calcROIS,
										 	calcArgs,
										 	cc,
										 	bgSwitches,
										 	logName);
			threads.add(R1);
		}
		
		if (!MultiFretIcy.PS.offlineBool) {
			try {
				System.out.println("SU: RUNQ");
				startReady = true; 
				MultiFretIcy.PS.QR.RunQueue();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			startReady = true; 
			try {
				System.out.println("SU-o: RUNQ");
				MultiFretIcy.PS.QR.RunQueue();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
}

		public void Tile() {	 
			System.out.println("Tiling frames…");
			JDesktopPane desk = Icy.getMainInterface().getDesktopPane();
			
	        // How many frames do we have?
			JInternalFrame[] allframes = Icy.getMainInterface().getDesktopPane().getAllFrames();
			ArrayList<JInternalFrame> framesList = new ArrayList<JInternalFrame>(Arrays.asList(allframes));
			List<JInternalFrame> toRemove = new ArrayList<JInternalFrame>();
			for (JInternalFrame f : framesList) {
			    if (f.getName() == null) {
			        toRemove.add(f);
			    }
			}
			framesList.removeAll(toRemove);
			Collections.sort(framesList, new Comparator<JInternalFrame>(){
				@Override
				public int compare(JInternalFrame f2, JInternalFrame f1)
				{
					return f1.getName().compareTo(f2.getName());
				}
			});
			
			int count = framesList.size();
	        if (count == 0) return;
	        
	        double AR = 0.54; // Height / width of graph 
	         
	        // Determine the necessary grid size
	        int sqrt = (int)Math.sqrt(count);
	        int rows = sqrt;
	        int cols = sqrt;
	        if (rows * cols < count) {
	            cols++;
	            if (rows * cols < count) {
	                rows++;
	            }
	        }
	         
	        // Define some initial values for size & location.
	        Dimension size = desk.getSize();
	         
	        int w = size.width / cols;
	        int h = size.height / rows;
	        int x = 0;
	        int y = 0;
	         
	        System.out.printf("y: %s%ncols: %s, rows: %s%nh: %s, w: %s, AR: %s", y, cols, rows,h,w,AR);

	        
	        // Iterate over the frames, deiconifying any iconified frames and then
	        // relocating & resizing each.
	        for (int i = 0; i < rows; i++) {
	            for (int j = 0; j < cols && ((i * cols) + j < count); j++) {
	                JInternalFrame f = framesList.get((i * cols) + j);
	                if (!f.isClosed() && f.isIcon()) {
	                	continue;
//	                    try {
//	                        f.setIcon(false);
//	                    } catch (PropertyVetoException ignored) {}
	                }
                	System.out.println("framnam:" + f.getName());
	             
	                if (f.getName() != null && f.getName().contains("graph")) {
		                desk.getDesktopManager().resizeFrame(f, x, (int) (y + (h-w*AR)), w, (int) (w*AR));
	                	System.out.println(x + " " + (int) (y + (h-w*AR)) + " " + w + " " + (int) (w*AR));
	                } else if (f.getName() != null && f.getName().contains("concS")) {
		                desk.getDesktopManager().resizeFrame(f, x, y, w, h);
	                	System.out.println(x + " " + y + " " + w + " " + h);
	                }
	                x += w;	            
	            }
	            y += h; // start the next row
	            x = 0;
	        }
		}

	
		public int GetChannel(String in) {
			int out = 0;
			for (int r = 0; r < MultiFretIcy.PS.S1.rois.size(); r++) {
				ROI roi = MultiFretIcy.PS.S1.rois.get(r);
				if (roi.getName().equals(in)) {
					out = r;
					System.out.println("found channel: " + roi.getName());
				}
			}
			return out;
		}
	
	   public ArrayList<CcArgs> GetArgs(String f) {
		   ArrayList<CcArgs> argsList = new ArrayList<CcArgs>();
		   
		   String regex = "(\\w*)\\[(\\w*?)\\]";
		   Pattern pattern = Pattern.compile(regex);
		   Matcher matcher = pattern.matcher(f);
 
		   int matchCount = 0;
		   while (matcher.find()) {
			   matchCount++;
			   System.out.printf("Match count: %s, Group Zero Text: '%s'%n", matchCount,
					   matcher.group());
			   for (int i = 1; i <= matcher.groupCount(); i++) {
				   System.out.printf("Capture Group Number: %s, Captured Text: '%s'%n", i, matcher.group(i));
   				}	
			   argsList.add(new CcArgs(matcher.group(1),GetChannel(matcher.group(2))));				   
   			}
		   return argsList;
		}			
	
	
	public void stopExecution()
	{
		answer = JOptionPaneTest3();
		if (answer.equals(JOptionPane.CANCEL_OPTION) || answer.equals(null)) { //TODO: cancel option isn't assigned?
			System.out.println("Exit Cancelled");
			return;
		} else {
		//TODO temporary troubleshooting outs
		System.out.println("S1");
		//When STOP button is pressed
		MultiFretIcy.PS.QR.ExitThis();
		//TODO temporary troubleshooting outs
		System.out.println("S2");
//		MultiFretIcy.PS.ExitThis();	//TODO not needed? already in QT?
		//TODO temporary troubleshooting outs
		System.out.println("S3");
		//ExitThis();
		}
	}

	public Object JOptionPaneTest3 () {
		    JDialog.setDefaultLookAndFeelDecorated(true);
		    Object[] selectionValues = { "Exit MultiFret, stop acquisition and close image stacks", "Exit MultiFret and stop acquisition", "Exit MultiFret" };
		    String initialSelection = "Exit MultiFret, stop acquisition and close image stacks";
		    Object selection = JOptionPane.showInputDialog(null, "Stop the amazing experiment?",
		        "Confirm Stop", JOptionPane.QUESTION_MESSAGE, null, selectionValues, initialSelection);
		   return(selection);
		  }

	
public void ExitThis() {
		switch(answer.toString()) {
			case "Exit MultiFret, stop acquisition and close image stacks":
//				for (String ac : MultiFretIcy.PS.mStudio.getAcquisitionNames()) { //These don't work in icy...
//					System.out.println("ac: " + ac);
//					try {
//						MultiFretIcy.PS.mStudio.closeAcquisitionWindow(ac);
//					} catch (MMScriptException e) {
//						e.printStackTrace();
//					}
//				}
//				MultiFretIcy.PS.mStudio.closeAllAcquisitions();
				for (Viewer v : Icy.getMainInterface().getViewers()) {
					if (v.getTitle().contains("Acquisition - ")) {
						v.dispose();
					}
				}

				if(!MultiFretIcy.PS.offlineBool){MultiFretIcy.PS.mStudio.stopAllActivity();}
			case "Exit MultiFret and stop acquisition":
				//MultiFretIcy.PS.mStudio.closeAllAcquisitions();
				if(!MultiFretIcy.PS.offlineBool){MultiFretIcy.PS.mStudio.stopAllActivity();}
			case "Exit MultiFret":
				//
			}
	
	
	//TODO temporary troubleshooting outs
	System.out.println("S4");
	stopFlag = true;
	//TODO temporary troubleshooting outs
	System.out.println("S5");
	try {
		theFrame.close();
		//TODO temporary troubleshooting outs
		System.out.println("S6");
	} catch (Exception e) {System.out.println("SU UI already closed");}
	for (Sequence concS : concSeqList) {
		concS.close();
		//TODO temporary troubleshooting outs
		System.out.println("S7");
	}
	theFrame.dispose();
	//TODO temporary troubleshooting outs
	System.out.println("S8");
}

	public Map<EzVarIntRoi, EzVarIntRoi> dupCheck(ArrayList<EzVarIntRoi> RoiNumbs) {
		//Usage of Map tools to create and return pairs of ROI
		Map<EzVarIntRoi, EzVarIntRoi> Pairs = new HashMap<>();

		for (int j=0; j<RoiNumbs.size(); j++) {
			for (int k=j+1;k<RoiNumbs.size();k++) {
	//			System.out.println(RoiNumbs.get(j)+" versus "+RoiNumbs.get(k));
				if (k!=j && RoiNumbs.get(k).getValue() == RoiNumbs.get(j).getValue()) {
					
					Pairs.put(RoiNumbs.get(j),(RoiNumbs.get(k))); //Store linked variables
				
				}
			}
    	}
		return Pairs;
	}

	public boolean ThreadsWaiting() {
		//True if Splitter is equal or higher than all threads, threads are paused
		//False if splitter is lower than threads, threads are active
		boolean threadsWait = true;
		for (Threading aThread : threads) {
			if (aThread.count <  MultiFretIcy.PS.S1.count) {
				System.out.println(aThread.count + " vs " + MultiFretIcy.PS.S1.count);
				threadsWait = false;
			}
		}
		System.out.println("threads waiting: " + threadsWait);
		return threadsWait;
	}
	
	public void RunThreads() {
			for (@SuppressWarnings("unused") Threading aThread : threads) {
				mnw2.doNotifyAll();
			}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == msButton /*&& startReady*/) { 
			System.out.println("MileStone pressed");
			
			long time = 0;
			long frame = 0;
			// Get frames and time from either the sequence+acqobj or the thread TODO (do we need both)
			if (threads.size()==0) { //Prenotation mode
				for (AcquiredObject ao : lastBatch) {
					ao.prenotation = msField.getText();
					time = (ao.time-MultiFretIcy.PS.startTime)/1000000000;
				}					
//				MultiFretIcy.PS.QR.AcqObjs.get(MultiFretIcy.PS.QR.AcqObjs.size()-1).prenotation = msField.getText();
				for (int i = 0; i < MultiFretIcy.PS.QR.AcqObjs.size(); i++) {
					if (MultiFretIcy.PS.QR.AcqObjs.get(i).position.equals(MultiFretIcy.PS.QR.AcqObjs.get(0).position)) {
						frame++;
					}
				}					
			} else {
				frame = threads.get(0).count;
				time = threads.get(0).timeD;
			}
			
			System.out.println(time == 0 ? "at frame " + frame : "at frame " + frame + " and time " + time);
			System.out.println("ms text= " + msField.getText());
			if(msField.getText() == null || msField.getText().equals("")) {msField.setText("Milestone#" + msInt);}
			msInt += 1;

			
			Milestone ms = new Milestone(msField.getText(), frame);
			milestones.add(ms);
			
			for (Threading t : threads) {
				t.annotate(msField.getText());
			}			
			JLabel recordedmsLabel = new JLabel(time != 0 ? msField.getText() + " at: " + frame + "(" + time + "s)" : msField.getText() + " at: " + frame);
			recordedmsLabel.setBackground(new Color(0,255,255));
			theFrame.add(recordedmsLabel);
			msField.setText("MileStone#" + msInt);
			theFrame.revalidate();
			theFrame.repaint();
			//Write to backup data log
			WriteLog(msField.getText() + " at: " + frame + "(" + time + "s)");
			
		} else if (e.getSource() == detectButton) {
			detectCells(MultiFretIcy.PS.S1.concSeqList, false);		
		} else if (e.getSource() == BGdetectButton) {
			detectCells(MultiFretIcy.PS.S1.concSeqList, true);	
		} else if (e.getSource() == startButton) {
			// Start the analysis
			startButton.setEnabled(false);
			stopButton.setEnabled(true);		
	
			try {
				Execute();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return;
			}
			
		} else if (e.getSource() == stopButton) {
			// Stop the analysis
			stopExecution();
			
		} else if (e.getSource() == saveButton) {
			// Save both the checked corrections as well as the last selection of ROI channels
			SaveCorrections();
			saveButton.setText("Saved");
		}
	}

	public void addPos(ArrayList<Sequence> sL, String[] pS) {
		
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		// Auto-generated method stub
		
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
	       JList<?> list = (JList<?>)e.getSource();
	       updateRoiSelection(list.getSelectedValue().toString());
	}
	
	protected void updateRoiSelection(String pos) {
		//show the correct roi panel thats in scrollroi
		System.out.println("Selected: " + pos);
		for (int x = 0; x < RPanel.getComponentCount(); x++) {
			if (RPanel.getComponent(x).getName().equals(pos)) {
				RPanel.getComponent(x).setVisible(true);
			} else {
				RPanel.getComponent(x).setVisible(false);
			}
		}
		scrollROI.revalidate();
		scrollROI.repaint();
	}

	protected void removeROI(String roi) {
		//Remove a deleted ROI from the interface
		for (int x = 0; x < RPanel.getComponentCount(); x++) {
			if (RPanel.getComponent(x) instanceof Container) {
				Container subContainer = (Container)RPanel.getComponent(x);
				for (int y = 0; y < subContainer.getComponentCount(); y++) {
					if (subContainer.getComponent(y) instanceof JLabel) {
						if (((JLabel)subContainer.getComponent(y)).getText().equals(roi)) {
							RPanel.remove(subContainer);
						}
					}
				}
			}	
		}
		scrollROI.revalidate();
		scrollROI.repaint();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// Checkbox handler
		if (e.getSource() instanceof JCheckBox && !((JCheckBox) e.getSource()).getText().equals("bg541a364")) {	
			
			//turn off other Checkboxes 
			if (((JCheckBox) e.getSource()).isSelected()){
				for (Component j : ((JCheckBox) e.getSource()).getParent().getComponents()) {
					if (j instanceof JCheckBox) {
						((JCheckBox) j).removeItemListener(this);					
						if (j != e.getSource()) {
							j.setEnabled(false);
						}
						((JCheckBox) j).addItemListener(this);
					}
				}
				scrollROI.revalidate();
				scrollROI.repaint();

			//turn on other Checkboxes 
			} else {
				for (Component j : ((JCheckBox) e.getSource()).getParent().getComponents()) {
					if (j instanceof JCheckBox) {
						((JCheckBox) j).removeItemListener(this);
						if (j != e.getSource()) {
							j.setEnabled(true);
						}
						((JCheckBox) j).addItemListener(this);
					}
				}
				scrollROI.revalidate();
				scrollROI.repaint();
			}
			
			// Set lastchoice and run bgdisabler
			if (((JCheckBox) e.getSource()).getName().equals("bg541a364")) {
				lastchoiceBool = ((JCheckBox) e.getSource()).isSelected();			
				BGdisabler();
			}
		}
		
		// JComboBox handler
		if (e.getSource() instanceof JComboBox) {
			for (JComboBox<String> source : numListList) {
				if (source == e.getItemSelectable()) {
					lastchoiceNum = source.getSelectedItem().toString();
					return;
				} 
			}
			for (JComboBox<String> source : divListList) {
				if (source == e.getItemSelectable()) {
					lastchoiceDiv = source.getSelectedItem().toString();
					return;
				}
			}
		}
		
	}
	
	private void BGdisabler() {
		//Disables all comboboxes that are designated as background
		for (int x = 0; x < RPanel.getComponentCount(); x++) {
			if (RPanel.getComponent(x) instanceof Container) {
				Container subContainer = (Container)RPanel.getComponent(x);
				for (int y = 0; y < subContainer.getComponentCount(); y++) {	
					System.out.println("checkbox::: " + subContainer.getComponent(y).getName());
					if (subContainer.getComponent(y) instanceof JCheckBox						
							&& subContainer.getComponent(y).getName().equals("bg541a364")) {
						if (((JCheckBox) subContainer.getComponent(y)).isSelected() == true) {
							for (int z = 0; z < subContainer.getComponentCount(); z++) {
								System.out.println("checkbox2::: " + subContainer.getComponent(z).getName());
								if (subContainer.getComponent(z) instanceof JComboBox) {
									subContainer.getComponent(z).setEnabled(false);
								}
							}
						} else { 
							for (int z = 0; z < subContainer.getComponentCount(); z++) {
								if (subContainer.getComponent(z) instanceof JComboBox) {
									subContainer.getComponent(z).setEnabled(true);
								}								
							}
						}
					}
				}
			}	
		}
		scrollROI.revalidate();
		scrollROI.repaint();
	}
	
	private Path GetLogPath(String name) {
		//Makes a path to %user%/MFI/<name>
 		Integer folderNum = 0;
 		Date date = new Date();
 	    SimpleDateFormat ft = 
 	    	     new SimpleDateFormat ("yyyy-MM-dd");
 		Path logName = Paths.get(System.getProperty("user.home") + "\\MFI\\" + name
 	    	     			+ ft.format(date) + "_" + folderNum.toString());
 		//Create directory if needed
 		File file = new File(logName.toString());
 		file.getParentFile().mkdirs();
 		//Number files //TODO: this is detrimental to the backupwrite milestone.
 		while (Files.exists(logName)) {
 			folderNum++;
 			logName = Paths.get(System.getProperty("user.home") + "\\MFI\\" + name 
 	     			+ ft.format(date) + "_" + folderNum.toString());
 		}
 		return logName;
	}
	
	private void SaveCorrections() {
		//Save corrections
		for (Component jc : bgSwitches.getComponents()) {
			if (jc instanceof JCheckBox) {
				cp.setProperty(((JCheckBox) jc).getName(), String.valueOf(((JCheckBox) jc).isSelected()));
			}
		}
		if(MultiFretIcy.PS.calcBool) {
			for (JCheckBox jc : cc.GetChoices()) {
	        	cp.setProperty(jc.getName(), String.valueOf(jc.isSelected()));
			}
		}
		
		
		//Save defaults
		cp.setProperty("DefaultNum", lastchoiceNum);
		cp.setProperty("DefaultDiv", lastchoiceDiv);		
		
		//Store config file
    	try {
			cp.store(new FileOutputStream(System.getProperty("user.home") + "\\MFIoptions.cfg"), null);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
    public void WriteLog(String text) {
    	Path ln = GetLogPath("data");
    	System.out.println("Printing to Log: " + ln + "\\" + "milestones" + "\\MFIDataLog.txt"); 

    	File logFile = new File(ln + "\\" + "milestones" + "\\MFIDataLog.txt");
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
    
    private void detectCells(Sequence seq, Boolean bg) {
		
		//Create detection ROI for BG or cell detection
		ROI rareROI = null;
		
		if (!bg && detRadioMP.isSelected()) 
		{ 
			seq.removeAllROI();
			rareROI = createROI(seq, maxPixel(seq.getFirstImage()));
		} 
		else if (bg && detRadioMP.isSelected()) 
		{
			lastchoiceBool = true;
			 rareROI = createROI(seq, maxPixel(seq.getFirstImage()));
			BGdisabler();
		} 
		else if (!bg && detRadioWF.isSelected()) 
		{
			seq.removeAllROI();
			rareROI = createROI(seq);
		} 
		else if (bg && detRadioWF.isSelected()) 
		{
			lastchoiceBool = true;
			 rareROI = createROI(seq);
			BGdisabler();
		}

		//TODO getfirstimage here might not always be good		
				
		ActiveContours myAC = new ActiveContours();
		myAC.createUI();
		myAC.hideUI();
		ROI[] r = {rareROI};
		
//		myAC.roiInput.add(rareROI);
		myAC.roiInput.setValue(r); //TODO: not quite working? On the third bgDetect press there's a mess of shit instead of 3
		
		System.out.println("size roiinput: " + myAC.roiInput.size()); 

		myAC.input.setValue(seq);
		myAC.convergence_nbIter.setValue(Integer.parseInt(maxIterations.getText())); //default is 100,000 but that may take too long.
//		myAC.regul_weight.setValue(value);
//		myAC.edge
//		myAC.edge_c
		if (!bg) {myAC.edge_weight.setValue(1.0);} else {myAC.edge_weight.setValue(-1.0);}
//		myAC.region
//		myAC.region_c
//		myAC.region_weight
//		myAC.region_sensitivity 
//		myAC.region_localise
//		myAC.balloon_weight 
//		myAC.axis_weight 
//		myAC.coupling_flag 
//		myAC.evolution 
//		myAC.evolution_bounds  
//		myAC.contour_resolution 
//		myAC.contour_timeStep 
//		myAC.convergence_winSize 
//		myAC.convergence_operation 
//		myAC.convergence_criterion 

		myAC.output_rois.setValue(ExportROI.ON_INPUT);
		myAC.output_roiType.setValue(ROIType.POLYGON);
		myAC.execute();
		myAC.stopExecution();
		myAC.clean();
		myAC.getUI().close();
		System.out.println("Deleting ROI " + rareROI.getName());
		seq.removeROI(rareROI);
	}		  
	
	private void detectCells(ArrayList<Sequence> sList, Boolean bg) {
		for (Sequence seq : sList) {
			if (seq != null) {
				detectCells(seq, bg);
				}
			}
		}
	
	private ROI createROI(Sequence seq) {
	    {
	        // Check if sequence exists.
	        if (seq == null)
	        {
	            MessageDialog.showDialog("Please open a sequence to use this plugin.", MessageDialog.WARNING_MESSAGE);
	            return null;
	        }

	        // Get the image at t=0 and z=0
	        IcyBufferedImage image = seq.getImage(0, 0);

	        // Check if the image exists
	        if (image == null)
	        {
	            MessageDialog.showDialog("No image is present at t=0 and z=0.", MessageDialog.WARNING_MESSAGE);
	            return null;
	        }

	        // create ROI
	        ROI2DRectangle roi = new ROI2DRectangle(10, 10, seq.getWidth()-20, seq.getHeight()-20);; //TODO: center properly

	        // add the roi to the sequence
	        seq.addROI(roi);
	        return roi;
	    }
	}
	
	private ROI createROI(Sequence seq, Point2D coords) {
	    {
	        // Check if sequence exists.
	        if (seq == null)
	        {
	            MessageDialog.showDialog("Please open a sequence to use this plugin.", MessageDialog.WARNING_MESSAGE);
	            return null;
	        }

	        // Get the image at t=0 and z=0
	        IcyBufferedImage image = seq.getImage(0, 0);

	        // Check if the image exists
	        if (image == null)
	        {
	            MessageDialog.showDialog("No image is present at t=0 and z=0.", MessageDialog.WARNING_MESSAGE);
	            return null;
	        }
	        
	        System.out.println("Width: " + coords.getX() + " " + seq.getFirstImage().getWidth());
	        System.out.println("Height: " + coords.getY() + " " + seq.getFirstImage().getHeight());

	        
	        // create ROI
	        double maxx = coords.getX()+5 < seq.getFirstImage().getWidth() ? coords.getX()+5 : seq.getFirstImage().getWidth();
	        double maxy = coords.getY()+5 < seq.getFirstImage().getHeight() ? coords.getY()+5 : seq.getFirstImage().getHeight();
	        double minx = coords.getX()-5 < 0 ? coords.getX()-5 : 0;
	        double miny = coords.getY()-5 < 0 ? coords.getY()-5 : 0;

	        ROI2DRectangle roi = new ROI2DRectangle(minx, miny, maxx, maxy); //TODO: make option for size?

	        // add the roi to the sequence
	        seq.addROI(roi);
	        return roi;
	    }
	}
	
	public Point2D maxPixel (IcyBufferedImage image) {
	        // get every pixel and average it
	        Point2D max = null;
	        double current = 0;
	        System.out.println("Iterating " );
		        for (int x = 0;	x < image.getSizeX(); x++)
		        {
		            for (int y = 0; y < image.getSizeY(); y++)
		            {		                
	                    if (current < image.getData(x, y, 0)) 
	                    {
	                    	current = image.getData(x, y, 0);
	                    	max = new Point2D.Double(x, y); //TODO probably better as a float
	                    }
		                
		            }
		        }
		       // System.out.println("pixel count: " + sample + " - mean intensity over ROI " + Roi.getName() + ": " + (mean / sample));
		        System.out.println("Highest value pixel X:Y is " + max.getX() + ":" + max.getY());
		   return (max);		   
	   }
	
	   @SuppressWarnings("null")
	public Point2D minPixel (IcyBufferedImage image) {
	        // get every pixel and average it
	        Point2D min = null;
	        double current = 0;
	        System.out.println("Iterating " );
		        for (int x = 0;	x < image.getSizeX(); x++)
		        {
		            for (int y = 0; y < image.getSizeY(); y++)
		            {		                
	                    if (current > image.getData(x, y, 0)) 
	                    {
	                    	current = image.getData(x, y, 0);
	                    	min.setLocation(x, y);
	                    }
		                
		            }
		        }
		       // System.out.println("pixel count: " + sample + " - mean intensity over ROI " + Roi.getName() + ": " + (mean / sample));
		        System.out.println("Lowest value pixel X:Y is " + min.getX() + ":" + min.getY());
		   return (min);		   
	   }
}
