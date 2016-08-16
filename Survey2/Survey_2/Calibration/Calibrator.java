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

  private Debugger debugger = Debugger.getInstance(false);
  private boolean ReflectanceImageOnly = false;




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

  public void subtractNIR(ImagePlus nirImage, ImagePlus visImage, double nirsub){
    double percentToSubtract =  nirsub/100.0;
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

  public RGBPhoto subtractNIR(RGBPhoto photo, double percent){
      ImagePlus img = photo.getImage();
      ImagePlus rimg = photo.getRedChannel();
      ImagePlus gimg = photo.getGreenChannel();
      ImagePlus bimg = photo.getBlueChannel();

      double redPixel = 0.0;
      double bluePixel = 0.0;
      double greenPixel = 0.0;


      int x = 0;
      int y = 0;
      while (y < img.getHeight()) {
          x = 0;
          while (x < img.getWidth()) {
              // NIR remove
              redPixel = (double)(rimg.getProcessor().getPixelValue(x, y) - percent*bimg.getProcessor().getPixelValue(x, y));
              //IJ.log((String)"NIR removed: " + redPixel);

              rimg.getProcessor().putPixelValue(x, y, (int)(redPixel));
              x++;
          }
          y++;
      }

      ImagePlus[] nchan = {rimg, gimg, bimg};
      RGBPhoto nphoto = new RGBPhoto(nchan, CalibrationPrompt.SURVEY2_NDVI, photo);

      return nphoto;
  }


  public double[] calculateCalibrationCoefficients(double[] calcValues, double[] refValues){
    double[] regressionParams = null;
    double r_Squared = 0.0;


    CurveFitter visRegression = new CurveFitter(calcValues, refValues);
    visRegression.doFit(0, true);
    regressionParams = visRegression.getParams();
    r_Squared = visRegression.getRSquared();

    double intercept = (double)regressionParams[0];
    double slope = (double)regressionParams[1];

    //IJ.log((String)("intercept: " + IJ.d2s((double)regressionParams[0], (int)8)));
    //IJ.log((String)("slope: " + IJ.d2s((double)regressionParams[1], (int)8)));

    PlotWindow.noGridLines = false;
    Plot visPlot = new Plot("Visible band regression", "Image values", "Reflectance values");
    //visPlot.setLimits(10.0/100f, 30.0/100f, 0.0, 1.0);
    visPlot.setLimits(0.0, 255.0, 0.0, 1.0);
    visPlot.setColor(Color.RED);
    visPlot.addPoints(calcValues, refValues, 0);
    visPlot.draw();
    double[] xVis = new double[]{0.0, 1.0};
    double[] yVis = new double[]{regressionParams[0], regressionParams[1] + regressionParams[0]};
    visPlot.addPoints(xVis, yVis, 2);
    visPlot.addLabel(0.05, 0.1, "R squared = " + Double.toString(r_Squared));

    if( Debugger.DEBUGMODE ){
        for( int i=0; i<calcValues.length; i++ ){
          //calcValues[i] = (double)calcValues[i]/100.0;
          IJ.log( (String)"Target " + (i+1) + " Mean: " + calcValues[i]);
        }
        IJ.log((String)("intercept: " + IJ.d2s((double)regressionParams[0], (int)8)));
        IJ.log((String)("slope: " + IJ.d2s((double)regressionParams[1], (int)8)));
        visPlot.show();
    }

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

    ImagePlus newRedImage = NewImage.createFloatImage((String)"redImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
    ImagePlus newBlueImage = NewImage.createFloatImage((String)"blueImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
    ImagePlus newGreenImage = NewImage.createFloatImage((String)"greenImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);

    double redReflectMax = (double)rimg.getDisplayRangeMax() * calibrationCeofs[1] + calibrationCeofs[0];
    double redReflectMin = (double)rimg.getDisplayRangeMin() * calibrationCeofs[1] + calibrationCeofs[0];
    double greenReflectMax = (double)gimg.getDisplayRangeMax() * calibrationCeofs[1] + calibrationCeofs[0];
    double greenReflectMin = (double)gimg.getDisplayRangeMin() * calibrationCeofs[1] + calibrationCeofs[0];
    double blueReflectMax = (double)bimg.getDisplayRangeMax() * calibrationCeofs[1] + calibrationCeofs[0];
    double blueReflectMin = (double)bimg.getDisplayRangeMin() * calibrationCeofs[1] + calibrationCeofs[0];

    double pixel = 0.0;
    double reflect = 0.0;
    double outPixel = 0.0;
    int x = 0;
    int y = 0;

    //IJ.log( (String)"Slope:" + calibrationCeofs[1] );
    //IJ.log( (String)"Intecept: " + calibrationCeofs[0] );

    while (y < img.getHeight()) {
        x = 0;
        while (x < img.getWidth()) {

            if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_RED) ){
              pixel = (double)rimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];

              if( !ReflectanceImageOnly ){

                pixel = (int)( (pixel - redReflectMin)/(redReflectMax-redReflectMin)*255 );
                // histogram stretch map to float-point THIS ONE WORKS!!
                pixel = (double)(pixel - 0)/(255-0)*1;

              }

              newRedImage.getProcessor().putPixelValue(x, y, pixel);
              newBlueImage.getProcessor().putPixelValue(x, y, 0.0);
              newGreenImage.getProcessor().putPixelValue(x, y, 0.0);
            }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_GREEN) ){
              pixel = (double)gimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];

              if( !ReflectanceImageOnly ){

                pixel = (int)( (pixel - greenReflectMin)/(greenReflectMax-greenReflectMin)*255 );
                // histogram stretch map to float-point THIS ONE WORKS!!
                pixel = (double)(pixel - 0)/(255-0)*1;

              }

              newRedImage.getProcessor().putPixelValue(x, y, 0.0);
              newGreenImage.getProcessor().putPixelValue(x, y, pixel);
              newBlueImage.getProcessor().putPixelValue(x, y, 0.0);
            }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_BLUE) ){
              pixel = (double)bimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];

              if( !ReflectanceImageOnly ){

                pixel = (int)( (pixel - blueReflectMin)/(blueReflectMax-blueReflectMin)*255 );
                // histogram stretch map to float-point THIS ONE WORKS!!
                pixel = (double)(pixel - 0)/(255-0)*1;

              }

              newRedImage.getProcessor().putPixelValue(x, y, 0.0);
              newGreenImage.getProcessor().putPixelValue(x, y, 0.0);
              newBlueImage.getProcessor().putPixelValue(x, y, pixel);
            }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_NIR) ){
              pixel = (double)rimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];

              if( !ReflectanceImageOnly ){

                pixel = (int)( (pixel - redReflectMin)/(redReflectMax-redReflectMin)*255 );
                // histogram stretch map to float-point THIS ONE WORKS!!
                pixel = (double)(pixel - 0)/(255-0)*1;

              }

              newRedImage.getProcessor().putPixelValue(x, y, pixel);
              newBlueImage.getProcessor().putPixelValue(x, y, 0.0);
              newGreenImage.getProcessor().putPixelValue(x, y, 0.0);
            }



            x++;
        }
        y++;
    }

    rimg = newRedImage;
    gimg = newGreenImage;
    bimg = newBlueImage;

    //rimg.show();


    ImagePlus[] nchan = {rimg, gimg, bimg};

    RGBPhoto nphoto = new RGBPhoto(nchan, photo.getCameraType(), photo);
    //nphoto.show();

    if( ReflectanceImageOnly ){
        IJ.save((ImagePlus)nphoto.getImage(), (String)(String.valueOf("C:\\Users\\Peau\\Desktop\\") + nphoto.getFileName() + "_index" + "." + nphoto.getExtension()));
    }

    return nphoto;
  }


  public RGBPhoto makeNDVI(RGBPhoto photo, RGBPhoto refphoto, double[] coeff, Roi[] rois){

      ImagePlus img = photo.getImage();
      ImagePlus imgRed = photo.getRedChannel();
      ImagePlus imgGreen = photo.getGreenChannel();
      ImagePlus imgBlue = photo.getBlueChannel();

      ImagePlus refImg = refphoto.getImage();
      ImagePlus refImgRed = refphoto.getRedChannel();
      ImagePlus refImgGreen = refphoto.getGreenChannel();
      ImagePlus refImgBlue = refphoto.getBlueChannel();

      double redPixel = 0.0;
      double greenPixel = 0.0;
      double bluePixel = 0.0;


      double redReflectMax = (double)imgRed.getDisplayRangeMax() * coeff[1] + coeff[0];
      double redReflectMin = (double)imgRed.getDisplayRangeMin() * coeff[1] + coeff[0];
      double blueReflectMax = (double)imgBlue.getDisplayRangeMax() * coeff[3] + coeff[2];
      double blueReflectMin = (double)imgBlue.getDisplayRangeMin() * coeff[3] + coeff[2];

      ImagePlus newRedImage = NewImage.createFloatImage((String)"redImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
      ImagePlus newBlueImage = NewImage.createFloatImage((String)"blueImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
      ImagePlus newGreenImage = NewImage.createFloatImage((String)"greenImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);

      // Setup ROI Manager for the reference image in order to calculate the NIR percent to subtractNIR

      RoiManager manager = setupROIManager(refImgRed, rois);
      List<HashMap<String, String>> bandSummary = null;
      double[] refmeans = new double[3];


      double eightySevenTargetMean = 0.0;
      double fiftyOneTargetMean = 0.0;
      double twentyThreeTargetMean = 0.0;

      bandSummary = processRois(refImgRed, manager); // <--- This is the key
      double[] redRefMean = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
          Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
          Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};

      manager.reset();
      manager.close();
      if( debugger.DEBUGMODE ){
          IJ.log( (String)"Red channel 87% target mean: " + redRefMean[0] );
          IJ.log( (String)"Red channel 51% target mean: " + redRefMean[1] );
          IJ.log( (String)"Red channel 23% target mean: " + redRefMean[2] );

          IJ.log( (String)"Red Intecept: " + coeff[0] );
          IJ.log( (String)"Red Slope: " + coeff[1] );
      }


      manager = setupROIManager(refImgBlue, rois);
      // bandSummary[0] = 87% target summary
      bandSummary = processRois(refImgBlue, manager);
      double[] blueRefMean = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
          Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
          Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};

      if( debugger.DEBUGMODE ){
          IJ.log( (String)"Blue channel 87% target mean: " + blueRefMean[0] );
          IJ.log( (String)"Blue channel 51% target mean: " + blueRefMean[1] );
          IJ.log( (String)"Blue channel 23% target mean: " + blueRefMean[2] );

          IJ.log( (String)"Blue Intercept: " + coeff[2] );
          IJ.log( (String)"Blue Slope: " + coeff[3] );
      }

      manager.reset();
      manager.close();

      // coeff[0] = red intercept
      // coeff[1] = red slope
      // coeff[2] = blue intercept
      // coeff[3] = blue slope
      double NDVI87 = -0.0117532235;
      double NDVI51 = 0.0031666667;
      double NDVI23 = -0.0233926435;
      //double nirSubtract87 = (blueRefMean[0]*coeff[3]+coeff[2])/(blueRefMean[0]*coeff[1]) * (NDVI87-1)/(NDVI87+1) + (redRefMean[0]*coeff[1]+coeff[0])/(blueRefMean[0]*coeff[1]);
      double nirSubtract87 = (coeff[3]/coeff[1] + coeff[2]/(coeff[1]*blueRefMean[0]))*((NDVI87-1)/(NDVI87+1)) + (coeff[0]/(coeff[1]*blueRefMean[0])) + (redRefMean[0]/blueRefMean[0]);
      double nirSubtract51 = (blueRefMean[1]*coeff[3]+coeff[2])/(blueRefMean[1]*coeff[1]) * (NDVI51-1)/(NDVI51+1) + (redRefMean[1]*coeff[1]+coeff[0])/(blueRefMean[1]*coeff[1]);
      double nirSubtract23 = (blueRefMean[2]*coeff[3]+coeff[2])/(blueRefMean[2]*coeff[1]) * (NDVI23-1)/(NDVI23+1) + (redRefMean[2]*coeff[1]+coeff[0])/(blueRefMean[2]*coeff[1]);

      if( debugger.DEBUGMODE ){
          IJ.log((String)"87 Target Reported Subtraction Percent: " + nirSubtract87);
          IJ.log((String)"51 Target Reported Subtraction Percent: " + nirSubtract51);
          IJ.log((String)"23 Target Reported Subtraction Percent: " + nirSubtract23);
      }


      int x = 0;
      int y = 0;
      while( y < img.getHeight() ){
          x = 0;
          while( x < img.getWidth() ){
              // @TODO Survey 1 support (remember that the bands are switched)
              redPixel = (double)imgRed.getProcessor().getPixelValue(x, y);
              bluePixel = (double)imgBlue.getProcessor().getPixelValue(x, y);

              // NIR remove
              if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_NDVI) ){
                  redPixel = (double)redPixel - nirSubtract87 * bluePixel;
              }else if( photo.getCameraType().equals(CalibrationPrompt.DJIPHANTOM4_NDVI) ){
                  redPixel = (double)redPixel - nirSubtract87 * bluePixel;
              }else if( photo.getCameraType().equals(CalibrationPrompt.DJIPHANTOM3_NDVI) ){
                  redPixel = (double)redPixel - nirSubtract87 * bluePixel;
              }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY1_NDVI) ){
                  bluePixel = bluePixel - redPixel;
              }else if( photo.getCameraType().equals(CalibrationPrompt.DJIX3_NDVI) ){
                 redPixel = (double)redPixel - nirSubtract87 * bluePixel;
              }
              //redPixel = (double)rimg.getProcessor().getPixelValue(x, y) - 0.8 * bimg.getProcessor().getPixelValue(x, y);

              // Apply reflectance mapping
              redPixel = (double)redPixel * coeff[1] + coeff[0];
                  //bluePixel = (double)bimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[3] + calibrationCeofs[2];
              bluePixel = (double)bluePixel * coeff[3] + coeff[2];


              // THIS ONE WORKS!!

              if( !ReflectanceImageOnly ){
                redPixel = (double)( (redPixel - redReflectMin)/(redReflectMax-redReflectMin)*255.0 );
                bluePixel = (double)( (bluePixel - blueReflectMin)/(blueReflectMax-blueReflectMin)*255.0 );


                // histogram stretch map to float-point THIS ONE WORKS!!
                redPixel = (double)(redPixel - 0.0)/(255.0-0.0)*1.0;
                bluePixel = (double)(bluePixel - 0.0)/(255.0-0.0)*1.0;
              }

              newRedImage.getProcessor().putPixelValue(x, y, redPixel);
              newBlueImage.getProcessor().putPixelValue(x, y, bluePixel);
              newGreenImage.getProcessor().putPixelValue(x, y, greenPixel);


              x++;
          }
          y++;
      }

      imgRed = newRedImage;
      imgGreen = newGreenImage;
      imgBlue = newBlueImage;

      if( ReflectanceImageOnly ){
          IJ.save((ImagePlus)imgRed, (String)(String.valueOf(photo.getDir() + photo.getFileName() + "_reflectance_red." + photo.getExtension())));
          IJ.save((ImagePlus)imgGreen, (String)(String.valueOf(photo.getDir() + photo.getFileName() + "_reflectance_green." + photo.getExtension())));
          IJ.save((ImagePlus)imgBlue, (String)(String.valueOf(photo.getDir() + photo.getFileName() + "_reflectance_blue." + photo.getExtension())));
      }


      ImagePlus[] nchan = {imgRed, imgGreen, imgBlue};
      RGBPhoto nphoto = new RGBPhoto(nchan, CalibrationPrompt.SURVEY2_NDVI, photo);

      if( ReflectanceImageOnly ){
          //IJ.save((ImagePlus)nphoto.getImage(), (String)(String.valueOf("C:\\Users\\Peau\\Desktop\\") + nphoto.getFileName() + "_index" + "." + nphoto.getExtension()));
      }

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

      // @TODO Create the float-point reflectance image
      // img.getBitDepth()
      int imgMax = (int)(Math.pow(2,8)-1);
      int imgMin = 0;
      double range[] = {(double)imgMin, (double)imgMax};
      double redReflectMax = (double)rimg.getDisplayRangeMax() * calibrationCeofs[1] + calibrationCeofs[0];
      double redReflectMin = (double)rimg.getDisplayRangeMin() * calibrationCeofs[1] + calibrationCeofs[0];
      double blueReflectMax = (double)bimg.getDisplayRangeMax() * calibrationCeofs[3] + calibrationCeofs[2];
      double blueReflectMin = (double)bimg.getDisplayRangeMin() * calibrationCeofs[3] + calibrationCeofs[2];
      double redRange[] = {0.0,redReflectMax};
      double blueRange[] = {0.0,blueReflectMax};

      double redPixel = 0.0;
      double reflect = 0.0;
      double bluePixel = 0.0;
      double greenPixel = 0.0;

      ImagePlus newRedImage = NewImage.createFloatImage((String)"redImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
      ImagePlus newBlueImage = NewImage.createFloatImage((String)"blueImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
      ImagePlus newGreenImage = NewImage.createFloatImage((String)"greenImage", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
      //rimg.show();
      //bimg.show();
      //IJ.log((String)"Red Slope: " + calibrationCeofs[1]);
      //IJ.log((String)"Red Intercept: " + calibrationCeofs[0]);
      //IJ.log((String)"Blue Slope: " + calibrationCeofs[3]);
      //IJ.log((String)"Blue Intercept: " + calibrationCeofs[2]);



      int x = 0;
      int y = 0;
      while (y < img.getHeight()) {
          x = 0;
          while (x < img.getWidth()) {
              // @TODO Survey 1 support (remember that the bands are switched)
              redPixel = (double)rimg.getProcessor().getPixelValue(x, y);
              bluePixel = (double)bimg.getProcessor().getPixelValue(x, y);

              // NIR remove
              if( photo.getCameraType().equals(CalibrationPrompt.SURVEY2_NDVI) ){
                  redPixel = (double)rimg.getProcessor().getPixelValue(x, y) - .62 * bimg.getProcessor().getPixelValue(x, y);
              }else if( photo.getCameraType().equals(CalibrationPrompt.DJIPHANTOM4_NDVI) ){
                  redPixel = (double)rimg.getProcessor().getPixelValue(x, y) - 1.0 * bimg.getProcessor().getPixelValue(x, y);
              }else if( photo.getCameraType().equals(CalibrationPrompt.DJIPHANTOM3_NDVI) ){
                  redPixel = (double)rimg.getProcessor().getPixelValue(x, y) - 1.0 * bimg.getProcessor().getPixelValue(x, y);
              }else if( photo.getCameraType().equals(CalibrationPrompt.SURVEY1_NDVI) ){
                  bluePixel = bluePixel - redPixel;
              }else if( photo.getCameraType().equals(CalibrationPrompt.DJIX3_NDVI) ){
                 redPixel = (double)rimg.getProcessor().getPixelValue(x, y) - 1.0 * bimg.getProcessor().getPixelValue(x, y);
              }
              //redPixel = (double)rimg.getProcessor().getPixelValue(x, y) - 0.8 * bimg.getProcessor().getPixelValue(x, y);

              // Apply reflectance mapping
              redPixel = (double)redPixel * calibrationCeofs[1] + calibrationCeofs[0];
                  //bluePixel = (double)bimg.getProcessor().getPixelValue(x, y) * calibrationCeofs[3] + calibrationCeofs[2];
              bluePixel = (double)bluePixel * calibrationCeofs[3] + calibrationCeofs[2];


              // THIS ONE WORKS!!

              if( !ReflectanceImageOnly ){
                redPixel = (double)( (redPixel - redReflectMin)/(redReflectMax-redReflectMin)*255.0 );
                bluePixel = (double)( (bluePixel - blueReflectMin)/(blueReflectMax-blueReflectMin)*255.0 );


                // histogram stretch map to float-point THIS ONE WORKS!!
                redPixel = (double)(redPixel - 0.0)/(255.0-0.0)*1.0;
                bluePixel = (double)(bluePixel - 0.0)/(255.0-0.0)*1.0;
              }

              newRedImage.getProcessor().putPixelValue(x, y, redPixel);
              newBlueImage.getProcessor().putPixelValue(x, y, bluePixel);
              newGreenImage.getProcessor().putPixelValue(x, y, greenPixel);


              x++;
          }
          y++;
      }

      rimg = newRedImage;
      gimg = newGreenImage;
      bimg = newBlueImage;


      ImagePlus[] nchan = {rimg, gimg, bimg};
      RGBPhoto nphoto = new RGBPhoto(nchan, CalibrationPrompt.SURVEY2_NDVI, photo);

      if( ReflectanceImageOnly ){
          IJ.save((ImagePlus)nphoto.getImage(), (String)(String.valueOf("C:\\Users\\Peau\\Desktop\\") + nphoto.getFileName() + "_index" + "." + nphoto.getExtension()));
      }

      return nphoto;
  }

  public double getNIRSubtractPercent(ImagePlus image, Roi[] rois){
      // The formula
      RoiManager manager = setupROIManager(image, rois);

      List<HashMap<String, String>> bandSummary = null;
      double[] refmeans = new double[3];

      if( image == null ){

      }else{


          /*
        if( channel.equals("Red") ){
          manager = calibrator.setupROIManager(qrphoto.getRedChannel(), rois);
          bandSummary = calibrator.processRois(qrphoto.getRedChannel(), manager); // <--- This is the key

          refmeans[0] = baseSummary[0][0];
          refmeans[1] = baseSummary[1][0];
          refmeans[2] = baseSummary[2][0];
        }else if( channel.equals("Green") ){
          manager = calibrator.setupROIManager(qrphoto.getGreenChannel(), rois);
          bandSummary = calibrator.processRois(qrphoto.getGreenChannel(), manager); // <--- This is the key

          refmeans[0] = baseSummary[0][1];
          refmeans[1] = baseSummary[1][1];
          refmeans[2] = baseSummary[2][1];
        }else if( channel.equals("Blue") ){
          //qrphoto.getBlueChannel().show();
          manager = calibrator.setupROIManager(qrphoto.getBlueChannel(), rois);
          bandSummary = calibrator.processRois(qrphoto.getBlueChannel(), manager); // <--- This is the key

          refmeans[0] = baseSummary[0][2];
          refmeans[1] = baseSummary[1][2];
          refmeans[2] = baseSummary[2][2];
      }*/
      }




      manager.reset();
      manager.close();
      return 0.0;
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
    //IJ.log((String)("Pixel count: " + count));
    //IJ.log((String)("Pixel sum: " + sum));
    //IJ.log((String)("Pixel mean value: " + IJ.d2s((double)(sum / (double)count), (int)4)));
    mean = (double)(sum/(double)count);

    values = new HashMap<String, String>();
    values.put(MAP_COUNT, String.valueOf(count) );
    values.put(MAP_SUM, String.valueOf(sum) );
    values.put(MAP_MEAN, String.valueOf(mean) );


    /* For debugging!
    channel.show();
    channel.getProcessor().drawRoi(roi);
    channel.updateAndDraw();
    */

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

    if( resimg == null || resimg.getProcessor() == null ){
      IJ.log("Could not create a resulting image.");
      return null;
    }

    ResultPoint[] points = result.getResultPoints();

    if( points == null ){
      IJ.log("Could not find resulting points.");
      return null;
    }

    //PolygonRoi qrRoi = qr.createPolygon( polyXCoords, polyYCoords );
    PolygonRoi qrRoi = qr.createPolygon( points );

    if( qrRoi == null ){
      IJ.log("Could not create QR ROI.");
      return null;
    }
    //qr.drawPolygonOn(qrRoi, resimg);
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

    resimg.show();
    qrimg.show();

    Roi[] rois = {target1Roi, target2Roi, target3Roi};

    //Roi[] rois = {target3Roi, target2Roi, target1Roi}; // @TODO TESTING,

    return rois;
  }



}
