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


package plugins.Zyzyx.theproj;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.json.JSONObject;
import org.micromanager.api.SequenceSettings;
import org.w3c.dom.Document;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.sequence.SequenceActionFrame;
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
                             implements ActionListener, AcquisitionListener {
    static private final String newline = "\n";
    JButton 							openButtonTransfo, openButtonChannel, saveButton;
    JTextArea 							log;
    static JFileChooser 				fc;
    static File 						channelFile;
    static File 						transfoFile;
    final static SequenceActionFrame 	mainFrame = new SequenceActionFrame("Example", true);
    public static boolean 				pause = false;
	ArrayList<Thread> 					threads = new ArrayList<Thread>();
	
	private static Sequence 					sequence;
    private Splitter 					S1;

	public Prestart() {
    	mainFrame.getOkBtn().setEnabled(false);
    	//Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

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

        //Add the buttons and the log to this panel.
        mainFrame.getMainPanel().add(buttonPanel, BorderLayout.PAGE_START);
        mainFrame.getMainPanel().add(logScrollPane, BorderLayout.CENTER);

        //define action to do when OK button is pressed
        mainFrame.setOkAction(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) //On OK click
            {
               // get selected sequence
               sequence = mainFrame.getSequence();
              // sequence.addListener(listener);
               //remove ROI and load contour ROI from channelFile
               sequence.removeAllROI();
               if(LoadRois(channelFile,sequence)) { log.append("Loaded ROIs from " + channelFile.getName() + newline);}
               else	{MessageDialog.showDialog("LoadRois failed"); return;}
               //Create Excel WB
                CreateWorkBook wbc = null;
				try {
					wbc = new CreateWorkBook();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				//Create Splitter thread
	       		S1 = new Splitter(sequence, transfoFile, wbc);
	
	    		// Split the image
	    		threads.add(S1);
	    		S1.setName("S1");
	    		System.out.println("start S1");
	    		S1.start();
	    		System.out.println(S1.getName());
	    	    MicroManager.addAcquisitionListener(Prestart.this);
	    	    
	    	    

	    		
               /*               //TODO:load channels file, split image, (remove channelsfile ROIs), apply correction from transfo file
               	*				//TODO:continue to Draw Roi(s) prompt which copy-pastes (and listens for drawn roi to update gui? and has a checkbox to auto-duplicate or manual?)
                * Prestart = micromanager listener, runs splitter every acquisition
                * Splitter(sequence, transfoFile) -> seqList,transfoFile
                * 	(run)TheProj(seqList, transfoFile) -> seqList
                * 	(start)Startup(seqList) -> seqList,ROIdata
                * 		Threading(seqList,ROIdata) -> ExcelData
                */
                
                // no sequence
                if (sequence == null || channelFile == null || transfoFile == null) {
                    MessageDialog.showDialog("No sequence/file/file selected");
                }
                else {
                    //MessageDialog.showDialog("You have selected : " + sequence.getName());
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
        //TODO: Saves settings to be loaded on next boot, remove the location prompt and save to documents
        } else if (e.getSource() == saveButton) {
            int returnVal = fc.showSaveDialog(fc);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //TODO:This is where a real application would save the file.
                log.append("Saving: " + file.getName() + "." + newline);
            } else {
                log.append("Save command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
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
		if (pause == false) {
			S1.run(sequence.getLastImage(), transfoFile);
		}
	}

	@Override
	public void acquisitionStarted(SequenceSettings settings, JSONObject metadata) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void acquisitionFinished(List<Sequence> result) {
		// TODO Auto-generated method stub
		
	}
	
}

