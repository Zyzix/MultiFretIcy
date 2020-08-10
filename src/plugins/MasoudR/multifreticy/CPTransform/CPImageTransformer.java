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


package plugins.masoud.multifreticy;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import Jama.Matrix;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import loci.formats.FormatTools;

/**
 * 
 * @author Perrine
 *
 *         This class ImageTransformer is part of EasyClem but could be used as
 *         a library. In this beta version, it makes use of Graphics2D This one was created
 *         during Icy coding party, with the help of Stéphane and Fabrice 2D!
 * 
 */
public class CPImageTransformer implements Runnable {

	AffineTransform transform;
	Image image;
	double[] matrix;

	private Sequence sequence;
	private BufferedImage imageDest;
	private DataType oriType;

	/**
	 * Constructor: would crate an identity transform by default
	 */
	public CPImageTransformer() {

		transform = new AffineTransform();
	}

	/**
	 * 
	 * @param value
	 *            ICY sequence on which it will be applied
	 */
	public void setImageSource(Sequence value) {

		sequence = value;

		oriType = value.getDataType_();
	}

	/**
	 * One way to set the parameters used by @see CPApplyTransformation. If input
	 * was not a 4x4 matrix, then back to idendity matrix.
	 * 
	 * @param Transfo
	 *            a Jama Matrix 4x4 , (for 3D version VT6K was used)
	 */
	public void setParameters(Matrix Transfo) {
		if (Transfo != null) {
			if (Transfo.getRowDimension() == 4)
				transform = new AffineTransform(Transfo.get(0, 0),
						Transfo.get(1, 0), Transfo.get(0, 1), Transfo.get(1, 1),
						Transfo.get(0, 3), Transfo.get(1, 3));
		} else {transform = null;}
	}

	/**
	 * another way to set the parameters, used by @see Select3DpointsMode
	 * 
	 * @param dx
	 *            translation in X
	 * @param dy
	 *            translation in Y
	 * @param S
	 *            sinus of rotation angle
	 * @param C
	 *            cosinus of rotation angle
	 * @param scale
	 *            scaling factor
	 */
	public void setParameters(double dx, double dy, double S, double C,
			double scale) {

		transform = new AffineTransform(scale * C, scale * S, -scale * S, C
				* scale, dx, dy);
	}

	/**
	 * This will set the size of the image after transformation
	 * 
	 * @param width
	 * @param height
	 */
	public void setDestinationsize(int width, int height) {
		imageDest = new BufferedImage(width, height,
				BufferedImage.TYPE_USHORT_GRAY);

	}
	/**
	 * This method will actually apply the transformation set to the FIRST IMAGE
	 * ONLY of the ICY sequence loaded. LUT problem have been solved. Maybe have
	 * a look from anti aliasing side for interpolation problem?
	 */
	public IcyBufferedImage tork() {
		//System.out.println("I will apply transfo now");
		// add the multi channel case
		//
		IcyBufferedImage img = null;
		int nbt = sequence.getSizeT();
		int nbz = sequence.getSizeZ();
		Sequence newseq = SequenceUtil.getCopy(sequence);
		sequence.beginUpdate();
		sequence.removeAllImages();
		ProgressFrame progress = new ProgressFrame("Applying the transformation...");
		progress.setLength(nbt*nbz);
		try {
			// final ArrayList<IcyBufferedImage> images =
			// sequence.getAllImage();
			
			for (int t = 0; t < nbt; t++) {
				for (int z = 0; z < nbz; z++) {
					
					
					// PB de CHANGEMENT DE TAILLE de SEQUENCE? regarder crop?
					// + pour memoire ou temps le faire uniquement sur les
					// images affichées?
					img = transformIcyImage(newseq, t, z);
					
					sequence.setImage(t, z, img);
					progress.setPosition(1*(z+t*nbz));
					System.out.println("transforstuff is done here 1111xx");
				}
			}
			//
		} finally {

			sequence.endUpdate();
			
			// sequence.
		}
		progress.close();
		System.out.println("have been aplied");
		return img;
	}

	private IcyBufferedImage transformIcyImage(Sequence seq, int t, int z) {
		int nbChannels = seq.getSizeC();
		IcyBufferedImage imagetobemodified = seq.getImage(t, z);
		System.out.println("DATA TYPE 1: " + imagetobemodified.getDataType_().toString());
		IcyBufferedImage imagetobekept = new IcyBufferedImage(
				imageDest.getWidth(), imageDest.getHeight(),
				imagetobemodified.getSizeC(), imagetobemodified.getDataType_());
		for (int c = 0; c < nbChannels; c++) {

			image = IcyBufferedImageUtil.getARGBImage(imagetobemodified
					.getImage(c));
			
			Graphics2D g2d = imageDest.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);// BICUBIC plutot? was bilinear
			g2d.drawImage(image, transform, null); //Set to null and remove width/height adjustment in splitter to cancel transform
			g2d.dispose();

			IcyBufferedImage icyImage = IcyBufferedImage.createFrom(imageDest);
			System.out.println("DATA TYPE 2: " + icyImage.getDataType_().toString());

			// convert with rescale
			// This was the antibug which is now causing a bug since 1.6.11 icy core update
			// double boundsDst[] = imagetobemodified.getImage(c)
			//		.getChannelsGlobalBounds();
			//;
			//double boundsSrc[] = icyImage.getChannelsGlobalBounds();

			// icyImage=IcyBufferedImageUtil.convertToType(icyImage, oriType,
			// false);// rescale for now intensity
			//Scaler scaler = new Scaler(boundsSrc[0], boundsSrc[1],
			//		boundsDst[0], boundsDst[1], false);
			// ICI: se debrouiller pour que l'instensité reste la meme qu'avant
			//icyImage = IcyBufferedImageUtil.convertToType(icyImage, oriType,
					//scaler);
					
		
			if (icyImage.getDataType_()!=oriType)
			{final IcyBufferedImage tmp= IcyBufferedImageUtil.convertToType(icyImage, oriType, false,true);
			tmp.dataChanged();
			imagetobekept.copyData(tmp, 0, c);
			imagetobekept.dataChanged(); System.out.println("we did it reddit");
			}
			else{
				final IcyBufferedImage tmp=IcyBufferedImageUtil.getCopy(icyImage);
				tmp.dataChanged();
				imagetobekept.copyData(tmp, 0, c);
				imagetobekept.dataChanged(); System.out.println("reddit loses again");
			}
			
			// sequence.setImage(0, 0, icyImage);
			// Object dataArraydest =icyImage.getDataXY(0);
			// double[] tocopy=Array1DUtil.arrayToDoubleArray(dataArraydest,
			// seq.isSignedDataType());
			// Object dataArraysource =imagetobemodified.getDataXY(c);
			// double[] result=Array1DUtil.arrayToDoubleArray(dataArraysource,
			// seq.isSignedDataType());
			// ArrayMath.add(tocopy,0.0,result);
			// Array1DUtil.doubleArrayToArray(result,
			// imagetobemodified.getDataXY(c));
		}

		//imagetobekept.dataChanged();
		return imagetobekept;

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	

}
