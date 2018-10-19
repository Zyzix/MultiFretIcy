/**
 * Copyright 2010-2017 Perrine Paul-Gilloteaux, CNRS.
 * Perrine.Paul-Gilloteaux@univ-nantes.fr
 * 
 * This file is part of EC-CLEM.
 * 
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 **/


/**
 * AUthor: Perrine.Paul-Gilloteaux@curie.fr
 * Main Class can be used alone or call from another plugin: 
 * will apply the transform content in an xml file as in easyclem
 */

package plugins.masoud.multifreticy;



import java.io.File;
import java.util.ArrayList;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import Jama.Matrix;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarSequence;

import plugins.adufour.vars.lang.VarSequence;

import vtk.vtkDataArray;
import vtk.vtkDataSet;
import vtk.vtkDoubleArray;
import vtk.vtkFloatArray;

import vtk.vtkImageData;
import vtk.vtkImageReslice;
import vtk.vtkIntArray;
import vtk.vtkMatrix4x4;
import vtk.vtkPointData;
import vtk.vtkPoints;
import vtk.vtkShortArray;
import vtk.vtkThinPlateSplineTransform;
import vtk.vtkTransform;

import vtk.vtkUnsignedCharArray;
import vtk.vtkUnsignedIntArray;
import vtk.vtkUnsignedShortArray;
import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.file.Saver;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.frame.sequence.SequenceActionFrame;
import icy.image.IcyBufferedImage;

import icy.preferences.ApplicationPreferences;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.util.XMLUtil;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzLabel;

/**
 * 
 * @author Perrine
 *
 */
public class CPApplyTransformation {
	
    private Sequence source;
	private int extentx;
	private int extenty;
	private int extentz;
	private double spacingx;
	private double spacingy;
	private double spacingz;
	vtkDataSet[] imageData;
	private double Inputspacingx;
	private double Inputspacingy;
	private double Inputspacingz;
	private Runnable transformer;
	private VarSequence out= new VarSequence("output sequence", null);
	private int auto;
   // final static SequenceActionFrame 	mainFrame = new SequenceActionFrame("Example", true);
    private Sequence seq;
	private File xmlFile;
	
