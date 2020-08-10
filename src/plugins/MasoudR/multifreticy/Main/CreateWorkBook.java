package plugins.MasoudR.multifreticy.Main;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.charts.AxisCrosses;
import org.apache.poi.ss.usermodel.charts.AxisPosition;
import org.apache.poi.ss.usermodel.charts.ChartDataSource;
import org.apache.poi.ss.usermodel.charts.DataSources;
import org.apache.poi.ss.usermodel.charts.LegendPosition;
import org.apache.poi.ss.usermodel.charts.ScatterChartSeries;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.charts.XSSFChartLegend;
import org.apache.poi.xssf.usermodel.charts.XSSFScatterChartData;
import org.apache.poi.xssf.usermodel.charts.XSSFValueAxis;
import org.jfree.data.xy.XYSeries;

import icy.main.Icy;
import loci.poi.hssf.util.CellReference;
import plugins.MasoudR.multifreticy.MultiFretIcy;
import plugins.MasoudR.multifreticy.DataObjects.VarIntRoi;

@SuppressWarnings("deprecation")
public class CreateWorkBook {

	//public String path = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
	public String path;
	public String name;
	public XSSFWorkbook workbook;
	private XSSFSheet spacerSheet;
	public File screenshot;
	private File backupFile;
	
	public CreateWorkBook(String outLoc) throws Exception {
		//Open or create workbook
		path = FilenameUtils.getFullPath(outLoc);
		name = FilenameUtils.getName(outLoc);
		if (!name.endsWith(".xlsx")) {
			name = name + ".xlsx";
		}
		OpenWB(path, name);
   }
	
