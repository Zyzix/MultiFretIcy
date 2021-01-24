	package plugins.MasoudR.multifreticy.Main;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import icy.gui.dialog.MessageDialog;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.rectangle.Rectangle5D;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.AcquiredObject;
import plugins.MasoudR.multifreticy.DataObjects.BPpair;
import plugins.MasoudR.multifreticy.DataObjects.MyCoordinates;

public class Splitter {
	public ArrayList<ROI> 					rois;
	private ArrayList<IcyBufferedImage> 	cropImages;
	private DataType 						dataType;
	private ArrayList<BPpair>				seqBundleList;
	boolean 								fuck;
	public Startup		 					SU1;
	String[] 								possibilities;
	//File[]				transfoFiles = new File[3];
	public long 							count = 0;
	public ArrayList<Sequence>				concSeqList;
	public Sequence justaseq;
	
	// get acquired image containing ROIs
	@SuppressWarnings("deprecation")
	Splitter (ArrayList<Sequence> acquiredSeqs, ArrayList<ArrayList<MyCoordinates>> allCorners) throws InterruptedException{
		System.out.println("started S1");
		justaseq = acquiredSeqs.get(0);
		rois = new ArrayList<ROI>();
		cropImages = new ArrayList<IcyBufferedImage>();
		seqBundleList = new ArrayList<>();
		concSeqList = new ArrayList<Sequence>();
		fuck = false;
		SU1 = null;
		ArrayList<Sequence>	seqBundle = null;

		System.out.println("splitter checkpoint 1"); //TODO remove
		for (int r = 0; r < rois.size(); r++) {
			rois.get(r).setName(rois.get(r).getName() + " Channel#" + Integer.toString(r));
			rois.get(r).setShowName(true);
		}
		
		for (Sequence acquiredSeq : acquiredSeqs) {			
		// get current position
		String pos = "Pos0";
		if (!MultiFretIcy.PS.offlineBool) {
			pos = ExtractPos(acquiredSeq);		
		} else if (MultiFretIcy.PS.mpBool) {
			pos = acquiredSeq.getName();
		}
		
		// get image type
		dataType = acquiredSeq.getDataType_();
		
		// get contours for splitting
		rois = acquiredSeqs.get(0).getROIs();
		// Sort them
		Collections.sort(rois, new Comparator<ROI>() {
		        @Override
		        public int compare(ROI r2, ROI r1)
		        {

		            return  r1.getName().compareToIgnoreCase(r2.getName());
		        }
		    });

		// Create storage
		possibilities = new String[rois.size()];
		
		//Create image per ROI
		seqBundle = new ArrayList<Sequence>();
			for(int r = 0; r < rois.size(); r++)
			{
			    ROI roi = rois.get(r);
			    Rectangle5D bnd = roi.getBounds5D();
			    IcyBufferedImage img = new IcyBufferedImage((int)Math.round(bnd.getSizeX()), (int) Math.round(bnd.getSizeY()), 1, dataType);
			 
		    	acquiredSeq.getLastImage();
			    
			    // store image
			    cropImages.add(r, img);
			 
			    // create sequence
			    Sequence s = new Sequence(img);
			    
			    // change sequence name
			    s.setName(roi.getName());

			    // ready ROI options
			    possibilities[r] = s.getName();
			    
			    // create bundle of split images from single sequence(position)
			    seqBundle.add(s);
			}
			// pair bundles with their pos name
		    seqBundleList.add(new BPpair(seqBundle, pos));

		    // Transform
		    ArrayList<Sequence> seqBundle3 = new ArrayList<Sequence>();
			Sequence[] seqBundle2 = new Sequence[seqBundle.size()];
		    if (allCorners != null && MultiFretIcy.PS.transformEnabled == true) {				
			    for (Sequence seq : seqBundle) {
			    	/*
			    	 * we get roi name from xml file in cornerfinder
			    	 * here we need to get image + roi name to send to transformer along with corners
			    	 */
			    	for (ArrayList<MyCoordinates> mc : allCorners) {
			    		if (seq.getName().equals(mc.get(0).getRoiName())) {
			    			AffineWarp AW = new AffineWarp(mc,seq);
			    			seqBundle3.add(new Sequence(AW.GeometricTransforms(mc, seq)));	    				
			    		}
			    	}
			    	
			    }
		    	//Concatenate split images
				seqBundle3.toArray(seqBundle2);			    
			}	else {				
			    	//Concatenate split images
					seqBundle.toArray(seqBundle2);
				}
		    // create sequence 
			Sequence concS = new Sequence();
			concS.beginUpdate();
			concS = SequenceUtil.concatZ(seqBundle2, false, false, true);
			concS.endUpdate();
			
		    // set sequence name
		    concS.setName(pos);
		    
		    // set channel names
		    for(int r = 0; r < rois.size(); r++)  {
		    	concS.setChannelName(r, rois.get(r).getName());
		    } 
		    
		    //TODO concS.setOriginChannel(0); Doesn't do what you'd like		    
		    System.out.println("imgbounds: " + concS.getFirstImage().getBounds());
		    concS.getBounds2D();
		    
		    // List
		    concSeqList.add(concS);
			System.out.println("Initial split triggered");	
			
			// Initial split so SU has something to work with
			Split(new AcquiredObject(acquiredSeq.getLastImage(),System.nanoTime(),pos),false); 

		    Icy.getMainInterface().addSequence(concS);
		    concS.getFirstViewer().getInternalFrame().setName("concS " + concS.getName());
			if (MultiFretIcy.PS.wsBool) {
				concS.getFirstViewer().getInternalFrame().setVisible(false);
			}
		    
			concS.updateChannelsBounds();
			System.out.println("CONQS BOUNDS: " + concS.getBounds());
		//Offline mode
			//TODO

		}
	}
	