    public CPApplyTransformation(Sequence seq, File transfoFile){
    	System.out.println("HELLO THERE I AM JUST HERE TO TELL YOU THAT YOU ARE IN THE RIGHT PACKAGE.");
		xmlFile = transfoFile;
		source = seq;
		Transform(seq);
    }
/**
 * play
 */
	protected void Transform(Sequence sourceseq) {
		//final Sequence sourceseq=source;
		//Icy.getMainInterface().addActiveSequenceListener(this);
		//String name=sourceseq.getFilename()+"_transfo.xml";
		if (sourceseq==null){
			MessageDialog.showDialog("Please make sure that your image is opened");
			return;
		}
		final Document document = XMLUtil.loadDocument(xmlFile);
		
		transformer = new Runnable() {
	        @Override
	        public void run()
	        {
		Element root = XMLUtil.getRootElement(document);
		
		// We check if it is non rigd transform:
		ArrayList<Element> transfoArrayList = XMLUtil.getElements(root,
				"pointspairsinphysicalcoordinates");
		if (transfoArrayList.size()>0){
			ProgressFrame progress = new ProgressFrame("Applying the NON RIGID transformation...");	
        	progress.setLength(10);
			ApplynonrigidTransformation(document);
			MessageDialog.showDialog("Non rigid transform as been applied");
			//sourceseq.setFilename(sourceseq.getFilename()+" (non rigidly transformed)");
			//System.out.println("Transformed Image will be saved as "+sourceseq.getFilename());
			//File file=new File(sourceseq.getFilename());
			//boolean multipleFiles=false;
			//boolean showProgress=true;
			//Saver.save(sourceseq, file, multipleFiles, showProgress);
			progress.setPosition(10);
			progress.close();
			return;
			
		}
		
		//Otherwise we check if it is a rigid transform file, otherwise, it eans there is some problem with the file
		ArrayList<Element> transfoElementArrayList = XMLUtil.getElements(root,
				"MatrixTransformation");
		if (transfoElementArrayList.size()==0){
			 ArrayList<Element> newsizeelement = XMLUtil.getElements( root , "TargetSize" );
			 if (newsizeelement.size()==0){
			new AnnounceFrame(
					"Please check the CONSOLE panel output");
			System.out.println("You have likely chosen a wrong file, it should be suffixed with _transfo.xml, not only .xml");
			//System.out.println("You had selected "+xmlFile.getPath());
			
			return;
			 }
			 else
			 {
				 new AnnounceFrame(
							"Please check the CONSOLE panel output");
			//	 System.out.println("You have selected "+xmlFile.getPath());
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
			Inputspacingx=	source.getPixelSizeX();
			Inputspacingy=	source.getPixelSizeY();
			Inputspacingz=	source.getPixelSizeZ();
			ApplyautoTransform(CombinedTransfo,width,height,nbz,targetsx,targetsy,targetsz);
			
			progress.close();
			return;
		}
		if (nbz==-1){// it is filled only in mode 3D, even if the original file was 3D.
			ProgressFrame progress = new ProgressFrame("Applying 2D RIGID transformation...");	
			CPImageTransformer mytransformer = new CPImageTransformer();

			mytransformer.setImageSource(source);
			mytransformer.setParameters(CombinedTransfo);
			// mytransformer.setParameters(0,0,0,0,scale);
			mytransformer.setDestinationsize(width,height);
			mytransformer.run();
			progress.close();
			if (targetsx!=-1) //xml fie generated with oldest version for 2D if -1, do nothing
			{
				source.setPixelSizeX(targetsx);
				source.setPixelSizeY(targetsy);
				source.setPixelSizeZ(targetsz);
			}
		}
		else {
			ProgressFrame progress = new ProgressFrame("Applying 3D RIGID transformation...");	
			
			CPSimilarityTransformation3D transfo = getCombinedTransfo3D(document);
			
			
			// write xml file
			Matrix transfomat = transfo.getMatrix();

			CPStack3DVTKTransformer transfoimage3D=new CPStack3DVTKTransformer();
			transfoimage3D.setImageSource(source,transfo.getorisizex(),transfo.getorisizey(), transfo.getorisizez());
			transfoimage3D.setDestinationsize(width, height, nbz,
					targetsx, targetsy, targetsz,recenter);
			transfoimage3D.setParameters(transfomat,transfo.getscalex(),transfo.getscalez());
			transfoimage3D.run();
			progress.close();

		}

		//sourceseq.setFilename(sourceseq.getFilename()+" (transformed)");
		//sourceseq.setName(sourceseq.getName()+ " (transformed)");
//		File file=new File(sourceseq.getFilename());
//		boolean multipleFiles=false;
//		boolean showProgress=true;
//		System.out.println("Transformed Image will be saved as "+sourceseq.getFilename());
		//Saver.save(sourceseq, file, multipleFiles, showProgress);
		
}
		};

		ThreadUtil.invokeNow(transformer);
		if (auto!=1)
			out.setValue(sourceseq);
		
	}
	protected void ApplyautoTransform(Matrix combinedTransfo, int w, int h, int nbz, double targetsx, double targetsy,
		double targetsz) {
		
		int		  nbc = source.getSizeC(); 
		imageData=new vtkDataSet[nbc]; 
		
		vtkTransform myvtkcombinedTransfo=new vtkTransform();
		vtkMatrix4x4 myvtkmatrix=new vtkMatrix4x4();
		for (int i=0;i<4;i++){
			for (int j=0;j<4;j++){
				myvtkmatrix.SetElement(i, j, combinedTransfo.get(i,j));
			}
		}
		myvtkcombinedTransfo.SetMatrix(myvtkmatrix);
		for (int c=0;c<nbc;c++)
		{
			
			converttoVtkImageData(c);

			vtkImageReslice ImageReslice = new vtkImageReslice();

			ImageReslice.SetInputData(imageData[c]);
			ImageReslice.SetOutputDimensionality(3);
			ImageReslice.SetOutputOrigin(0, 0, 0);
			ImageReslice.SetOutputSpacing(targetsx, targetsy,  targetsz); 
			if (nbz<0) // to get at least one z as output
				nbz=1;
			ImageReslice.SetOutputExtent(0, w-1, 0,		 h-1, 0, nbz-1); 

			ImageReslice.SetResliceTransform(myvtkcombinedTransfo.GetInverse());

			ImageReslice.SetInterpolationModeToLinear();

			ImageReslice.Update(); 


			imageData[c] = ImageReslice.GetOutput();

		}
		// convert back to icy image
		int nbt = source.getSizeT();

		DataType datatype =  source.getDataType_(); 
		Sequence  sequence2=SequenceUtil.getCopy(source); 
		sequence2.beginUpdate();
		sequence2.removeAllImages(); 
		try {
			switch (datatype){
			case UBYTE:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final byte[] inData=((vtkUnsignedCharArray) myvtkarray).GetJavaArray();

							byte[] outData=new byte[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsByte(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}

				break;
			case BYTE:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final byte[] inData=((vtkUnsignedCharArray) myvtkarray).GetJavaArray();

							byte[] outData=new byte[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsByte(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			case USHORT:

				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final short[] inData=((vtkUnsignedShortArray) myvtkarray).GetJavaArray();

							short[] outData=new short[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsShort(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			case SHORT:

				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final short[] inData=((vtkShortArray) myvtkarray).GetJavaArray();

							short[] outData=new short[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsShort(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			case INT:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final int[] inData=((vtkIntArray) myvtkarray).GetJavaArray();

							int[] outData=new int[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsInt(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			case UINT:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final int[] inData=((vtkUnsignedIntArray) myvtkarray).GetJavaArray();

							int[] outData=new int[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsInt(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			case FLOAT:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final float[] inData=((vtkFloatArray) myvtkarray).GetJavaArray();

							float[] outData=new float[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsFloat(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			case DOUBLE:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final double[] inData=((vtkDoubleArray) myvtkarray).GetJavaArray();

							double[] outData=new double[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsDouble(c, outData);

						}
						sequence2.setImage(t, z, image);

					}

				}
				break;
			default:
				break;
				//
			}

			sequence2.setPixelSizeX(targetsx);
			sequence2.setPixelSizeY(targetsy);
			sequence2.setPixelSizeZ(targetsz); 
		} 
		finally {

			sequence2.endUpdate();
		}
		// display the sequence


		System.out.println("WTF IS HAPPEN" + sequence2.getName());
		System.out.println("WTF IS HAPPEN");
		System.out.println("WTF IS HAPPEN");
		System.out.println("WTF IS HAPPEN");
		//sequence2.setName(source.getName()+"Transformed");
		System.out.println("WTF IS HAPPEN" + sequence2.getName());

		out.setValue(sequence2);

	//	System.out.println("have been applied"); 


	}
	

	private CPSimilarityTransformation3D getCombinedTransfo3D(Document document) {
	
		Element root = XMLUtil.getRootElement(document);
		
		ArrayList<Element> transfoElementArrayList = XMLUtil.getElements(root,
				"MatrixTransformation");
		// int nbtransfo=transfoElementArrayList.size();
		ArrayList<Matrix> listoftransfo = new ArrayList<Matrix>();
		boolean firsttime=true;
		// the default value of orisizex has to the actual pixel size:
		// otherwise during the initialisation (i.e the first tranform 
		//when getcombined transform has nothing to return
		double orisizex=source.getPixelSizeX();
		double orisizey=source.getPixelSizeY();
		double orisizez=source.getPixelSizeZ();
		
		for (Element transfoElement : transfoElementArrayList) {
			double[][] m = new double[4][4];
			// int order = XMLUtil.getAttributeIntValue( transfoElement, "order"
			// , -1 ); //to be check for now only: has to be used!!!
			// the only different pixel size (i.e the orginal source size) is given only at the first transformation
			if (firsttime){
				orisizex=XMLUtil.getAttributeDoubleValue(transfoElement, "formerpixelsizeX", 0);
				orisizey=XMLUtil.getAttributeDoubleValue(transfoElement, "formerpixelsizeY", 0);
				orisizez=XMLUtil.getAttributeDoubleValue(transfoElement, "formerpixelsizeZ", 0);
				firsttime=false;
			}
			
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
		
		
		CPSimilarityTransformation3D resulttransfo=new CPSimilarityTransformation3D(CombinedTransfo,orisizex,orisizey,orisizez);
		return resulttransfo;
	
}
	
	private  vtkPoints[] getLandmarks(Document document) {
	
		Element root = XMLUtil.getRootElement(document);
		ArrayList<Element> transfoElementArrayList = XMLUtil.getElements(root,
				"pointspairsinphysicalcoordinates");
		vtkPoints lmsource=new vtkPoints();
		vtkPoints lmtarget=new vtkPoints();
		
		for (Element transfoElement : transfoElementArrayList) {
			double[] pointsource=new double[3];
		
			pointsource[0]=XMLUtil.getAttributeDoubleValue(transfoElement, "xsource", 0);
			pointsource[1]=XMLUtil.getAttributeDoubleValue(transfoElement, "ysource", 0);
			pointsource[2]=XMLUtil.getAttributeDoubleValue(transfoElement, "zsource", 0);
			lmsource.InsertNextPoint(pointsource);
			double[] pointtarget=new double[3];
			
			pointtarget[0]=XMLUtil.getAttributeDoubleValue(transfoElement, "xtarget", 0);
			pointtarget[1]=XMLUtil.getAttributeDoubleValue(transfoElement, "ytarget", 0);
			pointtarget[2]=XMLUtil.getAttributeDoubleValue(transfoElement, "ztarget", 0);
			
			lmtarget.InsertNextPoint(pointtarget);
		}
	
		vtkPoints[] pointspairs=new vtkPoints[2];
		pointspairs[0]=lmsource;
		pointspairs[1]=lmtarget;
		
		Element transfoinfo = XMLUtil.getElements(root,"transfoelements").get(0);
		this.extentx=XMLUtil.getAttributeIntValue(transfoinfo, "extentx",0 );
		this.extenty=XMLUtil.getAttributeIntValue(transfoinfo, "extenty",0 );
		this.extentz=XMLUtil.getAttributeIntValue(transfoinfo, "extentz",0 );
		
		this.spacingx=	XMLUtil.getAttributeDoubleValue(transfoinfo, "sx" , 0 );
		this.spacingy=	XMLUtil.getAttributeDoubleValue(transfoinfo, "sy" , 0 );
		this.spacingz=	XMLUtil.getAttributeDoubleValue(transfoinfo, "sz" , 0 );
		this.Inputspacingx=	XMLUtil.getAttributeDoubleValue(transfoinfo, "ix" , 0 );
		this.Inputspacingy=	XMLUtil.getAttributeDoubleValue(transfoinfo, "iy" , 0 );
		this.Inputspacingz=	XMLUtil.getAttributeDoubleValue(transfoinfo, "iz" , 0 );
		int npoints= XMLUtil.getAttributeIntValue(transfoinfo, "Npoints" , 0 );
		System.out.println("For info "+npoints+" have been loaded.");
		return pointspairs;
	}
	
	private  void ApplynonrigidTransformation(Document document) {
		vtkPoints[] pointspairs=getLandmarks(document);
		
		vtkThinPlateSplineTransform myvtkTransform= new vtkThinPlateSplineTransform();
	    myvtkTransform.SetSourceLandmarks(pointspairs[0]);
	    myvtkTransform.SetTargetLandmarks(pointspairs[1]);
	    myvtkTransform.SetBasisToR2LogR();
	    
	    int nbc = source.getSizeC();
		imageData=new vtkDataSet[nbc];
		for (int c=0;c<source.getSizeC();c++){

			converttoVtkImageData(c);
			
			
			vtkImageReslice ImageReslice = new vtkImageReslice();
			ImageReslice.SetInputData(imageData[c]);
			ImageReslice.SetOutputDimensionality(3);
			ImageReslice.SetOutputOrigin(0, 0, 0);
			ImageReslice.SetOutputSpacing(this.spacingx, this.spacingy, this.spacingz);
			ImageReslice.SetOutputExtent(0, this.extentx, 0, this.extenty, 0, this.extentz); // to be checked: transform is applied twice?
			ImageReslice.SetResliceTransform(myvtkTransform.GetInverse());
			
			ImageReslice.SetInterpolationModeToLinear();

			ImageReslice.Update();
		

			imageData[c] = ImageReslice.GetOutput();
		}

		int nbt = source.getSizeT();
		int nbz = this.extentz+1;

		int w = this.extentx+1;
		int h = this.extenty+1;
		DataType datatype = source.getDataType_();
		source.beginUpdate();
		source.removeAllImages();
		try {// here finally we convert all 3D images to unsigned 8 bits
			// final ArrayList<IcyBufferedImage> images =
			// sequence.getAllImage();




			switch (datatype) {
			case UBYTE:
			for (int t = 0; t < nbt; t++) {
				for (int z = 0; z < nbz; z++) {
					IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
							datatype);
					for (int c=0;c<nbc;c++){
						vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
						final byte[] inData=((vtkUnsignedCharArray) myvtkarray).GetJavaArray();
						
						byte[] outData=new byte[w*h];
						for (int i = 0; i < h; i++) {
							for (int j = 0; j < w; j++) {

								outData[i * w + j] =  inData[z * w * h + i * w + j];

							}
						}


						image.setDataXYAsByte(c, outData);
						

					}
					source.setImage(t, z, image);

				}

			}
			break;
			case BYTE:
			for (int t = 0; t < nbt; t++) {
				for (int z = 0; z < nbz; z++) {
					IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
							datatype);
					for (int c=0;c<nbc;c++){
						vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
						final byte[] inData=((vtkUnsignedCharArray) myvtkarray).GetJavaArray();
						
						byte[] outData=new byte[w*h];
						for (int i = 0; i < h; i++) {
							for (int j = 0; j < w; j++) {

								outData[i * w + j] =  inData[z * w * h + i * w + j];

							}
						}


						image.setDataXYAsByte(c, outData);
						

					}
					source.setImage(t, z, image);

				}

			}
			break;
			case USHORT:
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final short[] inData=((vtkUnsignedShortArray) myvtkarray).GetJavaArray();
							
							short[] outData=new short[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsShort(c, outData);
							

						}
						source.setImage(t, z, image);

					}

				}
				break;
			case UINT:
			
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final int[] inData=((vtkUnsignedIntArray) myvtkarray).GetJavaArray();
							
							int[] outData=new int[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsInt(c, outData);
							

						}
						source.setImage(t, z, image);

					}

				}
				break;
			case INT:
				
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final int[] inData=((vtkIntArray) myvtkarray).GetJavaArray();
							
							int[] outData=new int[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsInt(c, outData);
							

						}
						source.setImage(t, z, image);

					}

				}
				break;
			case SHORT:
				
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final short[] inData=((vtkShortArray) myvtkarray).GetJavaArray();
							
							short[] outData=new short[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsShort(c, outData);
							

						}
						source.setImage(t, z, image);

					}

				}
				break;
			case FLOAT:
				
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final float[] inData=((vtkFloatArray) myvtkarray).GetJavaArray();
							
							float[] outData=new float[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsFloat(c, outData);
							

						}
						source.setImage(t, z, image);

					}

				}
				break;
			case DOUBLE:
				
				for (int t = 0; t < nbt; t++) {
					for (int z = 0; z < nbz; z++) {
						IcyBufferedImage image = new IcyBufferedImage(w, h, nbc,
								datatype);
						for (int c=0;c<nbc;c++){
							vtkDataArray myvtkarray = imageData[c].GetPointData().GetScalars();
							final double[] inData=((vtkDoubleArray) myvtkarray).GetJavaArray();
							
							double[] outData=new double[w*h];
							for (int i = 0; i < h; i++) {
								for (int j = 0; j < w; j++) {

									outData[i * w + j] =  inData[z * w * h + i * w + j];

								}
							}


							image.setDataXYAsDouble(c, outData);
							

						}
						source.setImage(t, z, image);

					}

				}
				break;
			default:
				System.err.println("unknown data format");
				break;
			}
		
			source.setPixelSizeX(this.spacingx);
			source.setPixelSizeY(this.spacingy);
			source.setPixelSizeZ(this.spacingz);
		//
	} finally {

		source.endUpdate();

		// sequence.
	}

	}
	private void converttoVtkImageData(int posC) {
		// TODO Auto-generated method stub
	
			final Sequence sequence2 = source;
			if (source == null)
				return;

			final int sizeX = sequence2.getSizeX();
			final int sizeY = sequence2.getSizeY();
			final int sizeZ = sequence2.getSizeZ();
			final DataType dataType = sequence2.getDataType_();
			final int posT;

				posT=0;
			

			// create a new image data structure
			final vtkImageData newImageData = new vtkImageData();

			newImageData.SetDimensions(sizeX, sizeY, sizeZ);
			newImageData.SetSpacing(this.Inputspacingx, this.Inputspacingy, this.Inputspacingz);
			

			vtkDataArray array;

			switch (dataType) {
			case UBYTE:

				// newImageData.SetScalarTypeToUnsignedChar();
				// pre-allocate data
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_UNSIGNED_CHAR, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkUnsignedCharArray) array).SetJavaArray(source
							.getDataCopyCXYZAsByte(posT));
				else
					((vtkUnsignedCharArray) array).SetJavaArray(source
							.getDataCopyXYZAsByte(posT, posC));
				break;

			case BYTE:

				// newImageData.SetScalarTypeToUnsignedChar();
				// pre-allocate data
				// newImageData.AllocateScalars();
				// pre-allocate data
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_UNSIGNED_CHAR, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkUnsignedCharArray) array).SetJavaArray(source
							.getDataCopyCXYZAsByte(posT));
				else
					((vtkUnsignedCharArray) array).SetJavaArray(source
							.getDataCopyXYZAsByte(posT, posC));
				break;

			case USHORT:
				// newImageData.SetScalarTypeToUnsignedShort();
				// pre-allocate data
				// newImageData.AllocateScalars();
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_UNSIGNED_SHORT, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkUnsignedShortArray) array).SetJavaArray(source
							.getDataCopyCXYZAsShort(posT));
				else
					((vtkUnsignedShortArray) array).SetJavaArray(source
							.getDataCopyXYZAsShort(posT, posC));
				break;

			case SHORT:
				// newImageData.SetScalarTypeToShort();
				// pre-allocate data
				// newImageData.AllocateScalars();
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_SHORT, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkShortArray) array).SetJavaArray(source
							.getDataCopyCXYZAsShort(posT));
				else
					((vtkShortArray) array).SetJavaArray(source
							.getDataCopyXYZAsShort(posT, posC));
				break;

			case UINT:
				// newImageData.SetScalarTypeToUnsignedInt();
				// pre-allocate data
				// newImageData.AllocateScalars();
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_UNSIGNED_INT, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkUnsignedIntArray) array).SetJavaArray(source
							.getDataCopyCXYZAsInt(posT));
				else
					((vtkUnsignedIntArray) array).SetJavaArray(source
							.getDataCopyXYZAsInt(posT, posC));
				break;

			case INT:
				// newImageData.SetScalarTypeToInt();
				// pre-allocate data
				// newImageData.AllocateScalars();
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_INT, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkIntArray) array).SetJavaArray(source
							.getDataCopyCXYZAsInt(posT));
				else
					((vtkIntArray) array).SetJavaArray(source
							.getDataCopyXYZAsInt(posT, posC));
				break;

			case FLOAT:
				// newImageData.SetScalarTypeToFloat();
				// pre-allocate data
				// newImageData.AllocateScalars();
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_FLOAT, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkFloatArray) array).SetJavaArray(source
							.getDataCopyCXYZAsFloat(posT));
				else
					((vtkFloatArray) array).SetJavaArray(source
							.getDataCopyXYZAsFloat(posT, posC));
				break;

			case DOUBLE:
				// newImageData.SetScalarTypeToDouble();
				// pre-allocate data
				// newImageData.AllocateScalars();
				newImageData.AllocateScalars(icy.vtk.VtkUtil.VTK_DOUBLE, 1);
				// get array structure
				array = newImageData.GetPointData().GetScalars();
				// set frame sequence data in the array structure
				if (posC == -1)
					((vtkDoubleArray) array).SetJavaArray(source
							.getDataCopyCXYZAsDouble(posT));
				else
					((vtkDoubleArray) array).SetJavaArray(source
							.getDataCopyXYZAsDouble(posT, posC));
				break;

			default:
				// we probably have an empty sequence
				newImageData.SetDimensions(1, 1, 1);
				newImageData.SetSpacing(source.getPixelSizeX(), source.getPixelSizeY(), source.getPixelSizeZ());
				newImageData.SetNumberOfScalarComponents(1, null);
				newImageData.SetExtent(0, 0, 0, 0, 0, 0);
				// newImageData.SetScalarTypeToUnsignedChar();
				// pre-allocate data
				newImageData.AllocateScalars(null);
				break;
			}

			// set connection
			// volumeMapper.SetInput(newImageData);
			// mark volume as modified
			// volume.Modified();

			// release previous volume data memory
			if (imageData[posC] != null) {
				final vtkPointData pointData = imageData[posC].GetPointData();
				if (pointData != null) {
					final vtkDataArray dataArray = pointData.GetScalars();
					if (dataArray != null)
						dataArray.Delete();
					pointData.Delete();
					imageData[posC].ReleaseData();
					imageData[posC].Delete();
				}
			}

			imageData[posC] = newImageData;
		}
		

	
	
	/**
	 * not used
	 */

	/**
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
		Document document = XMLUtil.loadDocument( xmlFile );
		return document;
	}

}
