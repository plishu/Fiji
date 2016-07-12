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

  public static final float SQ_TO_TARG = 2.1875f; // inches
  public static final float SQ_TO_SQ = 5.0f; // inches 4.9375f
  public static final float TARGET_LENGTH = 2f; // inches
  public static final float TARG_TO_TARG = 2.6f; // inches


  private float[] polyXCoords = null; // X points for polygon around QR blocks
  private float[] polyYCoords = null; // Y points for polygon around QR blocks
  private float qrBlockDistance = 0.0f; // How far apart qr blocks are for image. Calculated after qr decoded.
  private float qrToTargDistance = 0.0f; // How far apart center of top-left qr block is to center of 1st target. Calculated after qr decoded.
  private float targToTargDistance = 0.0f; // How far apart target centers are from each other. Calculated after qr decoded.
  private float angle = 0.0f; // Angle between horizontal and top-left to top-left qr block line. Should be in degrees.
  private float targetSize = 0.0f; // Size of target on image. It's the width measure, but the target is a square.

  private float[] target1Center = null;
  private float[] target2Center = null;
  private float[] target3Center = null;

  private float[] target1XCoords = null;
  private float[] target1YCoords = null;
  private float[] target2XCoords = null;
  private float[] target2YCoords = null;
  private float[] target3XCoords = null;
  private float[] target3YCoords = null;


  private Reader qrReader = null;

  private Attempt attempt = null;

  /*
   * Incompasses algorithms used to help decode QR code
   */
  public class Attempt{
    private ImagePlus manipImg = null; //  Modified image set by attempt algorithm

    /*
     * Acts as mux to select algorithm to use to decode QR code.
     * @param   qrimg   The image to detect QR code on
     * @return  Result object of the decoding processes. Returns null if no
     *          QR code detected after attempt to decode.
     */
    public Result runAttempt(ImagePlus qrimg){
      // Choose which method of qr detection to run
      return adaptiveResize(qrimg);
    }

    /*
     * Start at base scale, and resize the image by 100 more px everytime QR code
     * is not found for that scaled image. Attempts this 50 times. the algorithm
     * modifies manipImg.
     * @param  qrimg   Image to attempt to find and decode QR code.
     * @result Result object of the decoding process. Return null if no QR code
     *         detected after attempt to decode.
     */
    public Result adaptiveResize(ImagePlus qrimg){
      int baseResize = 200;
      int attempts = 60;
      int attempt = 1;
      ImagePlus resimg = resize(qrimg, baseResize);
      //resimg.show();

      Result result = decodeQR(qrimg);

      while( attempt <= attempts && result == null ){
        resimg.close();
        attempt += 1;
        baseResize += 100;

        resimg = resize(qrimg, baseResize);
        //resimg.show();
        result = decodeQR(resimg);
      }
      manipImg = resimg;
      return result;
    }

    public ImagePlus getImg(){
      return manipImg;
    }

  }

  // Anything to do that involves calibration using qr code
  public QRCalib(){
    qrReader = new QRCodeReader();
  }

  /*
   * Interface with Attempt object to run attempt algorithm
   * @param  qrimg   Image to attempt to find and decode QR code.
   * @result Result object of the decoding process. Return null if no QR code
   *         detected after attempt to decode.
   */
  public Result attemptDecode( ImagePlus qrimg ){
    attempt = new Attempt();
    return attempt.runAttempt(qrimg);
  }

  /*
   * Return manipImg from Attempt object
   * @param None
   * @return  manipImg
   */
  public ImagePlus getAttemptImg(){
    return attempt.getImg();
  }

  /*
   * Apply QR decoding on supplied image.
   * @param inImage   The image that contains a QR code to decodeQR
   * @return          Result object of the decoding process. Return null if no QR code
   *         detected after attempt to decode.
   */
  public Result decodeQR(ImagePlus inImg){
    //inImg.show();
    BufferedImage bfimg = inImg.getBufferedImage();
    LuminanceSource source = new BufferedImageLuminanceSource(bfimg);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    String resultstr = "";
    print("Detecting QR code. Please wait...");
    Result result = null;
    try{
      Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      result = qrReader.decode(bitmap, hints);

    } catch (NotFoundException nf){
      //print( "Could not find QR code.");
    } catch (ChecksumException cs){
      //print("Checksum Error on QR code.");
    } catch (FormatException fe){
      //print("Could not find QR code format.");
    }

    return result;
  }

  /*
   * Resize image by specified amount
   * @param inImg   Image to resize
   * @param amount  Amount to resize image by. This specifies the width
   *                that you want the resized image to have. Aspect ratio
   *                is preserved.
   * @return  The resized image
   */
  public ImagePlus resize(ImagePlus inImg, int amount){
    ImageProcessor ip = inImg.getProcessor();
    ip = ip.resize(amount);

    ImagePlus resimg = new ImagePlus("Resized", ip);
    return resimg;
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
      //IJ.log("Not enough points to create polygon. Required: 4");
      return null;
    }
  }

  public PolygonRoi createPolygon( ResultPoint[] points ){
    setQRBlockPoints(points);

    PolygonRoi polygon = null;
    if( polyXCoords.length == 4 && polyYCoords.length == 4 ){
      polygon = new PolygonRoi(polyXCoords, polyYCoords, Roi.POLYGON);
      polygon.setStrokeWidth(3);
      polygon.setStrokeColor(new Color(0,0,0));
      return polygon;
    }else{
      //IJ.log("Not enough points to create polygon. Required: 4");
      return null;
    }

  }

  public PolygonRoi createRectangleRoi( float[] pos, float resizepercent ){
    float[] targetXCoords = getTargetXCoords( pos, targetSize*targetSize*resizepercent );
    float[] targetYCoords = getTargetYCoords( pos, targetSize*targetSize*resizepercent );

    PolygonRoi targetRoi = createPolygon( targetXCoords, targetYCoords );

    return targetRoi;
  }

  public void drawPolygonOn( Roi roi, ImagePlus img ){
    img.getProcessor().drawRoi(roi);
    img.updateAndDraw();
    return;
  }

  public Roi mapRoiTo( ImagePlus targimg, ImagePlus refimg, float[] refcenter, float resizepercent ){
    float scalefactor = getScaleFactor( targimg, refimg );

    float[] targetCenter = convertCenter( refcenter, scalefactor );

    /*
    float[] targetXCoords = getTargetXCoords( targetCenter, scalefactor*targetSize*targetSize*resizepercent );
    float[] targetYCoords = getTargetYCoords( targetCenter, scalefactor*targetSize*targetSize*resizepercent );

    // Only creates square for now!!
    return createRectangleRoi(targetCenter, );
    */
    return createRectangleRoi(targetCenter, scalefactor*resizepercent);
  }

  public void setQRBlockPoints( ResultPoint[] points ){

    // Get x points
    float[] x = new float[points.length];
    for( int i=0; i<points.length; i++){
      x[i] = points[i].getX();
    }
    polyXCoords = x;

    // Get y points
    float[] y = new float[points.length];
    for( int i=0; i<points.length; i++ ){
      y[i] = points[i].getY();
    }
    polyYCoords = y;
  }

  public void setTargetsCenter( PolygonRoi qrRoi){
    setScaledSq_To_Sq();
    setScaledSq_To_Targ();
    setScaledTarg_To_Targ();
    setAngle(qrRoi);
    setTargetSize();

    target1Center = getCornerTargetCenter(polyXCoords[1], polyYCoords[1], qrToTargDistance, angle );
    target3Center = getCornerTargetCenter(polyXCoords[2], polyYCoords[2], qrToTargDistance, angle );
    //target2Center = getTargetCenterNextTo(target1Center, targToTargDistance , angle);
    target2Center = getMiddleTargetCenter(target1Center, target3Center, angle);
    //target3Center = getTargetCenterNextTo(target2Center, targToTargDistance, angle);

  }

  public float[] getPolyXCoords(){
    return polyXCoords;
  }

  public float[] getPolyYCoords(){
    return polyYCoords;
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

  public float[] getMiddleTargetCenter( float[] lefttarget, float[] righttarget, float angle ){
    float dx = righttarget[0] - lefttarget[0];
    float dy = righttarget[1] - lefttarget[1];
    float distance = (float)Math.sqrt(dx*dx + dy*dy)/2.0f;

    //IJ.log((String)("Angle: " + angle));

    float stdangle = (float)Math.toRadians(angle);
    float x = distance*(float)Math.cos(stdangle);
    float y = distance*(float)Math.sin(stdangle);

    // From left target
    float center[] = new float[2];
    center[0] = lefttarget[0] + x;
    center[1] = lefttarget[1] - y;

    return center;

  }

  public float[] getCornerTargetCenter( float xcoord, float ycoord, float distance, float angle ){
    float center[] = new float[2];
    // Note, angle is assumed in degrees. Must convert to radians for trig functions.
    float stdangle = (float)Math.toRadians(90+angle); // Make angle to standard form
    float dx = (float)( distance*Math.cos(stdangle) );
    float dy = (float)( distance*Math.sin(stdangle) );

    // QR center drift fix (drifts toward center; counteracted by shifting outward)
    float dfixangle = (float)Math.toRadians(180+angle);
    float sf = distance/SQ_TO_TARG;
    float dfixdistance = 0.05f*sf; // px
    float dfixex = dfixdistance*(float)Math.cos(dfixangle);
    float dfixy = dfixdistance*(float)Math.sin(dfixangle);
    float xc = xcoord + dfixex;
    float yc = ycoord - dfixy;


    center[0] = xc + dx;
    center[1] = yc - dy; // Up in direction is subtracting. Hint: (0,0) at top-left.

    //IJ.log( (String)("QR Center: " + xcoord + ", " + ycoord) );
    //IJ.log( (String)("Target Center: " + center[0] + ", " + center[1]) );

    return center;
  }

  public float[] getTarget1Center(){
    return target1Center;
  }

  public float[] getTarget2Center(){
    return target2Center;
  }

  public float[] getTarget3Center(){
    return target3Center;
  }

  public float getTargetSize(){
    return targetSize;
  }

  public void setTaget1Center(){
    float center[] = new float[2];
    float stdangle = (float)Math.toRadians(angle);
    float dx = (float)(qrToTargDistance*Math.cos(stdangle));
    float dy = (float)(qrToTargDistance*Math.sin(stdangle));

    center[0] = polyXCoords[1] + dx;
    center[1] = polyYCoords[1] + dy;

    target1Center = center;
    return;
  }

  public void setAngle( PolygonRoi qrRoi ){
    angle = (float)qrRoi.getAngle((int)polyXCoords[1], (int)polyYCoords[1], (int)polyXCoords[2], (int)polyYCoords[2]);
    return;
  }


  public float[] getTargetCenterNextTo( float[] neighbor, float distance, float angle ){
    float center[] = new float[2];

    float stdangle = (float)Math.toRadians(angle);
    float dx = (float)(distance*Math.cos(stdangle));
    float dy = (float)(distance*Math.sin(stdangle));

    center[0] = neighbor[0] + dx;
    center[1] = neighbor[1] - dy;

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

  public float[] convertCenter( float[] center, float scalefactor ){
    float[] ncenter = new float[2];

    float nx = scalefactor*center[0];
    float ny = scalefactor*center[1];

    ncenter[0] = nx;
    ncenter[1] = ny;

    return ncenter;

  }

  public float getScaleFactor( ImagePlus oimg, ImagePlus simg ){
    float scalefactor = ( (float)(oimg.getWidth())/(float)(simg.getWidth()) );
    //float scalefactor = SQ_TO_TARG/qrToTargDistance;
    //IJ.log( "Scale factor: " + scalefactor );
    return scalefactor;
  }


  public float getScaledSq_To_Sq(float x1, float y1, float x2, float y2){
    //float deltax = Math.abs(pos2[0]-pos1[0]);
    //float deltay = Math.abs(pos2[1]-pos1[1]);
    float deltax = (float)Math.abs(x2 - x1);
    float deltay = (float)Math.abs(y2 - y1);

    float sq_to_sq = (float)Math.sqrt(deltax*deltax + deltay*deltay);
    return sq_to_sq;
  }

  public void setScaledSq_To_Sq(){
    float x1 = polyXCoords[1];
    float x2 = polyXCoords[0];
    float y1 = polyYCoords[1];
    float y2 = polyYCoords[0];

    float deltax = (float)Math.abs(x2 - x1);
    float deltay = (float)Math.abs(y2 - y1);

    float sq_to_sq = (float)Math.sqrt(deltax*deltax + deltay*deltay);
    qrBlockDistance = sq_to_sq;

    return;
  }

  public float getScaledSq_To_Targ(float scaled_sq_to_sq){
    float sq_to_targ = (SQ_TO_TARG/SQ_TO_SQ)*scaled_sq_to_sq;
    return sq_to_targ;
  }

  public void setScaledSq_To_Targ(){
    float sq_to_targ = (SQ_TO_TARG/SQ_TO_SQ)*qrBlockDistance;
    qrToTargDistance = sq_to_targ;
    return;
  }

  public float getTargetSize(float scaled_sq_to_sq){
    float length = (TARGET_LENGTH/SQ_TO_SQ)*scaled_sq_to_sq;
    return length;
  }

  public void setTargetSize(){
    float length = (TARGET_LENGTH/SQ_TO_SQ)*qrBlockDistance;
    targetSize = length;
    return;
  }

  public float getScaledTarg_To_Targ(float scaled_sq_to_sq){
    float length = (TARG_TO_TARG/SQ_TO_SQ)*scaled_sq_to_sq;
    return length;
  }

  public void setScaledTarg_To_Targ(){
    float length = (TARG_TO_TARG/SQ_TO_SQ)*qrBlockDistance;
    targToTargDistance = length;
    return;
  }

}
