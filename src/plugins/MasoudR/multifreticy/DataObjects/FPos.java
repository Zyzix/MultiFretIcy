package plugins.MasoudR.multifreticy.DataObjects;

import java.awt.Point;
import java.awt.Rectangle;

public class FPos {
	public Rectangle fBounds;
	public Point fPoint;

	public FPos(Point framePoint, Rectangle frameBounds) {
		fPoint = framePoint;
		fBounds = frameBounds;		
	}
}
