package plugins.masoud.multifreticy;

import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import icy.image.IcyBufferedImage;

public class Queuer {
	private int 						counter;
	public ArrayList<AcquiredObject> 	AcqObjs;
	private static QueueThread 			QT;
	public MyWaitNotify 				QW;
	
	public Queuer() {
		AcqObjs = new ArrayList<AcquiredObject>();
		counter = 0;
		QW = new MyWaitNotify();
		QT = new QueueThread();
		}
	
	public void QueueUp(AcquiredObject a) {
		AcqObjs.add(a);
		System.out.println("size queue: " + AcqObjs.size());
	}
	
	public void RunQueue() throws InterruptedException {
		if(Prestart.S1.SU1.startReady) {
			if (QT.getState() == Thread.State.NEW ) {
			QT.start();
			System.out.println("QT Activated");
			} else {
				QW.doNotifyAll2();
				System.out.println("QT Notified");
			}
		}
	}
	
}
