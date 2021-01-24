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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.utils.MMScriptException;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.ToolTipFrame;
import net.miginfocom.swing.MigLayout;
import plugins.MasoudR.multifreticy.DataObjects.PicturePosition;
import plugins.MasoudR.multifreticy.DataObjects.StretchIcon;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;
 
public class Detector {
	IcyFrame theFrame 			= new IcyFrame();
    JTextField	pixelSize		= new JTextField("pixelSize");
    JTextField	viewportW		= new JTextField("Viewport Width");
    JTextField	viewportH		= new JTextField("Viewport Height");
    JButton		findPSize		= new JButton("Find Pixel Size");
    JButton		selectAll 		= new JButton("Select All");
    JButton		deselectAll		= new JButton("Deselect All");
    JButton		setTL 			= new JButton("Set Top-Left");
    JButton		setBR 			= new JButton("Set Bottom-Right");
    JButton		scanButton		= new JButton("Scan");
    JButton		exportButton	= new JButton("Send To MM-MDA");
    JCheckBox	focusBox		= new JCheckBox("Include Z");
    
    MigLayout grLayout = new MigLayout("wrap 4, w 700, h 500");
    JScrollPane scrollPane;
    
    Point2D.Double tlPos 		= null; 
    Point2D.Double brPos		= null;
    Point2D.Double currentPos	= null;    
    int findPStatus				= 0;
    Point2D.Double ppL			= null;
    Point2D.Double ppR			= null;       
    
	JList<PicturePosition> picList;

    final JPanel compsToExperiment 		= new JPanel();
	private boolean abort = false;
    
    public Detector(String name) throws Exception {

    	selectAll.setSize(100, 30);
    	setTL.setSize(100, 30);
    	setBR.setSize(100, 30);
    	
    	theFrame = new IcyFrame("MultiFret", true, true);
    	theFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
    	theFrame.setMaximisable(true);
    	theFrame.setAlwaysOnTop(true);
    	theFrame.getContentPane().setLayout(new MigLayout());

    	addComponentsToPane(theFrame.getContentPane());
    	
		theFrame.addToDesktopPane();
		theFrame.pack();
		theFrame.setVisible(true);
    }    

     
    public void addComponentsToPane(final Container pane) throws Exception {
        compsToExperiment.setLayout(grLayout);        
        JPanel controls = new JPanel();
        controls.setLayout(new MigLayout("wrap 5"));
         
        //Set up components preferred size
//        compsToExperiment.setPreferredSize(new Dimension(400,400));
//        controls.setPreferredSize(new Dimension(200,50)); 
        
        //Add controls to set up
        controls.add(pixelSize, "w 110:110:110, h 20:20:20");
        controls.add(viewportW, "w 110:110:110, h 20:20:20");
        controls.add(viewportH, "w 110:110:110, h 20:20:20");
        controls.add(findPSize, "w 110:110:110, h 20:20:20");
        controls.add(focusBox, "w 110:110:110, h 20:20:20");
        controls.add(setTL, "w 110:110:110, h 20:20:20");
        controls.add(setBR, "w 110:110:110, h 20:20:20");
        controls.add(scanButton, "w 110:110:110, h 20:20:20");
        controls.add(selectAll, "w 110:110:110, h 20:20:20");
        controls.add(deselectAll, "w 110:110:110, h 20:20:20");
        controls.add(exportButton, "w 120:120:120, h 20:20:20");

         
        //Process the buttons
        findPSize.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
					pixelSizer();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
        setTL.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
					tlPos = StageMover.getXY();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
            }
        });
        
        setBR.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
					try {
						brPos = StageMover.getXY();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
            }
        });
        
        scanButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		//Set up thread for scan
    			Thread t = new Thread(new Runnable() {
    				public void run() {
            			System.out.println("Initiating field scan...");
            			try {
							picList = new JList(scanField().toArray());
                			System.out.println("Field scan complete, populating gallery...");
                			addToPane(picList);
                			System.out.println("Gallery ready.");
						} catch (Exception e) {
							System.out.println("Scan Failed.");
							e.printStackTrace();
						}

    				}
    			});
    			// Activate thread and listen for abort
        		if (scanButton.getText().equals("Scan")) {
        		scanButton.setText("Abort");
    	    	theFrame.revalidate();
    	    	theFrame.repaint();
        			t.start();
        		} else if (scanButton.getText().equals("Abort")) {
        			try {
						StageMover.stopXYStage();
					} catch (Exception e1) {
						System.out.println("Could not stop stage!");
						e1.printStackTrace();
					}
//        			abort = true;
        			t.interrupt();
//        			while(abort) {} //TODO freezes
        			System.out.println("Aborted scan.");        			
        			scanButton.setText("Scan");
        	    	theFrame.revalidate();
        	    	theFrame.repaint();
        		}
        	}
        });
        
        selectAll.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
