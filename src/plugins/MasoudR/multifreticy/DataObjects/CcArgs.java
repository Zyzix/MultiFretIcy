package plugins.MasoudR.multifreticy.DataObjects;

public class CcArgs {
	private String argname;
	private int channel;
	private double value;

	public CcArgs(String n, int c, double f) {
		argname		=	n;
		channel		=	c;
		value 		=	f;		
	}
	
	public CcArgs(String n, int c) {
		argname		=	n;
		channel		=	c;
	}
	
	public String getArgName() {
		return argname;
	}
	
	public int getChannel() {
		return channel;
	}
	
	public double getValue() {
		return value;
	}

	public void setValue(double v) {
		value = v;
	}	
}
