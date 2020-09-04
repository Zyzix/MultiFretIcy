package plugins.MasoudR.multifreticy.DataObjects;

public class Milestone {
	private String name;
	private long frame;

	public Milestone(String n, long f) {
		name = n;
		frame = f;
	}
	
	public String getName() {
		return name;
	}
	
	public long getFrame() {
		return frame;
	}

}
