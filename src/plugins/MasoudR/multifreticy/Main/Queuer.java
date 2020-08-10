package plugins.MasoudR.multifreticy.Main;

import java.util.ArrayList;

import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.AcquiredObject;
import plugins.MasoudR.multifreticy.DataObjects.MyWaitNotify;

public class Queuer {
	public ArrayList<AcquiredObject> 	AcqObjs;
	private static QueueThread 			QT;
	public MyWaitNotify 				QW;
	
	public Queuer() {
		AcqObjs = new ArrayList<AcquiredObject>();
		QW = new MyWaitNotify();
		QT = new QueueThread();
		}
	
	public void QueueUp(AcquiredObject a) {
		AcqObjs.add(a);
		System.out.println("size queue: " + AcqObjs.size());
	}
	
	public void RunQueue() throws InterruptedException {
		if(!MultiFretIcy.PS.exit && MultiFretIcy.PS.S1.SU1.startReady) {
			if (QT.getState() == Thread.State.NEW ) {
				QT.start();
				System.out.println("QT Activated");
			} else {
				QW.doNotifyAll2();
				System.out.println("QT Notified");
			}
		}
	}
	
	public void ExitThis() {
		//TODO temporary troubleshooting outs
		System.out.println("Q1");
		QT.ExitThis();
		//TODO temporary troubleshooting outs
		System.out.println("Q2");
		QW.doNotifyAll2();
		//TODO temporary troubleshooting outs
		System.out.println("Q3");
		System.out.println("QT Notified");
	}
	
}
