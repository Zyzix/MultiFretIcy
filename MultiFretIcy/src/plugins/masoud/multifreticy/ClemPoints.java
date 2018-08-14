package plugins.masoud.multifreticy;

import java.awt.BasicStroke;
import java.awt.Color;

import java.awt.Font;
import java.awt.Graphics2D;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import Jama.Matrix;
import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;

import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;
import plugins.kernel.roi.descriptor.measure.ROIMassCenterDescriptorsPlugin;

import plugins.kernel.roi.roi2d.plugin.ROI2DPointPlugin;
import plugins.kernel.roi.roi3d.ROI3DPoint;
import plugins.kernel.roi.roi3d.plugin.ROI3DPointPlugin;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ToolTipFrame;
//import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.lut.LUT;
//import icy.imagej.ImageJUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.roi.ROI;


import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.sequence.SequenceUtil;
import icy.sequence.SequenceEvent.SequenceEventSourceType;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.point.Point5D;
import icy.util.XMLUtil;

public class ClemPoints extends Thread implements SequenceListener {
	
	private Sequence targetseq = new Sequence(); 
	private Sequence sourceseq = new Sequence();
	public boolean stopFlag = false;
	public boolean flagReadyToMove = false;
	private Sequence backupsource;
	// backup also calibration
	private double bucalibx;
	private double bucaliby;
	private double bucalibz;
	
	private boolean predictederrorselected = false;
	private boolean overlayerrorselected = false;

	Vector<CPPointsPair> fiducialsvector;

	double[][] targetpoints;
	double[][] sourcepoints;
	List<Double> listoftrevalues;
	List<Double> listofNvalues;
	private Runnable transformer;
	// EzVarDouble uFLE=new EzVarDouble("Fiducial localisation error in nm",
	// 200,0,10000,10);
	private boolean done;

	File XMLFile;
	private String fileName;
	private String seqName;
	private Overlay myoverlaysource;
	private Overlay myoverlaytarget;
	Overlay myoverlaypredictederror;
	Overlay myoverlayerror;
	private Overlay messageSource;
	private Overlay messageTarget;
	private Color[] Colortab;
	private String base;
	
	public ClemPoints(Sequence target, Sequence source, String name, String b) {
		System.out.println("we in CP");
		 targetseq = target;
		 sourceseq = source;
		 seqName = name;
		 fileName = "3transfo.xml";
		 base = b;
		 
			Colortab = new Color[9];
			Colortab[0] = Color.RED;
			Colortab[1] = Color.YELLOW;
			Colortab[2] = Color.PINK;
			Colortab[3] = Color.GREEN;
			Colortab[4] = Color.BLUE;
			Colortab[5] = Color.CYAN;
			Colortab[6] = Color.LIGHT_GRAY;
			Colortab[7] = Color.MAGENTA;
			Colortab[8] = Color.ORANGE;
			
			transformer = new Runnable() { //TODO: this is the thing

				@Override
				public void run() {

					if (stopFlag == false) {

						GetSourcePointsfromROI();
						GetTargetPointsfromROI();
						if (sourcepoints.length == targetpoints.length) {
								fiducialsvector = createVectorfromdoublearray(sourcepoints, targetpoints);
						} else
						// to do separate case where source points more than target
						// point= adding wrongly a point on target point
						{

							// removing roi not called Point number
							boolean removed = false;
							ArrayList<ROI> listroi = sourceseq.getROIs();
							for (ROI roi : listroi) {
								if (roi.getName().contains("Point2D")) {
									sourceseq.removeROI(roi);
									removed = true;
								}
								if (roi.getName().contains("Point3D")) {
									sourceseq.removeROI(roi);
									removed = true;
								}
							}
							listroi = targetseq.getROIs();
							for (ROI roi : listroi) {
								if (roi.getName().contains("Point2D")) {
									targetseq.removeROI(roi);
									removed = true;
								}
								if (roi.getName().contains("Point3D")) {
									targetseq.removeROI(roi);
									removed = true;
								}
							}

							GetSourcePointsfromROI();
							GetTargetPointsfromROI();
							if (removed)
								new AnnounceFrame(
										"All points named Point2D or Point3D and likely not added by you have been removed. Re click now on \"apply transform\"");
							if (sourcepoints.length != targetpoints.length) {
								MessageDialog.showDialog("Number of points", 
										"The number of points of ROI in source and target image are different. \n Check your ROI points and update transfo ");
								

							}
							Icy.getMainInterface().setSelectedTool(ROI2DPointPlugin.class.getName());
							return;

						}
						// Perrine: Why did I do that?
						int z = sourceseq.getFirstViewer().getPositionZ();
						ROI roi = sourceseq.getROIs().get(sourceseq.getROIs().size() - 1);// was
																											// get
																											// selected
																											// roi

						if (roi != null) {
							Point5D pos = roi.getPosition5D();
							// set z et recuperer
							pos.setZ(z);
							roi.setPosition5D(pos);
							// roi.setColor(Color.green);
								new AnnounceFrame("You are in pause mode, click on update transfo", 3);
								Icy.getMainInterface().setSelectedTool(ROI2DPointPlugin.class.getName());
							

						}
					}

				}

			};

	}

