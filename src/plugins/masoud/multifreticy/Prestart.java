/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 


package plugins.masoud.multifreticy;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

//import org.json.JSONException;
import org.json.JSONObject;
//import org.micromanager.api.MMTags;
import org.micromanager.api.SequenceSettings;
import org.w3c.dom.Document;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.sequence.SequenceActionFrame;
import icy.image.IcyBufferedImage;
import icy.plugin.abstract_.PluginActionable;
import icy.roi.ROI;
import icy.sequence.Sequence;

import icy.sequence.edit.ROIAddsSequenceEdit;
import icy.util.XMLUtil;
import mmcorej.TaggedImage;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.event.AcquisitionListener;
import poi.CreateWorkBook;

/*
 * FileChooserDemo.java uses these files:
 *   images/Open16.gif
 *   images/Save16.gif
 */
public class Prestart extends PluginActionable
                             implements ActionListener, AcquisitionListener, ItemListener {
	
    JButton 							openButtonTransfo, openButtonChannel, saveButton;
    JCheckBox							transfoEnable, offlineCheckBox;
    JTextArea 							log;
    public JFileChooser 				fc;
    public File 						channelFile;
    public Properties					cp;
    
    ArrayList<Thread> 					threads;
	public static long 					startTime;
	public static Sequence 				sequence;
	
    static private final String 		newline = "\n";
    public static Splitter 				S1;
    public static Queuer				QR;
    public static File 					transfoFile;
    public static SequenceActionFrame 	mainFrame;
    public static boolean 				exit, pause, transformEnabled, offlineBool;
    public static CreateWorkBook 		wbc;
    
	public Prestart() {
		startTime = 0;
				
		QR = new Queuer();
		
		mainFrame = new SequenceActionFrame("Settings", true);
		threads = new ArrayList<Thread>();
		pause = false;
		exit = false;
		transformEnabled = false;
		offlineBool = false;
		System.out.println("Running version MFI");
    	mainFrame.getOkBtn().setEnabled(false);
    	
    	//Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        
        //Create a checkboxes to enable transformations and offline
        transfoEnable = new JCheckBox("Transform");
        transfoEnable.setSelected(false);
        transfoEnable.addItemListener(this);
        
        offlineCheckBox = new JCheckBox("Offline");
        offlineCheckBox.setSelected(false);
        offlineCheckBox.addItemListener(this);
        
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.add(transfoEnable);
        checkboxPanel.add(offlineCheckBox);
        
        //Create a file chooser
        fc = new JFileChooser();

        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        openButtonChannel = new JButton("Open a File...",
                                 createImageIcon("images/Open16.gif"));
        openButtonChannel.addActionListener(this);
        
        //Same for the other file opening button
        openButtonTransfo = new JButton("Open a File...",
                createImageIcon("images/Open16.gif"));
        openButtonTransfo.addActionListener(this);

        //Create the save button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        saveButton = new JButton("Save Settings...",
                                 createImageIcon("images/Save16.gif"));
        saveButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButtonChannel);
        buttonPanel.add(openButtonTransfo);
        buttonPanel.add(saveButton);
    	openButtonTransfo.setEnabled(false);
        //Add the buttons and the log to this panel.
        mainFrame.getMainPanel().add(checkboxPanel, BorderLayout.PAGE_START);
        mainFrame.getMainPanel().add(buttonPanel, BorderLayout.CENTER);
        mainFrame.getMainPanel().add(logScrollPane, BorderLayout.PAGE_END);
        
        openButtonTransfo.setVisible(false);
        transfoEnable.setVisible(false);
        
		// create and load default properties
		cp = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream("options.cfg");
			cp.load(in);
			in.close();
			log.append("Config file loaded." + newline);
			
			String tfURL = cp.getProperty("transfofile", null);
			String cfURL = cp.getProperty("contourfile", null);
			
			//Open tranfofile
			if (tfURL != null) {
				transfoFile = new File(tfURL);
				if (transfoFile == null) {log.append("unable to open transformation file" + newline);}
					else{log.append("Opening: " + transfoFile.getName() + "." + newline);}
			}
			//Open contourfile
			if (cfURL != null) {
				channelFile = new File(cfURL);
				if (channelFile == null) {log.append("unable to open contour file" + newline);}
					else{log.append("Opening: " + channelFile.getName() + "." + newline);}
			}
			offlineBool = Boolean.parseBoolean(cp.getProperty("offline", "false"));
			transformEnabled = Boolean.parseBoolean(cp.getProperty("transform", "false"));
			
			log.append("Offline set to " + String.valueOf(offlineBool) + newline);
			offlineCheckBox.setSelected(offlineBool);
			log.append("Transform set to " + String.valueOf(transformEnabled) + newline);
			transfoEnable.setSelected(transformEnabled);
			//check here for previous channel comparison settings
			log.append("Previous channel settings primed." + newline);
			
		} catch (IOException e4) {
			e4.printStackTrace();
			log.append("unable to open config file" + newline);
		}
		
		ReadyCheck();
		
		/*
		 * Planned stuff to have in options file
		 * 	= Offline checkbox
		 *  = Transform checkbox + transfofile
		 *  = contour file
		 *  
		 *  = excel template
		 *  
		 *  = workbook save location
		 *  = workbook naming
		 *  
		 *  = settings for first and so forth ROI drawn
		 *  
		 * 	= background correction
		 *  = flat-fielding
		 *  = other calculations
		 *  
		 */
   

        //define action to do when OK button is pressed
        mainFrame.setOkAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) //On OK click
            {
               // get selected sequence
               sequence = mainFrame.getSequence();

               //remove ROI and load contour ROI from channelFile
               sequence.removeAllROI();
               if(LoadRois(channelFile,sequence)) { log.append("Loaded ROIs from " + channelFile.getName() + newline);}
               else	{MessageDialog.showDialog("LoadRois failed"); return;}
               //Create Excel WB
				try {
					wbc = new CreateWorkBook();
					System.out.println("WB created");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				//Create Splitter thread
	       		try {
					S1 = new Splitter(sequence, transfoFile);
				} catch (InterruptedException e3) {
					e3.printStackTrace();
				}
	
	    		// Split the image
	    		try {
	    			if(!Prestart.pause) {
	    				S1.run(sequence.getLastImage(), transfoFile);
	    			}
				} catch (InvocationTargetException | InterruptedException e2) {
					e2.printStackTrace();
				}
	    		
	    		//Start MM listener if online
	    		if (offlineBool == false) {
	    			try {
	    	    MicroManager.addAcquisitionListener(Prestart.this);
	    			} catch (Exception e6){
	    				e6.printStackTrace();
	    				MessageDialog.showDialog("You chose online, but MicroManager is not running!");
	    				ExitThis();
	    				//TODO: make it properly exit
	    			}
	    			
	    		} else { 	    		//Offline Run:
					try {
						RunOffline();
					} catch (InvocationTargetException | InterruptedException e1) {
						e1.printStackTrace();
					}
	    		}
            }
        });

        // define if the frame should be closed after OK action is done (default = true)
        mainFrame.setCloseAfterAction(true);
    }

    public void actionPerformed(ActionEvent e) {

        //Handle open button action.
        if (e.getSource() == openButtonChannel) {
            int returnVal = fc.showOpenDialog(fc);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                 channelFile = fc.getSelectedFile();
                // open the file
                log.append("Opening: " + channelFile.getName() + "." + newline);
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());}
            
        //Handle open button action.
            else if (e.getSource() == openButtonTransfo) {
            int returnVal = fc.showOpenDialog(fc);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                transfoFile = fc.getSelectedFile();
                //open the file
                log.append("Opening: " + transfoFile.getName() + "." + newline);
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
            

        //Handle save button action.
        } else if (e.getSource() == saveButton) {
        	cp.setProperty("offline", String.valueOf(offlineBool));
        	cp.setProperty("transform", String.valueOf(transformEnabled));
        	if (transfoFile != null) {cp.setProperty("transfofile", transfoFile.getPath());}
        		//else {log.append("No transformation file selected." + newline);} 
        	if (channelFile != null) {cp.setProperty("contourfile", channelFile.getPath());}
    			//else {log.append("No contour file selected." + newline);} 

        	try {
				cp.store(new FileOutputStream("options.cfg"), null);
	            log.append("Saving procedure as: options.cfg" + newline);
			} catch (IOException e1) {
				e1.printStackTrace();
				log.append("Save failed!" + newline);
			}

        }              
        ReadyCheck(); //Check if all files and selections are made, enable ok button if they are
    }
    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = Prestart.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private void createAndShowGUI() {
        //Create and set up the window.
        //mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
    	addIcyFrame(mainFrame);
        //Display the window.
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    //load ROI from file
    public boolean LoadRois(File file, Sequence seq)
    {
        if ((file != null) && (seq != null))
        {
            final Document doc = XMLUtil.loadDocument(file);

            if (doc != null)
            {
                final List<ROI> rois = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));

                seq.beginUpdate();
                try
                {
                    // add to sequence
                    for (ROI roi : rois)
                        seq.addROI(roi);
                }
                finally
                {
                    seq.endUpdate();
                }

                // add to undo manager
                seq.addUndoableEdit(new ROIAddsSequenceEdit(seq, rois)
                {
                    @Override
                    public String getPresentationName()
                    {
                        if (getROIs().size() > 1)
                            return "ROIs loaded from XML file";

                        return "ROI loaded from XML file";
                    };
                });

                return true;
            }
        }

        return false;
    }
    
    
    public void run() {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });
    }

    public void ReadyCheck() {
    	if (channelFile != null) {// && transfoFile != null) {
    		mainFrame.getOkBtn().setEnabled(true);
    	}
    }
    
	@Override
	public void acqImgReveived(TaggedImage image) {
		System.out.println("img reveived");
		if (startTime == 0) {
			startTime = System.nanoTime();
	 	    System.out.println("Start time = " + startTime);
		}
		if (exit == true ) {
			System.out.println("img reveived exit");
			MicroManager.removeAcquisitionListener(Prestart.this);
			return;
		} System.out.println(pause);
		if (pause == false) {
			System.out.println("img reveived run");
			AcquiredObject acqObj = new AcquiredObject(sequence.getLastImage(),System.nanoTime());
			//add to queue TODO: here we need to decide which queue to put this in based on position.
			QR.QueueUp(acqObj); //TODO: what'd happen if you went forward with taggedimage instead?
			//notify queue
			try {
				QR.RunQueue();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("S1 ran, invokelater workd");
	}

	@Override
	public void acquisitionStarted(SequenceSettings settings, JSONObject metadata) {
		
	}

	@Override
	public void acquisitionFinished(List<Sequence> result) {
		
	}
	
	public void RunOffline() throws InvocationTargetException, InterruptedException {
		startTime = System.nanoTime();
 	    System.out.println("Start time = " + startTime);
		for (IcyBufferedImage img : sequence.getAllImage()) {
			AcquiredObject AO = new AcquiredObject(img,System.nanoTime());
			QR.QueueUp(AO); //TODO: what'd happen if you went forward with taggedimage instead?
		}
	}
	
	public static void ExitThis() {
		exit = true;
		mainFrame.removeAll();
		mainFrame.dispose();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// Checkbox handler
		Object source = e.getItemSelectable();
		// TransfoEnable checkbox:
		if (source == transfoEnable && e.getStateChange() == ItemEvent.DESELECTED) {
	    	openButtonTransfo.setEnabled(false);
	    	transformEnabled = false;
	    } else if (source == transfoEnable && e.getStateChange() == ItemEvent.SELECTED) {
	    	openButtonTransfo.setEnabled(true);
	    	transformEnabled = true;
	    }		
		
		if (source == offlineCheckBox && e.getStateChange() == ItemEvent.DESELECTED) {
			offlineCheckBox.setEnabled(false);
	    	offlineBool = false;
	    } else if (source == offlineCheckBox && e.getStateChange() == ItemEvent.SELECTED) {
	    	offlineCheckBox.setEnabled(true);
	    	offlineBool = true;
	    }		
		
	}
	
}

