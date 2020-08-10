package plugins.MasoudR.multifreticy.DataObjects;

import icy.roi.ROI2D;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarText;

public class EzVarIntRoi extends EzVarInteger {
	public ROI2D EVIRoi;
	public EzVarBoolean EVIBool;
	public EzVarText EVINum = new EzVarText("Numerator", 0);
	public EzVarText EVIDiv = new EzVarText("Divisor", 0);
	public String position;
	
	public EzVarIntRoi(String varName, int value, int min, int max, int step, ROI2D EVIR, EzVarBoolean EVIB, EzVarText EVIN, EzVarText EVID, String pos) {
		super(varName, value, min, max, step);
		EVIRoi = EVIR;
		EVIBool = EVIB;
		EVINum = EVIN;
		EVIDiv = EVID;
		position = pos;
	}
}