	package plugins.masoud.multifreticy;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import Jama.Matrix;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.rectangle.Rectangle5D;
import icy.util.XMLUtil;
import poi.CreateWorkBook;

public class Splitterurk {
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
	File XMLFile;
	String fileName = "fuckMatrices.xml";
	
	// get acquired image containing ROIs
	Splitterurk (Sequence acquiredSeq, File transfoFile) throws InterruptedException{
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
		System.out.println("splitter checkpoint 1.2");
		int m = 460;
		for(int r = 0; r < rois.size(); r++)
		{
			System.out.println("splitter checkpoint 1.3");

		    ROI roi = rois.get(r);
		    
		    IcyBufferedImage img = new IcyBufferedImage(130,100, 1, dataType);
		    IcyBufferedImage copyImg = acquiredSeq.getLastImage();
		    //IcyBufferedImageUtil.toBufferedImage(copyImg, img);

			// store image
		    cropImages.add(r, img);
		 
//		    // create sequence
		    Sequence s = new Sequence(img);
		    
		    // change sequence name
		    s.setName(roi.getName() + "1");
		    
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
	    int m = 480;

	    
	    double[][] targetpoints = new double[4][3]; //X Y Z
	    targetpoints[0][0] = 0; //TL
	    targetpoints[0][1] = 0;
	    targetpoints[0][2] = 0;
	    targetpoints[0][0] = 130; //TR
	    targetpoints[0][1] = 0;
	    targetpoints[0][2] = 0;
	    targetpoints[0][0] = 0; //BL
	    targetpoints[0][1] = 100;
	    targetpoints[0][2] = 0;
	    targetpoints[0][0] = 130; //BR
	    targetpoints[0][1] = 100;
	    targetpoints[0][2] = 0;
	  //  m += 60;
		System.out.println("splitter checkpoint 1.4 m=" + m);


		try {
			newImage = acqImg;
		} catch (Exception e) {
			MessageDialog.showDialog("ERROR ACQUIRING IMAGE");
			e.printStackTrace();
		}
	    for(int r = 0; r < rois.size(); r++)
	    {  	   	
		    double[][] sourcepoints = new double[4][3]; //X Y Z
		    sourcepoints[0][0] = 250; //TL
		    sourcepoints[0][1] = 200;
		    sourcepoints[0][2] = 0;
		    sourcepoints[0][0] = 450; //TR
		    sourcepoints[0][1] = 200;
		    sourcepoints[0][2] = 0;
		    sourcepoints[0][0] = 0; //BL
		    sourcepoints[0][1] = 300;
		    sourcepoints[0][2] = 0;
		    sourcepoints[0][0] = 450; //BR
		    sourcepoints[0][1] = 300;
		    sourcepoints[0][2] = 0;
			Vector<CPPointsPair> fiducialsvector = createVectorfromdoublearray(sourcepoints, targetpoints);
			CPSimilarityRegistrationAnalytic meanfiducialsalgo = new CPSimilarityRegistrationAnalytic();
			CPSimilarityTransformation2D newtransfo = meanfiducialsalgo.apply(fiducialsvector);		  
		    
	    	
	    	seqList.get(r).beginUpdate();
	    	
	        Rectangle bnd = cropImages.get(r).getBounds();

			// write xml file
			Matrix transfo = newtransfo.getMatrix();
			writeTransfo(transfo, fiducialsvector.size(),newImage);
			// transform stuff
			CPImageTransformer mytransformer = new CPImageTransformer();
			Sequence s = new Sequence(newImage);
			mytransformer.setImageSource(s);
			mytransformer.setParameters(transfo);
			System.out.println(transfo.get(0, 0));
			mytransformer.setDestinationsize(seqList.get(r).getWidth(), seqList.get(r).getHeight());
			System.out.println("widd" + seqList.get(r).getWidth() + " heei" + seqList.get(r).getHeight());
			
			s.addImage(mytransformer.tork());
		    Icy.getMainInterface().addSequence(s);
	        
	        IcyBufferedImage img = cropImages.get(r);
	        img.copyData(mytransformer.tork(), bnd, null);
	        
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
	 
	Vector<CPPointsPair> createVectorfromdoublearray(double[][] sourcepoints2, double[][] targetpoints2) {

		Vector<CPPointsPair> points = new Vector<CPPointsPair>();
		if (targetpoints2.length == sourcepoints2.length) {
			for (int i = 0; i < sourcepoints2.length; i++) {
				points.addElement(new CPPointsPair(new Point2D.Double(sourcepoints2[i][0], sourcepoints2[i][1]),
						new Point2D.Double(targetpoints2[i][0], targetpoints2[i][1])));
				/*
				 * System.out.print("Point " + i + 1 + " source " +
				 * sourcepoints2[i][0] + " " + sourcepoints2[i][1] + " target "
				 * + targetpoints2[i][0] + " " + targetpoints2[i][1] + "\n");
				 */
			}
		} // else{
			// new AnnounceFrame("Warning: not the same number of point on both
			// image. Nothing done",5);
			// }
		return points;

	}

	private void writeTransfo(Matrix transfo, int order, IcyBufferedImage img) {
Sequence seeq = new Sequence(img);
		String name = fileName;
		XMLFile = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "\\" + name);
		String seqName = "seuqnace";
		Document myXMLdoc;
	    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    try {
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		if (XMLFile.exists()) {myXMLdoc = dBuilder.parse(XMLFile);}
		else { myXMLdoc = XMLUtil.createDocument(true);}
	    }
	    catch (ParserConfigurationException | SAXException | IOException e) { System.out.println("couldn't XML"); return;}
	    
	    //Create node for sequence
		Element seqNode = XMLUtil.getElement(myXMLdoc.getDocumentElement(), seqName);
		if (seqNode==null) {seqNode = XMLUtil.addElement(myXMLdoc.getDocumentElement(), seqName);}
		else {XMLUtil.removeAllChildren(seqNode);}
		//Set Base bool
		if (seqName.equals(base)) {XMLUtil.setBooleanValue(seqNode, true);}
		else {XMLUtil.setBooleanValue(seqNode, false);}
		//Create transformation data element and fill it
		Element transfoElement = XMLUtil.addElement(seqNode, "TargetSize");
		XMLUtil.setAttributeIntValue(transfoElement, "width", seeq.getWidth());
		XMLUtil.setAttributeIntValue(transfoElement, "height", seeq.getHeight());
		XMLUtil.setAttributeDoubleValue(transfoElement, "sx", seeq.getPixelSizeX());
		XMLUtil.setAttributeDoubleValue(transfoElement, "sy", seeq.getPixelSizeY());
		XMLUtil.setAttributeDoubleValue(transfoElement, "sz", seeq.getPixelSizeZ());

		if (!XMLFile.exists()) {
			try {
				XMLFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		XMLUtil.saveDocument(myXMLdoc, XMLFile);
		System.out.println("Transformation will be saved as " + XMLFile.getPath());
		
		
		// Matrix transfo = newtransfo.getMatrix();

		Document document = XMLUtil.loadDocument(XMLFile);
		if (document==null){
			MessageDialog.showDialog(
					"The document where to write the transfo could not be loaded:  \n "+ XMLFile.getPath() +"\n Check if the source image was saved on disk first, /n and if you have writing rights on the directory mentionned above",
					MessageDialog.QUESTION_MESSAGE);
			return;
		}
		seqNode = XMLUtil.getElement(document.getDocumentElement(), seqName);
		System.out.println(document.getDocumentElement().getNodeName());
		System.out.println(document.getNodeName());



		transfoElement = XMLUtil.addElement(seqNode, "MatrixTransformation");

		XMLUtil.setAttributeIntValue(transfoElement, "order", order);
		XMLUtil.setAttributeDoubleValue(transfoElement, "m00", transfo.get(0, 0));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m01", transfo.get(0, 1));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m02", transfo.get(0, 2));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m03", transfo.get(0, 3));

		XMLUtil.setAttributeDoubleValue(transfoElement, "m10", transfo.get(1, 0));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m11", transfo.get(1, 1));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m12", transfo.get(1, 2));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m13", transfo.get(1, 3));

		XMLUtil.setAttributeDoubleValue(transfoElement, "m20", transfo.get(2, 0));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m21", transfo.get(2, 1));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m22", transfo.get(2, 2));
		XMLUtil.setAttributeDoubleValue(transfoElement, "m23", transfo.get(2, 3));

		XMLUtil.setAttributeDoubleValue(transfoElement, "m30", 0);
		XMLUtil.setAttributeDoubleValue(transfoElement, "m31", 0);
		XMLUtil.setAttributeDoubleValue(transfoElement, "m32", 0);
		XMLUtil.setAttributeDoubleValue(transfoElement, "m33", 1);
		XMLUtil.setAttributeValue(transfoElement, "process_date", new Date().toString());
		XMLUtil.saveDocument(document, XMLFile);
		System.out.println("Transformation matrix as been saved as " + XMLFile.getPath());
		System.out.println("If there is no path indicated, it means it is in your ICY installation path");
	}
	
}
