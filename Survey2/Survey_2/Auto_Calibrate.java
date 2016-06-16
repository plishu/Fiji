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

    // I think variable resize will be needed
    int baseResize = 600;
    int attempts = 50;
    int attempt = 1;
    ImagePlus resimg = qr.resize(qrimg, baseResize); // 900 good
    resimg.show();

    Result result = qr.decodeQR(resimg);
    /*
    if( result == null ){

      IJ.log("Could not find QR code. Skiping this image.");
      return;
    }*/
    while( attempt <= attempts && result == null ){
      resimg.close();
      attempt += 1;
      baseResize += 100;

      resimg = qr.resize(qrimg, baseResize);
      resimg.show();
      result = qr.decodeQR(resimg);
    }

    if( result == null ){
      IJ.log( "Could not find QR code. Skipping this image." );
      return;
    }

    ResultPoint[] points = result.getResultPoints();



    // Draw polygon around qrcode
    float[] polyXCoords = getXResultPoints(points);
    float[] polyYCoords = getYResultPoints(points);

    IJ.log("X1: " + Float.toString(polyXCoords[0]) );
    IJ.log("Y1: " + Float.toString(polyYCoords[0]) );
    IJ.log("X2: " + Float.toString(polyXCoords[1]) );
    IJ.log("Y2: " + Float.toString(polyYCoords[1]) );
    IJ.log("X3: " + Float.toString(polyXCoords[2]) );
    IJ.log("Y3: " + Float.toString(polyYCoords[2]) );
    IJ.log("X4: " + Float.toString(polyXCoords[3]) );
    IJ.log("Y4: " + Float.toString(polyYCoords[3]) );

    PolygonRoi qrRoi = qr.createPolygon( polyXCoords, polyYCoords );
    qr.drawPolygonOn(qrRoi, resimg);


    float[] target1XCoords = null;
    float[] target1YCoords = null;
    float[] target2XCoords = null;
    float[] target2YCoords = null;
    float[] target3XCoords = null;
    float[] target3YCoords = null;

    // Prepare samplXCoords & sampleYCoords
    // How apart qr blocks are on this image
    float qrlength = qr.getScaledSq_To_Sq( polyXCoords[1], polyYCoords[1], polyXCoords[0], polyYCoords[0] );
    IJ.log( "QR Length: " + Float.toString(qrlength) );
    // Length between center of qr block and center of target
    float ls = qr.getScaledSq_To_Targ( qrlength );
    IJ.log( "Length: " + Float.toString(ls) );
    // Length between target centers for this image
    float targls = qr.getScaledTarg_To_Targ( qrlength );
    IJ.log( "Target lengh apart: " + targls);
    // Angle between horizontal line and top-left and top-right qr block line
    float angle = (float)qrRoi.getAngle((int)polyXCoords[1], (int)polyYCoords[1], (int)polyXCoords[2], (int)polyYCoords[2]);
    IJ.log("Angle: " + Float.toString(angle));
    // Coordinates to center of target
    float[] target1Center = qr.getTarget1Center(polyXCoords[1], polyYCoords[1], ls, angle );
    IJ.log( "Target center: (" + Float.toString(target1Center[0]) + "," + Float.toString(target1Center[1]) + ")" );
    float[] target2Center = qr.getTargetCenterNextTo(target1Center, targls , angle);
    float[] target3Center = qr.getTargetCenterNextTo(target2Center, targls, angle);
    // Get size of target (length, but it's a square)
    float targetLength = qr.getTargetSize(qrlength);
    IJ.log( "Target length: " + targetLength );


    // Create the three ROI's
    // @TODO I think this should be abstracted
    target1XCoords = qr.getTargetXCoords(target1Center, targetLength*targetLength*0.3f);
    target1YCoords = qr.getTargetYCoords(target1Center, targetLength*targetLength*0.3f);
    target2XCoords = qr.getTargetXCoords(target2Center, targetLength*targetLength*0.3f);
    target2YCoords = qr.getTargetYCoords(target2Center, targetLength*targetLength*0.3f);
    target3XCoords = qr.getTargetXCoords(target3Center, targetLength*targetLength*0.3f);
    target3YCoords = qr.getTargetYCoords(target3Center, targetLength*targetLength*0.3f);

    IJ.log("X1: " + Float.toString(target1XCoords[0]) );
    IJ.log("Y1: " + Float.toString(target1YCoords[0]) );
    IJ.log("X2: " + Float.toString(target1XCoords[1]) );
    IJ.log("Y2: " + Float.toString(target1YCoords[1]) );
    IJ.log("X3: " + Float.toString(target1XCoords[2]) );
    IJ.log("Y3: " + Float.toString(target1YCoords[2]) );
    IJ.log("X4: " + Float.toString(target1XCoords[3]) );
    IJ.log("Y4: " + Float.toString(target1YCoords[3]) );

    // Create the three ROI's

    PolygonRoi target1Roi = qr.createPolygon( target1XCoords, target1YCoords );
    qr.drawPolygonOn(target1Roi, resimg);
    PolygonRoi target2Roi = qr.createPolygon( target2XCoords, target2YCoords );
    qr.drawPolygonOn(target2Roi, resimg);
    PolygonRoi target3Roi = qr.createPolygon( target3XCoords, target3YCoords );
    qr.drawPolygonOn(target3Roi, resimg);


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