	public void run() {
	if (targetseq == sourceseq) {
		MessageDialog.showDialog(
				"You have selected the same sequence for target sequence and source sequence. \n Check the IMAGES to PROCESS selection");
		return;
	}
	if (sourceseq == null) {
		MessageDialog.showDialog(
				"No sequence selected for Source. \n Check the IMAGES to PROCESS selection");
		return;
	}
	if (targetseq == null) {
		MessageDialog.showDialog(
				"No sequence selected for Target. \n Check the IMAGES to PROCESS selection");
		return;
	}
	
	GetSourcePointsfromROI();
	GetTargetPointsfromROI();

		boolean mode3D = false;
		boolean nonrigid = false;
		boolean pause = true;

	Icy.getMainInterface().setSelectedTool(ROI2DPointPlugin.class.getName());
	// copysource=SequenceUtil.getCopy(sourceseq);

	if (sourceseq == null) {
		System.out.println("ss==null");
		return;
	}
	// sourceseq.getFirstViewer().getLutViewer().setAutoBound(false);

	backupsource = SequenceUtil.getCopy(sourceseq);
	bucalibx = backupsource.getPixelSizeX();
	bucaliby = backupsource.getPixelSizeY();
	bucalibz = backupsource.getPixelSizeZ();
	myoverlaysource = new VisiblepointsOverlay();
	myoverlaytarget = new VisiblepointsOverlay();
	myoverlayerror = new ErrorinPositionOverlay();
	myoverlaypredictederror = new PredictedErrorinPositionOverlay();
	messageSource = new MessageOverlay(
			"SourceImage: will be transformed. Do not add point here but drag the points added from target");
	messageTarget = new MessageOverlay("Target Message: add Roi points here");
	sourceseq.addOverlay(messageSource);
	targetseq.addOverlay(messageTarget);
	sourceseq.addOverlay(myoverlaysource);
	targetseq.addOverlay(myoverlaytarget);

	if (predictederrorselected) {
		sourceseq.addOverlay(myoverlaypredictederror);

	}
	if (overlayerrorselected) {
		sourceseq.addOverlay(myoverlayerror);
	}
	// To avoid overwriting LUT and metadata in the original filename
	// overlay.
	//sourceseq.setName(sourceseq.getName() + " (transformed)"); //TODO: FUCK this code.

	String name = fileName;
	XMLFile = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "\\" + name);
	sourceseq.setFilename(sourceseq.getName() + ".tif");
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
	XMLUtil.setAttributeIntValue(transfoElement, "width", targetseq.getWidth());
	XMLUtil.setAttributeIntValue(transfoElement, "height", targetseq.getHeight());
	XMLUtil.setAttributeDoubleValue(transfoElement, "sx", targetseq.getPixelSizeX());
	XMLUtil.setAttributeDoubleValue(transfoElement, "sy", targetseq.getPixelSizeY());
	XMLUtil.setAttributeDoubleValue(transfoElement, "sz", targetseq.getPixelSizeZ());

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
	System.out.println("checkpoint 1");
	new AnnounceFrame("Select point on image" + targetseq.getName() + ", then drag it on source image and RIGHT CLICK", 5);

