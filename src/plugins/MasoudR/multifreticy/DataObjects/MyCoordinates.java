package plugins.MasoudR.multifreticy.DataObjects;
import java.util.ArrayList;

public class MyCoordinates {
	private double x;
	private double y;
	private ArrayList<MyCoordinates> c = new ArrayList<MyCoordinates>();
	public boolean stopper = false;
	private String roiName;
	
	public MyCoordinates(double xCo, double yCo, String name){
		x = xCo;
		y = yCo;
		roiName = name;
	}
	
	public MyCoordinates(double xCo, double yCo, String name, ArrayList<MyCoordinates> Cords){
		x = xCo;
		y = yCo;
		roiName = name;
		c = Cords;		
	}
	
	public void setRoiName(String name) {
		roiName = name;
	}
	
	public String getRoiName() {
		return roiName;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public ArrayList<MyCoordinates> getPoints(){
		return c;
	}
	
	public void setX(double xCo) {
		x = xCo;
	}
	
	public void setY(double yCo) {
		y = yCo;
	}
	
	public void set(double xCo, double yCo) {
		x = xCo;
		y = yCo;
	}
}
