package plugins.MasoudR.multifreticy.DataObjects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JCheckBox;

public class CustomCalc {
	private ArrayList<String> names = new ArrayList<String>();
	private ArrayList<String[]> arguments = new ArrayList<String[]>();
	private ArrayList<String> formulae = new ArrayList<String>();
	private ArrayList<JCheckBox> choices = new ArrayList<JCheckBox>();
	
	public CustomCalc(File f) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(f));
		String line = r.readLine();
		
		int num = 0;
		boolean[] checks = {false, false, false};
			
		while (line != null) {
			if (line.matches("^>[^>]*") && !checks[0]) {
				checks[0] = true;
				names.add(num,line.substring(line.lastIndexOf(">")+1));
			} else if(line.matches("^>[^>]*")) {
				checks = new boolean[]{false, false, false};
				continue;
			}
			
			if (line.matches("^>>[^>]*") && !checks[1]) {
				checks[1] = true;
				String[] args = line.substring(line.lastIndexOf(">")+1).split(",");	
				for (int x = 0; x < args.length; x++) {
					args[x] = args[x].trim();
				}
				arguments.add(num,args);				
			} else if(line.matches("^>>[^>]*")) {
				checks = new boolean[]{false, false, false};
				continue;
			}
			
			if (line.matches("^>>>[^>]*") && !checks[2]) {
				checks[2] = true;
				formulae.add(num,line.substring(line.lastIndexOf(">")+1));
			} else if(line.matches("^>>>[^>]*")) {
				checks = new boolean[]{false, false, false};
				continue;
			}
			
			if (checks[0] && checks[1] && checks[2]) {
				num++;
				checks = new boolean[]{false, false, false};
				line = r.readLine();
			} else {line = r.readLine();}
		}
		r.close();
	}
	
	public String GetName(int x) {		
		return names.get(x);		
	}
	
	public String[] GetArg(int x) {		
		return arguments.get(x);		
	}
	
	public String GetFormula(int x) {		
		return formulae.get(x);		
	}
	
	public ArrayList<String> GetNames(){
		return names;
	}
	
	public ArrayList<String[]> GetArgs(){
		return arguments;
	}
	
	public ArrayList<String> GetFormulae(){
		return formulae;
	}
	
	public void SetChoice(int x, JCheckBox y) {
		choices.add(x, y);
	}
	
	public JCheckBox GetChoice(int x) {
		return choices.get(x);		 
	}
	
	public ArrayList<JCheckBox> GetChoices() {
		return choices;
	}
}