	// targetseq.addListener(this);
	// if (flagRegister)
	// sourceseq.addListener(this);
	while (!stopFlag) {
		ThreadUtil.sleep(10);
		//System.out.println("checkpoint 2");
	}

	}
	
	void GetSourcePointsfromROI() {
		if (sourceseq == null) {
			MessageDialog.showDialog("Make sure source image is openned");
			return;
		}
		sourceseq.removeListener(this);
		
		ArrayList<ROI> listfiducials = sourceseq.getROIs();
		for (int i = 0; i < listfiducials.size(); i++) {
			ROI roi = listfiducials.get(i);
			if (roi.getClassName() != "plugins.kernel.roi.roi3d.ROI3DPoint") {
				ROI3DPoint roi3D = new ROI3DPoint(roi.getPosition5D());
				roi3D.setName(roi.getName());
				roi3D.setColor(roi.getColor());
				roi3D.setStroke(roi.getStroke());
				listfiducials.set(i, roi3D);// then we convert the Roi
				
			}
		}

		sourceseq.removeAllROI();
		sourceseq.addROIs(listfiducials, false);
		// ORDER ROI by name
		ReOrder(listfiducials); // should be myRoi3d now

		// sourceseq.getROIs().replaceAll(arg0);
		this.sourcepoints = new double[listfiducials.size()][3];
		// fiducials=new double[10][3];
		int i = -1;

		for (ROI roi : listfiducials) {
			i++;
			Point5D p3D = ROIMassCenterDescriptorsPlugin.computeMassCenter(roi);
			if (roi.getClassName() == "plugins.kernel.roi.roi3d.ROI3DPoint")
				p3D = roi.getPosition5D();

			if (Double.isNaN(p3D.getX()))
				p3D = roi.getPosition5D(); // some Roi does not have gravity
											// center such as points
			this.sourcepoints[i][0] = p3D.getX();
			this.sourcepoints[i][1] = p3D.getY();

			this.sourcepoints[i][2] = p3D.getZ();// should be double here now

		}
		// sourceseq.addListener(this);

	}
	
	void GetTargetPointsfromROI() {
		if (targetseq == null) {
			MessageDialog.showDialog("Make sure target image is openned");
			return;
		}
		targetseq.removeListener(this);
		ArrayList<ROI> listfiducials = targetseq.getROIs();
		for (int i = 0; i < listfiducials.size(); i++) {
			ROI roi = listfiducials.get(i);
			if (roi.getClassName() != "plugins.kernel.roi.roi3d.ROI3DPoint") {
				ROI3DPoint roi3D = new ROI3DPoint(roi.getPosition5D());
				roi3D.setName(roi.getName());
				roi3D.setColor(roi.getColor());
				roi3D.setStroke(roi.getStroke());
				listfiducials.set(i, roi3D);// then we convert the Roi
			}
		}

		targetseq.removeAllROI();

		targetseq.addROIs(listfiducials, false);
		
		ReOrder(listfiducials);
		// targetseq.removeAllROI();
		// targetseq.addROIs(listfiducials, true);
		this.targetpoints = new double[listfiducials.size()][3];

		int i = -1;
		for (ROI roi : listfiducials) {
			i++;
			Point5D p3D = ROIMassCenterDescriptorsPlugin.computeMassCenter(roi);
			if (roi.getClassName() == "plugins.kernel.roi.roi3d.ROI3DPoint")
				p3D = roi.getPosition5D();
			if (Double.isNaN(p3D.getX()))
				p3D = roi.getPosition5D(); // some Roi does not have gravity
											// center such as points
			this.targetpoints[i][0] = p3D.getX();
			this.targetpoints[i][1] = p3D.getY();
			this.targetpoints[i][2] = p3D.getZ();
			// if (targetseq.getSizeZ()==1){
			// this.targetpoints[i][2] =1.0;
			// }
			// else{
			this.targetpoints[i][2] = p3D.getZ();
			// }
		}
		targetseq.addListener(this);
	}

	@Override
	public void sequenceChanged(SequenceEvent event) {
		if (stopFlag == false) {
			if (event.getSequence() == targetseq)
				if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI) {
					// System.out.println("event on target type ROI");
					if (event.getType() == SequenceEventType.ADDED) {
						targetseq.removeListener(this);
						flagReadyToMove = false;
						//System.out.println("event on target type ROI ADDED");
						double z = targetseq.getFirstViewer().getPositionZ(); // was
																// z
						ROI roi = (ROI) event.getSource();

						Point5D pos = roi.getPosition5D();
						// set z et recuperer
						pos.setZ(z);
						roi.setPosition5D(pos);

						int colornb = (int) Math.round(Math.random() * (Colortab.length));
						if (colornb > 8)
							colornb = 8;
						System.out.println("Selected color" + colornb);
						roi.setColor(Colortab[colornb]);
						roi.setName("Point " + targetseq.getROIs().size());

						ROI roisource = roi.getCopy();
						// ROI roisource = new myRoi3D(roi);
						if (sourceseq == null) {
							new AnnounceFrame("You've closed the source image");

							return;
						}
						int zs = sourceseq.getFirstViewer().getPositionZ(); // was
																					// get
																					// active
																					// viewer,
																					// changed
																					// to
																					// prevent
																					// bad
																					// placing
																					// of
																					// z

						Point5D pos2 = roisource.getPosition5D();
						// set z et recuperer
						pos2.setZ(zs);
						roisource.setPosition5D(pos2);
						if ((sourceseq.getWidth() != targetseq.getWidth())
								|| (sourceseq.getHeight() != targetseq.getHeight()))// source
																									// is
																									// different,
																									// meaning
																									// we
																									// had
						// less than 3 poinst, and if the size
						// is different,
						// so we position
						// arbitrarilly the point at the
						// middle.
						// This should happen ONLY the first time since then
						// images will be transformed
						{

							Point5D position = (Point5D) pos.clone();

							position.setLocation(sourceseq.getWidth() / 2, sourceseq.getHeight() / 2,
									sourceseq.getFirstViewer().getPositionZ(),
									sourceseq.getFirstViewer().getPositionT(), pos.getC());
							roisource.setPosition5D(position);

						}
						/*
						 * else // we still need to get the correct z in case of
						 * 3D { Point5D position = (Point5D) pos.clone();
						 * position.setLocation(pos.getX(), pos.getY(), source
						 * .getValue().getFirstViewer().getPositionZ(),
						 * sourceseq.getFirstViewer() .getPositionT(),
						 * pos.getC()); roisource.setPosition5D(position); }
						 */
						System.out.println("Adding Roi Landmark " + targetseq.getROIs().size() + " on source");
						roisource.setName("Point " + targetseq.getROIs().size());
						sourceseq.removeListener(this); // to avoid to
																// catch the add
																// roi as an
																// event....

						sourceseq.addROI(roisource);

						/*
						 * vtkActor sphereActor= new vtkActor();
						 * 
						 * sphereActor.SetPosition(roisource.getPosition5D().
						 * getX() * sourceseq.getPixelSizeX(),
						 * roisource.getPosition5D().getY() *
						 * sourceseq.getPixelSizeY(),
						 * roisource.getPosition5D().getZ() *
						 * sourceseq.getPixelSizeZ());
						 */

						roisource.setStroke(9); // change size

						// sourceseq.setSelectedROI(roisource);
						roisource.setFocused(false);
						flagReadyToMove = true;
						done = false;

						sourceseq.addListener(this);

					}
				}

			if (flagReadyToMove) { //checks for new points

				if (event.getSequence() == sourceseq)

					if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI) {
						// System.out.println("event on SOURCE type ROI");

						if (event.getType() == SequenceEventType.CHANGED) {
							//System.out.println("event on SOURCE type ROI CHANGED");
							
							
							boolean test= ((ROI)event.getSource()).isSelected()||((ROI)event.getSource()).isFocused();
							//System.out.println(test);
							ThreadUtil.sleep(10);
							
							if (test) {
								//System.out.println("Roi is still updated");
								 ThreadUtil.sleep(1);
								
							} else {
								targetseq.addListener(this);
								sourceseq.removeListener(this);

								if (!done) {
									ThreadUtil.bgRunSingle(transformer);
									done = true;

								}
							}

						}

					}
			}
		}
		/**
		 * else // flage not ready to move {
		 * //sourceseq.addListener(this); if (event.getSequence() ==
		 * sourceseq){
		 * 
		 * if (event.getSourceType() == SequenceEventSourceType.SEQUENCE_ROI) {
		 * if (event.getType() == SequenceEventType.ADDED) {
		 * System.out.println("added"); }
		 * 
		 * } } }
		 **/

	}

	@Override
	public void sequenceClosed(Sequence sequence) {
		// TODO Auto-generated method stub
		
	}

	private class VisiblepointsOverlay extends Overlay {
		public VisiblepointsOverlay() {
			super("Visible points");
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			// check if we are dealing with a 2D canvas and we have a valid
			// Graphics object
			if ((canvas instanceof IcyCanvas2D) && (g != null)) {

				ArrayList<ROI> listfiducials = sequence.getROIs();

				for (ROI roi : listfiducials) {

					// @SuppressWarnings("deprecation")
					Point5D p3D = ROIMassCenterDescriptorsPlugin.computeMassCenter(roi);
					if (Double.isNaN(p3D.getX()))
						p3D = roi.getPosition5D(); // some Roi does not have
													// gravity

					g.setColor(Color.BLACK);
					g.setStroke(new BasicStroke(5));
					Font f = g.getFont();
					f = FontUtil.setName(f, "Arial");
					f = FontUtil.setSize(f, (int) canvas.canvasToImageLogDeltaX(20));
					g.setFont(f);

					g.drawString(roi.getName(), (float) p3D.getX(), (float) p3D.getY());

					g.setColor(Color.YELLOW);
					g.drawString(roi.getName(), (float) p3D.getX() + 1, (float) p3D.getY() + 1);

				}
			}

		}
	}

	private class ErrorinPositionOverlay extends Overlay {
		public ErrorinPositionOverlay() {
			super("Difference in position");
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			// check if we are dealing with a 2D canvas and we have a valid
			// Graphics object
			if ((canvas instanceof IcyCanvas2D) && (g != null)) {
				if ((sourcepoints != null) || (targetpoints != null)) {

						fiducialsvector = createVectorfromdoublearray(sourcepoints, targetpoints);
						for (int index = 0; index < fiducialsvector.size(); index++) {

							g.setStroke(new BasicStroke((int) canvas.canvasToImageLogDeltaX(5)));
							g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
							g.setColor(Color.RED);
							double error = fiducialsvector.get(index).getDiffinpixels();

							/*
							 * g.drawOval( (int)
							 * Math.round(fiducialsvector.get(index).first
							 * .getX() - error), (int)
							 * Math.round(fiducialsvector.get(index).first
							 * .getY() - error), (int) Math .round(error * 2),
							 * (int) Math .round(error * 2));
							 */
							// now we draw an arrow:

							double l = error / 4;
							double w = 3;

							plotarrow(fiducialsvector.get(index).getfirstxinpixels(),
									fiducialsvector.get(index).getfirstyinpixels(),
									fiducialsvector.get(index).getsecondxinpixels(),
									fiducialsvector.get(index).getsecondyinpixels(), l, w, g);

							// g.draw( l3 );

						
					} 
				}

			}
		}

	}
	
	
	/**
	 * convert the array of source and taget point in a pait of ficulial vector
	 * for 2D points
	 * 
	 * @param sourcepoints2
	 * @param targetpoints2
	 * @return
	 */
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

	private class PredictedErrorinPositionOverlay extends Overlay {
		public PredictedErrorinPositionOverlay() {
			super("Predicted Error from point configuration");
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			// check if we are dealing with a 2D canvas and we have a valid
			// Graphics object
			if ((canvas instanceof IcyCanvas2D) && (g != null)) {
				CPTargetRegistrationErrorMap ComputeFRE = new CPTargetRegistrationErrorMap();
				ComputeFRE.ReadFiducials(sequence);
				// OMEXMLMetadataImpl sourcepixelsize = sequence.getMetadata();
				double xsource = sequence.getPixelSizeX();

				// PositiveFloat ysource =
				// sourcepixelsize.getPixelsPhysicalSizeY(0);
				double[] f = ComputeFRE.PreComputeTRE();

				ArrayList<ROI> listfiducials = sequence.getROIs();

				for (ROI roi : listfiducials) {

					Point5D p3D = ROIMassCenterDescriptorsPlugin.computeMassCenter(roi);
					if (Double.isNaN(p3D.getX()))
						p3D = roi.getPosition5D(); // some Roi does not have
													// gravity
					// center such as points

					int x = (int) Math.round(p3D.getX());
					int y = (int) Math.round(p3D.getY());
					g.setColor(Color.ORANGE);
					g.setStroke(new BasicStroke(5));
					double FLEmax = maxdifferrorinnm();
					double diameter = ComputeFRE.ComputeTRE(FLEmax, x, y, 0, f); // in
																					// nanometers->
																					// convert
																					// to
					// dimater was radius // pixels

					diameter = (diameter * 2) / (1000 * xsource); // in
																	// pixels
																	// from
																	// nm
					x = (int) Math.round(p3D.getX() - diameter / 2);
					y = (int) Math.round(p3D.getY() - diameter / 2);
					g.drawOval(x, y, (int) Math.round(diameter), (int) Math.round(diameter));

				}

			}
		}
	}
	
	private void plotarrow(double x1, double y1, double x2, double y2, double l, double w, Graphics2D g) {
		/*
		 * c a ------------------- b d
		 */
		double[] ab = { x2 - x1, y2 - y1 }; // ab vector
		double norm = Math.sqrt(ab[0] * ab[0] + ab[1] * ab[1]);
		if (norm > l) {// draw only if length(ab) > head length
			// t = ab vector normalized to l
			int[] t = { (int) Math.rint((double) ab[0] * (l / norm)), (int) Math.rint((double) ab[1] * (l / norm)) };

			double[] r = { ab[1], -ab[0] };
			norm = Math.sqrt(r[0] * r[0] + r[1] * r[1]);
			r[0] = (int) Math.rint((double) r[0] / norm * (w / 2));
			r[1] = (int) Math.rint((double) r[1] / norm * (w / 2));

			double[][] tri = { { x2, x2 - t[0] + r[0], x2 - t[0] - r[0], x2 },
					{ y2, y2 - t[1] + r[1], y2 - t[1] - r[1], y2 } };
			Line2D l1 = new Line2D.Double(x1, y1, x2, y2);
			g.draw(l1);

			GeneralPath filledPolygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
			filledPolygon.moveTo(tri[0][0], tri[1][0]);
			for (int index = 1; index < 3; index++) {
				filledPolygon.lineTo(tri[0][index], tri[1][index]);
			}
			;
			filledPolygon.closePath();

			g.fill(filledPolygon);

			g.draw(filledPolygon);

		}
		;
	}

	public double maxdifferrorinnm() {
		// TODO Auto-generated method stub
		// the min localization error is one pixel or the resolution of
		// fluorescence
		/*
		 * double error = Math.max(sourceseq.getPixelSizeX(),
		 * targetseq.getPixelSizeX()); error=error*1000; // in nm, was
		 * in um error=Math.max(error, 200);
		 */
		if (sourcepoints == null) {
			System.err.println("Please initialize EasyClem first by pressing the Play button");
			return 0.0;
		}
		if (sourcepoints.length < 5) {// then the points are perfectly
										// registered which may be a non sense
										// from FLE,
			// we then assume an error of 20 pixels
			double error = Math.max(sourceseq.getPixelSizeX(), targetseq.getPixelSizeX());
			error = 20 * error * 1000; // in nm, was in um
			error = Math.max(200, error);
			error = Math.min(1000, error);
			return error;
		}
		double error = 200; // this is the min error in fluorescence
		if ((sourcepoints != null) && (targetpoints.length == sourcepoints.length)) {

				fiducialsvector = createVectorfromdoublearray(sourcepoints, targetpoints);
				double newerror = 0;
				// ReOrder(fiducialsvector);
				for (int index = 0; index < fiducialsvector.size(); index++) {

					newerror += fiducialsvector.get(index).getDiffinpixels() * sourceseq.getPixelSizeX() * 1000;

				}
				newerror = newerror / fiducialsvector.size();
				// if (error>100/(sourceseq.getPixelSizeX()*1000))
				// //minimal fidulcial localisation error is the fluorecsnce
				// limitation
				// error=error*sourceseq.getPixelSizeX()*1000;// the
				// pixel size is returned in um and we want in in nm.
				if (newerror > error)
					error = newerror;
		}

		return error;
	}

	/**
	 * This methods reorder in alphanumerical order the ROI in order to have
	 * matching point pairs
	 * 
	 * @param listfiducials
	 */
	private void ReOrder(ArrayList<ROI> listfiducials) {

		int longueur = listfiducials.size();

		ROI tampon;
		boolean permut;

		do {

			permut = false;
			for (int i = 0; i < longueur - 1; i++) {

				if (listfiducials.get(i).getName().compareTo(listfiducials.get(i + 1).getName()) > 0) {

					tampon = listfiducials.get(i);
					listfiducials.set(i, listfiducials.get(i + 1));
					listfiducials.set(i + 1, tampon);
					permut = true;
				}
			}
		} while (permut);

	}

	/**
	 * Display an informative image on the top of sequences
	 * 
	 * @author Perrine
	 *
	 */
	private class MessageOverlay extends Overlay {
		String mytext;

		public MessageOverlay(String text) {

			super("Message");
			mytext = text;
		}

		@Override
		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
			// check if we are dealing with a 2D canvas and we have a valid
			// Graphics object
			if ((canvas instanceof IcyCanvas2D) && (g != null)) {

				g.setColor(Color.RED);
				g.setStroke(new BasicStroke(5));
				Font f = g.getFont();
				f = FontUtil.setName(f, "Arial");
				f = FontUtil.setSize(f, (int) canvas.canvasToImageLogDeltaX(20));
				g.setFont(f);

				// String mytext="test";

				g.drawString(mytext, 10, (int) canvas.canvasToImageLogDeltaX(50));

			}

		}
	}

	void ComputeTransfo() {
		// fiducialsvector(mode2D) OR fiducialsvector3D (mode3D)
		// could have been thinking differently

		if ((fiducialsvector.size() > 2)) {
			double back_up_pixelsizex = sourceseq.getPixelSizeX();
			double back_up_pixelsizey = sourceseq.getPixelSizeY();
			double back_up_pixelsizez = sourceseq.getPixelSizeZ();
			sourceseq.setAutoUpdateChannelBounds(false);
			sourceseq.beginUpdate();
			sourceseq.removeAllImages();
			if (backupsource == null) {
				MessageDialog.showDialog("Please press the Play button to initialize process first");
				return;
			}
			try {
				// final ArrayList<IcyBufferedImage> images =
				// sequence.getAllImage();

				for (int t = 0; t < backupsource.getSizeT(); t++) {
					for (int z = 0; z < backupsource.getSizeZ(); z++) {

						sourceseq.setImage(t, z, backupsource.getImage(t, z));

					}
				}
			}
			//

			finally {

				sourceseq.endUpdate();

				// sequence.
			}
			sourceseq.setAutoUpdateChannelBounds(true);
			
			// we apply the previous combined transfo to the orginal image
			// before applying the new transfo in order to avoid bad cropping of
			// the pixels intensity values
			Document document = XMLUtil.loadDocument(XMLFile);
			CPSimilarityTransformation2D lasttransfo = null;

				Matrix combinedtransfobefore = getCombinedTransfo(document);
				
				CPSimilarityRegistrationAnalytic meanfiducialsalgo = new CPSimilarityRegistrationAnalytic();
				CPSimilarityTransformation2D newtransfo = meanfiducialsalgo.apply(fiducialsvector);
				lasttransfo = newtransfo;
				//double Sangle = newtransfo.getS();
				//double Cangle = newtransfo.getC();
				double dx = newtransfo.getdx();
				double dy = newtransfo.getdy();
				double scale = newtransfo.getscale();
				// write xml file
				Matrix transfo = newtransfo.getMatrix();
				writeTransfo(transfo, fiducialsvector.size());

				// combined the matrix and the new one in order to apply it
				// directly to the new image
				transfo = transfo.times(combinedtransfobefore);

				CPImageTransformer mytransformer = new CPImageTransformer();

				mytransformer.setImageSource(sourceseq);
				// mytransformer.setParameters(dx, dy, Sangle, Cangle, scale);
				mytransformer.setParameters(transfo);
				mytransformer.setDestinationsize(targetseq.getWidth(), targetseq.getHeight());
				mytransformer.run();
				
				
				// set the calibration to target calibration
				double pixelsizexum = targetseq.getPixelSizeX();
				double pixelsizeyum = targetseq.getPixelSizeY();
				sourceseq.setPixelSizeX(pixelsizexum);// TO DO rather by
																// scale
				sourceseq.setPixelSizeY(pixelsizeyum);
				double angleyz = Math.atan2(transfo.get(2, 1), transfo.get(2, 2));
				double anglexz = Math.atan2(-transfo.get(2, 0),
						Math.sqrt(transfo.get(2, 1) * transfo.get(2, 1) + transfo.get(2, 2) * transfo.get(2, 2)));
				double anglexy = Math.atan2(transfo.get(1, 0), transfo.get(0, 0));
				angleyz = Math.round(Math.toDegrees(angleyz) * 1000.0) / 1000.0;
				anglexz = Math.round(Math.toDegrees(anglexz) * 1000.0) / 1000.0;
				anglexy = Math.round(Math.toDegrees(anglexy) * 1000.0) / 1000.0;
				double dxt = Math.round(transfo.get(3, 0) * 1000.0) / 1000.0;
				double dyt = Math.round(transfo.get(3, 1) * 1000.0) / 1000.0;
				dx = Math.round(dx * 1000.0) / 1000.0;
				dy = Math.round(dy * 1000.0) / 1000.0;
				scale = Math.round(scale * 1000.0) / 1000.0;

				System.out.println("Total computed Translation x " + dxt + " Total Translation y " + dyt
						+ " angle Oz (in degrees) " + anglexy + " Scale " + scale);

				updateSourcePoints2D(newtransfo);
				updateRoi();
				new AnnounceFrame("Transformation Updated", 5);

				System.out.println("One more point"); // We did transform at the
													// beginning such that we
													// have images at the same
													// size to find the points
													// more easily.
			// targetseq.addListener(this);
				new AnnounceFrame("No transformation will be computed with less than 3 points. You have placed "
						+ fiducialsvector.size() + " points", 2);
			}

		

			Icy.getMainInterface().setSelectedTool(ROI2DPointPlugin.class.getName());
		
			// corrected for LUT adjustement
		sourceseq.getFirstViewer().getLutViewer().setAutoBound(false);
	}