//TODO
            }
        });
        
        deselectAll.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
//TODO
            }
        });
        
        exportButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
     		   PositionList mmPositions = new PositionList();

            	
         	   for (int i = 0; i < picList.getModel().getSize(); i++) {
        		   PicturePosition pp = (PicturePosition) picList.getModel().getElementAt(i);
        		   if (pp.getSel()) {
        			   System.out.println(pp.getPos());
        			   MultiStagePosition ppPos = new MultiStagePosition(
        					   							StageMover.getXYStageDevice(), 
        					   							pp.getPos().getX(),
        					   							pp.getPos().getY(),
        					   							StageMover.getZFocusDevice(),
        					   							pp.getZFocus());
        			   mmPositions.addPosition(ppPos);

        		   } else {System.out.println("Not sel");}
         	   }
    		   try {
				MicroManager.getMMStudio().setPositionList(mmPositions);
				MessageDialog.showDialog("Positions exported to Micro-Manager, Multi-Dimensional Acquisition!");
    		   } catch (MMScriptException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
    		   }
            }
        });
        
        scrollPane = new JScrollPane(compsToExperiment);
//    	scrollPane.createVerticalScrollBar();
    	scrollPane.getHorizontalScrollBar().setUnitIncrement(50);
    	scrollPane.getVerticalScrollBar().setUnitIncrement(50);  
    	
        pane.add(scrollPane, "grow 1");
        pane.add(new JSeparator(), "center");
        pane.add(controls, "south");
    }   

   private ArrayList<PicturePosition> scanField() throws Exception {
	   //Check if range is set
	   if (tlPos == null || brPos == null || tlPos == brPos) 
	   {
		   new ToolTipFrame("Set bottom-left and top-right of your dish first!", 10); return null;
	   } else {System.out.println("TL: " + tlPos + " BR: " +  brPos);}
	   //Generate vars and move to starting position
	   Point2D.Double currentTL = tlPos;	   
	   ArrayList<PicturePosition> list = new ArrayList<PicturePosition>();	
	   System.out.println("Moving to TL");
	   StageMover.moveXYAbsolute(tlPos.getX(), tlPos.getY());
	   //Determine axis direction
	   int altx = (tlPos.getX()<=brPos.getX()) ? 1:-1;
	   int alty = (tlPos.getY()<=brPos.getY()) ? 1:-1;  
	   	   
	   while ( //Check which way the Y-axis runs and if we are between the points 
				(alty>0 && StageMover.getXY().getY() >= tlPos.getY() && StageMover.getXY().getY() <= brPos.getY()) 
				|| (alty<0 && StageMover.getXY().getY() <= tlPos.getY() && StageMover.getXY().getY() >= brPos.getY())
			   ) 
	   {	
   		   if (abort) 
   		   {
   			   abort = false;  //TODO: test the following:
               SwingUtilities.invokeLater(new Runnable() 
               {
            	   public void run() 
            	   {
            		   scanButton.setText("Scan");	
            	   }
        	   });
   			   System.out.println("abort set to false");
   			   return null;
   		   }
   		   while ( //Check which way the x-axis runs and if we are between the points 
   				(altx>0 && StageMover.getXY().getY() >= tlPos.getY() && StageMover.getXY().getY() <= brPos.getY()) 
   				|| (altx<0 && StageMover.getXY().getY() <= tlPos.getY() && StageMover.getXY().getY() >= brPos.getY())
   			   ) 	   
   		   {
	   		   if (abort) 
	   		   {
	   			   abort = false; 
	               SwingUtilities.invokeLater(new Runnable() 
	               {
	            	   public void run() 
	            	   {
	            		   scanButton.setText("Scan");	
	            	   }
	        	   });	   			   
	   			   System.out.println("abort set to false");
	   			   return null;
	   		   }
		   		System.out.println("Next in Row");
			   if (!focusBox.isEnabled()) {list.add(new PicturePosition(MicroManager.snapImage(), StageMover.getXY()));}
			   else {list.add(new PicturePosition(MicroManager.snapImage(), StageMover.getXY(), StageMover.getZ()));}
			   StageMover.moveXYRelative(altx * Double.parseDouble(viewportW.getText()) * Double.parseDouble(pixelSize.getText()), 0);
		   }
		   System.out.println("New row");
		   StageMover.moveXYAbsolute(currentTL.getX(), currentTL.getY());
		   StageMover.moveXYRelative(0, alty * Double.parseDouble(viewportH.getText()) * Double.parseDouble(pixelSize.getText()));
		   currentTL = StageMover.getXY();
	   }
       SwingUtilities.invokeLater(new Runnable() 
       {
    	   public void run() 
    	   {
    		   scanButton.setText("Scan");	
    	   }
	   });	  
		System.out.println("Scan success");
	   return list;
   }
   
   
   private void pixelSizer() throws Exception {
	   switch (findPStatus) {
	   case 0:
			findPSize.setText("Continue:1");   
	    	theFrame.revalidate();
	    	theFrame.repaint();
			new ToolTipFrame("Move an identifiable pixel to the top-left and click Continue:1.");
			findPStatus = 1;
		break;
	   case 1:
		    ppL = StageMover.getXY();
			findPSize.setText("Continue:2");   
	    	theFrame.revalidate();
	    	theFrame.repaint();
			new ToolTipFrame("Move the same pixel to the bottom-right and click Continue:2.");
			findPStatus = 2;
		    break;
	   case 2:
		    ppR = StageMover.getXY();
			findPSize.setText("Finish"); 
			new ToolTipFrame("Enter viewport width and height, then click Finish to obtain pixel-Size in Micro-Manager's used units.");
			findPStatus = 3;
			break;
	   case 3:		  
		    findPStatus = 0;
			findPSize.setText("Find Pixel Size"); 
		    double distX = ppR.getX() - ppL.getX();
		    double distY = ppR.getX() - ppL.getX();
		    pixelSize.setText(Double.toString(
		    		(distX / Double.parseDouble(viewportW.getText())) 
		    		+ (distY / Double.parseDouble(viewportH.getText())) / 2));    
		    break;
	   }	   
   }
   
   public void addToPane(JList<PicturePosition> pL) throws Exception {
	   //Selection mouse event
	   for (int i = 0; i < pL.getModel().getSize(); i++) {
		   PicturePosition pp = (PicturePosition) pL.getModel().getElementAt(i);
		   BufferedImage img = pp.getImg();
		   int rsW = img.getWidth() < 100 ? 100 : img.getWidth();
		   int rsH = img.getHeight() < 100 ? 100 : img.getHeight();			   
		   JLabel imgHolder = new JLabel(new StretchIcon(resizeImage(img, rsW, rsH)));

		   Border b1 = new BevelBorder(
	                BevelBorder.LOWERED, Color.GREEN, Color.BLUE);
		   
		   imgHolder.addMouseListener(new MouseAdapter()  
		   	{  
		   	    public void mouseClicked(MouseEvent e)  
		   	    {  
	   	    	
		   	    	if(imgHolder.getBorder() == null) 
		   	    	{
		   	    		imgHolder.setBorder(b1); 
		   	    		pp.setSel(true);
		   	    		} 
		   	    	else {
		   	    		imgHolder.setBorder(null);
		   	    		pp.setSel(false);
		   	    		}		   	    	
	
				   	compsToExperiment.revalidate();
				   	compsToExperiment.repaint();
		   	    }
		   	});         				
		   	
		   	compsToExperiment.add(imgHolder, "grow 1, push, w 100!, h 100!");
		   	compsToExperiment.revalidate();
		   	compsToExperiment.repaint();

		   }
   }   
   
   BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
	    BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
	    Graphics2D graphics2D = resizedImage.createGraphics();
	    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	    graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
	    graphics2D.dispose();
	    return resizedImage;
	}
}


