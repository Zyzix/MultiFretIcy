package plugins.Zyzyx.theproj;

import icy.gui.dialog.MessageDialog;
import icy.plugin.abstract_.PluginActionable;

import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarSequence;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;

import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ProgressFrame;


import icy.preferences.ApplicationPreferences;
import icy.roi.ROI;
import icy.sequence.Sequence;

import icy.system.thread.ThreadUtil;
import icy.type.geom.Polygon2D;
import icy.type.point.Point5D;
import icy.util.XMLUtil;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzLabel;

import java.awt.geom.Point2D;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Jama.Matrix;

public class TheProj extends EzPlug implements Block {

	EzVarSequence source=new EzVarSequence("Select Source Image (showing Rois)");
	EzVarSequence target=new EzVarSequence("Select Target Image (to set the new Rois)");
	private EzVarFile xmlFile=new EzVarFile("Xml file containing list of transformation", ApplicationPreferences.getPreferences().node("frame/imageLoader").get("path", "."));;
	
	private Runnable transformer;
	
	protected VarROIArray outputROIs = new VarROIArray("list of ROI");
	private int auto;
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		MessageDialog.showDialog("TheProj is working fine !");
		
		final Sequence sourceseq=source.getValue();
		//Icy.getMainInterface().addActiveSequenceListener(this);
		//String name=sourceseq.getFilename()+"_transfo.xml";
		if (sourceseq==null){
			MessageDialog.showDialog("Please make sure that your image is opened");
			return;
		}
		
		final Document document = XMLUtil.loadDocument( xmlFile.getValue());
		
