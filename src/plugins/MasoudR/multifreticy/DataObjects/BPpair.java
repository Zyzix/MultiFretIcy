package plugins.MasoudR.multifreticy.DataObjects;

import java.util.ArrayList;

import icy.sequence.Sequence;

public class BPpair {
	public ArrayList<Sequence> Bundle;
	public String Pos;
	public BPpair(ArrayList<Sequence> b, String p) {
		Bundle = b;
		Pos = p;
	}
}
