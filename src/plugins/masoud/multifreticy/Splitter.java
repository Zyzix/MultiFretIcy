	package plugins.masoud.multifreticy;

import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Jama.Matrix;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.rectangle.Rectangle5D;
import icy.util.XMLUtil;
import poi.CreateWorkBook;

public class Splitter {
	private ArrayList<ROI> 					rois;
	private ArrayList<IcyBufferedImage> 	cropImages;
	private DataType 						dataType;
	private ArrayList<Sequence> 			seqList;
	boolean 								fuck;
	public static Startup 					SU1;
	private boolean 						doneSU;
	String[] 								possibilities;
	//File[]				transfoFiles = new File[3];
	private String 							base = "TL";
	public long 							count = 0;
	
	// get acquired image containing ROIs
	Splitter (Sequence acquiredSeq, File transfoFile) throws InterruptedException{
		System.out.println("started S1");
		rois = new ArrayList<ROI>();
		cropImages = new ArrayList<IcyBufferedImage>();
		seqList = new ArrayList<Sequence>();
		fuck = false;
		SU1 = null;
		doneSU = false;
		
		System.out.println("splitter checkpoint 1");
		// get image type
		dataType = acquiredSeq.getDataType_();
		// get rois
		rois = acquiredSeq.getROIs();
		possibilities = new String[rois.size()];
		//Create image per ROI
		for(int r = 0; r < rois.size(); r++)
		{
		    ROI roi = rois.get(r);
		    Rectangle5D bnd = roi.getBounds5D();
		    IcyBufferedImage img = new IcyBufferedImage((int)Math.round(bnd.getSizeX()), (int) Math.round(bnd.getSizeY()), 1, dataType);
		 
		    // store image
		    cropImages.add(r, img);
		 
		    // create sequence
		    Sequence s = new Sequence(img);
		    // change sequence name
		    s.setName(roi.getName());
		    // show it
		    Icy.getMainInterface().addSequence(s);
		    seqList.add(s);
		    possibilities[r] = s.getName();
		}
    	//Split images
		if (!Prestart.offlineBool) {
			Split(acquiredSeq.getLastImage(),transfoFile,false);
		} else {Split(acquiredSeq.getFirstImage(),transfoFile,false);}
		//If no transfoFile has been selected, we run a new transformation
		if (transfoFile == null && Prestart.transformEnabled == true) {
			//Select base channel
	    	base = (String)JOptionPane.showInputDialog(
                    null,
                    "Select Base channel",
                    "Select Base channel",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    possibilities,
                    possibilities[0]);
			//Run TransfoMaker
			System.out.println("No transfoFile, initiating generation procedure, sequence list size: " + seqList.size());
			TransfoMaker TFM = new TransfoMaker(seqList,base);
			TFM.run();
			Prestart.pause = true;
			System.out.println("a wait");
		}
	}

	public void run(IcyBufferedImage acqImg, File transfoFile) throws InvocationTargetException, InterruptedException {
System.out.println("running splitter my dude");
		if (doneSU == false && Prestart.offlineBool == false) {
			SU1 = new Startup(seqList, possibilities);
			SU1.createUI();
			SU1.showUI();
			doneSU = true;
			System.out.println("###Initialisation complete###############");
		} else if (Prestart.offlineBool == false) {
			Split(acqImg,transfoFile,true);
	        //SU1.RunThreads();
		}
		if (doneSU == false && Prestart.offlineBool == true) {
			SU1 = new Startup(seqList, possibilities);
			SU1.createUI();
			SU1.showUI();
			doneSU = true;
		} else if (Prestart.offlineBool == true) {
			Split(acqImg,transfoFile,true);
		}
	}

	public void Split(IcyBufferedImage acqImg, File transfoFile,boolean x) {
	    // Update images
	    IcyBufferedImage newImage = null;
		try {
			newImage = acqImg;
		} catch (Exception e) {
			MessageDialog.showDialog("ERROR ACQUIRING IMAGE");
			e.printStackTrace();
		}
	    for(int r = 0; r < rois.size(); r++)
	    {
	    	seqList.get(r).beginUpdate();
	    	
	    	ROI roi = rois.get(r);
	        Rectangle bnd = roi.getBounds5D().toRectangle2D().getBounds();
	        IcyBufferedImage img = cropImages.get(r);
	        img.copyData(newImage, bnd, null);
	        
	        seqList.get(r).removeAllImages();
	        seqList.get(r).addImage(img);
	        // If it's not the initial split, we set x to true and do transformations
	        if (x==true) {
	       // System.out.println("Running transformation for new image");
		        String roiN = rois.get(r).getName();
		        System.out.println("Split made: " + roiN);
		        // Check if roiN is not the base, and if transforms are a go -> transform it
		        if (roiN != base && Prestart.transformEnabled == true) { 
		        Transform(seqList.get(r), transfoFile, roiN);
		        } 
	        }
	        seqList.get(r).endUpdate();	        
	    }
	}
		