public Matrix getCombinedTransfo(Document document) {
	// to fix the java null exception
	if (XMLFile==null)
	{
		System.out.println("XMLFile Not created yet, return identity");
		return Matrix.identity(4, 4);
	}
	if (document==null)
	{
		System.out.println("XMLFile Not created yet, return identity");
		return Matrix.identity(4, 4);
	}
	Element root = XMLUtil.getRootElement(document);
	Element seqNode = XMLUtil.getElement(root, seqName);
	if (seqNode==null)
	{
		System.out.println("XMLFile Not created yet, return identity");
		return Matrix.identity(4, 4);
	}
	
	// V1.0.7 add securities
	if (root == null) {
		// @TODO : verifier le java null exception ici.
		new AnnounceFrame("Could not find " + XMLFile.getName() + ". Check the CONSOLE output.", 5);
		System.out.println("The file " + XMLFile.getName()
				+ "was not found , check that you have writing right in the directory");
		System.out.println(
				"If no directory for this file is indicated, check that you have writing rights to the ICY directory (ex: C:/ICY)");
		System.out.println(
				"Reminder: as indicated on ICY download webpage, ICY should not be copy under the Program files directory");
		if (seqNode == null) { System.out.println("Node not found, make a proper transfo file");}
	}
	
	ArrayList<Element> transfoElementArrayList = XMLUtil.getElements(seqNode, "MatrixTransformation");
	if (transfoElementArrayList == null) {
		new AnnounceFrame(
				"You have likely chosen a wrong file, it should be suffixed with _transfo.xml, not only .xml", 5);

	}
	// int nbtransfo=transfoElementArrayList.size();
	ArrayList<Matrix> listoftransfo = new ArrayList<Matrix>();
	for (Element transfoElement : transfoElementArrayList) {
		double[][] m = new double[4][4];
		// int order = XMLUtil.getAttributeIntValue( transfoElement, "order"
		// , -1 ); //to be check for now only: has to be used!!!

		m[0][0] = XMLUtil.getAttributeDoubleValue(transfoElement, "m00", 0);
		m[0][1] = XMLUtil.getAttributeDoubleValue(transfoElement, "m01", 0);
		m[0][2] = XMLUtil.getAttributeDoubleValue(transfoElement, "m02", 0);
		m[0][3] = XMLUtil.getAttributeDoubleValue(transfoElement, "m03", 0);

		m[1][0] = XMLUtil.getAttributeDoubleValue(transfoElement, "m10", 0);
		m[1][1] = XMLUtil.getAttributeDoubleValue(transfoElement, "m11", 0);
		m[1][2] = XMLUtil.getAttributeDoubleValue(transfoElement, "m12", 0);
		m[1][3] = XMLUtil.getAttributeDoubleValue(transfoElement, "m13", 0);

		m[2][0] = XMLUtil.getAttributeDoubleValue(transfoElement, "m20", 0);
		m[2][1] = XMLUtil.getAttributeDoubleValue(transfoElement, "m21", 0);
		m[2][2] = XMLUtil.getAttributeDoubleValue(transfoElement, "m22", 0);
		m[2][3] = XMLUtil.getAttributeDoubleValue(transfoElement, "m23", 0);

		m[3][0] = XMLUtil.getAttributeDoubleValue(transfoElement, "m30", 0);
		m[3][1] = XMLUtil.getAttributeDoubleValue(transfoElement, "m31", 0);
		m[3][2] = XMLUtil.getAttributeDoubleValue(transfoElement, "m32", 0);
		m[3][3] = XMLUtil.getAttributeDoubleValue(transfoElement, "m33", 0);

		Matrix T = new Matrix(m);
		listoftransfo.add(T);

	}
	Matrix CombinedTransfo = Matrix.identity(4, 4);
	for (int i = 0; i < listoftransfo.size(); i++) {
		CombinedTransfo = listoftransfo.get(i).times(CombinedTransfo);
	}
	return CombinedTransfo;
}


