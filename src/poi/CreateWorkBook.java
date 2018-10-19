package poi;

import java.awt.Desktop;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.charts.AxisCrosses;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.DataSources;
import org.apache.poi.ss.usermodel.charts.LegendPosition;
import org.apache.poi.ss.usermodel.charts.ScatterChartSeries;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.usermodel.charts.XSSFChartLegend;
import org.apache.poi.xssf.usermodel.charts.XSSFScatterChartData;
import org.apache.poi.xssf.usermodel.charts.XSSFValueAxis;
import org.jfree.data.xy.XYSeries;

import icy.main.Icy;
import loci.poi.hssf.util.CellReference;
import plugins.masoud.multifreticy.Splitter;

public class CreateWorkBook {

	public String path = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
	public XSSFWorkbook workbook;
	public String name = "DataSheet";
	public CreateWorkBook() throws Exception {
		//Open or create workbook
		OpenWB(path, name);
   }
	
	public XSSFWorkbook OpenWB(String directory, String name) {
 	      File file = new File(directory + "\\" + name + ".xlsx");
	      FileInputStream fIP;
      
	      if(file.exists() && file.length() != 0) {
	  		try {
				fIP = new FileInputStream(file);
			    //Get the workbook instance for XLSX file 
				 workbook = new XSSFWorkbook(fIP);
				 fIP.close();
		         System.out.println(name + ".xlsx file open successfully.");
		         return workbook;
			} catch (IOException e) {
				e.printStackTrace();
		         System.out.println("Error to open " + name + ".xlsx file, creating blank");
			      //Create Blank workbook
			      workbook = new XSSFWorkbook(); 
			      Integer i = 0;
			      while (file.isFile() && file.exists()) {
			    	  name = name.concat(i.toString());
			    	  file = new File(directory + "\\" + name + ".xlsx");
			    	  i++;
			      }
			      return workbook;
			}
	      } else {
	         System.out.println("Error to open " + name + ".xlsx file, creating blank");
		      //Create Blank workbook
		      workbook = new XSSFWorkbook(); 
		      return workbook;
	      }
//	      System.out.println("Error opening Workbook");
//		return null;
	}	

	public void ApplyData(XYSeries chartData, String XYinfo) {
		DateFormat dateFormat = new SimpleDateFormat("dd-MM");
		Date date = new Date();
		System.out.println(dateFormat.format(date)); //2016/11/16 12:08:43
		
		XYinfo = dateFormat.format(date) + " " + XYinfo;
		String infoString = StringUtils.abbreviate(XYinfo, 25);

		//Get sheet or create a blank spreadsheet
		XSSFSheet spreadSheet = null;
		Integer j = 0;
		boolean done = false;
		String infoString0 = infoString;
		//Finding free sheet name
		while (!done) { 
				if (workbook.getSheet(infoString) == null) {
					System.out.println("Creating sheet " + infoString);
					done = true;
				}
				else {
					infoString = infoString0.concat(j.toString());
					j++;
				}
		}
		//Cloning template or creating a new sheet
		XSSFRow row;
		if (workbook.getSheet("template") == null) {
			spreadSheet = workbook.createSheet(infoString);	
			System.out.println("No template, creating sheet " + spreadSheet.getSheetName());
			//Create headers
			row = spreadSheet.getRow(0);
			if (row == null) { //somehow doing getRow and this ifblock makes a null exception on createcell
				row = spreadSheet.createRow(0);
			}
			
			Cell cell = row.createCell(0);
			cell.setCellValue("Frame");
	
			cell = row.createCell(1);
			cell.setCellValue("Time");
	
			cell = row.createCell(2);
			cell.setCellValue("Mean Intensity");
			
			cell = row.createCell(12);
			cell.setCellValue("Milestones");
		} else {
			//Clone template
			System.out.println(workbook.getNameIndex("template"));
			spreadSheet = workbook.cloneSheet(workbook.getNameIndex("template")+1, infoString);
		}
	//	CopySheets.copySheets(spreadSheet, templateSheet, true);

		//Apply data to cells
		for (int i = 0; i < chartData.getItemCount();i++) {
//			List<XSSFTable> tableList = spreadSheet.getTables();
//			CellReference CR = new CellReference(0, 0); 
//			CellReference CR2 = new CellReference(5, 2);
//			AreaReference my_data_range = new AreaReference(CR,CR2, SpreadsheetVersion.EXCEL2007);
//			tableList.get(0).setArea(my_data_range);
//			
			row = spreadSheet.getRow(i+1);
			if (row == null) {
				row = spreadSheet.createRow(i+1);
			}
			
			Cell cell = row.createCell(0);
			cell.setCellValue(i+1);

			cell = row.createCell(1);
			cell.setCellValue(chartData.getDataItem(i).getXValue());

			cell = row.createCell(2);
			cell.setCellValue(chartData.getDataItem(i).getYValue());	
		}
		//Apply Milestones
		for (int i = 0; i < Splitter.SU1.milestones.size();i++) {
			row = spreadSheet.getRow(i+1);
			if (row == null) {
				row = spreadSheet.createRow(i+1);
			}
			
			Cell cell = row.createCell(12);
			cell.setCellValue(Splitter.SU1.milestones.get(i));
			
			long k = i-10;
			long l = Splitter.SU1.milestones.get(i)+1;
			if (k < 1) {k = 2;}
			
			cell = row.createCell(13);
			cell.setCellFormula("AVERAGE(C" + l + ":C"+ k + ")");
		}

		
		GenerateChart(spreadSheet);
		System.out.println("APPLYDATA COMPLETE");
	}
	
