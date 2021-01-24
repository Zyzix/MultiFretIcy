package plugins.MasoudR.multifreticy.Main;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import icy.gui.dialog.MessageDialog;
import icy.gui.main.MainInterface;
import icy.main.Icy;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.AcquiredObject;

public class QueueThread extends Thread {
	private int counter;
	private boolean exit;
	private boolean first = true;

	
	public QueueThread() {
		counter = 1;
		exit = false;
	}
	
	// This will run through accumulated images and initiate split+analysis for them.
	public void run(){
		int runCount = 0;
		while(!exit) {
			System.out.println("QT Woke, count = " + counter + " size: " + MultiFretIcy.PS.QR.AcqObjs.size());
			if(MultiFretIcy.PS.QR.AcqObjs.size() >= counter) {
				//Turn off graph updates TODO: doesn't seem to work
//				if (!first) {
//					for(Threading threed : MultiFretIcy.PS.S1.SU1.threads) {
//						threed.chartPanel.getChart().setNotify(false);
//					}			
//				}
				System.out.println("QT Start, count = " + counter);
				for (int x = counter; x <= MultiFretIcy.PS.QR.AcqObjs.size(); x++, counter=x) {
					try {
						RunSplitter(MultiFretIcy.PS.QR.AcqObjs.get(x-1));
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("Ran Splitter #" + x);
					RunAnalyses(MultiFretIcy.PS.QR.AcqObjs.get(x-1),x);
					System.out.println("Ran Analyses #" + x);
					if (first && MultiFretIcy.PS.wsBool) {BuildWorkspace(); first = false;}
					//RunAnalyses(Prestart.QR.AcqObjs.get(x));
				}
				runCount++; System.out.println("runcount: " + runCount);
				//if (first) {MultiFretIcy.PS.S1.SU1.Tile(); first = false;}
			}
			
			if(!MultiFretIcy.PS.offlineBool) { //clear queue to free up memory
			MultiFretIcy.PS.QR.AcqObjs.clear(); //TODO: should have a failsafe to not clear while imgreveived is a go?
			counter = 1;
			System.out.println("AcqObjs cleared");
			}
			
			if (!first) {
				for(Threading threed : MultiFretIcy.PS.S1.SU1.threads) {
					threed.chartPanel.getChart().setNotify(true);
				}		
			}
			first = false;
			
			//Wait for an update, to reduce CPU usage
			if (!exit) {
				if(!MultiFretIcy.PS.offlineBool) {
					System.out.println("QW wait");
					MultiFretIcy.PS.QR.QW.doWait2();
				} else {
					System.out.println("QW wait we done");
					MessageDialog.showDialog("Finished");
					MultiFretIcy.PS.QR.QW.doWait2();
				}
			} else System.out.print("QT Exit");
		}
		finaliseThreads();
		//TODO temporary troubleshooting outs
		System.out.println("QT3");
	}

	public void RunSplitter(AcquiredObject a) throws InvocationTargetException, InterruptedException {
		System.out.println("run splitter");
		MultiFretIcy.PS.S1.Split(a, true);
	}
	
	
	public void RunAnalyses(AcquiredObject a, int x) {
		boolean z = false;
		for (Threading threed : MultiFretIcy.PS.S1.SU1.threads) {
			System.out.println("threads: " + threed.getName());
			if(threed.pos.equals(a.position)) {
				threed.run(a.time,x, a.prenotation);
				z = true;
			}
		}			
		if (!z) {System.out.println("No thread associated with image");}		
		System.out.println("Num of frames: " + Icy.getMainInterface().getInternalFrames().size());
	}
	
	private void BuildWorkspace() {
		// Workspace builder
				for (int i = 0; i < MultiFretIcy.PS.S1.concSeqList.size(); i++) {		
					System.out.println("conqsnam: " + MultiFretIcy.PS.S1.concSeqList.get(i).getName());
					int j = 0;
					for (Threading threed : MultiFretIcy.PS.S1.SU1.threads) {	
						System.out.println(threed.getName());
						System.out.println("threadnam: " + MultiFretIcy.PS.S1.SU1.threads.get(j).pos);
						System.out.println("chartnam: " + MultiFretIcy.PS.S1.SU1.threads.get(j).chartHolder.getName());
						if (MultiFretIcy.PS.S1.SU1.threads.get(j).chartHolder.getName().equals(MultiFretIcy.PS.S1.concSeqList.get(i).getName())) {
							try {
								MultiFretIcy.PS.S1.SU1.ws.addComponent(1, i, MultiFretIcy.PS.S1.SU1.threads.get(j).chartHolder);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						j++;
					}
					MultiFretIcy.PS.S1.SU1.ws.repaint();
				}
			}
		
	
	
	public void RunStartup() {
		//run startup

	}
	
	public void ExitThis() {
		exit = true;		
	}
	
	public void finaliseThreads() {
		System.out.println("###Exit initiated##############");
		
		//Get Main Window
		MainInterface MI = Icy.getMainInterface();
		//Create full-window screenshot
		//captureComponent(MI.getMainFrame()); 
		//Create internal pane screenshot
		byte[] sf = null;

		try {
			if (!MultiFretIcy.PS.wsBool) {
			sf = captureView(MI.getDesktopPane());
			} else {sf = captureView(MultiFretIcy.PS.S1.SU1.ws.getScrollPane());}
		} catch (IOException | InvocationTargetException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	

		//Remove concSequences
		MultiFretIcy.PS.S1.SU1.ExitThis();

		//Stopping every thread
		for(Threading aThread : MultiFretIcy.PS.S1.SU1.threads) {
			aThread.exitThis();
			System.out.println("Exited thread " + aThread.getName());

		}
		MultiFretIcy.PS.S1.ExitThis();
		MultiFretIcy.PS.exitThis();
		try {
			System.out.println("###Saving####################");
			MultiFretIcy.PS.wbc.SaveAndClose(sf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] captureView(Component component) throws IOException, InvocationTargetException, InterruptedException
	{
	    Rectangle rect = component.getBounds();
		 
	        String format = "png";
	        byte[] bytes = null;
	        
		    BufferedImage captureImage =
	                new BufferedImage(rect.width, rect.height,
	                                    BufferedImage.TYPE_INT_ARGB);	
		    
	        final Runnable doScreenshot = new Runnable() {
	             public void run() {
	            	 component.paint(captureImage.createGraphics());
	            	 }
	         };
		    
		    SwingUtilities.invokeAndWait(doScreenshot); 
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	ImageIO.write(captureImage, format, baos);
        	bytes = baos.toByteArray();
		   
	    return bytes;
	}
}
