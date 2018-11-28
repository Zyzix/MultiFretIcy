package plugins.masoud.multifreticy;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import com.ochafik.lang.Threads;

import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.sequence.SequenceListener;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGUI;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;
import plugins.adufour.vars.lang.VarROIArray;
import poi.CreateWorkBook;

public class Startup extends EzPlug implements Block, EzStoppable, SequenceListener, ActionListener {
	
	protected VarROIArray 		outputROIs;
	EzVarBoolean				timeBool;
	EzVarBoolean				unlinked;
	public MyWaitNotify 		mnw;
	
	ArrayList<EzVarIntRoi> 		RoiNums;
	ArrayList<EzVarIntRoi> 		RoiNums2;
	ArrayList<Threading> 		threads;
	ArrayList<Sequence>			sequences;
	public ArrayList<Long>		milestones = new ArrayList<Long>();
	
	private int 				inty;	

	//Create Arraylists
	ArrayList<ROI2D> 			Rois; //get ROIs from active image 
//TODO make it not explode when you don't have an image with ROIs open. 
	
	boolean						stopFlag, threadsBooted;
	private ArrayList<Integer> 	royList;
	public InterruptHandler 	iH;
	public MyWaitNotify 		mnw2, mnw3;
	private ROI2D 				bgROI;
	public String[] 			choices;
	public int 					offlineNumber = 0;
	public boolean				startReady = false;
	
	public EzButton msButton;
	
	private ArrayList<EzGroup>	EzGroups = new ArrayList<EzGroup>();
	
	//Get Rois from split sequences and add to Rois list
	public Startup(ArrayList<Sequence> seqList, String[] possibilities) {
		choices = possibilities;
		outputROIs = new VarROIArray("list of ROI");
		mnw = new MyWaitNotify();
		RoiNums = new ArrayList<EzVarIntRoi>();
		RoiNums2 = new ArrayList<EzVarIntRoi>();
		threads = new ArrayList<Threading>();
		sequences = new ArrayList<Sequence>();
		inty = 0;	
		//Create Arraylists
		Rois = null; //get ROIs from active image 
	//TODO make it not explode when you don't have an image with ROIs open. 
		
		stopFlag = false;
		threadsBooted = false;
		royList = new ArrayList<Integer>();
		iH = new InterruptHandler();
		mnw2 = new MyWaitNotify();
		mnw3 = new MyWaitNotify();
		sequences = seqList;
		for (Sequence seq : seqList) {
			for (ROI2D r : seq.getROI2Ds()) {
				Rois.add(r);
			}
		}
	}


	@Override
	public void declareInput(VarList inputMap) {
		//Auto-generated method stub
	}

	@Override
	public void declareOutput(VarList outputMap) {
		//Auto-generated method stub
	}

	@Override
	public void clean() {
		//TODO:put stuff here
	}