	public XSSFWorkbook OpenWB(String directory, String name) {
 	      File file = new File(directory + name);
	      FileInputStream fIP;	      
	      
	      String backupName = name + "_backup";
	      backupFile = new File(directory + backupName);
	      Integer b = 0;
	      while (backupFile.isFile() && backupFile.exists()) {
	    	  String newName = backupName.concat(b.toString());
	    	  backupFile = new File(directory + newName);
	    	  b++;
	      }
	      
	    //Create Backup file
	      if(file.exists() && file.length() != 0) {
	    	try {
				FileUtils.copyFile(file, backupFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	    	
	    	//Open file
	  		try {
				fIP = new FileInputStream(file);
			    //Get the workbook instance for XLSX file 
				 workbook = new XSSFWorkbook(fIP);
				 fIP.close();				 
		         System.out.println(name + " file open successfully.");
		     	//Create spacer sheet
		        Boolean done = false;
		        int num = 0;
		        String sheetName = "Experiment#" + num;		        
		 		while (!done) { 
					if (workbook.getSheet(sheetName) == null) {
						System.out.println("Creating sheet " + sheetName);
						done = true;
					}
					else {
						sheetName = "Experiment#" + num;
						num++;
					}
		 		}        
		         spacerSheet = workbook.createSheet(sheetName);
		         return workbook;
			} catch (IOException e) {
				e.printStackTrace();
		         System.out.println("Error to open " + name + " file, creating blank");
			      //Create Blank workbook
			      workbook = new XSSFWorkbook(); 
			      Integer i = 0;
			      while (file.isFile() && file.exists()) {
			    	  String newName = name.concat(i.toString());
			    	  file = new File(directory + newName);
			    	  i++;
			      }
			  	//Create spacer sheet
			  	spacerSheet = workbook.createSheet("Experiment");
			      return workbook;
			}
	      } else {
	         System.out.println("Error to open " + name + "file, creating blank");
		      //Create Blank workbook
		      workbook = new XSSFWorkbook(); 
		  	//Create spacer sheet
		  	spacerSheet = workbook.createSheet("Experiment");
		      return workbook;
	      }
//	      System.out.println("Error opening Workbook");
//		return null;
	}	


	public void ApplyData(	ArrayList<XYSeries> kVRatio, 
							Map<String,ArrayList<ArrayList<Double>>> rawDataPack,
							String XYinfo, 
							ArrayList<VarIntRoi> roi) {
		DateFormat dateFormat = new SimpleDateFormat("dd-MM");
		Date date = new Date();
	
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
					infoString = infoString0 + "#" + j;
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
			
			Cell cell = row.createCell(1);
			cell.setCellValue("Milestones");
			
		} else {
			try {
			//Clone template
			System.out.println(workbook.getNameIndex("template"));
			spreadSheet = workbook.cloneSheet(workbook.getNameIndex("template")+1, infoString);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Cannot clone Template sheet, POI 4.0 is missing or a clashing version is present.");
			}
		}
	//	CopySheets.copySheets(spreadSheet, templateSheet, true);

		//Apply data to cells
		int col = 0;
		ArrayList<CellRangeAddress> dataRanges = new ArrayList<CellRangeAddress>();		
		for (XYSeries serie : kVRatio) {
			// Title dataset
			row = spreadSheet.getRow(1);
			if (row == null) {
				row = spreadSheet.createRow(1);
			}
			Cell cell = row.createCell(col+9);
			cell.setCellValue((String) serie.getKey());
			// Populate
			for (int i = 0; i < serie.getItemCount(); i++) {
				System.out.println("serie is " + serie.getKey());
	//			List<XSSFTable> tableList = spreadSheet.getTables();
	//			CellReference CR = new CellReference(0, 0); 
	//			CellReference CR2 = new CellReference(5, 2);
	//			AreaReference my_data_range = new AreaReference(CR,CR2, SpreadsheetVersion.EXCEL2007);
	//			tableList.get(0).setArea(my_data_range);
	//			
				// Headers
				XSSFRow headerRow = spreadSheet.getRow(0);
				if (headerRow == null) {
					headerRow = spreadSheet.createRow(0);
				}
				
				cell = headerRow.createCell(col+9);
				cell.setCellValue("ROI");
				
				cell = headerRow.createCell(col+10);
				cell.setCellValue("Frame");
		
				cell = headerRow.createCell(col+11);
				cell.setCellValue("Time");
		
				cell = headerRow.createCell(col+12);
				cell.setCellValue("Mean Intensity");
				
				cell = headerRow.createCell(col+14);
				cell.setCellValue("Numerator Raw");
				
				cell = headerRow.createCell(col+15);
				cell.setCellValue("Numerator Background Raw");
				
				cell = headerRow.createCell(col+16);
				cell.setCellValue("Divisor Raw");
				
				cell = headerRow.createCell(col+17);
				cell.setCellValue("Divisor Background Raw");
				
				// Data
				row = spreadSheet.getRow(i+1);
				if (row == null) {
					row = spreadSheet.createRow(i+1);
				}
				
				cell = row.createCell(col + 10);
				cell.setCellValue(i+1);
	
				cell = row.createCell(col + 11);
				cell.setCellValue(serie.getDataItem(i).getXValue());
	
				cell = row.createCell(col + 12);
				cell.setCellValue(serie.getDataItem(i).getYValue());
				
				cell = row.createCell(col + 14);
				cell.setCellValue(rawDataPack.get(serie.getKey()).get(0).get(i));
				
				cell = row.createCell(col + 15);
				if (rawDataPack.get("bg").get(0).size() > 0) {
					cell.setCellValue(rawDataPack.get("bg").get(0).get(i));
					}
				
				cell = row.createCell(col + 16);
				cell.setCellValue(rawDataPack.get(serie.getKey()).get(1).get(i));
				
				cell = row.createCell(col + 17);
				if (rawDataPack.get("bg").get(1).size() > 0) {
					cell.setCellValue(rawDataPack.get("bg").get(1).get(i));
					}
				
			}
	        dataRanges.add(new CellRangeAddress(1, spreadSheet.getLastRowNum(), col+12, col+12));
			col += 10;
		}
		
		//Apply Milestones
		for (int i = 0; i < MultiFretIcy.PS.S1.SU1.milestones.size();i++) {
			row = spreadSheet.getRow(i+1);
			if (row == null) {
				row = spreadSheet.createRow(i+1);
			}
			
			Cell cell = row.createCell(0);
			cell.setCellValue(MultiFretIcy.PS.S1.SU1.milestones.get(i).getName());
			
			cell = row.createCell(1);
			cell.setCellValue(MultiFretIcy.PS.S1.SU1.milestones.get(i).getFrame());
			
			
			long l = MultiFretIcy.PS.S1.SU1.milestones.get(i).getFrame()+1;
			long k = l-10;
			if (k < 1) {k = 2;}
			
			cell = row.createCell(2);
			cell.setCellFormula("AVERAGE(M" + l + ":M"+ k + ")");
		}

		
		GenerateChart(spreadSheet, dataRanges);
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
	
	public void SaveAndClose(byte[] sf) throws IOException {
		
		//Get the contents of an image file as a byte[].
//		byte[] imageData = Files.readAllBytes(sf.toPath());   
		//Adds a picture to the workbook
		int pictureureIdx = workbook.addPicture(sf, XSSFWorkbook.PICTURE_TYPE_PNG);
	   //Returns an object that handles instantiating concrete classes
	   CreationHelper helper = workbook.getCreationHelper();
	   //Creates the top-level drawing patriarch.
	   Drawing<?> drawing = spacerSheet.createDrawingPatriarch();
	   //Create an anchor that is attached to the worksheet
	   ClientAnchor anchor = helper.createClientAnchor();
	   //set top-left corner for the image
	   anchor.setCol1(1);
	   anchor.setRow1(2);
	   //Creates a picture
	   Picture pict = drawing.createPicture(anchor, pictureureIdx);
	   //Reset the image to the original size
	   pict.resize();

      //Create file system using specific name
		boolean re = true;
		FileOutputStream out = null;
		while (re) {
			try {
		      out = new FileOutputStream(new File(path + name));
			  workbook.write(out);
			  out.close();
		      re = false;		      
			}
			catch(IOException e){
				e.printStackTrace();
                JOptionPane.showMessageDialog(Icy.getMainInterface().getMainFrame(),
                							  "Close the Excel sheet", "Datasheet is in use",
                							  JOptionPane.ERROR_MESSAGE);}
		}

		  workbook.close();
	      System.out.println(name + "written successfully");
	      
	      FileUtils.deleteQuietly(backupFile);
	      
	      //Open the file
	      ShowWB();
	      //Exit plugin
	      
		}
	
	public void GenerateChart(XSSFSheet sheet, ArrayList<CellRangeAddress> dataRanges) {
		if (sheet.getLastRowNum()==0) {return;}
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 10, 15);

        XSSFChart chart = drawing.createChart(anchor);
        XSSFChartLegend legend = chart.getOrCreateLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);

        XSSFValueAxis bottomAxis = chart.createValueAxis(AxisPosition.BOTTOM);
        XSSFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        CellRangeAddress crXData = new CellRangeAddress(1, sheet.getLastRowNum(), 11, 11);
        ChartDataSource<Number> dsXData = DataSources.fromNumericCellRange(sheet, crXData);

//        CellRangeAddress crYData = new CellRangeAddress(1, sheet.getLastRowNum(), 12, 12);
//        CellReference crTitle = new CellReference(0,2);
//        Cell cell = sheet.getRow(crTitle.getRow()).getCell(crTitle.getCol());
        
        XSSFScatterChartData data = chart.getChartDataFactory().createScatterChartData();
        for (CellRangeAddress crYData : dataRanges) {        
	        ChartDataSource<Number> dsYData = DataSources.fromNumericCellRange(sheet, crYData);        
	        ScatterChartSeries seriesTitler = data.addSerie(dsXData, dsYData); //Add series
	        try {
	        seriesTitler.setTitle(sheet.getSheetName()); // Chart title, change to cell.getvalue for title from cell
	        } catch (Exception e) {seriesTitler.setTitle("seriesTitle");}
        }
        
        chart.setTitle(sheet.getSheetName());
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
	        desktop.open(new File(path + name));
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	}
}