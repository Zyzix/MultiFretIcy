package plugins.Zyzyx.theproj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import icy.gui.dialog.MessageDialog;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.sequence.SequenceListener;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.VarROIArray;
import poi.CreateWorkBook;

public class Startup extends EzPlug implements Block, EzStoppable, SequenceListener {
	
	protected VarROIArray 		outputROIs = new VarROIArray("list of ROI");
	EzVarSequence 				image = new EzVarSequence("Select Image (showing Rois)");
	final Sequence 				imageseq = image.getValue();
	EzVarBoolean				timeBool = new EzVarBoolean("Frames",false);
	
	public MyWaitNotify 		mnw = new MyWaitNotify();
	
	ArrayList<EzVarIntRoi> 		RoiNums = new ArrayList<EzVarIntRoi>();
	ArrayList<EzVarIntRoi> 		RoiNums2 = new ArrayList<EzVarIntRoi>();
	ArrayList<Thread> 			threads = new ArrayList<Thread>();
	ArrayList<Sequence>			sequences = new ArrayList<Sequence>();

	private int 				inty = 0;	

	//Create Arraylists
	ArrayList<ROI2D> 			Rois = null; //get ROIs from active image 
//TODO make it not explode when you don't have an image with ROIs open. 
	
	boolean						stopFlag;
	private ArrayList<Integer> 	royList = new ArrayList<Integer>();
	public CreateWorkBook 		wbc;
	public InterruptHandler 	iH = new InterruptHandler();
	public MyWaitNotify 		mnw2 = new MyWaitNotify();
	private ROI2D bgROI;

	//Get Rois from split sequences and add to Rois list
	public Startup(ArrayList<Sequence> seqList, CreateWorkBook w) {
		sequences = seqList;
		wbc = w;
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
		
	}

	@Override
	protected void execute() {
		// Run the program
		System.out.println("executing!");
		
		// Read ROIs, check if Copy checkbox is ticked
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
					sequences.get(rei.EVINum.getValue()).removeListener(Startup.this);
					sequences.get(rei.EVIDiv.getValue()).removeListener(Startup.this);
					
					rei.EVIRoi.remove();
					sequences.get(rei.EVINum.getValue()).addROI(rei.EVIRoi);
					sequences.get(rei.EVIDiv.getValue()).addROI(rei.EVIRoi);
					
					sequences.get(rei.EVINum.getValue()).addListener(Startup.this);
					sequences.get(rei.EVIDiv.getValue()).addListener(Startup.this);
					
					Threading R1 = new Threading (	rei, sequences.get(rei.EVINum.getValue()), 
												 	sequences.get(rei.EVIDiv.getValue()), 
												 	timeBool, 
												 	wbc,
												 	mnw,
												 	mnw2,
												 	iH,
												 	bgROI);
					threads.add(R1);
					R1.start();
				}
			}
		
		
		// Remove ROI from anywhere else
		
		// Make Entry-Pair from these two ROI
		
		// If Copy is not checked...
		
		// Read the images
//		for (Map.Entry<EzVarIntRoi, EzVarIntRoi> entry : dupCheck(RoiNums).entrySet())
//		  {
//		    System.out.println(entry.getKey().EVIRoi.getName() + " = " + entry.getValue().EVIRoi.getName()); //TODO This only properly does single pairs, make a system to not break on multiple same numbers (1,1,1,0,0) or better yet a system that allows combining 3 or more ROIs
//		  	      Threading R1 = new Threading( entry, timeBool );
//			      threads.add(R1);
//			      R1.start();
//		  }
		
		//Stop button stuff
		stopFlag = false;
