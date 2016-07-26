import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;

import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

public class CalibrationPrompt{
  private static CalibrationPrompt prompt = null;
  private GenericDialog mainDialog = null;
  private GenericDialog dualBandDialog = null;
  private OpenDialog qrFileDialog = null;
  private OpenDialog imageFileDialog = null;
  private SaveDialog saveFileDialog = null;
  private GenericDialog fullDialog = null;

  /*
  private Map<String, String> mainDialogValues = null;
  private Map<String, String> dualBandDialogValues = null;
  private Map<String, String> qrFileDialogValues = null;
  private Map<String, String> imageFileDialog = null;
  */
  public static final String MAP_CAMERA = "CAMERA";
  public static final String MAP_FILTER = "FILTER";
  public static final String MAP_USEQR = "USEQR";
  public static final String MAP_REMOVEGAMMA = "REMOVEGAMMA";
  public static final String MAP_GAMMA = "GAMMA";
  public static final String MAP_REMOVENIR = "REMOVENIR";
  public static final String MAP_NIRSUB = "NIRSUB";
  public static final String MAP_IMAGEDIR = "IMAGEDIR";
  public static final String MAP_IMAGEFILENAME = "IMAGEFILENAME";
  public static final String MAP_IMAGEPATH = "IMAGEPATH";
  public static final String MAP_SAVEDIR = "SAVEDIR";
  public static final String MAP_TIFFTOJPG = "TIFFTOJPG";
  /* Treat QR image as an image - use same keys
  public static String MAP_QRDIR = "QRDIR";
  public static String MAP_QRFILENAME = "QRFILENAME";
  public static String MAP_QRPATH = "QRPATH";
  */

  public static final String SURVEY2_RED = "Survey2 RED";
  public static final String SURVEY2_GREEN = "Survey2 GREEN";
  public static final String SURVEY2_BLUE = "Survey2 BLUE";
  public static final String SURVEY2_NDVI = "Survey2 NDVI";
  public static final String SURVEY2_NIR = "Survey2 NIR";
  public static final String DJIX3_NDVI = "DJI X3 NDVI (Red + NIR)";
  public static final String DJIPHANTOM3_NDVI = "DJI Phantom 3 NDVI (397mm)";
  public static final String DJIPHANTOM4_NDVI = "DJI Phantom 4 NDVI (397mm)";
  public static final String GOPRO_HERO4_NDVI = "GoPro Hero 4 NDVI";
  public static final String OTHER_CAMERA = "OTHER";


  /*
  private String[] cameras = new String[]{SURVEY2_RED, SURVEY2_GREEN,
    SURVEY2_BLUE, SURVEY2_NDVI, SURVEY2_NIR, DJIX3_NDVI, GOPRO_HERO4_NDVI, OTHER_CAMERA};*/
  private String[] cameras = new String[]{SURVEY2_NDVI, SURVEY2_NIR,
    SURVEY2_RED, SURVEY2_GREEN, SURVEY2_BLUE, DJIX3_NDVI, DJIPHANTOM4_NDVI,
    DJIPHANTOM3_NDVI};
  //private String[] dualBand = new String[]{SURVEY2_NDVI, DJIX3_NDVI, GOPRO_HERO4_NDVI};
  private String[] dualBand = new String[]{SURVEY2_NDVI, DJIX3_NDVI, DJIPHANTOM4_NDVI,
    DJIPHANTOM3_NDVI};
  private double gamma = 2.2;
  private double nirsub = 80.0; // Percentage

  private boolean useQR = false;
  private boolean removeGamma = true;
  private boolean removeNIR = true;
  private boolean tifsToJpgs = false;

  public CalibrationPrompt(){
    mainDialog = new GenericDialog("Calibrate Image");
    mainDialog.addChoice("Camera: ", cameras, cameras[0]);
    mainDialog.addCheckbox("Calibrate with MAPIR Reflectance Ground Target image", useQR);
    mainDialog.addCheckbox("Remove gamma? (Only applies to JPG images) ", removeGamma);
    mainDialog.addNumericField("Gamma value: ", gamma, 3);

    dualBandDialog = new GenericDialog("NDVI options");
    //dualBandDialog.addCheckbox("Remove gamma effect? ", removeGamma);
    //dualBandDialog.addNumericField("Gamma value: ", gamma, 5);
    dualBandDialog.addCheckbox("Subtract NIR? ", removeNIR);
    dualBandDialog.addNumericField( "Subtract amount percentage: ", nirsub, 3);


    fullDialog = new GenericDialog("Calibrate Images in Directory");
    fullDialog.addChoice("Select camera model", cameras, cameras[0]);
    fullDialog.addCheckbox("Convert calibrated TIFFs to JPGs", tifsToJpgs);
    fullDialog.addCheckbox("Calibrate with MAPIR Reflectance Ground Target image", useQR);
    /*
    fullDialog.addCheckbox("Remove Gamma (Applies to JPG images only)", removeGamma);
    fullDialog.addNumericField("Gamma value", gamma, 3);
    fullDialog.addCheckbox("Subtract NIR (Use this for NDVI only)", removeNIR);
    fullDialog.addNumericField("Subtract amount percentage", nirsub, 3);
    */
    fullDialog.addMessage("If a QR target image is not supplied, or the supplied");
    fullDialog.addMessage("image fails to be detected, base calibration values taken");
    fullDialog.addMessage("during a clear sunny day will be used.");

    fullDialog.centerDialog(true);
    fullDialog.setOKLabel("Begin");
    fullDialog.setCancelLabel("Quit");
    fullDialog.setSmartRecording(false);

  }