	@Override
	protected void execute() {
		// Run the program
		System.out.println("###Executing####################");
		
		// Read ROIs, copy background rois to all sequences
		for (EzVarIntRoi rei : RoiNums) {
			if (rei.EVIBool.getValue() == true) {
				bgROI = rei.EVIRoi;
				bgROI.setName("bgROI");
				bgROI.remove();
				for (Sequence s : sequences) {s.addROI(bgROI);}
			}
		}
		
		if (bgROI == null) {MessageDialog.showDialog("no background selected"); return;}
		
		for (EzVarIntRoi rei : RoiNums) {
			if (rei.EVIBool.getValue() == false) {
				// Copy ROI to ROI-D[x] where x is the number of divisor sequence, same for ROI-N[y]
				Sequence numy = null;
				Sequence divy = null;
				for (Sequence s : sequences) {
					System.out.println(s.getName() + " num: " + rei.EVINum.getValue() + " div: " + rei.EVIDiv.getValue());
					if (s.getName().equals(rei.EVINum.getValue())) {
					numy = s;
					}
					if (s.getName().equals(rei.EVIDiv.getValue())) {
					divy = s;
					}
				}
				//Remove listeners to not trigger upon moving the ROIs
				numy.removeListener(Startup.this);
				divy.removeListener(Startup.this);
				for (Sequence seq : sequences) {
					seq.removeListener(this);
				}
				//Remove the ROIs from everywhere
				rei.EVIRoi.remove();
				//Add ROIs to selected sequences and add the listeners back
				if ( unlinked.getValue() == true ) {
					ROI2D nRoi = (ROI2D) rei.EVIRoi.getCopy();
					ROI2D dRoi = (ROI2D) rei.EVIRoi.getCopy();
					
					numy.addROI(nRoi);
					divy.addROI(dRoi);
					} else {
					numy.addROI(rei.EVIRoi);
					divy.addROI(rei.EVIRoi);
					}
				numy.addListener(Startup.this);
				divy.addListener(Startup.this);	
				
//		        EzGUI g = getUI(); TODO: remove unnecessary components after startup
//		        if (g != null) {
//		        	for (Component c : g.getInternalFrame().getComponents()) {
//		        		System.out.println(c.getName());
//		        		g.remove(c);
//		        	}
//		        }
			
			//Milestone button
//				msButton = new EzButton("Milestone", this);
//				addEzComponent(msButton);
//
//				EzGUI g = getUI();
//				g.repack(true);
				
			//Open threads
				Threading R1 = new Threading (	rei, 
												numy, 
											 	divy, 
											 	timeBool, 
											 	mnw,
											 	mnw2,
											 	iH,
											 	bgROI);
				threads.add(R1);
				}

			}
		//Milestone button
		msButton = new EzButton("Milestone", this);
		addEzComponent(msButton);

		EzGUI g = getUI();
		g.repack(true);
		if (!Prestart.offlineBool) {
			try {
				System.out.println("SU: RUNQ");
				startReady = true; 
				Prestart.QR.RunQueue();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			startReady = true; 
		try {
			System.out.println("SU-o: RUNQ");
			Prestart.QR.RunQueue();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}
		//Stop button stuff
		stopFlag = false;
//		EzVarBoolean graphSwitch = new EzVarBoolean("Apply Calculations", false);
//		addEzComponent( graphSwitch );
		super.getUI().setProgressBarMessage("Waiting...");
		
		int cpt = 0;
		while (!stopFlag)
		{
//			if (graphSwitch.getValue()) {
//				//TODO:anything							
//			}
			
			cpt++;
			if (cpt % 10 == 0) super.getUI().setProgressBarValue((cpt % 5000000) / 5000000.0);
			Thread.yield();
		}
}


	@Override
	protected void initialize() {	//This creates the GUI and sets up RoiNums (containing EzVarIntRois)
		System.out.println("###Initialising####################");
		EzLabel textinfo=new EzLabel("Draw atleast one ROI and a background ROI");
		timeBool = new EzVarBoolean("Offline", false);
		unlinked = new EzVarBoolean("Unlinked ROIs", false);
		//Sort Rois
		if (Rois != null) {
			Collections.sort(Rois, new Comparator<ROI2D>() {
				@Override
				public int compare(ROI2D Roi2, ROI2D Roi1) {
					return Roi1.getName().compareTo(Roi2.getName());
				}
			});
		}
		addEzComponent(textinfo);
		//addEzComponent(timeBool);
		addEzComponent(unlinked);
		for (Sequence seq : sequences) {
			seq.addListener(this);
		}
		System.out.println("###Listeners Active####################");

	}

	@Override
	public void stopExecution()
	{
		// this method is from the EzStoppable interface
		// if this interface is implemented, a "stop" button is displayed
		// and this method is called when the user hits the "stop" button
		Prestart.exit = true;
		try {
			Prestart.QR.RunQueue();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void ExitThis() {
		stopFlag = true;
		try {
		this.getUI().close();
		} catch (Exception e) {System.out.println("SU UI already closed");}
				
		this.clean();
	}

	public Map<EzVarIntRoi, EzVarIntRoi> dupCheck(ArrayList<EzVarIntRoi> RoiNumbs) {
		//Usage of Map tools to create and return pairs of ROI
		Map<EzVarIntRoi, EzVarIntRoi> Pairs = new HashMap<>();

		for (int j=0; j<RoiNumbs.size();j++) {
			for (int k=j+1;k<RoiNumbs.size();k++) {
	//			System.out.println(RoiNumbs.get(j)+" versus "+RoiNumbs.get(k));
				if (k!=j && RoiNumbs.get(k).getValue() == RoiNumbs.get(j).getValue()) {
					
					Pairs.put(RoiNumbs.get(j),(RoiNumbs.get(k))); //Store linked variables
				
				}
			}
    	}
		return Pairs;
	}


	@Override
	public void sequenceChanged(SequenceEvent event)
	{
	//	if (!stopFlag) {
		    // Change type event ?
		    if (event.getType() == SequenceEventType.ADDED)
		    {
		        // ROI change ?
		        if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI)
		        {
		        	SwingUtilities.invokeLater(new Runnable(){
		        		public void run(){
				            ROI2D roy = (ROI2D) event.getSource();
				        	if (!royList.contains(roy.getId())) {
				            String Roiname = "ROI" + String.valueOf(inty);
				            roy.setName(Roiname);
				            roy.setShowName(true);
				            inty += 1;
				            // test roi properties here
				            System.out.println("Roi " + Roiname + " made in " + roy.getFirstSequence());
				            
				    		//Generate Roi integer components
			    			EzVarBoolean RoiBool = new EzVarBoolean("Background " + roy.getName(), false);
			    			EzVarText RoiNumer = new EzVarText("Numerator", choices, 2, false);
			    			EzVarText RoiDivis = new EzVarText("Divisor", choices, 2, false);

			    			EzVarIntRoi RoiNum = new EzVarIntRoi(roy.getName(), 0, 0, 99, 1,roy, RoiBool, RoiNumer, RoiDivis);
			    			RoiBool.addVisibilityTriggerTo(RoiNumer, false);
			    			RoiBool.addVisibilityTriggerTo(RoiDivis, false);
			    			EzGroup groupSequence = new EzGroup(roy.getName(),RoiNum.EVINum, RoiNum.EVIDiv, RoiNum.EVIBool);

			    			//Add to UI
			    			addEzComponent(groupSequence);
			    			EzGroups.add(groupSequence);			
			    			
			    			//Update UI
			    			//System.out.println("Updating UI for: " + Splitter.SU1.getName());
			    			EzGUI g = getUI();			    			
			    			g.repack(true);
			    			
			    			//Update variables
			    			RoiNums.add(RoiNum);
			    			royList.add(roy.getId());
				        	}
		        		}
		        	});

	          }
	        }
	  //  }
		    //TODO: test
		    if (event.getType() == SequenceEventType.REMOVED)
		    	{
		        if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI)
		        	{
		        	ROI2D roy = (ROI2D) event.getSource();
		        	String Roiname = roy.getName();
		            System.out.println("Deleting Roi " + Roiname);
		            EzVarIntRoi er = null;
		            
		            for (EzGroup c : EzGroups) {
		            	if (c.name.equals(roy.getName())) {
		            	c.dispose();
			            System.out.println("Deleted Roi interface for " + Roiname);
		            	for (EzVarIntRoi r : RoiNums) {
		            		if (r.name.equals(roy.getName())) {
		            			er = r;
		            		}
		            	}
		            	for (int i = 0; i < royList.size(); i++) {
		            		if (royList.get(i) == roy.getId()){
		            			royList.remove(i);
		            			System.out.println("Removed ROI ID: " + roy.getId());
		            		}
		            	}
		    			if (er != null) {
			    			RoiNums.remove(er);
				            System.out.println("Deleted Roi data for " + Roiname);
		    			} else {
		    				System.out.println("Unable to delete data for " + Roiname);
		    				}
		            	}
		            }
		        }
    			EzGUI g = getUI();			    			
    			g.repack(true); //TODO: this makes the uithreadingviolation 
		    }
		    
	}


	public boolean ThreadsWaiting() {
		//True if Splitter is equal or higher than all threads, threads are paused
		//False if splitter is lower than threads, threads are active
		boolean threadsWait = true;
		for (Threading aThread : threads) {
			if (aThread.count <  Prestart.S1.count) {
				System.out.println(aThread.count + " vs " + Prestart.S1.count);
				threadsWait = false;
			}
		}
		System.out.println("threads waiting: " + threadsWait);
		return threadsWait;
	}
	
	public void RunThreads() {
			for (Threading aThread : threads) {
				mnw2.doNotifyAll();
			}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
	
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
//		if (e.getSource() == msButton) { //TODO: not sure why we can't detect msButton as the culprit, EzButton messes it up?
			System.out.println("MileStone pressed");
			System.out.println("at frame " + threads.get(0).count + " and time " + threads.get(0).timeD );
			long frame = threads.get(0).count;
			long time = threads.get(0).timeD;
			milestones.add(frame);
			
			EzLabel msLabel = new EzLabel("Milestone at: " + frame + "(" + time + "s)");
			addEzComponent(msLabel);
			
			EzGUI g = getUI();
			g.repack(true);
//		}
	}
}