		transformer = new Runnable() {
	        @Override
	        public void run()
	        {
		Element root = XMLUtil.getRootElement(document);
		// We check if it is non rigd transform:
		ArrayList<Element> transfoArrayList = XMLUtil.getElements(root,
				"pointspairsinphysicalcoordinates");
		if (transfoArrayList.size()>0){
			ProgressFrame progress = new ProgressFrame("Non implemented yet for non rigid transformations...");	
        	
			return;
			
		}
		
		//Otherwise we check if it is a rigid transform file, otherwise, it means there is some problem with the file
		ArrayList<Element> transfoElementArrayList = XMLUtil.getElements(root,
				"MatrixTransformation");
		if (transfoElementArrayList.size()==0){
			 ArrayList<Element> newsizeelement = XMLUtil.getElements( root , "TargetSize" );
			 if (newsizeelement.size()==0){
			new AnnounceFrame(
					"Please check the CONSOLE panel output");
			System.out.println("You have likely chosen a wrong file, it should be suffixed with _transfo.xml, not only .xml");
			System.out.println("You had selected "+xmlFile.getValue().getPath());
			
			return;
			 }
			 else
			 {
				 new AnnounceFrame(
							"Please check the CONSOLE panel output");
				 System.out.println("You have selected "+xmlFile.getValue().getPath());
				 System.out.println("This transformation file does not contain any transform Matrix. It means that you asked for showing the ROI on the original source image.");
						 System.out.println("This ROI should be still here, open the target image and update transformation.");	
						 return;
			 }
		}
		Element newsizeelement = XMLUtil.getElements( root , "TargetSize" ).get(0);

		int width = XMLUtil.getAttributeIntValue( newsizeelement, "width" , -1 );
		int height = XMLUtil.getAttributeIntValue( newsizeelement, "height" , -1 );
		int recenter= XMLUtil.getAttributeIntValue( newsizeelement, "recenter" , 0 );
		// the following variable will get the default value is the transformation was computed in manual 2D.
		double targetsx =XMLUtil.getAttributeDoubleValue( newsizeelement, "sx" , -1 );
		double targetsy =XMLUtil.getAttributeDoubleValue( newsizeelement, "sy" , -1 );
		double targetsz =XMLUtil.getAttributeDoubleValue( newsizeelement, "sz" , -1 );
		
		int nbz = XMLUtil.getAttributeIntValue( newsizeelement, "nz" , -1 );
		auto= XMLUtil.getAttributeIntValue( newsizeelement, "auto" , 0 );
		Matrix CombinedTransfo=getCombinedTransfo(document);
		// check if it comes from the autofinder (tag auto set to 1, 0 otherwise)
		if (auto==1){
			ProgressFrame progress = new ProgressFrame("Applying transform from AUTOFINDER");
			
			//ApplyautoTransform(CombinedTransfo,width,height,nbz,targetsx,targetsy,targetsz);
			
			progress.close();
			return;
		}
		if (nbz==-1){// it is filled only in mode 3D, even if the original file was 3D.
			ProgressFrame progress = new ProgressFrame("Applying 2D RIGID transformation...");	
			ArrayList<ROI> Rois = sourceseq.getROIs();
			for (ROI Roi : Rois){
				
				ROI newRoi=Roi.getCopy();
				switch(newRoi.getSimpleClassName()){
				case "ROI2DRectangle": 
					Point5D oldposition = Roi.getPosition5D();
					Point5D newposition=transformPoints5D(CombinedTransfo,oldposition);
				   //newposition.setZ(newpositionmatrix.get(0,2));
					newRoi.setPosition5D(newposition);
					if (!isHeadLess()){
						target.getValue().addROI(newRoi);
						}
					else{
						outputROIs.add(newRoi);
					}
					break;
				case "ROI2DEllipse":
					Point5D oldposition2 = Roi.getPosition5D();
					Point5D newposition2=transformPoints5D(CombinedTransfo,oldposition2);
					//newposition.setZ(newpositionmatrix.get(0,2));
					newRoi.setPosition5D(newposition2);
					ArrayList<Point2D> ControlPoints = ((plugins.kernel.roi.roi2d.ROI2DEllipse) newRoi).getPoints();
					
					for (Point2D pt2D:ControlPoints){
					oldposition2 = new Point5D.Double(pt2D.getX(),pt2D.getY(),1,1,1);
					newposition2=transformPoints5D(CombinedTransfo,oldposition2);
					//newposition.setZ(newpositionmatrix.get(0,2));
					pt2D.setLocation(newposition2.getX(), newposition2.getY());
					}
					newRoi.roiChanged(true);
					if (!isHeadLess()){
					target.getValue().addROI(newRoi);
					}
					else{
						outputROIs.add(newRoi);
					}
					break;
				case "ROI2DPolygon":
					
					Point5D oldposition3 = Roi.getPosition5D();
					Point5D newposition3=transformPoints5D(CombinedTransfo,oldposition3);
					//newposition.setZ(newpositionmatrix.get(0,2));
					//newRoi.setPosition5D(newposition3);
					
					Polygon2D poly = ((plugins.kernel.roi.roi2d.ROI2DPolygon) newRoi).getPolygon2D();
					
					for (int i=0; i<poly.npoints;i++){
					oldposition3 = new Point5D.Double(poly.xpoints[i],poly.ypoints[i],1,1,1);
					newposition3=transformPoints5D(CombinedTransfo,oldposition3);
					//newposition.setZ(newpositionmatrix.get(0,2));
					poly.xpoints[i]=newposition3.getX();
					poly.ypoints[i]=newposition3.getY();
					}
					((plugins.kernel.roi.roi2d.ROI2DPolygon) newRoi).setPolygon2D(poly);
					newRoi.roiChanged(true);
					if (!isHeadLess()){
						target.getValue().addROI(newRoi);
						}
					else{
						outputROIs.add(newRoi);
					}
					break;
				default: 
					System.err.println("Roi of type "+ newRoi.getSimpleClassName()+" non implemented yet ");
					break;
				}
				
			    
				
			}
			progress.close();
		}
		else {
			

		}
		if (!isHeadLess()){
		IcyCanvas sourcecanvas = source.getValue().getFirstViewer().getCanvas();
		if (sourcecanvas instanceof IcyCanvas2D)
			((IcyCanvas2D) sourcecanvas).fitCanvasToImage();
		}
		
		
	}
		};
		if (!this.isHeadLess()){
		ThreadUtil.bgRun(transformer);
		}
		else{
		ThreadUtil.invokeNow(transformer);
		}
		
	}

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		EzLabel textinfo=new EzLabel("Please open images with Roi (source) and the destination target image, and the xml file containing the transformations (likely your source file name _transfo.xml)");
		
		String varName ="Xml file containing list of transformation";
		if (source.getValue()!=null)
			xmlFile=new EzVarFile(varName, source.getValue().getFilename());
		else
			xmlFile=new EzVarFile(varName, ApplicationPreferences.getPreferences().node("frame/imageLoader").get("path", "."));
		
		addEzComponent(textinfo);
		addEzComponent(source);
		addEzComponent(target);
		addEzComponent(xmlFile);
	}

	protected Point5D transformPoints5D(Matrix combinedTransfo, Point5D oldposition) {
		 double[][] array = {{oldposition.getX(),oldposition.getY(),1,1}};
		   
		    Matrix oldpositionmatrix= new Matrix(array);
		    Matrix newpositionmatrix=combinedTransfo.times(oldpositionmatrix.transpose());
		    Point5D newposition = new Point5D.Double();
			newposition.setX(newpositionmatrix.get(0,0));
			newposition.setY(newpositionmatrix.get(1,0));
			return newposition;
	}	/**
	 * compute (again) the combine transformed to avoid interpolation arrors	
	 * @param document
	 * @return
	 */
	public Matrix getCombinedTransfo(Document document){
		Element root = XMLUtil.getRootElement(document);




		ArrayList<Element> transfoElementArrayList = XMLUtil.getElements( root , "MatrixTransformation" );

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
	public Document getdocumentTitle() {
		Document document = XMLUtil.loadDocument( xmlFile.getValue());
		return document;
	}
	@Override
	public void declareInput(VarList inputMap) {
		// TODO Auto-generated method stub
		inputMap.add("Input Image",source.getVariable());
		inputMap.add("Imput XML File",xmlFile.getVariable());
		
	}
	@Override
	public void declareOutput(VarList outputMap) {
		// TODO Auto-generated method stub
		 outputMap.add("output transformedregions", outputROIs);
	}
}