  /*
   * Singleton implementation. Creates Prompt object if it has not been
   * created yet. Return the Prompt object.
   * @return: Non-null Prompt object.
   */
  public static CalibrationPrompt getPrompt(){
    if( prompt == null ){
      prompt = new CalibrationPrompt();
    }
    return prompt;
  }

  public boolean wasCanceledFullDialog(){
    return fullDialog.wasCanceled();
  }

  /*
   * Show the dialog Prompt
   */
  public void showMainDialog(){
    mainDialog.showDialog();
  }

  public void showFullDialog(){
    fullDialog.showDialog();
  }

  public void showDualBandDialog(){
    dualBandDialog.showDialog();
  }

  public void showQRJPGFileDialog(){
    qrFileDialog = new OpenDialog("Select JPG Image with Calibration Targets (QR Code)");
  }

  public void showQRTIFFileDialog(){
    qrFileDialog = new OpenDialog("Select TIF Image with Calibration Targets (QR Code)");
  }

  public void showImageFileDialog(){
    imageFileDialog = new OpenDialog("Select First Image in Directory to Calibrate");
  }

  public void showSaveFileDialog(String filename, String ext){
    saveFileDialog = new SaveDialog("Choose Directory to Save", filename, "");
  }

  public boolean showQRNotDetectedDialog() throws IOException{
    GenericDialog dialog = new GenericDialog("QR Not Found");
    dialog.addMessage("QR code unable to be detected, would you like to proceed");
    dialog.addMessage("using the base calibration values?");
    dialog.enableYesNoCancel("Proceed", "Choose New Image");
    dialog.setCancelLabel("Quit");

    dialog.showDialog();

    if( dialog.wasOKed() ){
      // Continue with base values
      return false;
    }else if( dialog.wasCanceled() ){
      throw new IOException("No QR code selected. This means wants to quit program.");
    }else{
      // Choose QR code
      return true;
    }

  }

  public boolean showQRNotSameModelDialog(String qrModel, String selectedModel){
    GenericDialog dialog = new GenericDialog("QR Calibration Image Does Not Match Selected Camera Model");
    dialog.addMessage("It appears the QR target image camera model you supplied ("+qrModel+")");
    dialog.addMessage("does not match the camera model choosen ("+selectedModel+")");
    dialog.setOKLabel("Choose New Image");
    dialog.setCancelLabel("Quit");

    dialog.showDialog();

    if( dialog.wasOKed() ){
      return true;
    }else if( dialog.wasCanceled() ){
      return false;
    }
    return false;
  }

  public boolean showCalibrationFinishedDialog(){
    GenericDialog dialog = new GenericDialog("Calibration Completed");
    dialog.addMessage("Calibration successful for the choosen directory of images");
    dialog.setOKLabel("Calibrate New Directory");
    dialog.setCancelLabel("Quit");

    dialog.showDialog();

    if( dialog.wasOKed() ){
      return true;
    }else if( dialog.wasCanceled() ){
      return false;
    }
    return false;
  }

  public HashMap<String, String> getMainDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();
    String theCamera = mainDialog.getNextChoice();
    IJ.log("The camera selected is: " + theCamera);
    values.put( MAP_CAMERA, theCamera );
    values.put( MAP_USEQR, String.valueOf(mainDialog.getNextBoolean()) );
    values.put( MAP_REMOVEGAMMA, String.valueOf(mainDialog.getNextBoolean()) );
    values.put( MAP_GAMMA, String.valueOf(mainDialog.getNextNumber()) );

    return values;
  }

  public HashMap<String, String> getFullDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();
    String theCamera = fullDialog.getNextChoice();
    values.put( MAP_CAMERA, theCamera );
    values.put( MAP_TIFFTOJPG, String.valueOf(fullDialog.getNextBoolean()) );
    values.put( MAP_USEQR, String.valueOf(fullDialog.getNextBoolean()) );
    //values.put( MAP_REMOVEGAMMA, String.valueOf(fullDialog.getNextBoolean()) );
    //values.put( MAP_GAMMA, String.valueOf(fullDialog.getNextNumber()) );
    //values.put( MAP_REMOVENIR, String.valueOf(fullDialog.getNextBoolean()) );
    //values.put( MAP_NIRSUB, String.valueOf(fullDialog.getNextNumber()) );

    return values;
  }

  public HashMap<String, String> getDualBandDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();

    //values.put( MAP_REMOVEGAMMA, String.valueOf(dualBandDialog.getNextBoolean()) );
    //values.put( MAP_GAMMA, String.valueOf(dualBandDialog.getNextNumber()) );
    values.put( MAP_REMOVENIR, String.valueOf(dualBandDialog.getNextBoolean()) );
    values.put( MAP_NIRSUB, String.valueOf(dualBandDialog.getNextNumber()) );

    return values;
  }

  public HashMap<String, String> getQRFileDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();

    values.put( MAP_IMAGEDIR, qrFileDialog.getDirectory() );
    values.put( MAP_IMAGEFILENAME, qrFileDialog.getFileName() );
    values.put( MAP_IMAGEPATH, qrFileDialog.getPath() );

    return values;
  }

  public HashMap<String, String> getImageFileDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();

    values.put( MAP_IMAGEDIR, imageFileDialog.getDirectory() );
    values.put( MAP_IMAGEFILENAME, imageFileDialog.getFileName() );
    values.put( MAP_IMAGEPATH, imageFileDialog.getPath() );

    return values;
  }

  public HashMap<String, String> getSaveFileDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();

    values.put( MAP_SAVEDIR, saveFileDialog.getDirectory() );

    return values;
  }

}
