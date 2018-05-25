package plugins.Zyzyx.theproj;

import java.awt.Point;
import java.awt.Rectangle;

import javafx.geometry.Bounds;

public class fPos {
	public Rectangle fBounds;
	public Point fPoint;

	public fPos(Point framePoint, Rectangle frameBounds) {
		fPoint = framePoint;
		fBounds = frameBounds;		
	}
}
