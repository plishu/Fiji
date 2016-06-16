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
import java.lang.Math;
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

public class QRCalib{

  private Reader qrReader = null;

  // Anything to do that involves calibration using qr code
  public QRCalib(){
    qrReader = new QRCodeReader();
  }

  public Result decodeQR(ImagePlus inImg){
    BufferedImage bfimg = inImg.getBufferedImage();
    LuminanceSource source = new BufferedImageLuminanceSource(bfimg);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    String resultstr = "";
    print("Decoding...");
    Result result = null;
    try{
      Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      result = qrReader.decode(bitmap, hints);

    } catch (NotFoundException nf){
      print( "Could not find QR code.");
    } catch (ChecksumException cs){
      print("Checksum Error.");
    } catch (FormatException fe){
      print("Could not find QR format.");
    }

    return result;
  }

  public ImagePlus resize(ImagePlus inImg){
    return null;
  }

  public void print(String str){
    // @TODO Make it that it is not limited to IJ.log, but any output stream
    // (command prompt, log file, etc)
    IJ.log(str);
  }

  public PolygonRoi createPolygon( float[] xcoords, float[] ycoords ){
    PolygonRoi polygon = null;
    if( xcoords.length == 4 && ycoords.length == 4 ){
      polygon = new PolygonRoi(xcoords, ycoords, Roi.POLYGON);
      polygon.setStrokeWidth(3);
      polygon.setStrokeColor(new Color(0,0,0));
      return polygon;
    }else{
      IJ.log("Not enough points to create polygon. Required: 4");
      return null;
    }
  }

  public void drawPolygonOn( PolygonRoi roi, ImagePlus img ){
    img.getProcessor().drawRoi(roi);
    img.updateAndDraw();
    return;
  }

  /*
   * Transform QR point 2 to the target directly above it. This will return
   * coordinates to the center of the target.
   * @param xcoord    The x coordinate of QR point 2
   * @param ycoord    The y coordinate of QR point 2
   * @return          y and y coordinates to center of target
   */
  public float[] getTarget1Center( float xcoord, float ycoord, float distance, float angle ){
    float center[] = new float[2];
    // Note, angle is assumed in degrees. Must convert to radians for trig functions.
    float stdangle = (float)Math.toRadians(90+angle); // Make angle to standard form
    float dx = (float)( distance*Math.cos(stdangle) );
    float dy = (float)( distance*Math.sin(stdangle) );


    center[0] = xcoord + dx;
    center[1] = ycoord - dy; // Up in direction is subtracting. Hint: (0,0) at top-left.

    return center;
  }

  public float[] getTargetXCoords( float[] targetCenter, float size ){
    /*...................................
     *.....(x1,y1)----------(x2,y2)......
     *............|........|.............
     *............|........|.............
     *............|........|.............
     *.....(x3,y3)----------(x4,y4)......
     *...................................
     */
     float[] xcoords = new float[4];
     xcoords[0] = (float)(targetCenter[0] - 0.5*Math.sqrt(size));
     xcoords[3] = xcoords[0];

     xcoords[1] = (float)(targetCenter[0] + 0.5*Math.sqrt(size));
     xcoords[2] = xcoords[1];

     /*
     for(int i=0; i<xcoords.length; i++){
       IJ.log(Float.toString(xcoords[i]));
     }*/
    return xcoords;
  }

  public float[] getTargetYCoords( float[] targetCenter, float size ){
    /*...................................
     *.....(x1,y1)----------(x2,y2)......
     *............|........|.............
     *............|........|.............
     *............|........|.............
     *.....(x3,y3)----------(x4,y4)......
     *...................................
     */

     // Dumb square fix; p3 and p4 are flipped
     float[] ycoords = new float[4];
     ycoords[0] = (float)(targetCenter[1] - 0.5*Math.sqrt(size));
     ycoords[1] = ycoords[0];

     // Dumb square fix; p3 and p4 are flipped
     ycoords[3] = (float)(targetCenter[1] + 0.5*Math.sqrt(size));
     ycoords[2] = ycoords[3];

     /*
     for(int i=0; i<ycoords.length; i++){
       IJ.log(Float.toString(ycoords[i]));
     }*/
    return ycoords;
  }



}
