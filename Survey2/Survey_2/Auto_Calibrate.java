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

    Result result = qr.decodeQR(qrimg);
    if( result == null ){
      IJ.log("Could not find QR code. Skiping this image.");
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
    qr.drawPolygonOn(qrRoi, qrimg);


    float[] target1XCoords = null;
    float[] target1YCoords = null;

    // Prepare samplXCoords & sampleYCoords
    float ls = (float)21.0;
    float angle = (float)qrRoi.getAngle((int)polyXCoords[1], (int)polyYCoords[1], (int)polyXCoords[2], (int)polyYCoords[2]);
    IJ.log("Angle: " + Float.toString(angle));
    float[] target1Center = qr.getTarget1Center(polyXCoords[1], polyYCoords[1], ls, angle );
    IJ.log( "Target center: (" + Float.toString(target1Center[0]) + "," + Float.toString(target1Center[1]) + ")" );



    target1XCoords = qr.getTargetXCoords(target1Center, (float)100);
    target1YCoords = qr.getTargetYCoords(target1Center, (float)200);

    IJ.log("X1: " + Float.toString(target1XCoords[0]) );
    IJ.log("Y1: " + Float.toString(target1YCoords[0]) );
    IJ.log("X2: " + Float.toString(target1XCoords[1]) );
    IJ.log("Y2: " + Float.toString(target1YCoords[1]) );
    IJ.log("X3: " + Float.toString(target1XCoords[2]) );
    IJ.log("Y3: " + Float.toString(target1YCoords[2]) );
    IJ.log("X4: " + Float.toString(target1XCoords[3]) );
    IJ.log("Y4: " + Float.toString(target1YCoords[3]) );



    PolygonRoi target1Roi = qr.createPolygon( target1XCoords, target1YCoords );
    qr.drawPolygonOn(target1Roi, qrimg);


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
