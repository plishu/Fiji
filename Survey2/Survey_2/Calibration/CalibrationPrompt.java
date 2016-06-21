import ij.gui.GenericDialog;
import ij.io.OpenDialog;

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
  public static String MAP_CAMERA = "CAMERA";
  public static String MAP_USEQR = "USEQR";
  public static String MAP_REMOVEGAMMA = "REMOVEGAMMA";
  public static String MAP_GAMMA = "GAMMA";
  public static String MAP_QRDIR = "QRDIR";
  public static String MAP_QRFILENAME = "QRFILENAME";
  public static String MAP_QRPATH = "QRPATH";
  public static String MAP_IMAGEDIR = "IMAGEDIR";
  public static String MAP_IMAGEFILENAME = "IMAGEFILENAME";
  public static String MAP_IMAGEPATH = "IMAGEPATH";

  private String[] cameras = new String[]{"Survey2 Red", "Survey2 Green",
    "Survey2 Blue", "Survey2 NDVI", "Survey2 NIR"};
  private String[] dualBand = new String[]{"Survey2 NDVI"};
  private double gamma = 0.0;

  private boolean useQR = false;
  private boolean removeGamma = true;

  public CalibrationPrompt(){
    mainDialog = new GenericDialog("Calibrate Image");
    mainDialog.addChoice("Camera: ", cameras, cameras[3]);
    mainDialog.addCheckbox("Calibrate with QR Calibration Photo: ", useQR);

    dualBandDialog = new GenericDialog("Remove Gamma Effect");
    dualBandDialog.addCheckbox("Remove gamma effect? ", removeGamma);
    dualBandDialog.addNumericField("Gamma value: ", gamma, 5);



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

    values.put(MAP_CAMERA, mainDialog.getNextChoice());
    values.put( MAP_USEQR, String.valueOf(mainDialog.getNextBoolean()) );

    return values;
  }

  public HashMap<String, String> getDualBandDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();

    values.put( MAP_REMOVEGAMMA, String.valueOf(dualBandDialog.getNextBoolean()) );
    values.put( MAP_GAMMA, String.valueOf(dualBandDialog.getNextNumber()) );

    return values;
  }

  public HashMap<String, String> getQRFileDialogValues(){
    HashMap<String, String> values = new HashMap<String, String>();

    values.put( MAP_QRDIR, qrFileDialog.getDirectory() );
    values.put( MAP_QRFILENAME, qrFileDialog.getFileName() );
    values.put( MAP_QRPATH, qrFileDialog.getPath() );

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
