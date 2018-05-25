package plugins.Zyzyx.theproj;

import icy.roi.ROI;
import icy.roi.ROI2D;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarInteger;

public class EzVarIntRoi extends EzVarInteger {
	public ROI2D EVIRoi;
	public EzVarBoolean EVIBool;
	public EzVarInteger EVINum = new EzVarInteger("Numerator", 0, 0, 1);
	public EzVarInteger EVIDiv = new EzVarInteger("Divisor", 0,0,1);

	public EzVarIntRoi(String varName, int value, int min, int max, int step, ROI2D EVIR, EzVarBoolean EVIB, EzVarInteger EVIN, EzVarInteger EVID) {
		super(varName, value, min, max, step);
		EVIRoi = EVIR;
		EVIBool = EVIB;
		EVINum = EVIN;
		EVIDiv = EVID;
	}
}