	public void Save(XSSFWorkbook wb, String n) throws IOException {
	      //Create file system using specific name
	      FileOutputStream out = new FileOutputStream(new File(path + "\\" + n + ".xlsx"));
		  wb.write(out);
		  out.close();
	      System.out.println(n + ".xlsx written successfully");
	      ShowWB();
	}
	
	public void SaveAndClose(XSSFWorkbook workbook, String fname) throws IOException {
	      //Create file system using specific name
		boolean re = true;
		FileOutputStream out = null;
		while (re) {
			try {
		      out = new FileOutputStream(new File(path + "\\" + fname + ".xlsx"));
		      re = false;
			}
			catch(IOException e){
				e.printStackTrace();
                JOptionPane.showMessageDialog(Icy.getMainInterface().getMainFrame(),
                							  "Close the Excel sheet", "Datasheet is in use",
                							  JOptionPane.ERROR_MESSAGE);}
		}
		  workbook.write(out);
		  out.close();
		  workbook.close();
	      System.out.println(fname + ".xlsx written successfully");
	      //Open the file
	      ShowWB();
	      //Exit plugin
	      
		}
	
	public void GenerateChart(XSSFSheet sheet) {
		if (sheet.getLastRowNum()==0) {return;}
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 10, 15);

        XSSFChart chart = drawing.createChart(anchor);
        XSSFChartLegend legend = chart.getOrCreateLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);

        XSSFValueAxis bottomAxis = chart.createValueAxis(AxisPosition.BOTTOM);
        XSSFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        CellRangeAddress crXData = new CellRangeAddress(1, sheet.getLastRowNum(), 0, 0);
        CellRangeAddress crYData = new CellRangeAddress(1, sheet.getLastRowNum(), 1, 1);
        CellReference crTitle = new CellReference(0,1);
        Cell cell = sheet.getRow(crTitle.getRow()).getCell(crTitle.getCol());
        
        ChartDataSource<Number> dsXData = DataSources.fromNumericCellRange(sheet, crXData);
        ChartDataSource<Number> dsYData = DataSources.fromNumericCellRange(sheet, crYData);
        
        XSSFScatterChartData data = chart.getChartDataFactory().createScatterChartData();
        ScatterChartSeries seriesTitler = data.addSerie(dsXData, dsYData);
        
        seriesTitler.setTitle(cell.getStringCellValue());
        chart.plot(data, bottomAxis, leftAxis);
        
        //set properties of first scatter chart data series to not smooth the line:
        ((XSSFChart)chart).getCTChart().getPlotArea().getScatterChartArray(0).getSerArray(0)
         .addNewSmooth().setVal(false);

        //set properties of first scatter chart to not vary the colors:
        ((XSSFChart)chart).getCTChart().getPlotArea().getScatterChartArray(0)
         .addNewVaryColors().setVal(false);
	}
	
	
	public void ShowWB() {
		if (!Desktop.isDesktopSupported()) {
	        System.err.println("Desktop not supported");
	        // use alternative (Runtime.exec)
	        return;
	    }

	    Desktop desktop = Desktop.getDesktop();
	    if (!desktop.isSupported(Desktop.Action.EDIT)) {
	        System.err.println("EDIT not supported");
	        // use alternative (Runtime.exec)
	        return;
	    }

	    try {
	        desktop.open(new File(path + "\\" + name + ".xlsx"));
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	}
}