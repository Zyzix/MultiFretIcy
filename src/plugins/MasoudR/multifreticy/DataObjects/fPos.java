package plugins.MasoudR.multifreticy.DataObjects;

import java.awt.Point;
import java.awt.Rectangle;

public class fPos {
	public Rectangle fBounds;
	public Point fPoint;

	public fPos(Point framePoint, Rectangle frameBounds) {
		fPoint = framePoint;
		fBounds = frameBounds;		
	}
}