private void writeTransfo(Matrix transfo, int order) {

	// Matrix transfo = newtransfo.getMatrix();

	Document document = XMLUtil.loadDocument(XMLFile);
	if (document==null){
		MessageDialog.showDialog(
				"The document where to write the transfo could not be loaded:  \n "+ XMLFile.getPath() +"\n Check if the source image was saved on disk first, /n and if you have writing rights on the directory mentionned above",
				MessageDialog.QUESTION_MESSAGE);
		return;
	}
	Element seqNode = XMLUtil.getElement(document.getDocumentElement(), seqName);
	System.out.println(document.getDocumentElement().getNodeName());
	System.out.println(document.getNodeName());



	Element transfoElement = XMLUtil.addElement(seqNode, "MatrixTransformation");

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
	sourceseq.removeAllROI();
	targetseq.removeAllROI();
	targetseq.removeListener(this);
	sourceseq.removeListener(this);
	System.out.println("Listeners off now");
	Prestart.transfoFile = XMLFile;
	sourceseq.removeOverlay(messageSource);
	targetseq.removeOverlay(messageTarget);
	sourceseq.removeOverlay(myoverlaysource);
	targetseq.removeOverlay(myoverlaytarget);
}

void updateRoi() {

	ArrayList<ROI> listfiducials = sourceseq.getROIs();

	ReOrder(listfiducials);
	// fiducials=new double[10][3];
	int i = -1;
	//System.out.println("True Z position (zd in roi xml):");
	for (ROI roi : listfiducials) {
		// roi=(myRoi3D)roi;
		i++;
		Point5D position = roi.getPosition5D();
		position.setX(this.sourcepoints[i][0]);
		position.setY(this.sourcepoints[i][1]);
		position.setZ(this.sourcepoints[i][2]);
		roi.setPosition5D(position); // should now copy zd as well
		System.out.println(roi.getName() + " " + this.sourcepoints[i][0] + " " + this.sourcepoints[i][1] + " "
				+ this.sourcepoints[i][2]);

	}

}

private void updateSourcePoints2D(CPSimilarityTransformation2D newtransfo) {

	for (int i = 0; i < this.sourcepoints.length; i++) {
		Point2D testPoint = new Point2D.Double(this.sourcepoints[i][0], this.sourcepoints[i][1]);
		newtransfo.apply(testPoint);
		this.sourcepoints[i][0] = testPoint.getX();
		this.sourcepoints[i][1] = testPoint.getY();
	}

}


}