//		EzVarBoolean graphSwitch = new EzVarBoolean("Apply Calculations", false);
//		addEzComponent( graphSwitch );
		super.getUI().setProgressBarMessage("Waiting...");
		
		int cpt = 0;
		while (!stopFlag)
		{
//			if (graphSwitch.getValue()) {
//				//TODO:							
//			}
			
			cpt++;
			if (cpt % 10 == 0) super.getUI().setProgressBarValue((cpt % 5000000) / 5000000.0);
			Thread.yield();
		}
}


	@Override
	protected void initialize() {	//This creates the GUI and sets up RoiNums (containing EzVarIntRois)
		System.out.println("Initialising!");
		EzLabel textinfo=new EzLabel("Choose the sequence and ROIs");
		
		//Sort Rois
		if (Rois != null) {
			Collections.sort(Rois, new Comparator<ROI2D>() {
				@Override
				public int compare(ROI2D Roi2, ROI2D Roi1) {
					return Roi1.getName().compareTo(Roi2.getName());
				}
			});
			//Generate Roi integer components
			for (ROI2D Roi : Rois){ //TODO: redundant?
	    		//Generate Roi integer components
    			EzVarBoolean RoiBool = new EzVarBoolean("Background " + Roi.getName(), false);
    			EzVarInteger RoiNumer = new EzVarInteger(Roi.getName() + " Numerator", 0, 999, 1);
    			EzVarInteger RoiDivis = new EzVarInteger(Roi.getName() + " Divisor", 0, 999, 1);
    			EzVarIntRoi RoiNum = new EzVarIntRoi(Roi.getName(), 0, 0, 99, 1,Roi, RoiBool, RoiNumer, RoiDivis);
    			RoiBool.addVisibilityTriggerTo(RoiNumer, false);
    			RoiBool.addVisibilityTriggerTo(RoiDivis, false);
    			EzGroup groupSequence = new EzGroup(Roi.getName(),RoiNum.EVINum, RoiNum.EVIDiv, RoiNum.EVIBool);

				RoiNums.add(RoiNum);			
			}
		}
		addEzComponent(textinfo);
		addEzComponent(image);
		addEzComponent(timeBool);
		
		for (Sequence seq : sequences) {
			seq.addListener(this);
		}
		
	}

	@Override
	public void stopExecution()
	{
		// this method is from the EzStoppable interface
		// if this interface is implemented, a "stop" button is displayed
		// and this method is called when the user hits the "stop" button
		wbc.writenow=true;

		stopFlag = true; //TODO: replace fully with iH.interruptvar
		iH.interruptVar = true;
		for (Thread thread : threads) {
			mnw2.doNotify();
			}
		finaliseThreads();
	}
	
	public void finaliseThreads() {
		mnw.doNotify();
		for(Thread thread : threads) {
			System.out.println("Stopping thread: " + thread.getName());
//			thread.interrupt();
			System.out.println("Starting SU notify");

			System.out.println("Finished SU notify, trying Join");
			try {
				thread.join();
				System.out.println("Join complete for thread " + thread.getName());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//wait on a second mnw which gets notified somehow when all threads are done?
		try {
			//TimeUnit.SECONDS.sleep(10);	
			System.out.println("Saving!");
			wbc.SaveAndClose(wbc.workbook, wbc.name);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		            ROI2D roy = (ROI2D) event.getSource();
		        	if (!royList.contains(roy.getId())) {
		            String Roiname = "ROI" + String.valueOf(inty);
		            System.out.println(Roiname);
		            roy.setName(Roiname);
		            roy.setShowName(true);
		            inty += 1;
		            // test roi properties here
		            System.out.println("Roi made in " + roy.getFirstSequence());
		            
		    		//Generate Roi integer components
	    			EzVarBoolean RoiBool = new EzVarBoolean("Background " + roy.getName(), false);
	    			EzVarInteger RoiNumer = new EzVarInteger(roy.getName() + " Numerator", 0, 999, 1);
	    			EzVarInteger RoiDivis = new EzVarInteger(roy.getName() + " Divisor", 0, 999, 1);
	    			EzVarIntRoi RoiNum = new EzVarIntRoi(roy.getName(), 0, 0, 99, 1,roy, RoiBool, RoiNumer, RoiDivis);
	    			RoiBool.addVisibilityTriggerTo(RoiNumer, false);
	    			RoiBool.addVisibilityTriggerTo(RoiDivis, false);
	    			EzGroup groupSequence = new EzGroup(roy.getName(),RoiNum.EVINum, RoiNum.EVIDiv, RoiNum.EVIBool);
	
	    			//Add to UI
	    			addEzComponent(groupSequence);
	
	    			//Update UI
	    			System.out.println("Updating UI for: " + this.getName());
	    			this.getUI().repack(true);
	    			
	    			//Update variables
	    			RoiNums.add(RoiNum);
	    			royList.add(roy.getId());
		        	}
	          }
	        }
	  //  }
	}

	public void RunThreads() {
			for (Thread thread : threads) {
				System.out.println(thread.getName() + " notifying!");
				mnw2.doNotifyAll();
			}
	}

	@Override
	public void sequenceClosed(Sequence sequence) {
	
	}
}
