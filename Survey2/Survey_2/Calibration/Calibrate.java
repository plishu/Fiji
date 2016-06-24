import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.CurveFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;


import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Vector;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;



public class Calibrate implements PlugIn{

  public void run(String arg){
    CalibrationPrompt prompt = CalibrationPrompt.getPrompt();
    prompt.showMainDialog();
    HashMap<String, String> mainDialogValues = prompt.getMainDialogValues();
    HashMap<String, String> dualBandDialogValues = null;
    HashMap<String, String> qrFileDialogValues = null;
    HashMap<String, String> imageFileDialogValues = null;
    printAll( mainDialogValues.values() );

    // Image that contains QR calibration (if option is choosen)
    ImagePlus qrimg = null;

    // Check if camera selected is NDVI
    if( mainDialogValues.get(CalibrationPrompt.MAP_CAMERA).equals("Survey2 NDVI") ){
      prompt.showDualBandDialog();
      dualBandDialogValues = prompt.getDualBandDialogValues();
      printAll( dualBandDialogValues.values() );
    }

    // Check if user wants to provide calibration target image
    RGBPhoto qrphoto = null;
    if( mainDialogValues.get(CalibrationPrompt.MAP_USEQR).equals("true") ){
      prompt.showQRFileDialog(); // Get QR image
      qrFileDialogValues = prompt.getQRFileDialogValues();
      printAll( qrFileDialogValues.values() );

      qrphoto = new RGBPhoto( qrFileDialogValues );
      qrphoto.show();
      qrimg = qrphoto.getImage(); // QR picture in ImagePlus form

    }

    // Ask user for input image to calibrate
    prompt.showImageFileDialog();
    imageFileDialogValues = prompt.getImageFileDialogValues();
    printAll( imageFileDialogValues.values() );

    // Load image to calibrate

    RGBPhoto photo = new RGBPhoto( imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR),
              imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEFILENAME),
              imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEPATH),
              mainDialogValues.get(CalibrationPrompt.MAP_CAMERA) );
    photo.show();


    Calibrator calibrator = new Calibrator();
    RGBPhoto procPhoto = null;

    /*
    if( mainDialogValues.get(CalibrationPrompt.MAP_CAMERA).equals("Survey2 NDVI") ){
      // Scale image only in NDVI!!
      procPhoto = new RGBPhoto( calibrator.scaleImages(photo.splitStack()) );

      if( dualBandDialogValues.get(CalibrationPrompt.MAP_REMOVEGAMMA).equals("true") ){
        IJ.log("Removing gamma");
        ImagePlus red = procPhoto.getRedChannel();
        ImagePlus green = procPhoto.getGreenChannel();
        ImagePlus blue = procPhoto.getBlueChannel();
        double gm = Double.parseDouble( dualBandDialogValues.get(CalibrationPrompt.MAP_GAMMA) );
        calibrator.removeGamma(blue, red, gm);

        ImagePlus[] bands = {red, green, blue};
        procPhoto = new RGBPhoto(bands);

      }
      procPhoto.show();
    }else{
      // Every other image
      procPhoto = new RGBPhoto( photo.splitStack() );
      procPhoto.show();
    }*/

    //procPhoto = new RGBPhoto( calibrator.scaleImages())

    // Scale all channels. Use only the ones you need.
    procPhoto = calibrator.scaleChannels(photo);

    //procPhoto.getBlueChannel().show();
    //procPhoto.getGreenChannel().show();
    //procPhoto.getRedChannel().show();

    // Remove gamma (JPG only!!)
    double gm = Double.parseDouble( mainDialogValues.get(CalibrationPrompt.MAP_GAMMA) );
    procPhoto = calibrator.removeGamma(procPhoto, gm);
    //procPhoto.show();

    //procPhoto.getBlueChannel().show();
    //procPhoto.getGreenChannel().show();
    //procPhoto.getRedChannel().show();

    Roi[] rois = null;
    RoiManager manager = null;

    if( qrphoto == null ){
      // Use base
    }else{
      // Use calibration targets
      // @TODO BUG: ROI's not mapping correctly
      rois = calibrator.getRois(qrimg);
      manager = calibrator.setupROIManager(qrimg, rois);
    }

    // Generate mean
    String camera = mainDialogValues.get(CalibrationPrompt.MAP_CAMERA);

    HashMap<String, String> visBandIndex = null;
    HashMap<String, String> nirBandIndex = null;

    List<HashMap<String, String>> redBandSummary = null;
    List<HashMap<String, String>> greenBandSummary = null;
    List<HashMap<String, String>> blueBandSummary = null;
    double[][] baseSummary = null;

    // Get reference values
    OpenDialog baseFileDialog = new OpenDialog("Select Base File");
    File bfs = new File(baseFileDialog.getPath());
    //baseSummary = calibrator.getRefValues(bfs, "850/660"); //@TODO Replace hard coded value
    double coefficients[] = null;

    double redCoeff[] = null;
    double greenCoeff[] = null;
    double blueCoeff[] = null;



    // @TODO Option to use base value summary if no QR calibration
    if( camera.equals("Survey2 NDVI") ){
      //baseSummary = calibrator.getRefValues(bfs, "850/660");
      baseSummary = calibrator.getRefValues(bfs, "660/850");
      redBandSummary = calibrator.processRois(procPhoto.getRedChannel(), manager);
      blueBandSummary = calibrator.processRois(procPhoto.getBlueChannel(), manager);

      double[] blue = {Double.parseDouble(blueBandSummary.get(0).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(1).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(2).get(Calibrator.MAP_MEAN))};
      double[] red = {Double.parseDouble(redBandSummary.get(0).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(redBandSummary.get(1).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(redBandSummary.get(2).get(Calibrator.MAP_MEAN))};

      double[] refred = {baseSummary[0][0], baseSummary[1][0], baseSummary[2][0]};
      double[] refblue = {baseSummary[0][2], baseSummary[1][2], baseSummary[2][2]};

      redCoeff = calibrator.calculateCalibrationCoefficients( red, refred );
      blueCoeff = calibrator.calculateCalibrationCoefficients( blue, refblue );

    }else if( camera.equals("Survey2 NIR") ){
      redBandSummary = calibrator.processRois(procPhoto.getRedChannel(), manager);
    }else if( camera.equals("Survey2 Red") ){
      redBandSummary = calibrator.processRois(procPhoto.getRedChannel(), manager);
    }else if( camera.equals("Survey2 Green") ){
      greenBandSummary = calibrator.processRois(procPhoto.getGreenChannel(), manager);
    }else if( camera.equals("Survey2 Blue") ){

      baseSummary = calibrator.getRefValues(bfs, "450");
      //blueBandSummary = calibrator.processRois(procPhoto.getBlueChannel(), manager);
      blueBandSummary = calibrator.processRois(procPhoto.getBlueChannel(), manager);

      double[] blue = {Double.parseDouble(blueBandSummary.get(0).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(1).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(2).get(Calibrator.MAP_MEAN))};

      /*
      double[] refblue = {baseSummary.get(Calibrator.MAP_TARG1)[2],
        baseSummary.get(Calibrator.MAP_TARG2)[2], baseSummary.get(Calibrator.MAP_TARG3)[2]};
      coefficients = calibrator.calculateCalibrationCoefficients(blue, refblue); ----> HERE */
      double[] refblue = {baseSummary[0][2], baseSummary[1][2], baseSummary[2][2]};

      for(int i=0; i<3; i++){
        IJ.log(String.valueOf(refblue[i]));
      }

      blueCoeff = calibrator.calculateCalibrationCoefficients(blue, refblue);

    }else{
      IJ.log("Camera " + camera +" currently not supported");
      return;
    }




    ImagePlus indexImage = null;
    if( mainDialogValues.get(CalibrationPrompt.MAP_CAMERA).equals("Survey2 NDVI") ){
      // Scale image only in NDVI!!
      //indexImage = calibrator.makeNDVI(procPhoto.getRedChannel(), procPhoto.getBlueChannel(), )
    }else{
      // Every other image

    }




  }

  public double[][] calculateCoefficients(RGBPhoto photo){
    /*
    if( camera.equals("Survey2 NDVI") ){
      baseSummary = calibrator.getRefValues(bfs, "850/660");
      redBandSummary = calibrator.processRois(procPhoto.getRedChannel(), manager);
      blueBandSummary = calibrator.processRois(procPhoto.getBlueChannel(), manager);

      double[] blue = {Double.parseDouble(blueBandSummary.get(0).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(1).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(2).get(Calibrator.MAP_MEAN))};

    }else if( camera.equals("Survey2 NIR") ){
      redBandSummary = calibrator.processRois(procPhoto.getRedChannel(), manager);
    }else if( camera.equals("Survey2 Red") ){
      redBandSummary = calibrator.processRois(procPhoto.getRedChannel(), manager);
    }else if( camera.equals("Survey2 Green") ){
      greenBandSummary = calibrator.processRois(procPhoto.getGreenChannel(), manager);
    }else if( camera.equals("Survey2 Blue") ){

      baseSummary = calibrator.getRefValues(bfs, "450");
      //blueBandSummary = calibrator.processRois(procPhoto.getBlueChannel(), manager);
      blueBandSummary = calibrator.processRois(procPhoto.getBlueChannel(), manager);

      double[] blue = {Double.parseDouble(blueBandSummary.get(0).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(1).get(Calibrator.MAP_MEAN)),
         Double.parseDouble(blueBandSummary.get(2).get(Calibrator.MAP_MEAN))};

      /*
      double[] refblue = {baseSummary.get(Calibrator.MAP_TARG1)[2],
        baseSummary.get(Calibrator.MAP_TARG2)[2], baseSummary.get(Calibrator.MAP_TARG3)[2]};
      coefficients = calibrator.calculateCalibrationCoefficients(blue, refblue);
      double[] refblue = {baseSummary[0][2], baseSummary[1][2], baseSummary[2][2]};

      for(int i=0; i<3; i++){
        IJ.log(String.valueOf(refblue[i]));
      }

      blueCoeff = calibrator.calculateCalibrationCoefficients(blue, refblue);

    }else{
      IJ.log("Camera " + camera +" currently not supported");
      return;
    } */
    return null;
  }

  public void printAll(Collection col){
    Iterator itr = col.iterator();
    while( itr.hasNext() ){
      IJ.log( (String)itr.next() );
    }
  }

}
