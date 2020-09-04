package plugins.MasoudR.multifreticy.DataObjects;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.micromanager.utils.MMScriptException;

import mmcorej.CMMCore;
import plugins.MasoudR.multifreticy.MultiFretIcy;

public class PicturePosition  {
	/*TODO: seems this shit is too complicated or wont tie into Icy
	 * Instead use mStudio as per usual and play with the poslist.
	 * >clear poslist
	 * >setTL does markcurrentpos
	 * >getcurrentpos lets you retrieve pos
	 * >on/off listening to imgreveive lets you get acquired images
	 * 
	 * since that sounds awful play with this a little more first
	 */
	
	CMMCore core = new CMMCore(); 
	private BufferedImage img;
	private Point2D.Double pos;
	
	public PicturePosition() throws Exception{
		core.loadDevice("Camera", "DemoCamera", "DCam");
		core.initializeDevice("Camera");
		core.setCameraDevice("Camera");
		core.setExposure(20);
	}
	
	public void setPos() throws Exception {
		pos = core.getXYStagePosition();
	}
	
	public Point2D.Double getPos() {
		return pos;
	}
	
	public void setImg() throws Exception {
		MultiFretIcy.PS.mStudio.doSnap();
		byte image[] = (byte[]) core.getImage();
		long width = core.getImageWidth();
		long height = core.getImageHeight();
		InputStream in = new ByteArrayInputStream(image);
		img = ImageIO.read(in);
	}
	
	public BufferedImage getImg() {
		return img;
	}
	
}
