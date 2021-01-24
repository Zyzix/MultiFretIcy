package plugins.MasoudR.multifreticy.Main;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import icy.gui.dialog.MessageDialog;
import plugins.MasoudR.multifreticy.DataObjects.MyCoordinates;


@SuppressWarnings("serial")
public class CornerFinder extends JPanel{

	public CornerFinder() {
		/*
		 * 1. 	Convert ROI into arraylists
		 * 2. 	For each point, we go forward 15 points and backwards 15 points
		 * 3. 	We check that all these points are along one line (hurrahs)
		 * 4. 	The line is added to a list, and this process is repeated until all lines are found
		 * ** 	multiple lines may be found alone the same edge of a polygon, 
		 * 		this results in an error (probably means the edge was too distorted)
		 * ->	TODO: could make it so similar lines are averaged
		 * 5. 	We use the intersect of these lines to create a corner
		 * ** 	if 4 lines are not found, we get an error
		 */
	}
	
	public ArrayList<ArrayList<MyCoordinates>> Aktivat(File contourFile){
		//Holder for all corners holders
		ArrayList<ArrayList<MyCoordinates>> allCorners = new ArrayList<ArrayList<MyCoordinates>>();
		// TODO Auto-generated method stub
		try {

			File fXmlFile = contourFile;
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
					
			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
					
			NodeList rList = doc.getElementsByTagName("roi");
			

			//For loop iterating each ROI
			for (int roiNum = 0; roiNum < rList.getLength(); roiNum++) {
				System.out.println("\nWe are at ROI #" + roiNum);
				Node rNode = rList.item(roiNum);	
				Element erElement = (Element) rNode;
				
				NodeList rnl = erElement.getElementsByTagName("name");
				Node rn = rnl.item(0);
				String roiName = rn.getTextContent();
				System.out.println("Roi Name = " + roiName);

				NodeList nList = erElement.getElementsByTagName("point");
						System.out.println(nList.getLength());
						System.out.println("----------------------------");
						System.out.println("////////////////////////////");
						System.out.println("----------------------------");
				int count1 = 0;
				ArrayList<MyCoordinates> points = new ArrayList<MyCoordinates>();
				ArrayList<MyCoordinates> formulae = new ArrayList<MyCoordinates>();
				ArrayList<MyCoordinates> corners = new ArrayList<MyCoordinates>();
				ArrayList<MyCoordinates> xmlPoints = new ArrayList<MyCoordinates>();
				//lowest/highest
				double lowX = 0, lowY = 0, highX = 0, highY = 0;			
				//convert xml into arraylist and record lowest/highest points
				for (int temp = 0; temp < nList.getLength(); temp++) {
					System.out.println("temp at " + temp + " out of " + nList.getLength());
					Node nNode = nList.item(temp);

					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						
						Element eElement = (Element) nNode;
						
						MyCoordinates xy = new MyCoordinates(Double.parseDouble(eElement.getElementsByTagName("pos_x").item(0).getTextContent()), 
															Double.parseDouble(eElement.getElementsByTagName("pos_y").item(0).getTextContent()),
															roiName);
						if (lowX == 0 || xy.getX() < lowX) {lowX = xy.getX();}
						if (highX == 0 || xy.getX() > highX) {highX = xy.getX();}
						if (lowY == 0 || xy.getY() < lowY) {lowY = xy.getY();}
						if (highY == 0 || xy.getY() > highY) {highY = xy.getY();}

						xmlPoints.add(xy);	
					}	
				}
				
				//For loop iterating each Point
				for (int temp = 0; temp < xmlPoints.size(); temp++) {
					System.out.println("temp at " + temp + " out of " + xmlPoints.size());
					double x1 = 0,x2 = 0,x3 = 0,y1 = 0,y2 = 0,y3 = 0;
					
					
					//a priori
					
					int t = temp;
					//Adjust these variables if difficulties
					int spreadvar =15;
					double distanceLimit = 0.8;
					int hurrahLimit = 30;
					
					if (t-spreadvar < 0) {t = xmlPoints.size()+(t-spreadvar);}
						else {t = temp-spreadvar;}
					int t1 = t;
					
					System.out.println("\nCurrent Element-1 : " + t);
					if (xmlPoints.get(t).stopper) {System.out.println("stopper found"); continue;} //TODO:perhaps better to make t-1 and spreadvar -1 beign taken instead of continue?

						x2 = xmlPoints.get(t).getX();
						y2 = xmlPoints.get(t).getY();
						System.out.println("\tx : " + x2);
						System.out.println("\ty : " + y2);
	
											
					//a currenci
						
					System.out.println("\nCurrent Element : " + temp);
					if (xmlPoints.get(temp).stopper) {System.out.println("stopper found"); continue;}

						x1 = xmlPoints.get(temp).getX();
						y1 = xmlPoints.get(temp).getY();
						System.out.println("\tx : " + x1);
						System.out.println("\ty : " + y1);
						MyCoordinates p1 = new MyCoordinates(x1,y1,roiName);
						points.add(p1);
					
							
					//a posteriori
					t = temp;
					
					if (t+spreadvar > xmlPoints.size()-1) {t = (t+spreadvar) - (xmlPoints.size());}
					else {t = temp+spreadvar;}
	
					System.out.println("\nCurrent Element+1 : " + t);
					if (xmlPoints.get(t).stopper) {System.out.println("stopper found"); continue;}

						x3 = xmlPoints.get(t).getX();
						y3 = xmlPoints.get(t).getY();
						System.out.println("\tx : " + x3);
						System.out.println("\ty : " + y3);

					
					System.out.println("-------------Angle---------------");
					double angle = CalculateAngle(x1,x2,x3,y1,y2,y3)[0];
					System.out.println(angle);
					if (angle < 91 ) {System.out.println("Possible corner");}
					System.out.println("----------------------------");
					System.out.println("----------------------------");
					
					if (count1 == 1) {
							
						//TestPoints(nList, t1, t, x2, y2, x3, y3);
						
						// We store the numbers for the line of t1(x2y2) - t2 - t3(x3y3)
						int firstT = t1;
						int lastT = t;
						double firstX = x2;
						double lastX = x3;
						double firstY = y2;
						double lastY = y3;
						
						System.out.println("Testing between points " + firstT + " and " + lastT);
						int hurrah = 0;
						ArrayList<MyCoordinates> ac = new ArrayList<MyCoordinates>();

//						if (firstT+1 > xmlPoints.size()-1) {firstT = (firstT+1) - (xmlPoints.size());}
//						else {firstT = firstT+1;} //??why was this here
						int hurrahTrain = 1;
						int lastTHolder = lastT;
						while (hurrahTrain == 1) { //while between the two lines

							if (lastT-1 < 0) {lastT = xmlPoints.size()+lastT-1;}
							else {lastT = lastT-1;}
							
							System.out.println("\nTesting Current Element : " + lastT);

								double pointX = xmlPoints.get(lastT).getX();
								double pointY = xmlPoints.get(lastT).getY();
								
								System.out.println(lastT + " THE TEES " +  firstT);
								System.out.println(firstX + " THE AXES " + lastX + " n " + pointX);
								
								if (lastT == firstT) {
									MyCoordinates c1 = new MyCoordinates(pointX,pointY,roiName);
									   hurrah++;
										System.out.println("Passing left boundary");
										System.out.println("----------------------------");
										System.out.println("hurrah " + hurrah );
										System.out.println("x2,y2= " + firstX + "," + firstY + " x3,y3= " + lastX + "," + lastY);
										System.out.println("x1,y1= " + pointX + "," + pointY);
										System.out.println("----------------------------");
									   ac.add(c1);
									   continue;
									}

								System.out.println("----------------------------");
								double d = CalculateAngle(pointX,firstX,lastX,pointY,firstY,lastY)[1];
								if (xmlPoints.get(lastT).stopper) {System.out.println("stopper found, switching direction");}
								if (d < distanceLimit && !xmlPoints.get(lastT).stopper) { 
								   hurrah++; 

								   System.out.println("hurrah " + hurrah );
								   System.out.println("x2,y2= " + firstX + "," + firstY + " x3,y3= " + lastX + "," + lastY);
								   System.out.println("x1,y1= " + pointX + "," + pointY);
								   
								   MyCoordinates c1 = new MyCoordinates(pointX,pointY,roiName);
								  // MyCoordinates c2 = new MyCoordinates(lastX,lastY);
								   
								   ac.add(c1);
								  // ac.add(c2);
									
									System.out.println("1Distance of point to line: " + d);
									System.out.println("Number of points in polygon: " + xmlPoints.size());
									System.out.println("----------------------------");

								} else if(hurrah > hurrahLimit) {	
									System.out.println("MISSED HURRAH, SWITCHING DIRECTION");
									//We have a new left-bound
									firstT = lastT; 
									lastT = lastTHolder;
									
									firstX = ac.get(ac.size()-1).getX();
									firstY = ac.get(ac.size()-1).getY();
									//Flip the list, we're going in the opposite direction now
									Collections.reverse(ac);
									while (hurrahTrain == 1) { //while between the two lines

										if (lastT+1 > xmlPoints.size()-1) {lastT = (lastT+1) - (xmlPoints.size());}
										else {lastT = lastT+1;}
																		
										System.out.println("\nRTesting Current Element : " + lastT);
								
											double pointX2 = xmlPoints.get(lastT).getX();
											double pointY2 = xmlPoints.get(lastT).getY();
											
											if (lastT == firstT) {
												MyCoordinates c1 = new MyCoordinates(pointX2,pointY2,roiName);
												   hurrah++;
													System.out.println("Passing right boundary");
													System.out.println("----------------------------");
													System.out.println("hurrah " + hurrah );
													System.out.println("x2,y2= " + firstX + "," + firstY + " x3,y3= " + lastX + "," + lastY);
													System.out.println("x1,y1= " + pointX2 + "," + pointY2);
													System.out.println("----------------------------");
												   ac.add(c1);
												   continue;
												}
											
										System.out.println("----------------------------");
											double d2 = CalculateAngle(pointX2,firstX,lastX,pointY2,firstY,lastY)[1];
											if (xmlPoints.get(lastT).stopper) {System.out.println("stopper found, finalising");}
											if (d2 < distanceLimit && !xmlPoints.get(lastT).stopper ) { 
											   hurrah++; 

											   System.out.println("hurrah " + hurrah );
											   System.out.println("x2,y2= " + firstX + "," + firstY + " x3,y3= " + lastX + "," + lastY);
											   System.out.println("x1,y1= " + pointX2 + "," + pointY2);
											   
											   MyCoordinates c1 = new MyCoordinates(pointX2,pointY2,roiName);
											  // MyCoordinates c2 = new MyCoordinates(lastX,lastY);
											   
											   ac.add(c1);
											  // ac.add(c2);
												
												System.out.println("2Distance of point to line: " + d2);
												System.out.println("----------------------------");

											   } else {hurrahTrain = 0;
											   System.out.println("final # at " + hurrah);
											   //New right-bound
												lastX = ac.get(ac.size()-1).getX();
												lastY = ac.get(ac.size()-1).getY();
												   if (ac.size() > hurrahLimit) { 

													   while (lastT > firstT) {
														   System.out.println("for loop test: " + lastT);
														   lastT--;
													   	   xmlPoints.remove(lastT);
													   }
													   //Make Line formula and store in arraylist
													   /*
													    * get slope a = y1-y2 / x1-x2
													    * y = ax+b
													    * b = y-ax
													    * intersect: ax+b = cx+d, solve x = the intersect x
													    * input in y=ax+b for y
													    * how to code:
													    * intersect =  x = (d-b)/(a-c)
													    * 
													    */
													   if (!xmlPoints.get(lastT).stopper) {
														   MyCoordinates placeholder = new MyCoordinates(0,0,roiName);
														   placeholder.stopper = true;
														   xmlPoints.add(lastT, placeholder);
														   }
													   
													   System.out.println("Codordonets:" + firstX + "," + firstY + " " + lastX + "," + lastY);
													   double slope = (firstY-lastY)/(firstX-lastX);
													   double intercept = firstY - (slope*firstX);
													   System.out.println("slope "+slope);
													   System.out.println("intercept "+intercept);
													   
													   MyCoordinates p2 = new MyCoordinates(slope,intercept,roiName,ac);
													   formulae.add(p2);		
													   System.out.println("Formula added, now at "+ formulae.size());
												   }
											   	}
											
										}	
									hurrahTrain = 0;} else {hurrahTrain = 0;}
								
							
						}
						
						
						
						
						count1 = 0;
					}
					count1++;					
				} //ShapesDemo2D sd2 = new ShapesDemo2D(); sd2.DrawPoly(PathPolygon(points));
				corners = FindCorners(formulae,lowX,highX,lowY,highY);
				if(corners == null) {return null;}
				allCorners.add(corners);
				//PathPolygon(points);
			}
	    } catch (Exception e) {
		e.printStackTrace();
    	}
		return allCorners;
	}

	public double[] CalculateAngle(double x1,double x2,double x3,double y1,double y2,double y3){
		System.out.println("x1 :" + x1 + "\nx2 :" + x2 + "\nx3 :" + x3 + "\ny1 :" + y1 + "\ny2 :" + y2 + "\ny3 :" + y3);
		double dx12 = Math.abs(x1-x2);
		double dx13 = Math.abs(x1-x3);
		double dy12 = Math.abs(y1-y2);
		double dy13 = Math.abs(y1-y3);
		double dx23 = Math.abs(x2-x3);
		double dy23 = Math.abs(y2-y3);
								
		double a = Math.sqrt(Math.pow(dx12,2) + Math.pow(dy12,2));
		double b = Math.sqrt(Math.pow(dx13,2) + Math.pow(dy13,2));
		double c = Math.sqrt(Math.pow(dx23,2) + Math.pow(dy23,2));
			System.out.println("a = " + a + " b = " + b + " c = " + c);
		
		double angleC = Math.abs(Math.acos((Math.pow(a, 2)+Math.pow(b, 2)-Math.pow(c,2))/(2*a*b)));
		double angleB = Math.abs(Math.acos((Math.pow(a, 2)+Math.pow(c, 2)-Math.pow(b,2))/(2*a*c)));
		//double angleA = Math.abs(Math.acos((Math.pow(b, 2)+Math.pow(c, 2)-Math.pow(a,2))/(2*b*c)));
			System.out.println("Angle C: "+angleC);		
		angleC = (180*angleC)/Math.PI; //Convert to degrees from rads

		double distance = Math.sin(angleB)*a;
		
		System.out.println("sin"+angleB+"*"+a+"="+distance);
		double[] values = new double[4];
		values[0] = angleC;
		values[1] = distance;
		values[2] = dx23;
		values[3] = dy23;
		return values;
	}
	
	public ArrayList<MyCoordinates> FindCorners(ArrayList<MyCoordinates> pList, double lowX, double highX, double lowY, double highY) {
		ArrayList<MyCoordinates> corners = new ArrayList<MyCoordinates>();
		
		for (int i = 0; i < pList.size(); i++) { //For each slope/intercept in the list
			for (int j = i+1; j < pList.size(); j++) { //Compare to each other pair
				
				int goAhead = 1;
				//for loops to test for common point
				for (int k = 0; k < pList.get(i).getPoints().size();k++) { //See if these pairs have a common point
					for(int l = 0; l < pList.get(j).getPoints().size();l++) {						
						if (pList.get(j).getPoints().get(l).getX() == pList.get(i).getPoints().get(k).getX() && 
							pList.get(j).getPoints().get(l).getY() == pList.get(i).getPoints().get(k).getY() ){ //If so, abort.
									goAhead = 0;
									System.out.println("Common point found");
							}
						if(pList.get(i).getX()*pList.get(j).getPoints().get(l).getX()+pList.get(i).getY() == pList.get(j).getPoints().get(l).getY()) {
							System.out.println("Common formula found");
							goAhead = 0;

						}
					}
					
					

				}
				if (goAhead == 1) {
//						System.out.println("1 : " + pList.get(i).getX() + "," + pList.get(i).getY());
//						System.out.println("2 : " + pList.get(j).getX() + "," + pList.get(j).getY());
						//intersect =  x = (d-b)/(a-c)
						double intersectX = (pList.get(j).getY()-pList.get(i).getY())/(pList.get(i).getX()-pList.get(j).getX());
						double intersectY = pList.get(i).getX()*intersectX+pList.get(i).getY();
						String roiName = pList.get(0).getRoiName();

						if (0.8*lowX<intersectX && intersectX<1.2*highX) {			//TODO: ass	
								if (0.8*lowY<intersectY && intersectY<1.2*highY) {
							System.out.println("Intersect at (" + intersectX + "," + intersectY + ")");
							MyCoordinates p3 = new MyCoordinates(intersectX,intersectY,roiName);
							
							corners.add(p3);	
							
							//TODO:add autocalibration for low/high XY.

//							for (int m = 0; m < pList.get(j).c.size(); m++) {
//								System.out.println("\n"+pList.get(j).c.get(m).getX());
//								System.out.println(pList.get(j).c.get(m).getY());
//							}
//							System.out.println("\n\nvs\n\n");
//							for (int m = 0; m < pList.get(i).c.size(); m++) {
//								System.out.println("\n"+pList.get(i).c.get(m).getX());
//								System.out.println(pList.get(i).c.get(m).getY());
//							}
							
							
							}
						}
						
				} else { System.out.println("Same line");} 
//d
			}		
		}	
		
		//Remove duplicate corners through rounding
		for (int n = corners.size()-1; n > -1; n--) {
			for (int n2 = n-1; n2 > -1; n2--) {
				System.out.println(n + " " + n2);

				if(Math.abs(corners.get(n).getX()-corners.get(n2).getX()) < 5 
					&& Math.abs(corners.get(n).getY()-corners.get(n2).getY()) < 5){
						corners.get(n2).setX((corners.get(n).getX()+corners.get(n2).getX())/2);
						corners.get(n2).setY((corners.get(n).getY()+corners.get(n2).getY())/2);
						corners.remove(n); 
						System.out.println("Averaged a cornerpair");
						break;
					}
								
//				int x1 = (int) corners.get(n).getX();
//				int y1 = (int) corners.get(n).getY();
//				int x2 = (int) corners.get(n2).getX();
//				int y2 = (int) corners.get(n2).getY();
//				
//				if (x1 == x2 && y1 == y2 && n != n2) {
//					corners.remove(n); 
//					System.out.println("Removed a duplicate corner");
//					break;
//					}
			}			
		}
		
		if (corners.size() != 4) {System.out.println("\n\nWE FUCKED UP\n");
		MessageDialog.showDialog("Unable to find corners, ROI may be too distorted.");
		return null;
		}
		System.out.println("\nCorners found:");
		for (int m = 0; m < corners.size(); m++) {System.out.println(corners.get(m).getX() + " " + corners.get(m).getY());}
		return corners;
	}
	
	
	public Path2D PathPolygon(ArrayList<MyCoordinates> points) {
		Path2D path = new Path2D.Double();

		path.moveTo(points.get(0).getX(), points.get(0).getY());
		for(int i = 1; i < points.size(); ++i) {
		   path.lineTo(points.get(i).getX(), points.get(i).getY());
		}
		path.closePath();
		System.out.println("Bounds: " + path.getBounds().getX() + " " + 
				path.getBounds2D().getY() + " " +
				path.getBounds2D().getWidth() + " " +
				path.getBounds2D().getHeight());

		return path;
	}	
	
	public double[] getItem(int t, NodeList nodeList){
		
		Node nNode = nodeList.item(t);
		double[] result = new double[2];
		
		System.out.println("\nCurrent Element : " + nNode.getNodeName() + " " + t);
				
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

			Element eElement = (Element) nNode;

			result[0] = Double.parseDouble(eElement.getElementsByTagName("pos_x").item(0).getTextContent());
			result[1] = Double.parseDouble(eElement.getElementsByTagName("pos_y").item(0).getTextContent());
			System.out.println("\tx : " + result[0]);
			System.out.println("\ty : " + result[1]);

		}
		
		return result;
	}
	
}