	public void CreateSU(File calcsFile) throws IOException {
		SU1 = new Startup(concSeqList, possibilities, calcsFile); 
		SU1.initialize();
		System.out.println("###Initialisation complete###############");	
	}	
	
	public void Split(AcquiredObject acqObj, boolean x) {
	    // Update images
	    IcyBufferedImage newImage = null;
		try {
			newImage = acqObj.acqImg;
		} catch (Exception e) {
			MessageDialog.showDialog("ERROR ACQUIRING IMAGE");
			e.printStackTrace();
		}
		
		for(int q = 0; q < seqBundleList.size(); q++) {
			if(seqBundleList.get(q).Pos.equals(acqObj.position)) {
				ArrayList<Sequence> seqBundle = seqBundleList.get(q).Bundle;
				System.out.println("splitting "+ seqBundleList.get(q).Pos + "which is " + acqObj.position);
		        Sequence concS = null;
		        
				for(int r = 0; r < rois.size(); r++)  {  	 
			       	seqBundle.get(r).beginUpdate();

			    	ROI roi = rois.get(r);
			    	
			    	//get roi area
			        Rectangle bnd = roi.getBounds5D().toRectangle2D().getBounds();
			        //retrieve canvas
			        IcyBufferedImage img = cropImages.get(r);
			        //write image from roi area to canvas 			     
			        img.copyData(newImage, bnd, null);
			        
			        seqBundle.get(r).removeAllImages();
			        seqBundle.get(r).addImage(img);
			        
			        for (Sequence s : concSeqList) {
			        	if (s.getName().equals(acqObj.position)) {
			        		concS = s;
			        	}
			        }
			        
			        if (concS == null) {System.out.println("No Matching concS");}
			       
			        concS.beginUpdate();	
		        	concS.setImage(0, r, IcyBufferedImageUtil.scale(img, concS.getWidth(), concS.getHeight())); 
		        	concS.endUpdate();

			        // If it's not the initial split, we set x to true and do transformations
			        if (x==true) {
				        String roiN = rois.get(r).getName();
				        System.out.println("Split made: " + roiN);
				        // Check if roiN is not the base, and if transforms are a go -> transform it
				        if (MultiFretIcy.PS.transformEnabled == true) { 
				        } 
			        }
			        seqBundle.get(r).endUpdate();

			    }			
			    // show it
			    //concS.setAutoUpdateChannelBounds(true); //generates much lag
			}
		}
	}
		
	public void Transform(Sequence source, File transfoFile, String transfoName) {
	

	}


	public void ExitThis() {
		//TODO temporary troubleshooting outs
		System.out.println("SP1");
		for (BPpair seqBundle : seqBundleList) {
			for (Sequence seq : seqBundle.Bundle) {
				seq.close();
			}
			if ( SU1 != null ) {
				//SU1.clean();
				SU1 = null;
			}
		}
		//TODO temporary troubleshooting outs
		System.out.println("SP2");
	}
	 
	public String ExtractPos(Sequence posSeq) {
		 String Pos;
			Pos = StringUtils.substringBetween(posSeq.getName(), " - ");		
			return Pos;		
	}

}
