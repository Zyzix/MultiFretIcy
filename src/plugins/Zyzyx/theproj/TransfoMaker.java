package plugins.Zyzyx.theproj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.sequence.SequenceActionFrame;
import icy.sequence.Sequence;

public class TransfoMaker extends Thread implements ActionListener {
    final SequenceActionFrame 			mainFrame = new SequenceActionFrame("Example", true);
    private ArrayList<JButton>					buttonList = new ArrayList<JButton>();
    public File							transfoFile;
    private ArrayList<Sequence> seqList;
	private ClemPoints CP;
    public TransfoMaker(ArrayList<Sequence> SL) {
    	seqList = SL;
    	mainFrame.getOkBtn().setEnabled(false);
        JPanel buttonPanel = new JPanel(); //use FlowLayout

    	for (Sequence seq : seqList) {
    		if (seq.getName()!= "TL"){
    		JButton button = new JButton(seq.getName());
    		button.setText(seq.getName());
    		System.out.println("button: " + button.getText() +" from seq "+ seq.getName());
            button.addActionListener(this);
    		buttonList.add(button);
    		buttonPanel.add(button);
    		}
    		else {System.out.println("skipping TL button");}
    	}
    	
    	mainFrame.getMainPanel().add(buttonPanel, BorderLayout.PAGE_START);
    	
        mainFrame.setCloseAfterAction(true);
  	
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		//Handle open button action.
		int i = 0;
		switch(((JButton) e.getSource()).getText()) {
		case "TL":
				break;
		case "TR":
				((JButton) e.getSource()).setText("Done TR");
				awaitPoints("TR");
				break;
		case "BL":
				((JButton) e.getSource()).setText("Done BL");
				awaitPoints("BL");
				break;
		case "BR": 
				((JButton) e.getSource()).setText("Done BR");
				awaitPoints("BR");
				break;
		case "Done TR":
				i++;
				CP.ComputeTransfo();
				((JButton) e.getSource()).setText("TR Complete");
				((JButton) e.getSource()).setEnabled(false);
				break;
		case "Done BL":
				i++;
				CP.ComputeTransfo();
				((JButton) e.getSource()).setText("BL Complete");
				((JButton) e.getSource()).setEnabled(false);
				break;
		case "Done BR": 
				i++;
				CP.ComputeTransfo();
				((JButton) e.getSource()).setText("BR Complete");
				((JButton) e.getSource()).setEnabled(false);
				break;
		default:
				System.out.println("ERROR:TFM01");
				MessageDialog.showDialog("Name contourROIs TL/TR/BL/BR");
				System.exit(0);
				break;
		}
		if (i == 3) {Prestart.pause = false;}
	}
	
	public void awaitPoints(String name) {
			
			System.out.println("awaiting points... list size: " + seqList.size());
			Sequence sourceSeq = null;
			Sequence targetSeq = null;
	    	for (Sequence seq : seqList) {
	    		System.out.println(name + " " + seq.getName());
	    		if (seq.getName().equals("TL")) {	
											sourceSeq = seq;
											sourceSeq.setName(seq.getName());
	    									seq.removeAllROI();		
	    									}
	    		else if (name.equals(seq.getName())) {
	    			System.out.println("button: " + seq.getName() + " pressed!");
		    		targetSeq = seq; targetSeq.setName(seq.getName());
					seq.removeAllROI();		
	    		}
	    	}
	    	if (sourceSeq.equals(null)) {System.out.println("NULL");}
			System.out.println("source: " + sourceSeq.getName() + //" ID: " + sourceSeq.getId() + 
								" target: " + targetSeq.getName()); //+ " ID: " + targetSeq.getId());
			CP = new ClemPoints(sourceSeq, targetSeq, name);
			CP.start();
	}
	
    private void createAndShowGUI() {
        //Create and set up the window.
        //mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	
        //Add content to the window.
    	addIcyFrame(mainFrame);
        //Display the window.
        mainFrame.pack();
        mainFrame.setVisible(true);
        System.out.println("GUId");
    }

	@Override
	public void run() {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
		System.out.println("runnd");

                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();

        System.out.println("runndone");
	}

    public void addIcyFrame(final IcyFrame frame)
    {
        frame.addToDesktopPane();
    }
}