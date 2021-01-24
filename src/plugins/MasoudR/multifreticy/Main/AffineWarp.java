package plugins.MasoudR.multifreticy.Main;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import icy.sequence.Sequence;
import plugins.MasoudR.multifreticy.DataObjects.MyCoordinates;

public class AffineWarp {
    public AffineWarp(ArrayList<MyCoordinates> mc, Sequence seq) {
        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    
    public static Mat BufferedImageToMat(BufferedImage bi) {
    	  Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_16U);
    	  short[] data = ((DataBufferUShort) bi.getRaster().getDataBuffer()).getData();
    	  mat.put(0, 0, data);
    	  return mat;
    	}
    
    public static Mat BufferedImage2Mat(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "BMP", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
    }

    public static BufferedImage mat2Img(Mat in,BufferedImage bi)
    {
        BufferedImage out;
        short[] data = new short[128*98*(int)in.elemSize()];
        in.get(0, 0, data);

        out = new BufferedImage(128, 98, BufferedImage.TYPE_USHORT_GRAY);

        out.getRaster().setDataElements(0, 0, 128, 98, data);
        return out;
    } 
    
public BufferedImage GeometricTransforms(ArrayList<MyCoordinates> mc, Sequence seq) {
		System.out.println("DATATYPE: " + seq.getLastImage().getDataType_().toString());
		
		/*
		 * having data type woes
		 * save seq.image and use a testerproj to examine it
		 */		
		File outputfile = new File("testimgfromseq.bmp");
		try {
			ImageIO.write(seq.getLastImage(), "bmp", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		
	     Mat src = null;
 		src = BufferedImageToMat(seq.getLastImage());


        if (src.empty()) {
            System.err.println("Cannot read image");
            System.exit(0);
        }
       
        Point[] srcTri = new Point[3];
        srcTri[0] = new Point( mc.get(0).getX(), mc.get(0).getY() );
        srcTri[1] = new Point( mc.get(1).getX(), mc.get(1).getY() );
        srcTri[2] = new Point( mc.get(2).getX(), mc.get(2).getY() );
//        srcTri[3] = new Point( mc.get(3).getX(), mc.get(3).getY() );
        Point[] dstTri = new Point[3];
        dstTri[0] = new Point( 0, 0 );
        dstTri[1] = new Point( 128, 0 );
        dstTri[2] = new Point( 128, 98 );
//        dstTri[3] = new Point( 0, 98 );

        System.out.println("Affine warp origin " + srcTri[0] + ", " + srcTri[1] + ", " + srcTri[2]);
        System.out.println("Affine warp destination " + dstTri[0] + ", " + dstTri[1] + ", " + dstTri[2]);
        
        Mat warpMat = Imgproc.getAffineTransform( new MatOfPoint2f(srcTri), new MatOfPoint2f(dstTri) );
        Mat warpDst = Mat.zeros( 98, 128, src.type() );
        Imgproc.warpAffine( src, warpDst, warpMat, warpDst.size() );
//        Point center = new Point(warpDst.cols() / 2, warpDst.rows() / 2);
//        double angle = -50.0;
//        double scale = 0.6;
        double angle = getAngle(srcTri);
        Mat rotMat = Imgproc.getRotationMatrix2D( srcTri[0], angle, 1 );
        Mat warpRotateDst = new Mat();
        Imgproc.warpAffine( warpDst, warpRotateDst, rotMat, warpDst.size() );
        
        
        BufferedImage newImg = mat2Img(warpRotateDst,seq.getLastImage());
        return newImg;
//        HighGui.imshow( "Source image", src );
//        HighGui.imshow( "Warp", warpDst );
//        HighGui.imshow( "Warp + Rotate", warpRotateDst );
//        HighGui.waitKey(0);
//        System.exit(0);
    }

public double getAngle(Point[] pointArray) {
	double pointDistance = Math.sqrt(Math.pow(pointArray[1].x - pointArray[0].x,2) + Math.pow(pointArray[1].y + pointArray[0].y,2));
	int k = 1;
	for(int i = 2; i < pointArray.length; i++) {
		double pointDistance2 = Math.sqrt(Math.pow(pointArray[i].x - pointArray[0].x,2) + Math.pow(pointArray[i].y + pointArray[0].y,2));
		if (pointDistance2 < pointDistance) {pointDistance = pointDistance2; k = i;}
	}
	
	
	
	double angle = Math.toDegrees(Math.atan2(pointArray[k].y - pointArray[0].y, pointArray[k].x - pointArray[0].x));

    if(angle < 0){
        angle += 360;
    }

    return angle;
}
}
