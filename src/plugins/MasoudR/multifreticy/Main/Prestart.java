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


package plugins.MasoudR.multifreticy.Main;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.api.PositionList;
import org.micromanager.api.SequenceSettings;
import org.w3c.dom.Document;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.ActionFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.edit.ROIAddsSequenceEdit;
import icy.util.XMLUtil;
import mmcorej.TaggedImage;
import net.miginfocom.swing.MigLayout;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.AcquiredObject;
import plugins.MasoudR.multifreticy.DataObjects.MyCoordinates;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.event.AcquisitionListener;


/*
 * FileChooserDemo.java uses these files:
 *   images/Open16.gif
 *   images/Save16.gif
 */
public class Prestart  
                             implements ActionListener, AcquisitionListener, ItemListener {
	
    JButton 							calcsButton, browseButton, openButtonTransfo, openButtonChannel, saveButton, detectorButton;
    JCheckBox							transfoEnable, offlineCheckBox, calcCheckBox, wsEnable;
    JTextArea 							log;
    JTextField							outputLocation;
    public JFileChooser 				fc;
    public  File 						transfoFile, calcsFile, channelFile;
    public Properties					cp;
    
    ArrayList<Thread> 					threads;
	public long 						startTime;
	public ArrayList<Sequence>			sequences;
	
    private final String 				newline = "\n";
    public 	Splitter 					S1;
    public 	Queuer						QR;
    public 	ActionFrame 				mainFrame;
    public 	JFrame						detectorPanel;
    public  boolean 					exit, pause, transformEnabled, offlineBool, mpBool, calcBool, wsBool;
    public  CreateWorkBook 				wbc;
    
	public MMStudio 					mStudio;
	public PositionList 				posList;
	
	private ArrayList<ArrayList<MyCoordinates>> allCorners = new ArrayList<ArrayList<MyCoordinates>>();
	private ArrayList<AcquiredObject> lastBatch = new ArrayList<AcquiredObject>();

	
	public Prestart() { 
		startTime = 0;						
		sequences = new ArrayList<Sequence>();		
		mainFrame = new ActionFrame("Settings", true);
		threads = new ArrayList<Thread>();
		pause = false;
		exit = false;
		transformEnabled = false;
		offlineBool = false;
		mpBool = true;
		calcBool = false;
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
        
//        mpCheckBox = new JCheckBox("MultiPos"); //Deprecated, always true
//        mpCheckBox.setSelected(false);
//        mpCheckBox.addItemListener(this);
        
        calcCheckBox = new JCheckBox("CustomCalcs");
        calcCheckBox.setSelected(false);
        calcCheckBox.addItemListener(this);
        
        wsEnable = new JCheckBox("Workspace");
        wsEnable.setSelected(false);
        wsEnable.addItemListener(this);
        
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.add(transfoEnable);
        checkboxPanel.add(offlineCheckBox);
//        checkboxPanel.add(mpCheckBox); //Deprecated, always true
        checkboxPanel.add(calcCheckBox);
        checkboxPanel.add(wsEnable);
        
        //Create a file chooser
        fc = new JFileChooser();

        detectorButton = new JButton("Detector");
        detectorButton.addActionListener(this);
        
        openButtonChannel = new JButton("Select Contour-file",
                                 createImageIcon("images/Open16.gif"));
        openButtonChannel.addActionListener(this);
        
        calcsButton = new JButton("Select Calculations",
                createImageIcon("images/Open16.gif"));
        calcsButton.addActionListener(this);

        
        //Same for the other file opening button
        openButtonTransfo = new JButton("Open a File...",
                createImageIcon("images/Open17.gif"));
        openButtonTransfo.addActionListener(this);

        //Create the save button.
        saveButton = new JButton("Save Settings...",
                                 createImageIcon("images/Save16.gif"));
        saveButton.addActionListener(this);
        
        //Textbox for excel output URL
        outputLocation = new JTextField(System.getProperty("user.home") + "\\Datasheet.xlsx");

        browseButton = new JButton("Open a File...",
                createImageIcon("images/Browse16.gif"));
        browseButton.addActionListener(this);
        
        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(detectorButton);
        buttonPanel.add(openButtonChannel);
        buttonPanel.add(calcsButton);
        buttonPanel.add(openButtonTransfo);
        buttonPanel.add(saveButton);
        
        JPanel outputPanel = new JPanel(); //use FlowLayout
        outputPanel.add(outputLocation);
        outputPanel.add(browseButton);
        
    	openButtonTransfo.setEnabled(false);
        //Add the buttons and the log to this panel.
    	JPanel settingsPanel = new JPanel(new MigLayout("wrap 1"));
    	settingsPanel.add(checkboxPanel, "grow");
    	settingsPanel.add(buttonPanel, "grow");
    	settingsPanel.add(outputPanel, "grow");
        settingsPanel.add(logScrollPane, "grow");
        mainFrame.getMainPanel().add(settingsPanel);
        openButtonTransfo.setVisible(false); //TODO remove deprecated
//        transfoEnable.setVisible(false);
        
		// create and load default properties
		cp = new Properties();
		FileInputStream in;
		try {
			in = new FileInputStream(System.getProperty("user.home") + "\\MFIoptions.cfg");
			cp.load(in);
			in.close();
			log.append("Config file loaded." + newline);
			
			String tfURL = cp.getProperty("transfofile", null);
			String cfURL = cp.getProperty("contourfile", null);
			String mfURL = cp.getProperty("calcsfile", null);
			
			outputLocation.setText(cp.getProperty("outputLocation", System.getProperty("user.home") + "\\Datasheet.xlsx"));
			
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
			//Open calcsfile
			if (mfURL != null) {
				calcsFile = new File(mfURL);
				if (calcsFile == null) {log.append("unable to open calcs file" + newline);}
					else{log.append("Opening: " + calcsFile.getName() + "." + newline);}
			}
			
			offlineBool = Boolean.parseBoolean(cp.getProperty("offline", "false"));
			transformEnabled = Boolean.parseBoolean(cp.getProperty("transform", "false"));
//			mpBool = Boolean.parseBoolean(cp.getProperty("multipos", "true")); //Deprecated, always true
			calcBool = Boolean.parseBoolean(cp.getProperty("customcalcs", "false"));
			wsBool = Boolean.parseBoolean(cp.getProperty("workspace", "false"));
			
			log.append("Offline set to " + String.valueOf(offlineBool) + newline);
			offlineCheckBox.setSelected(offlineBool);
			log.append("Transform set to " + String.valueOf(transformEnabled) + newline);
			transfoEnable.setSelected(transformEnabled);
//			log.append("MultiPos set to " + String.valueOf(mpBool) + newline);
//			mpCheckBox.setSelected(mpBool); //Deprecated, always true
			log.append("CustomCalcs set to " + String.valueOf(calcBool) + newline);
			calcCheckBox.setSelected(calcBool);
			log.append("Workspace set to " + String.valueOf(wsBool) + newline);
			wsEnable.setSelected(wsBool);
			//check here for previous channel comparison settings
			log.append("Previous channel settings primed." + newline);
			
		} catch (IOException e4) {
			e4.printStackTrace();
			log.append("unable to open config file" + newline);
		}
		
		readyCheck();

        //define action to do when OK button is pressed
        mainFrame.setOkAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) //On OK click
            {
            	System.out.println("Prestart finishing");
               // Get selected sequence
               sequences = Icy.getMainInterface().getSequences();
               
               for (Sequence sequence: sequences) {
            	   // Remove ROI and load contour ROI from channelFile
	               sequence.removeAllROI();
	               if(loadRois(channelFile,sequence)) { log.append("Loaded ROIs from " + channelFile.getName() + newline);}
	               else	{MessageDialog.showDialog("LoadRois failed"); return;}	               
               }
               
               JInternalFrame[] frames = Icy.getMainInterface().getDesktopPane().getAllFrames();
               for (int i = 0; i < frames.length; i++) {
            	   if (frames[i].isIconifiable()) {
            		   Icy.getMainInterface().getDesktopPane().getDesktopManager().iconifyFrame(frames[i]);
            	   }
               }
               
               // Get MultiPos marked positions
               if (mpBool && !offlineBool) {
	    			try {
	    		   		mStudio = MicroManager.getMMStudio();
						posList = mStudio.getPositionList();
					} catch (Exception e4) {
						System.out.println("No MM detected");
						MessageDialog.showDialog("Micro-Manager must be running to use multiple positions");
						return;
					}
               }
               
               // Create Excel backup
               File f = new File(outputLocation.getText());
               if(f.exists() && !f.isDirectory()) { 
            	   //TODO Finish backup thing
               }
               
    			// Create Excel WB
				try {
					wbc = new CreateWorkBook(outputLocation.getText());
					System.out.println("WB created");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				// Find Corners
				if (transformEnabled) {
					CornerFinder CF = new CornerFinder();
					allCorners = CF.Aktivat(channelFile);
					if (allCorners == null) {return;}
				}
				
				// Create Splitter object
	       		try {
					S1 = new Splitter(sequences, allCorners);
				} catch (InterruptedException e3) {
					e3.printStackTrace();
				}
	
	    		// Create Startup interface
    			if(!pause) {
    				try {
						S1.CreateSU(calcsFile);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
    			}
	    		
	    		// Start MM listener if online
	    		if (offlineBool == false) {
	    			try {
			    	    MicroManager.addAcquisitionListener(Prestart.this);
			    		QR = new Queuer(); 		
	    			} catch (Exception e6){
	    				e6.printStackTrace();
	    				MessageDialog.showDialog("You chose online, but MicroManager is not running!");
	    				return;
	    			}	    			
	    		} else { 	    		//Offline Run:
					try {
						runOffline();
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
            
        //Handle Detector button action.
        else if (e.getSource() == detectorButton) {
        	try {
				new Detector("Detector");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        
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
            
        //Handle calcs file open button action.
        } else if (e.getSource() == calcsButton) {
            int returnVal = fc.showOpenDialog(fc);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                calcsFile = fc.getSelectedFile();
                //open the file
                log.append("Opening: " + calcsFile.getName() + "." + newline);
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
            
            
            
        //Handle save button action.
        } else if (e.getSource() == saveButton) {
        	cp.setProperty("offline", String.valueOf(offlineBool));
        	cp.setProperty("transform", String.valueOf(transformEnabled));
        	//cp.setProperty("multipos", String.valueOf(mpBool)); //Deprecated, always true
        	cp.setProperty("customcalcs", String.valueOf(calcBool));
        	cp.setProperty("workspace", String.valueOf(wsBool));

        	cp.setProperty("outputLocation", outputLocation.getText());
        	if (transfoFile != null) {cp.setProperty("transfofile", transfoFile.getPath());}
        		//else {log.append("No transformation file selected." + newline);} 
        	if (channelFile != null) {cp.setProperty("contourfile", channelFile.getPath());}
    			//else {log.append("No contour file selected." + newline);} 
        	if (calcsFile != null) {cp.setProperty("calcsfile", calcsFile.getPath());}
    			//else {log.append("No calcs file selected." + newline);} 
        	//TODO: add a safe go-on without calcs file or a checkbox for calcsenabled
        	try {
				cp.store(new FileOutputStream(System.getProperty("user.home") + "\\MFIoptions.cfg"), null);
	            log.append("Saving procedure as: " + System.getProperty("user.home") + "\\MFIoptions.cfg" + newline);
			} catch (IOException e1) {
				e1.printStackTrace();
				log.append("Save failed!" + newline);
			}
        	
        //Handle open button action.
        } else if (e.getSource() == browseButton) {
        	JFileChooser ch = new JFileChooser();
        	ch.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnVal = ch.showOpenDialog(ch);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = ch.getSelectedFile();
                if (file.isDirectory()) { outputLocation.setText(file.getAbsolutePath() + "\\" + "Datasheet.xlsx");}
                else {outputLocation.setText(file.getAbsolutePath());}
                log.append("Saving output under: " + outputLocation.getText() + newline);
            } else {
                log.append("Browse command cancelled by user." + newline);
            }
        }
        
        
        readyCheck(); //Check if all files and selections are made, enable ok button if they are
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
    	mainFrame.addToDesktopPane();
        //Display the window.
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    //load ROI from file
    public boolean loadRois(File file, Sequence seq)
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
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
//        });
//    }

    public void readyCheck() {
    	if (channelFile != null) {// && transfoFile != null) {
    		mainFrame.getOkBtn().setEnabled(true);
    	}
    }
    
	@Override
	public void acqImgReveived(TaggedImage image) {
		System.out.println("img reveived ");
		String pos;    
		try {
			pos = image.tags.getString("PositionName");
	    	System.out.println(newline + "current pos: " + image.tags.get("PositionName"));
		} catch (JSONException e1) {
			System.out.println("Could not get name, setting to Pos0"); //TODO triggers when multifret with one pos
			if (mpBool) {
				e1.printStackTrace();			
				return;
			} else {pos = "Pos0";}
		}
		
		if (startTime == 0) {
			startTime = System.nanoTime();
	 	    System.out.println("Start time = " + startTime);
		}	

		if (pause == false) {
			for (AcquiredObject ao : lastBatch) {
				if (ao.position.equals(pos)) {
					MultiFretIcy.PS.S1.SU1.lastBatch = lastBatch;
					lastBatch = new ArrayList<AcquiredObject>();
					break;
				}
			}
			
			
			System.out.println("img reveived run");
			for (Sequence z: sequences) {
				if (z.getName().contains("Acquisition") && z.getName().contains(" - " + pos + " - ")) {
					System.out.println("AcqObj made, pos: " + pos);
					AcquiredObject acqObj = new AcquiredObject(z.getLastImage(),System.nanoTime(),pos);
					lastBatch.add(acqObj);
					QR.QueueUp(acqObj);										
					//notify queue
					try {
						QR.RunQueue();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} 
	}

	@Override
	public void acquisitionStarted(SequenceSettings settings, JSONObject metadata) {
		
	}

	@Override
	public void acquisitionFinished(List<Sequence> result) {
		
	}
	
	public void runOffline() throws InvocationTargetException, InterruptedException {
		startTime = System.nanoTime();
 	    System.out.println("Start time = " + startTime);
		QR = new Queuer(); 		
		System.out.println("Offline MP run");
		for (Sequence s : sequences) {
			System.out.println("Queueing " + s.getName());
			for (IcyBufferedImage img : s.getAllImage()) {
				AcquiredObject AO = new AcquiredObject(img,System.nanoTime(),s.getName());
				QR.QueueUp(AO);
			}
	}				
		try {
			QR.RunQueue();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void exitThis() {
		System.out.println("img reveived exit");
		if (!offlineBool) {
			MicroManager.removeAcquisitionListener(Prestart.this);
		}
		//TODO temporary troubleshooting outs
		System.out.println("PS1");
		mainFrame.removeAll();
		//TODO temporary troubleshooting outs
		System.out.println("PS2");
		mainFrame.dispose();
		//TODO temporary troubleshooting outs
		System.out.println("PS3");
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// Checkbox handlers
		Object source = e.getItemSelectable();
		// TransfoEnable checkbox:
		if (source == transfoEnable && e.getStateChange() == ItemEvent.DESELECTED) {
	    	openButtonTransfo.setEnabled(false);
	    	transformEnabled = false;
	    } else if (source == transfoEnable && e.getStateChange() == ItemEvent.SELECTED) {
	    	openButtonTransfo.setEnabled(true);
	    	transformEnabled = true;
	    }		
		// Offline checkbox:
		if (source == offlineCheckBox && e.getStateChange() == ItemEvent.DESELECTED) {
			offlineCheckBox.setEnabled(false);
	    	offlineBool = false;
	    } else if (source == offlineCheckBox && e.getStateChange() == ItemEvent.SELECTED) {
	    	offlineCheckBox.setEnabled(true);
	    	offlineBool = true;
	    }				
    	// CustomCalc checkbox:
    	if (e.getSource() == calcCheckBox) {    	
    		calcBool = calcCheckBox.isSelected();
    	}    	
    	// Workspace checkbox:
    	if (e.getSource() == wsEnable) {    	
    		wsBool = wsEnable.isSelected();
    	}
		
	}
}

