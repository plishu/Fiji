import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.IJ;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;

import java.util.Map;
import java.util.HashMap;

public class CalibrationPrompt{
  private static CalibrationPrompt prompt = null;
  private GenericDialog mainDialog = null;
  private GenericDialog dualBandDialog = null;
  private OpenDialog qrFileDialog = null;
  private OpenDialog imageFileDialog = null;

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
  /* Treat QR image as an image - use same keys
  public static String MAP_QRDIR = "QRDIR";
  public static String MAP_QRFILENAME = "QRFILENAME";
  public static String MAP_QRPATH = "QRPATH";
  */

  public static final String SURVEY2_RED = "Survey2 Red";
  public static final String SURVEY2_GREEN = "Survey2 Green";
  public static final String SURVEY2_BLUE = "Survey2 Blue";
  public static final String SURVEY2_NDVI = "Survey2 NDVI";
  public static final String SURVEY2_NIR = "Survey2 NIR";
  public static final String OTHER_CAMERA = "OTHER";


  private String[] cameras = new String[]{SURVEY2_RED, SURVEY2_GREEN,
    SURVEY2_BLUE, SURVEY2_NDVI, SURVEY2_NIR, OTHER_CAMERA};
  private String[] dualBand = new String[]{SURVEY2_NDVI};
  private double gamma = 2.2;
  private double nirsub = 80.0; // Percentage

  private boolean useQR = false;
  private boolean removeGamma = true;
  private boolean removeNIR = true;

  public CalibrationPrompt(){
    mainDialog = new GenericDialog("Calibrate Image");
    mainDialog.addChoice("Camera: ", cameras, cameras[3]);
    mainDialog.addCheckbox("Calibrate with QR Calibration Photo ", useQR);
    mainDialog.addCheckbox("Remove gamma? (Only applies to JPG images) ", removeGamma);
    mainDialog.addNumericField("Gamma value: ", gamma, 3);

    dualBandDialog = new GenericDialog("NDVI options");
    //dualBandDialog.addCheckbox("Remove gamma effect? ", removeGamma);
    //dualBandDialog.addNumericField("Gamma value: ", gamma, 5);
    dualBandDialog.addCheckbox("Subtract NIR? ", removeNIR);
    dualBandDialog.addNumericField( "Subtract amount percentage: ", nirsub, 3);

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

  /*
   * Show the dialog Prompt
   */
  public void showMainDialog(){
    mainDialog.showDialog();
  }

  public void showDualBandDialog(){
    dualBandDialog.showDialog();
  }

  public void showQRFileDialog(){
    qrFileDialog = new OpenDialog("Select Image wih Calibration Targets (QR code)");
  }

  public void showImageFileDialog(){
    imageFileDialog = new OpenDialog("Select Image to Calibrate");
  }

  public HashMap<String, String> getMainDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();
    String theCamera = mainDialog.getNextChoice();
    IJ.log("THe camera selected is: " + theCamera);
    values.put( MAP_CAMERA, theCamera );
    values.put( MAP_USEQR, String.valueOf(mainDialog.getNextBoolean()) );
    values.put( MAP_REMOVEGAMMA, String.valueOf(mainDialog.getNextBoolean()) );
    values.put( MAP_GAMMA, String.valueOf(mainDialog.getNextNumber()) );

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

}
