package plugins.Zyzyx.theproj;

public class MyWaitNotify{

	  MonitorObject myMonitorObject = new MonitorObject();
	  boolean wasSignalled = false;

	  public void doWait(){
	    synchronized(myMonitorObject){
	      while(!wasSignalled){
	        try{
	          myMonitorObject.wait();
	         } catch(InterruptedException e){e.printStackTrace();}
	      }
	      //clear signal and continue running.
	      wasSignalled = false;
	    }
	  }

	  public void doNotify(){
	    synchronized(myMonitorObject){
	      wasSignalled = true;
	      myMonitorObject.notify();
	    }
	  }
	  
	  public void doNormalWait() {
		  synchronized(myMonitorObject) {
			  try {
				myMonitorObject.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
	  }
	  
	  public void doNotifyAll() {
		  synchronized(myMonitorObject) {
			  myMonitorObject.notifyAll();
		  }
	  }
	}