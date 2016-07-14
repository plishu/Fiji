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
import ij.plugin.RGBStackConverter;


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
    HashMap<String, String> saveFileDialogValues = null;
    printAll( mainDialogValues.values() );

    // Image that contains QR calibration (if option is choosen)
    ImagePlus qrimg = null;

    // Check if camera selected is NDVI
    if( mainDialogValues.get(CalibrationPrompt.MAP_CAMERA).equals("Survey2 NDVI") ){
      prompt.showDualBandDialog();
      dualBandDialogValues = prompt.getDualBandDialogValues();
      printAll( dualBandDialogValues.values() );
    }

    Calibrator calibrator = new Calibrator();
    RGBPhoto procPhoto = null;

    // Check if user wants to provide calibration target image
    RGBPhoto qrphoto = null; // Use to detect qr code
    RGBPhoto qrscaled = null; // Use to feed into roi processing

    if( mainDialogValues.get(CalibrationPrompt.MAP_USEQR).equals("true") ){
      prompt.showQRJPGFileDialog(); // Get QR image
      qrFileDialogValues = prompt.getQRFileDialogValues();
      printAll( qrFileDialogValues.values() );

      qrphoto = new RGBPhoto( qrFileDialogValues );
      qrscaled = calibrator.scaleChannels(qrphoto);
      qrphoto.show();

      double gm = Double.parseDouble( mainDialogValues.get(CalibrationPrompt.MAP_GAMMA) );
      if( mainDialogValues.get(CalibrationPrompt.MAP_REMOVEGAMMA).equals("true") ){
        qrphoto = calibrator.removeGamma(qrphoto, gm);
      }

    }

    // Ask user for input image to calibrate
    prompt.showImageFileDialog();
    imageFileDialogValues = prompt.getImageFileDialogValues();
    printAll( imageFileDialogValues.values() );

    // Load image to calibrate
    RGBPhoto photo = new RGBPhoto( imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR),
              imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEFILENAME),
              imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEPATH),
              mainDialogValues.get(CalibrationPrompt.MAP_CAMERA), false );

    // Ask user where to save calibrated image
    prompt.showSaveFileDialog(photo.getFileName(), photo.getExtension());
    saveFileDialogValues = prompt.getSaveFileDialogValues();

    /*
    // Scale all channels. Use only the ones you need.
    procPhoto = calibrator.scaleChannels(photo);

    // Remove gamma (JPG only!!)
    double gm = Double.parseDouble( mainDialogValues.get(CalibrationPrompt.MAP_GAMMA) );
    if( mainDialogValues.get(CalibrationPrompt.MAP_REMOVEGAMMA).equals("true") ){
      procPhoto = calibrator.removeGamma(procPhoto, gm);
    }*/

    photo.show();

    // Open reference file
    OpenDialog baseFileDialog = new OpenDialog("Select Base File");
    File bfs = new File(baseFileDialog.getPath());

    double[][] baseSummary = null;
    double[] coeffs = new double[4];
    double[] tmpcoeff = null;

    List<HashMap<String, String>> redBandSummary = null;

    String camera = mainDialogValues.get(CalibrationPrompt.MAP_CAMERA);
    RGBPhoto resultphoto = null;

    RoiManager manager = null;
    Roi[] rois = calibrator.getRois(qrphoto.getImage());

    if( camera.equals(CalibrationPrompt.SURVEY2_NDVI) ){
      // Use red channel and blue channel
      baseSummary = calibrator.getRefValues(bfs, "660/850");
      tmpcoeff = calculateCoefficients(qrscaled, rois, calibrator, baseSummary, "Red");
      coeffs[0] = tmpcoeff[0];
      coeffs[1] = tmpcoeff[1];
      tmpcoeff = calculateCoefficients(qrscaled, rois, calibrator, baseSummary, "Blue");
      coeffs[2] = tmpcoeff[0];
      coeffs[3] = tmpcoeff[1];

      resultphoto = calibrator.makeNDVI(photo, coeffs);

    }else if( camera.equals(CalibrationPrompt.SURVEY2_NIR) ){
      baseSummary = calibrator.getRefValues(bfs, "850");
      tmpcoeff = calculateCoefficients(qrscaled, rois, calibrator, baseSummary, "Red");
      coeffs[0] = tmpcoeff[0];
      coeffs[1] = tmpcoeff[1];

      resultphoto = calibrator.makeSingle(photo, coeffs);

    }else if( camera.equals(CalibrationPrompt.SURVEY2_RED) ){
      baseSummary = calibrator.getRefValues(bfs, "650");
      tmpcoeff = calculateCoefficients(qrscaled, rois, calibrator, baseSummary, "Red");
      coeffs[0] = tmpcoeff[0];
      coeffs[1] = tmpcoeff[1];

      resultphoto = calibrator.makeSingle(photo, coeffs);

    }else if( camera.equals(CalibrationPrompt.SURVEY2_GREEN) ){
      baseSummary = calibrator.getRefValues(bfs, "548");
      tmpcoeff = calculateCoefficients(qrscaled, rois, calibrator, baseSummary, "Green");
      coeffs[0] = tmpcoeff[0];
      coeffs[1] = tmpcoeff[1];

      resultphoto = calibrator.makeSingle(photo, coeffs);

    }else if( camera.equals(CalibrationPrompt.SURVEY2_BLUE) ){
      baseSummary = calibrator.getRefValues(bfs, "450");
      tmpcoeff = calculateCoefficients(qrscaled, rois, calibrator, baseSummary, "Blue");
      coeffs[0] = tmpcoeff[0];
      coeffs[1] = tmpcoeff[1];

      resultphoto = calibrator.makeSingle(photo, coeffs);

    }else{
      // IDK
    }

    //manager = RoiManager.getInstance();

    RGBStackConverter.convertToRGB(resultphoto.getImage());
    resultphoto.copyFileData(photo);
    resultphoto.show();
    IJ.log("Saving Image");
    saveToDir(saveFileDialogValues.get(CalibrationPrompt.MAP_SAVEDIR), resultphoto.getFileName(), resultphoto.getExtension(), resultphoto.getImage());
    IJ.log("Saved");

  }


  public double[] calculateCoefficients(RGBPhoto qrphoto, Roi[] rois, Calibrator calibrator, double[][] baseSummary, String channel){
    //Roi[] rois = null;
    RoiManager manager = null;
    double[] coeff = null;
    List<HashMap<String, String>> bandSummary = null;
    double[] refmeans = new double[3];

    //baseSummary = calibrator.getRefValues(bfs, "660/850"); // <-----Change this!!
    if( qrphoto == null ){

    }else{
      //rois = calibrator.getRois(qrphoto.getImage());
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
        qrphoto.getBlueChannel().show();
        manager = calibrator.setupROIManager(qrphoto.getBlueChannel(), rois);
        bandSummary = calibrator.processRois(qrphoto.getBlueChannel(), manager); // <--- This is the key

        refmeans[0] = baseSummary[0][2];
        refmeans[1] = baseSummary[1][2];
        refmeans[2] = baseSummary[2][2];
    }

    //@TODO supply QR CODE channels, not image to calibrate


    double[] means = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
        Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
        Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};
    //double[] refmeans = {baseSummary[0][0], baseSummary[1][0], baseSummary[2][0]};

    coeff = calibrator.calculateCalibrationCoefficients( means, refmeans );

    manager.reset();
    manager.close();
    }
    return coeff;
  }


  public void saveToDir(String outdir, String filename, String ext, ImagePlus image){
    String NDVIAppend = "_Calibrated";

    //IJ.log("Output Directory: " + outdir);
    //IJ.log("Filename: " + filename);
    //IJ.log("Save as extension: " + ext);



    IJ.save((ImagePlus)image, (String)(String.valueOf(outdir) + filename + NDVIAppend + "." + ext));
    return;
  }

  public void printAll(Collection col){
    Iterator itr = col.iterator();
    while( itr.hasNext() ){
      IJ.log( (String)itr.next() );
    }
  }

}
