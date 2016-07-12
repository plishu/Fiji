import ij.ImagePlus;
import ij.IJ;
import ij.gui.NewImage;
import ij.plugin.frame.RoiManager;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.process.ImageProcessor;
import ij.measure.CurveFitter;
import ij.gui.Plot;
import ij.gui.PlotWindow;

import java.lang.Math;
import java.util.Map;
import java.util.HashMap;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.Exception;

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

  public static String MAP_TARG1 = "TARGET1";
  public static String MAP_TARG2 = "TARGET2";
  public static String MAP_TARG3 = "TARGET3";



  public ImagePlus[] scaleImages(ImagePlus[] channels){
    ImagePlus[] scaledImages = new ImagePlus[3];
    String imageTitle = "";
    for( int i=0; i<channels.length; i++ ){

      if( i==0 ){ imageTitle = "red";}
      else if( i==1 ){ imageTitle = "green";}
      else if( i==2 ){ imageTitle = "blue";}

      scaledImages[i] = scaleImage(channels[i], imageTitle);
      //scaledImages[i].show();
    }
    return scaledImages;
  }

  public RGBPhoto scaleChannels( RGBPhoto inPhoto ){
    ImagePlus redChannel = inPhoto.getRedChannel();
    ImagePlus greenChannel = inPhoto.getGreenChannel();
    ImagePlus blueChannel = inPhoto.getBlueChannel();

    redChannel = scaleImage(redChannel, "red");
    greenChannel = scaleImage(greenChannel, "green");
    blueChannel = scaleImage(blueChannel, "blue");

    //redChannel.show();
    //greenChannel.show();
    //blueChannel.show();

    ImagePlus[] rgb = {redChannel, greenChannel, blueChannel};

    return ( new RGBPhoto(rgb, inPhoto.getCameraType(), inPhoto) );
  }


  public ImagePlus scaleImage(ImagePlus inImage, String imageName) {
      double inPixel = 0.0;
      double outPixel = 0.0;
      double minVal = inImage.getProcessor().getMin();
      double maxVal = inImage.getProcessor().getMax();

      //IJ.log("Pixel min: " + String.valueOf(minVal));
      //IJ.log("Pixel max: " + String.valueOf(maxVal));

      double inverseRange = 1.0 / (maxVal - minVal);


      ImagePlus newImage = NewImage.createFloatImage((String)imageName, (int)inImage.getWidth(), (int)inImage.getHeight(), (int)1, (int)1);
      int y = 0;
      int x = 0;
      while (y < inImage.getHeight()) {
          x = 0;
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

  public ImagePlus removeGamma(ImagePlus channel, double gamma){
    //IJ.log("Gamma to apply: " + String.valueOf(gamma));
    double undoGamma = 1.0 / gamma;
    double cPixel = 0.0;
    int x2 = 0;
    int y2 = 0;

    ImagePlus nChannel = channel;

    while (y2 < nChannel.getHeight()) {
        x2 = 0; // Reset for new row
        while (x2 < nChannel.getWidth()) {
            //IJ.log( "Old pixel value: " + String.valueOf(nChannel.getProcessor().getPixel(x2,y2)));
            cPixel = Math.pow(nChannel.getProcessor().getPixelValue(x2, y2), undoGamma);
            //IJ.log( "New pixel value: " + String.valueOf(cPixel) );
            nChannel.getProcessor().putPixelValue(x2, y2, cPixel);
            ++x2;
        }
        ++y2;
    }

    //nChannel.show();
    return nChannel;
  }

  public RGBPhoto removeGamma(RGBPhoto inImage, double gamma){
    ImagePlus redChannel = inImage.getRedChannel();
    ImagePlus greenChannel = inImage.getGreenChannel();
    ImagePlus blueChannel = inImage.getBlueChannel();


    redChannel = removeGamma(redChannel, gamma);
    greenChannel = removeGamma(greenChannel, gamma);
    blueChannel = removeGamma(blueChannel, gamma);




    ImagePlus[] rgb = {redChannel, greenChannel, blueChannel};
    return (new RGBPhoto(rgb, inImage.getCameraType(), inImage) );

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


  public double[] calculateCalibrationCoefficients(double[] calcValues, double[] refValues){
    double[] regressionParams = null;
    double r_Squared = 0.0;

    for( int i=0; i<calcValues.length; i++ ){
      calcValues[i] = (double)calcValues[i]/100.0;
    }

    CurveFitter visRegression = new CurveFitter(calcValues, refValues);
    visRegression.doFit(0, true);
    regressionParams = visRegression.getParams();
    r_Squared = visRegression.getRSquared();

    double intercept = (double)regressionParams[0];
    double slope = (double)regressionParams[1];

    IJ.log((String)("intercept: " + IJ.d2s((double)regressionParams[0], (int)8)));
    IJ.log((String)("slope: " + IJ.d2s((double)regressionParams[1], (int)8)));

    PlotWindow.noGridLines = false;
    Plot visPlot = new Plot("Visible band regression", "Image values", "Reflectance values");
    visPlot.setLimits(10.0/100f, 30.0/100f, 0.0, 1.0);
    visPlot.setColor(Color.RED);
    visPlot.addPoints(calcValues, refValues, 0);
    visPlot.draw();
    double[] xVis = new double[]{0.0, 1.0};
    double[] yVis = new double[]{regressionParams[0], regressionParams[1] + regressionParams[0]};
    visPlot.addPoints(xVis, yVis, 2);
    visPlot.addLabel(0.05, 0.1, "R squared = " + Double.toString(r_Squared));
    visPlot.show();

    //HashMap<String, double[]> values = new HashMap<String, double[]>();
    double[] values = {intercept, slope};

    return values;
  }


  public ImagePlus createIndexImage(){
    return null;
  }

  public double[][] getRefValues(File directory, String filter){
    String fullLine = "";
    BufferedReader fileReader = null;
    try{
      fileReader = new BufferedReader( new FileReader(directory.getPath()) );
    }catch( Exception e ){
      e.printStackTrace();
    }
    String line = "";
    int numLines = 0;

    double[] t1 = new double[3];
    double[] t2 = new double[3];
    double[] t3 = new double[3];

    String[] split = null;

    //HashMap<String, String> value = null;
    //List<HashMap<String, String>> values = null;
    double[][] values = new double[3][3];
    try{
      while ((line = fileReader.readLine()) != null) {
          if (line.length() <= 0) continue;
          ++numLines;

          split = line.split(",");


          if( split.length <= 0 ) continue;

          if( split.length == 10 && split[0].equals(filter) ){

            for( int i=0; i<split.length; i++){
              //IJ.log( String.valueOf(split[i]) );
            }


            values[0][0] = Double.parseDouble(split[1])/100.0;
            values[0][1] = Double.parseDouble(split[2])/100.0;
            values[0][2] = Double.parseDouble(split[3])/100.0;

            // Target 2 RGB
            values[1][0] = Double.parseDouble(split[4])/100.0;
            values[1][1] = Double.parseDouble(split[5])/100.0;
            values[1][2] = Double.parseDouble(split[6])/100.0;


            // Target 3 RGB
            values[2][0] = Double.parseDouble(split[7])/100.0;
            values[2][1] = Double.parseDouble(split[8])/100.0;
            values[2][2] = Double.parseDouble(split[9])/100.0;

            break;
          }
      }
    }catch( Exception e ){
      e.printStackTrace();
    }

    return values;
  }

  public RGBPhoto makeSingle(RGBPhoto photo, double[] calibrationCeofs){
    ImagePlus img = photo.getImage();
    ImagePlus rimg = photo.getRedChannel();
    ImagePlus gimg = photo.getGreenChannel();
    ImagePlus bimg = photo.getBlueChannel();

    //rimg.show();
    //gimg.show();
    //bimg.show();

    //RGBPhoto nphoto = null;

    double pixel = 0.0;
    double outPixel = 0.0;
    int x = 0;
    int y = 0;
    while (y < img.getHeight()) {
        x = 0;
        while (x < img.getWidth()) {

            if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_RED) ){
              pixel = (double)rimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];
              rimg.getProcessor().putPixelValue(x, y, pixel);
            }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_GREEN) ){
              pixel = (double)gimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];
              gimg.getProcessor().putPixelValue(x, y, pixel);
            }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_BLUE) ){
              pixel = (double)bimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];
              bimg.getProcessor().putPixelValue(x, y, pixel);
            }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_NIR) ){
              pixel = (double)rimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];
              rimg.getProcessor().putPixelValue(x, y, pixel);
            }
            x++;
        }
        y++;
    }

    ImagePlus[] nchan = {rimg, gimg, bimg};

    RGBPhoto nphoto = new RGBPhoto(nchan, photo.getCameraType(), photo);
    //nphoto.show();

    return nphoto;
  }

  public RGBPhoto makeNDVI(RGBPhoto photo, double[] calibrationCeofs) {
      ImagePlus img = photo.getImage();
      ImagePlus rimg = photo.getRedChannel();
      ImagePlus gimg = photo.getGreenChannel();
      ImagePlus bimg = photo.getBlueChannel();

      //img.show();
      //rimg.show();
      //gimg.show();
      //bimg.show();

      double redPixel = 0.0;
      double bluePixel = 0.0;
      int x = 0;
      int y = 0;
      while (y < img.getHeight()) {
          x = 0;
          while (x < img.getWidth()) {
              redPixel = (double)rimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];
              bluePixel = (double)bimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[3] + calibrationCeofs[2];
              rimg.getProcessor().putPixelValue(x, y, redPixel);
              bimg.getProcessor().putPixelValue(x, y, bluePixel);

              x++;
          }
          y++;
      }

      ImagePlus[] nchan = {rimg, gimg, bimg};
      RGBPhoto nphoto = new RGBPhoto(nchan, CalibrationPrompt.SURVEY2_NDVI, photo);

      return nphoto;
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
      //IJ.log("No area selection");
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
    IJ.log((String)("Pixel count: " + count));
    IJ.log((String)("Pixel sum: " + sum));
    IJ.log((String)("Pixel mean value: " + IJ.d2s((double)(sum / (double)count), (int)4)));
    mean = (double)(sum/(double)count);

    values = new HashMap<String, String>();
    values.put(MAP_COUNT, String.valueOf(count) );
    values.put(MAP_SUM, String.valueOf(sum) );
    values.put(MAP_MEAN, String.valueOf(mean) );


    channel.show();
    channel.getProcessor().drawRoi(roi);
    channel.updateAndDraw();

    return values;
  }

  public List<HashMap<String, String>> processRois(ImagePlus channel, RoiManager roiManager){

    if( roiManager == null ){
      IJ.log("Roi manager was not created correctly");
      return null;
    }
    //channel.show();
    //roiManager.selectAndMakeVisible(channel, 1);
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

    if( resimg == null ){
      return null;
    }

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

    target1Roi = qr.mapRoiTo( qrimg, resimg, target1Center, 0.5f );
    qr.drawPolygonOn( target1Roi, qrimg );
    target2Roi = qr.mapRoiTo( qrimg, resimg, target2Center, 0.5f );
    qr.drawPolygonOn( target2Roi, qrimg );
    target3Roi = qr.mapRoiTo( qrimg, resimg, target3Center, 0.5f );
    qr.drawPolygonOn( target3Roi, qrimg );

    Roi[] rois = {target1Roi, target2Roi, target3Roi};

    //Roi[] rois = {target3Roi, target2Roi, target1Roi}; // @TODO TESTING,

    return rois;
  }



}
