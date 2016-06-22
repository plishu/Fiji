import ij.ImagePlus;
import ij.IJ;
import ij.gui.NewImage;
import ij.plugin.frame.RoiManager;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.process.ImageProcessor;

import java.lang.Math;
import java.util.Map;
import java.util.HashMap;
import java.awt.Rectangle;
import java.util.List;
import java.util.ArrayList;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.ResultPoint;

/*
 * Reusable calibrator class. Houses operations to perform calibration.
 */
public class Calibrator{

  public static String MAP_COUNT = "COUNT";
  public static String MAP_SUM = "SUM";
  public static String MAP_MEAN = "MEAN";



  public ImagePlus[] scaleImages(ImagePlus[] channels){
    ImagePlus[] scaledImages = new ImagePlus[3];
    String imageTitle = "";
    for( int i=0; i<channels.length; i++ ){

      if( i==0 ){ imageTitle = "red";}
      else if( i==1 ){ imageTitle = "green";}
      else if( i==2 ){ imageTitle = "blue";}

      scaledImages[i] = scaleImage(channels[i], imageTitle);
      scaledImages[i].show();
    }
    return scaledImages;
  }


  public ImagePlus scaleImage(ImagePlus inImage, String imageName) {
      double inPixel = 0.0;
      double outPixel = 0.0;
      double minVal = inImage.getProcessor().getMin();
      double maxVal = inImage.getProcessor().getMax();
      double inverseRange = 1.0 / (maxVal - minVal);
      ImagePlus newImage = NewImage.createFloatImage((String)imageName, (int)inImage.getWidth(), (int)inImage.getHeight(), (int)1, (int)1);
      int y = 0;
      while (y < inImage.getHeight()) {
          int x = 0;
          while (x < inImage.getWidth()) {
              inPixel = inImage.getPixel(x, y)[0];
              outPixel = inverseRange * (inPixel - minVal);
              newImage.getProcessor().putPixelValue(x, y, outPixel);
              ++x;
          }
          ++y;
      }
      return newImage;
  }

  /*
   * Use only for dualband NDVI
   * @param nirImage - Channel that contains the NIR capture
   * @param visImage - Channel that contains the R or G or B capture
   * @return none - Note that nirImage and visImage channel will have the
   * gamma removal applied
   */
  public void removeGamma(ImagePlus nirImage, ImagePlus visImage, double gamma){
    double undoGamma = 1.0 / gamma;
    double nirPixel = 0.0;
    double visPixel = 0.0;
    int x2 = 0;
    int y2 = 0;
    while (y2 < nirImage.getHeight()) {
        x2 = 0; // Reset for new row
        while (x2 < nirImage.getWidth()) {
            nirPixel = Math.pow(nirImage.getProcessor().getPixelValue(x2, y2), undoGamma);
            visPixel = Math.pow(visImage.getProcessor().getPixelValue(x2, y2), undoGamma);
            visImage.getProcessor().putPixelValue(x2, y2, visPixel);
            nirImage.getProcessor().putPixelValue(x2, y2, nirPixel);
            ++x2;
        }
        ++y2;
    }
  }

  public void subtractNIR(ImagePlus nirImage, ImagePlus visImage, double gamma, double nirsub){
    double percentToSubtract =  nirsub/100.0;
    double undoGamma = 1.0/gamma;
    double nirPixel = 0.0;
    double visPixel = 0.0;

    int y3 = 0;
    int x3 = 0;
    while (y3 < nirImage.getHeight()) {
        x3 = 0; // Reset for new row
        while (x3 < nirImage.getWidth()) {
            nirPixel = nirImage.getProcessor().getPixelValue(x3, y3);
            visPixel = (double)visImage.getProcessor().getPixelValue(x3, y3) - percentToSubtract * nirPixel;
            visImage.getProcessor().putPixelValue(x3, y3, visPixel);
            ++x3;
        }
        ++y3;
    }
  }


  public RoiManager setupROIManager(ImagePlus img, Roi[] rois){
    RoiManager manager = new RoiManager();

    for( int i=0; i<rois.length; i++){
      manager.add(img, rois[i], i);
    }

    return RoiManager.getInstance();
  }

  public HashMap<String, String> processRoi(ImagePlus channel, Roi roi){
    ImageProcessor ip = null;
    ImageProcessor mask = null;
    Rectangle r = null;

    double sum = 0.0;
    int count = 0;
    double mean = 0.0;

    int y = 0;
    int x = 0;

    HashMap<String, String> values = null;

    // Begin processing roi
    if( roi != null && !roi.isArea() ){
      roi = null;
      IJ.log("No area selection");
      return null;
    }

    ip = channel.getProcessor();
    mask = (roi != null) ? roi.getMask() : null;
    r = (roi != null) ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
    while (y < r.height) {
        x = 0;
        while (x < r.width) {
            if (mask == null || mask.getPixel(x, y) != 0) {
                ++count;
                // Is this staying at 0?
                sum += (double)ip.getPixelValue(x + r.x, y + r.y);
            }
            ++x;
        }
        ++y;
    }
    IJ.log((String)("count: " + count));
    IJ.log((String)("sum: " + sum));
    IJ.log((String)("mean: " + IJ.d2s((double)(sum / (double)count), (int)4)));
    mean = (double)(sum/(double)count);

    values = new HashMap<String, String>();
    values.put(MAP_COUNT, String.valueOf(count) );
    values.put(MAP_SUM, String.valueOf(sum) );
    values.put(MAP_MEAN, String.valueOf(mean) );

    return values;
  }

  public List<HashMap<String, String>> processRois(ImagePlus channel, RoiManager roiManager){

    if( roiManager == null ){
      IJ.log("Roi manager was not created correctly");
      return null;
    }

    Roi[] rois = roiManager.getRoisAsArray();
    HashMap<String, String> value = null;
    //HashMap<String, String>[] values = new HashMap<String, String>[rois.length];
    List<HashMap<String, String>> values = new ArrayList<HashMap<String, String>>();

    for( int i=0; i<rois.length; i++ ){
      value = processRoi(channel, rois[i]);
      values.add(value);
    }

    return values;
  }


  public Roi[] getRois(ImagePlus qrimg ){
    QRCalib qr = new QRCalib();

    Result result = qr.attemptDecode(qrimg); // Blocking

    if( result == null ){
      IJ.log( "Could not find QR code. Skipping this image." );
      return null;
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

    target1Roi = qr.mapRoiTo( qrimg, resimg, target1Center, 0.6f );
    qr.drawPolygonOn( target1Roi, qrimg );
    target2Roi = qr.mapRoiTo( qrimg, resimg, target2Center, 0.6f );
    qr.drawPolygonOn( target2Roi, qrimg );
    target3Roi = qr.mapRoiTo( qrimg, resimg, target3Center, 0.6f );
    qr.drawPolygonOn( target3Roi, qrimg );

    Roi[] rois = {target1Roi, target2Roi, target3Roi};

    return rois;
  }



}
