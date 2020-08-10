package plugins.masoud.multifreticy;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.TitledFrame;
import icy.sequence.Sequence;

public class TransfoMaker implements ActionListener {
    TitledFrame 			mainFrame;
    private ArrayList<JButton>					buttonList;
    public File							transfoFile;
    private ArrayList<Sequence> seqList;
	private ClemPoints CP;
	private int transNum;
	private String base;
	private int transMax;
    private JButton exitButton; 
	
	public TransfoMaker(ArrayList<Sequence> SL,String b) {
    	seqList = SL;
		transNum = 0;
		transMax = SL.size();
		mainFrame = new TitledFrame("TransfoMaker", true);
		buttonList = new ArrayList<JButton>();
    	base = b;
    	JPanel buttonPanel = new JPanel(); //use FlowLayout

    	for (Sequence seq : seqList) {
    		if (seq.getName()!= base){
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
        exitButton = new JButton("Cancel");
        exitButton.addActionListener(this);
        mainFrame.getMainPanel().add(exitButton, BorderLayout.SOUTH);
  	
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		//Handle open button action.
		if( e.getSource() == exitButton ) {
			System.out.println("Cancelling tranfoMaker");
			ExitThis();
		}
		else if ( ((JButton) e.getSource()).getText().equals(base) ){
			//break;
		}
		else if(!((JButton) e.getSource()).getText().contains("Done")) {
			String name = ((JButton) e.getSource()).getText();
			awaitPoints(name);
			((JButton) e.getSource()).setText("Done "+ name);
		}
		else {
			String name = ((JButton) e.getSource()).getText();
			transNum++;
			CP.ComputeTransfo();
			((JButton) e.getSource()).setText(name + " Complete");
			((JButton) e.getSource()).setEnabled(false);
		}
		
/*		switch(((JButton) e.getSource()).getText()) {
			case base:
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
					transNum++;
					CP.ComputeTransfo();
					((JButton) e.getSource()).setText("TR Complete");
					((JButton) e.getSource()).setEnabled(false);
					break;
			case "Done BL":
					transNum++;
					CP.ComputeTransfo();
					((JButton) e.getSource()).setText("BL Complete");
					((JButton) e.getSource()).setEnabled(false);
					break;
			case "Done BR": 
					transNum++;
					CP.ComputeTransfo();
					((JButton) e.getSource()).setText("BR Complete");
					((JButton) e.getSource()).setEnabled(false);
					break;
			default:
					System.out.println("ERROR:TFM01");
					MessageDialog.showDialog("Name contourROIs TL/TR/BL/BR");
					System.exit(0);
					break;
			}*/
		if (transNum  == transMax-1) {
				Prestart.pause = false;
				mainFrame.close();
				try {
					Prestart.S1.run(Prestart.sequence.getLastImage(), transfoFile);
				} catch (InvocationTargetException | InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				}
	}
	
	public void awaitPoints(String name) {
			
			System.out.println("awaiting points... list size: " + seqList.size());
			Sequence sourceSeq = null;
			Sequence targetSeq = null;
	    	for (Sequence seq : seqList) {
	    		System.out.println(name + " " + seq.getName());
	    		if (seq.getName().equals(base)) {	
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
			CP = new ClemPoints(sourceSeq, targetSeq, name, base);
			CP.start();
	}
	
    private void createAndShowGUI() {
        //Create and set up the window.
        //mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	
        //Add content to the window.
    	addIcyFrame(mainFrame);

        //Display the window.
        mainFrame.pack();
        mainFrame.setSize(450,100);

        mainFrame.setVisible(true);
        System.out.println("GUId");
    }

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
    
    public void ExitThis() {
		mainFrame.close();
		Prestart.S1.ExitThis();
		Prestart.ExitThis();
    }
}