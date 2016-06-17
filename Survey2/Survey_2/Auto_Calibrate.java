import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;

import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.awt.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.lang.Runtime;
import java.lang.ProcessBuilder;
import java.lang.Process;
import ij.io.*;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.ResultPoint;

public class Auto_Calibrate implements PlugIn{

  public void run(String args){

    String imageDir = IJ.getFilePath("Input QR Code image");
    ImagePlus qrimg = IJ.openImage(imageDir);
    qrimg.show();

    QRCalib qr = new QRCalib();

    Result result = qr.attemptDecode(qrimg); // Blocking

    if( result == null ){
      IJ.log( "Could not find QR code. Skipping this image." );
      return;
    }

    ImagePlus resimg = qr.getAttemptImg(); // Needed for processing

    ResultPoint[] points = result.getResultPoints();

    //PolygonRoi qrRoi = qr.createPolygon( polyXCoords, polyYCoords );
    PolygonRoi qrRoi = qr.createPolygon( points );
    qr.drawPolygonOn(qrRoi, resimg);
    qr.setTargetsCenter(qrRoi);

    float[] target1Center = qr.getTarget1Center();
    float[] target2Center = qr.getTarget2Center();
    float[] target3Center = qr.getTarget3Center();
    float targetLength = qr.getTargetSize();

    Roi target1Roi = qr.createRectangleRoi( target1Center, 0.3f );
    qr.drawPolygonOn( target1Roi, resimg );
    Roi target2Roi = qr.createRectangleRoi( target2Center, 0.3f );
    qr.drawPolygonOn( target2Roi, resimg );
    Roi target3Roi = qr.createRectangleRoi( target3Center, 0.3f );
    qr.drawPolygonOn( target3Roi, resimg );

    target1Roi = qr.mapRoiTo( qrimg, resimg, target1Center, 0.9f );
    qr.drawPolygonOn( target1Roi, qrimg );
    target2Roi = qr.mapRoiTo( qrimg, resimg, target2Center, 0.9f );
    qr.drawPolygonOn( target2Roi, qrimg );
    target3Roi = qr.mapRoiTo( qrimg, resimg, target3Center, 0.9f );
    qr.drawPolygonOn( target3Roi, qrimg );



  }

  /*
   * Returns x coordinates of the result point from the qr Code
   * @param: points   ResultPoint obtained from qr decode result
   * @return: x coordinates of result points
   */
  public float[] getXResultPoints(ResultPoint[] points){
    float[] x = new float[points.length];
    for( int i=0; i<points.length; i++){
      x[i] = points[i].getX();
    }
    return x;
  }

  /*
   * Returns y coordinates of the result point from the qr Code
   * @param: points   ResultPoint obtained from qr decode result
   * @return: y coordinates of result points
   */
  public float[] getYResultPoints(ResultPoint[] points){
    float[] y = new float[points.length];
    for( int i=0; i<points.length; i++ ){
      y[i] = points[i].getY();
    }
    return y;
  }

  public PolygonRoi createPolygon(float[] xcoords, float[] ycoords){
    PolygonRoi polygon = null;
    if( xcoords.length == 4 && ycoords.length == 4 ){
      polygon = new PolygonRoi(xcoords, ycoords, Roi.POLYGON);
      polygon.setStrokeWidth(10);
      polygon.setStrokeColor(new Color(0,0,0));
      return polygon;
    }else{
      IJ.log("Not enough points to create polygon. Required: 4");
      return null;
    }

  }
}
