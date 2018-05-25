	package plugins.Zyzyx.theproj;

import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.rectangle.Rectangle5D;
import icy.util.XMLUtil;
import poi.CreateWorkBook;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Jama.Matrix;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.main.MainFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;

public class Splitter extends Thread {
	private ArrayList<ROI> 					rois = new ArrayList<ROI>();
	private ArrayList<IcyBufferedImage> 	cropImages = new ArrayList<IcyBufferedImage>();
	private DataType 						dataType;
	private ArrayList<Sequence> 			seqList = new ArrayList<Sequence>();
	 boolean fuck = false;
	private Startup SU1 = null;
	private boolean doneSU = false;

	ArrayList<Thread> 			threads = new ArrayList<Thread>();
	//File[]				transfoFiles = new File[3];
	private CreateWorkBook wbc; //remove and just make wbc in prestart static
	
	
	// get acquired image containing ROIs
	Splitter (Sequence acquiredSeq, File transfoFile, CreateWorkBook w){
		wbc = w;
		System.out.println("splitter checkpoint 1");
		// get image type
		dataType = acquiredSeq.getDataType_();
		// get rois
		rois = acquiredSeq.getROIs();
		
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
		}
		
		if (transfoFile == null) { //Check if transformation needs to be done
			System.out.println("No transfoFile, initiating generation procedure, sequence list size: " + seqList.size());
			TransfoMaker TFM = new TransfoMaker(seqList);
			TFM.run();
			Prestart.pause = true;
			System.out.println("a wait");
		}
	}

	public void run(IcyBufferedImage acqImg, File transfoFile) {
		if (doneSU == false) {
			SU1 = new Startup(seqList, wbc);
			System.out.println("splitter checkpoint 2");
			SU1.createUI();
			SU1.showUI();
			doneSU = true;
		}
		System.out.println("splitter checkpoint 3");
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
		        
		       // System.out.println("Running transformation for new image");
				String transfoName = null;
			        String roiN = rois.get(r).getName();
			        System.out.println(roiN);
						switch(roiN) {
						case "TL":
								transfoName = "TL";
								break;
						case "TR":
								transfoName = "TR";
								break;
						case "BL":
								transfoName = "BL";
								break;
						case "BR": 
								transfoName = "BR";
								break;
						default:
								System.out.println("no name, EXITING!");
								MessageDialog.showDialog("Name contourROIs TL/TR/BL/BR");
								System.exit(0);
								break;
						}
			        if (transfoName != "TL") {
			        Transform(seqList.get(r), transfoFile, transfoName);
			        }
		        seqList.get(r).endUpdate();
		        
		    }
	        SU1.RunThreads();
		}

	
	//TODO: finish this side issue, needs main icy window size
/*	public Point FramePositioner(ArrayList<Sequence> seqs) {
		JInternalFrame[] frames = Icy.getMainInterface().getDesktopPane().getAllFrames();
		fPos frameData = null;
		fPos2 seqData = null;
		set viewer to 0
		 * if intersect with any other frame, move to edge of that frame
		 * 	(check again)
		 * next viewer, set to first viewer rightside edge 
		 * if intersect with any other frame, move to edge of that frame
		 *  (check again)
		 * 	(check if still inside desktop pane, otherwise set to 0,0+firstviewerY)
		 * repeat
		 
		for (Sequence seq : seqs) {
			ArrayUtils.removeElement(frames, seq.getFirstViewer());
		}
		
		for (JInternalFrame frame : frames) {
			frameData.fPoint = frame.getLocation();
			frameData.fBounds = frame.getBounds();
		
			for (Sequence seq : seqs){
				seq.getFirstViewer().setLocation(seqData.fPoint); //make this a variable that stays when you go to the next frame
				if(seq.getFirstViewer().getBounds().intersects(frameData.fBounds)) {
					seq.getFirstViewer().setLocation(seqData.fPoint.x+frameData.fBounds.x,seqData.fPoint.y);//if intersects with the frame, move it to the end of the bounds and check again
					//if x > mainwindow x then x=0 & y=ybounds+1
					//if yboudns+1 > mainwindow y then cancel and plop it down elsewhere
				}
			}
		}

		
	}
	*/
	
	public void Transform(Sequence source, File transfoFile, String transfoName) {
		
		final Document document = XMLUtil.loadDocument(transfoFile);
		Element root = XMLUtil.getRootElement(document);
		Element subroot = XMLUtil.getElement(root, transfoName);
		Element newsizeelement = XMLUtil.getElements( subroot , "TargetSize" ).get(0);

		int width = XMLUtil.getAttributeIntValue( newsizeelement, "width" , -1 );
		int height = XMLUtil.getAttributeIntValue( newsizeelement, "height" , -1 );
		int recenter= XMLUtil.getAttributeIntValue( newsizeelement, "recenter" , 0 );
		// the following variable will get the default value is the transformation was computed in manual 2D.
		double targetsx =XMLUtil.getAttributeDoubleValue( newsizeelement, "sx" , -1 );
		double targetsy =XMLUtil.getAttributeDoubleValue( newsizeelement, "sy" , -1 );
		double targetsz =XMLUtil.getAttributeDoubleValue( newsizeelement, "sz" , -1 );
		
		Matrix CombinedTransfo=getCombinedTransfo(document, transfoName);
        
		
		ProgressFrame progress = new ProgressFrame("Applying 2D RIGID transformation...");	
		ImageTransformer mytransformer = new ImageTransformer();

		mytransformer.setImageSource(source);
		mytransformer.setParameters(CombinedTransfo);
		// mytransformer.setParameters(0,0,0,0,scale);
		mytransformer.setDestinationsize(width,height);
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
	 * compute (again) the combine transformed to avoid interpolation arrors	
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


	 
	 

}