	public void Transform(Sequence source, File transfoFile, String transfoName) {
		
		final Document document = XMLUtil.loadDocument(transfoFile);
		System.out.println("transform on file: " + transfoFile);
		Element root = XMLUtil.getRootElement(document);
		Element subroot = XMLUtil.getElement(root, transfoName);
			
		System.out.println(root.getNodeName());
		System.out.println(subroot.getNodeName());
		Element newsizeelement = XMLUtil.getElement( subroot , "TargetSize" );

		// the following variable will get the default value is the transformation was computed in manual 2D.
		double targetsx =XMLUtil.getAttributeDoubleValue( newsizeelement, "sx" , -1 );
		double targetsy =XMLUtil.getAttributeDoubleValue( newsizeelement, "sy" , -1 );
		double targetsz =XMLUtil.getAttributeDoubleValue( newsizeelement, "sz" , -1 );
		
		ProgressFrame progress = new ProgressFrame("Applying 2D RIGID transformation...");	
		CPImageTransformer mytransformer = new CPImageTransformer();
		
		Matrix CombinedTransfo = null;
		if(!XMLUtil.getBooleanValue(subroot, false)) {
			CombinedTransfo=getCombinedTransfo(document, transfoName);
			mytransformer.setParameters(CombinedTransfo);
		}    

		mytransformer.setImageSource(source);
		mytransformer.setDestinationsize(168,133);
		mytransformer.run();
		progress.close();
		if (targetsx!=-1) //xml file generated with oldest version for 2D if -1, do nothing
		{
			source.setPixelSizeX(targetsx);
			source.setPixelSizeY(targetsy);
			source.setPixelSizeZ(targetsz);
		}
	}
	
	/**
	 * compute (again) the combine transformed to avoid interpolation errors	
	 * @param document
	 * @return
	 */
	public Matrix getCombinedTransfo(Document document, String transfoName){
		Element root = XMLUtil.getRootElement(document);
		Element subroot = XMLUtil.getElement(root, transfoName);



		ArrayList<Element> transfoElementArrayList = XMLUtil.getElements( subroot , "MatrixTransformation" );

		ArrayList<Matrix> listoftransfo=new ArrayList<Matrix>();
		for ( Element transfoElement : transfoElementArrayList )
		{
			double[][] m=new double[4][4];


			m[0][0] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m00" , 0 );
			m[0][1] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m01" , 0 );
			m[0][2] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m02" , 0 );	
			m[0][3] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m03" , 0 );

			m[1][0] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m10" , 0 );
			m[1][1] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m11" , 0 );
			m[1][2]= XMLUtil.getAttributeDoubleValue(  transfoElement, "m12" , 0 );	
			m[1][3] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m13" , 0 );

			m[2][0]= XMLUtil.getAttributeDoubleValue(  transfoElement, "m20" , 0 );
			m[2][1] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m21" , 0 );
			m[2][2] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m22" , 0 );	
			m[2][3] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m23" , 0 );

			m[3][0] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m30" , 0 );
			m[3][1] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m31" , 0 );
			m[3][2] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m32" , 0 );	
			m[3][3] = XMLUtil.getAttributeDoubleValue(  transfoElement, "m33" , 0 );


			Matrix T=new Matrix(m);
			listoftransfo.add(T);


		}
		Matrix CombinedTransfo=Matrix.identity(4, 4);
		for (int i=0;i<listoftransfo.size();i++){
			CombinedTransfo=listoftransfo.get(i).times(CombinedTransfo);
		}
		return CombinedTransfo;
	}
	/**
	 * get the xml file
	 * @return
	 */
	public Document getdocumentTitle(File xmlFile) {
		Document document = XMLUtil.loadDocument( xmlFile );
		return document;
	}

	public void ExitThis() {
		for (Sequence seq : seqList) {
			seq.close();
		}
		if ( SU1 != null ) {
			SU1.clean();
			SU1 = null;
		}
	}
	 
	 

}
