package plugins.Zyzyx.theproj;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import poi.CreateWorkBook;

public class TestRun {

	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		CreateWorkBook wbc = new CreateWorkBook();
		TimeUnit.SECONDS.sleep(5);	

		XYSeries s = new XYSeries("ser");
		String nom = new String();
		nom = "asd";
		for (int z = 0; z < 100; z++) {
			XYDataItem xy = new XYDataItem(z, z+1);
			xy.setY(z);
			s.add(xy);
			
			wbc.ApplyData(s, nom);
			//TimeUnit.SECONDS.sleep(1);	
		}
		wbc.SaveAndClose(wbc.workbook, wbc.name);
		System.out.println("done");
	}

}
