package plugins.masoud.multifreticy;

import icy.image.IcyBufferedImage;

public class AcquiredObject {
	public IcyBufferedImage acqImg;
	public long time;
	public AcquiredObject(IcyBufferedImage a, long t) {
		acqImg = a;
		time = t;
	}

}
