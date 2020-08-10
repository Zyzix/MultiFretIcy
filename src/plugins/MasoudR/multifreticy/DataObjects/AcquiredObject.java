package plugins.MasoudR.multifreticy.DataObjects;

import icy.image.IcyBufferedImage;
import mmcorej.TaggedImage;

public class AcquiredObject {
	public IcyBufferedImage acqImg;
	public TaggedImage tImg;
	public long time;
	public String position;
	public String prenotation;
	
	public AcquiredObject(IcyBufferedImage a, long t, String p) {
		acqImg = a;
		time = t;
		position = p;
	}
	
	public AcquiredObject(IcyBufferedImage a, String ano, long t) {
		acqImg = a;
		time = t;
		prenotation = ano;
	}
	
	public AcquiredObject(IcyBufferedImage a, long t) {
		acqImg = a;
		time = t;
	}
	
	public AcquiredObject(TaggedImage a, long t, String p) {
		tImg = a;
		time = t;
		position = p;
	}
	
	public AcquiredObject(TaggedImage a, long t) {
		tImg = a;
		time = t;
	}
}
