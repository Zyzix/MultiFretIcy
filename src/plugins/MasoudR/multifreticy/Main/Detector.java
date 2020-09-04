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
import java.awt.GridLayout;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.micromanager.utils.MMScriptException;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyInternalFrame;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.PicturePosition;
import plugins.MasoudR.multifreticy.DataObjects.StretchIcon;
 
public class Detector {
	IcyFrame theFrame = new IcyFrame();
    static final String gapList[] = {"0", "10", "15", "20"};
    final static int maxGap = 20;
    JButton		selectAll 	= new JButton("Select All");
    JButton		setTL 		= new JButton("Set Top-Left");
    JButton		setBR 		= new JButton("Set Bottom-Right");
    GridLayout experimentLayout = new GridLayout(0,2);
    Point2D.Double tlPos = null; 
    Point2D.Double brPos = null;
    Point2D.Double currentPos = null;
    

    
    // File representing the folder that you select using a FileChooser
    static final File dir = new File("C:\\Users\\mr2617\\Desktop\\imges");

    // array of supported extensions (use a List if you prefer)
    static final String[] EXTENSIONS = new String[]{
        "gif", "jpg", "png", "bmp" // and other formats you need
    };
    
    // filter to identify images based on their extensions
    static final FilenameFilter IMAGE_FILTER = new FilenameFilter() {

        @Override
        public boolean accept(final File dir, final String name) {
            for (final String ext : EXTENSIONS) {
                if (name.endsWith("." + ext)) {
                    return (true);
                }
            }
            return (false);
        }
    };
    
    public Detector(String name) throws Exception {
    	selectAll.setSize(100, 30);
    	setTL.setSize(100, 30);
    	setBR.setSize(100, 30);
    	
    	theFrame = new IcyFrame("MultiFret", true, true);
    	theFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
    	theFrame.setMaximisable(true);
    	theFrame.setAlwaysOnTop(true);
    	theFrame.getContentPane().setLayout(new BoxLayout(theFrame.getContentPane(), BoxLayout.Y_AXIS));
    	
    	addComponentsToPane(theFrame.getContentPane());
    	
		theFrame.addToDesktopPane();
		theFrame.pack();
		theFrame.setVisible(true);
    }    

     
    public void addComponentsToPane(final Container pane) throws Exception {
        final JPanel compsToExperiment = new JPanel();
        compsToExperiment.setLayout(experimentLayout);
        JPanel controls = new JPanel();
        controls.setLayout(new GridLayout(2,3));
         
        //Set up components preferred size
        compsToExperiment.setPreferredSize(new Dimension(400,400));
        
        for(BufferedImage img : imageGetter(dir)) {
        	JLabel imgHolder = new JLabel(new StretchIcon(img));

        	imgHolder.addMouseListener(new MouseAdapter()  
        	{  
        	    public void mouseClicked(MouseEvent e)  
        	    {  
        	    	for(int y = 0; y < 	((BufferedImage)((StretchIcon) imgHolder.getIcon()).getImage()).getHeight(); y++)
        	    	    for(int x = 0; x < ((BufferedImage)((StretchIcon) imgHolder.getIcon()).getImage()).getWidth(); x++)
        	    	    {
        	    	        Color imageColour = new Color(((BufferedImage)((StretchIcon) imgHolder.getIcon()).getImage()).getRGB(x, y));
        	    	        //mix imageColor and desired color 
        	    	        Color newColour = new Color(255-imageColour.getRed(), 255-imageColour.getGreen(), 255-imageColour.getBlue());
        	    	        ((BufferedImage)((StretchIcon) imgHolder.getIcon()).getImage()).setRGB(x, y, newColour.getRGB());
        	    	    }
        	    	
                	theFrame.revalidate();
                	theFrame.repaint();
        	    }
        	});         		

        	
        	compsToExperiment.add(imgHolder);
        }
        
        //Add MM snap
        PicturePosition pP = new PicturePosition();
        pP.setPos();
        pP.setImg();
        JLabel snapHolder = new JLabel(new StretchIcon(pP.getImg()));
        compsToExperiment.add(snapHolder);
        
        //Add controls to set up
        controls.add(setTL);
        controls.add(setBR);
        controls.add(selectAll);

         
        //Process the buttons
        setTL.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
					tlPos = MultiFretIcy.PS.mStudio.getXYStagePosition();
				} catch (MMScriptException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
        setBR.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
            	try {
					brPos = MultiFretIcy.PS.mStudio.getXYStagePosition();
				} catch (MMScriptException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
        selectAll.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){

            }
        });
        
        pane.add(compsToExperiment, BorderLayout.NORTH);
        pane.add(new JSeparator(), BorderLayout.CENTER);
        pane.add(controls, BorderLayout.SOUTH);
    }   


   public ArrayList<BufferedImage> imageGetter(File path){
	   ArrayList<BufferedImage> imgList = new ArrayList<BufferedImage>();
    	if (dir.isDirectory()) { // make sure it's a directory
            for (final File f : dir.listFiles(IMAGE_FILTER)) {
                BufferedImage img = null;

                try {
                    img = ImageIO.read(f);
                    imgList.add(img);
                    
                    System.out.println("image: " + f.getName());
                    System.out.println(" width : " + img.getWidth());
                    System.out.println(" height: " + img.getHeight());
                    System.out.println(" size  : " + f.length());
                } catch (final IOException e) {
                    // handle errors here
                }
            }
        }
		return imgList;
    }
    
}
