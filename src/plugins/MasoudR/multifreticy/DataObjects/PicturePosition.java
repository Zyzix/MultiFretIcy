package plugins.MasoudR.multifreticy.DataObjects;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;

public class PicturePosition  {

	private BufferedImage img;
	private Point2D.Double pos;
	private Double posZ;
	private Boolean sel = false;
	
	public PicturePosition(BufferedImage i, Point2D.Double p) throws Exception{
		img = i;
		pos = p;
		posZ = StageMover.getZ();		
	}
	
	public PicturePosition(BufferedImage i, Point2D.Double p, Double pz){
		img = i;
		pos = p;
		posZ = pz;
	}
	
	public PicturePosition() {
		
	}

	public void setPos() throws Exception {
		pos = StageMover.getXY();
		posZ = StageMover.getZ();
	}
	
	public Point2D.Double getPos() {
		try {
			return pos;
		} catch (Exception e) {
			System.out.println("Could not get Position of image.");
			return null;
		}		
	}
	
	public Double getZFocus() {
		return posZ;
	}
	
	public void setImg() throws Exception {
		img = MicroManager.snapImage();
	}
	
	public void setImg(BufferedImage i) {
		img = i;
	}
	
	public BufferedImage getImg() {
		try {
			return img;
		} catch (Exception e) {
			System.out.println("Could not get image.");
			return null;
		}	
	}
	
	public void setSel(Boolean s) {
		sel = s;
	}
	
	public Boolean getSel() {
		return sel;
	}
}
