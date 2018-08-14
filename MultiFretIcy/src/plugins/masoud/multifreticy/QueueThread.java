package plugins.masoud.multifreticy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class QueueThread extends Thread {
	private int counter;
	private boolean exit;
	private File transfoFile;
	
	public QueueThread() {
		counter = 1;
		exit = false;
		transfoFile = Prestart.transfoFile;
	}
	
	public void run(){
		while(!Prestart.exit) {
			System.out.println("QT Woke, count = " + counter + " size: " + Prestart.QR.AcqObjs.size());
			if(Prestart.QR.AcqObjs.size() >= counter) {
				System.out.println("QT Start, count = " + counter);
				for (int x = counter; x <= Prestart.QR.AcqObjs.size(); x++, counter=x) {
					try {
						RunSplitter(Prestart.QR.AcqObjs.get(x-1),transfoFile);
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println("Ran Splitter #" + x);
					RunAnalyses(Prestart.QR.AcqObjs.get(x-1),x);
					System.out.println("Ran Analyses #" + x);
					//RunAnalyses(Prestart.QR.AcqObjs.get(x));
				}
			}
			//Wait for an update, to reduce CPU usage
			if (!Prestart.exit) {
				if(!Prestart.offlineBool) {
					System.out.println("QW wait");
				Prestart.QR.QW.doWait2();
				} else if (Prestart.sequence.getAllImage().size() == Prestart.QR.AcqObjs.size()) {
					System.out.println("QW wait we done");
					Prestart.QR.QW.doWait2();
				}
			} else System.out.print("QT Exit");
		}
		finaliseThreads();
	}
	
	public void RunSplitter(AcquiredObject a, File f) throws InvocationTargetException, InterruptedException {
		System.out.println("run splitter");
		Prestart.S1.run(a.acqImg, f);
	}
	
	
	//acqimg = split image
	public void RunAnalyses(AcquiredObject a, int x) {
		
		for (Threading threed : Prestart.S1.SU1.threads) {
			threed.run(a.time,x);
		}
	}
	
	public void RunStartup() {
		//run startup

	}
	
	public void finaliseThreads() {
		System.out.println("###Exit initiated##############");
		for(Threading aThread : Prestart.S1.SU1.threads) {
			//Stopping every thread
			aThread.ExitThis();
		}
		Prestart.S1.SU1.ExitThis();
		Prestart.S1.ExitThis();
		Prestart.ExitThis();
		try {
			System.out.println("###Saving####################");
			Prestart.wbc.SaveAndClose(Prestart.wbc.workbook, Prestart.wbc.name);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
