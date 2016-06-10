import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.LutLoader;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Image;
import java.awt.TextField;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Vector;

public class NewApplyCalibration implements PlugIn, DialogListener {

  private static String[] indexTypes = new String[]{"NDVI (NIR-Vis)/(NIR+Vis)", "DVI NIR-Vis"};
  private static String logName = "log.txt";


  private String WorkingDirectory = null;
  private String lutLocation = null;
  private String[] lutNames = null;
  private String lutName = null;
  private String indexType = null;
  private Boolean createIndexColor = false;
  private Boolean createIndexFloat = false;
  private double minColorScale;
  private double maxColorScale;

  private Boolean saveParameters = true;
  private Boolean useDefaults = false;
  private String calibDir = null;
  private String inputDir = null;
  private String outputDir = null;

  private boolean DEBUG = true;


  // @Override
  public void run(String arg){
    IJ.log("hello :)");
    setup();

    setDefaultPrefs();
    GenericDialog prefsDialog = createPrefsDialog();
    if( prefsDialog == null ){
      return;
    }
    getPrefsFromDialog(prefsDialog);

    // Check to see if user wants to save parameters
    if (saveParameters.booleanValue()) {
      DebugPrint("Saving parameters...");
      savePrefs();
      DebugPrint("Done.");
    }

    // Select calibration file
    calibDir = getCalibDir(arg);
    if( calibDir == null){
      IJ.log("No calibration file selected");
      return;
    }else{
      DebugPrint("Calibration file: " + calibDir);
    }
    // Select input image directory
    inputDir = getInputDir();
    if( inputDir == null ){
      IJ.log("No input directory selected");
      return;
    }else{
      DebugPrint("Input directory: " + inputDir);
    }
    // Select output image directory
    outputDir = getOutputDir();
    if( outputDir == null ){
      IJ.log("No output directory selected");
      return;
    }else{
      DebugPrint("Output directory: " + outputDir);
    }

    // Get inputDir files
  }

  /*
   * Setup directories that will be used
   * @param none
   * @return void
   */
  public void setup(){
    WorkingDirectory = IJ.getDirectory("imagej");
    lutLocation = WorkingDirectory+"luts";
    lutNames = (new File(lutLocation)).list();

    DebugPrint("---------------- Luts Detected ------------------");
    for( int i=0; i<lutNames.length; i++ ){
      DebugPrint(lutNames[i]);
    }



  }


  /*
   * Set defualt preferences for calibration. If gui mode is enabled,
   * these preferences will apply to the preferences dialog box.
   * @param none
   * @return void
   */
  public void setDefaultPrefs(){
    indexType = Prefs.get( (String)"pm.fromSBImage.indexType", (String)indexTypes[0] );
    createIndexColor = Prefs.get( (String)"pm.ac.createIndexColor", (boolean)true );
    createIndexFloat = Prefs.get( (String)"pm.ac.createIndexFloat", (boolean)true );
    maxColorScale = Prefs.get( (String)"pm.ac.maxColorScale", (double)1.0 );
    minColorScale = Prefs.get( (String)"pm.ac.minColorScale", (double)0.0 );
    lutName = Prefs.get( (String)"pc.ac.lutName", (String)lutNames[0]);
  }

  /*
   * Get preferences inputted by the user
   * @param none
   * @return void
   */
  public void getPrefsFromDialog(GenericDialog dialog){
    indexType = dialog.getNextChoice();
    createIndexColor = dialog.getNextBoolean();
    minColorScale = dialog.getNextNumber();
    maxColorScale = dialog.getNextNumber();
    createIndexFloat = dialog.getNextBoolean();
    lutName = dialog.getNextChoice();
    saveParameters = dialog.getNextBoolean();
  }

  /*
   * Save parameters
   * @param none
   * @return void
   */
  public void savePrefs(){
    Prefs.set((String)"pm.ac.indexType", (String)indexType);
    Prefs.set((String)"pm.ac.createIndexColor", (boolean)createIndexColor);
    Prefs.set((String)"pm.ac.createIndexFloat", (boolean)createIndexFloat);
    Prefs.set((String)"pm.ac.maxColorScale", (double)maxColorScale);
    Prefs.set((String)"pm.ac.minColorScale", (double)minColorScale);
    Prefs.set((String)"pm.ac.lutName", (String)lutName);
    Prefs.savePreferences();
  }

  /*
   * Create and display the Preferences dialog box.
   * @param none
   * @return Instance to the created dialog. If dialog was cancelled, then
   * returns null
   */
  public GenericDialog createPrefsDialog(){
    GenericDialog dialog = new GenericDialog("Enter preferences");
    dialog.addCheckbox("Load default parameters (click OK below to reload)", false);
    dialog.addChoice("Select index type for calculation", indexTypes, indexType);
    dialog.addMessage("Output image options:");
    dialog.addCheckbox("Output Color Index image?", createIndexColor.booleanValue());
    dialog.addNumericField("Minimum Index value for scaling color Index image", minColorScale, 1);
    dialog.addNumericField("Maximum Index value for scaling color Index image", maxColorScale, 1);
    dialog.addCheckbox("Output floating point Index image?", createIndexFloat.booleanValue());
    dialog.addChoice("Select output color table for color Index image", lutNames, lutName);
    dialog.addCheckbox("Save parameters for next session", true);
    dialog.addDialogListener((DialogListener)this);
    dialog.showDialog();
    if (dialog.wasCanceled()) {
      IJ.log("Goodbye!");
        return null;
    }

    // Check whether to use default values
    useDefaults = dialog.getNextBoolean();
    if (useDefaults.booleanValue()) {
        dialog = null;
        dialog = new GenericDialog("Enter preferences");
        dialog.addCheckbox("Load default parameters (click OK below to reload)", false);
        dialog.addChoice("Select index type for calculation", indexTypes, indexTypes[0]);
        dialog.addMessage("Output image options:");
        dialog.addCheckbox("Output Color Index image?", true);
        dialog.addNumericField("Enter the minimum Index value for scaling color Index image", -1.0, 1);
        dialog.addNumericField("Enter the maximum Index value for scaling color Index image", 1.0, 1);
        dialog.addCheckbox("Output floating point Index image?", true);
        dialog.addChoice("Select output color table for color Index image", lutNames, lutNames[0]);
        dialog.addCheckbox("Save parameters for next session", false);
        dialog.addDialogListener((DialogListener)this);
        dialog.showDialog();
        if (dialog.wasCanceled()) {
          IJ.log("Goodbye!");
            return null;
        }
    }
    if (useDefaults.booleanValue()) {
        dialog.getNextBoolean();
    }

    return dialog;

  }



  /*
   * Print to ImageJ log when the DEBUG flag is set to true
   * @param str   string to print to imagej log
   * @return      void
   */
  public void DebugPrint(String str){
    if( DEBUG ){
      IJ.log(str);
    }
  }

  // @Override
  public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
      Checkbox IndexColorCheckbox = (Checkbox)gd.getCheckboxes().get(1);
      Vector numericChoices = gd.getNumericFields();
      Vector choices = gd.getChoices();
      if (IndexColorCheckbox.getState()) {
          ((TextField)numericChoices.get(0)).setEnabled(true);
          ((TextField)numericChoices.get(1)).setEnabled(true);
          ((Choice)choices.get(1)).setEnabled(true);
      } else {
          ((TextField)numericChoices.get(0)).setEnabled(false);
          ((TextField)numericChoices.get(1)).setEnabled(false);
          ((Choice)choices.get(1)).setEnabled(false);
      }
      return true;
  }

  /*
   * Return the path string to the calibration file user wants to user
   * @param none
   * @return    Absolute path string of calibration file user chooses.
   *            Null if no path is specified
   */
  public String getCalibDir(String arg){
    OpenDialog od = new OpenDialog("Select calibration file", arg);
    String calibrationDirectory = od.getDirectory();
    String calibrationFileName = od.getFileName();
    if (calibrationFileName == null) {
        IJ.error((String)"No file was selected");
    }

    return calibrationDirectory+calibrationFileName;
  }

  /*
   * Return path string of image input directory to calibrated
   * @param none
   * @return  absoulue path string of image input directory to calibrated.
   *          Null if no path is specified
   */
  public String getInputDir(){
    DirectoryChooser inDirChoose = new DirectoryChooser("Input image directory");
    String inDir = inDirChoose.getDirectory();
    if (inDir == null) {
        IJ.error((String)"Input image directory was not selected");
    }

    return inDir;
  }

  /*
   * Return path string of image output directory of calibrated images
   * @param none
   * @return  absoulue path string of image output directory of calibrated images.
   *          Null if no path is specified
   */
  public String getOutputDir(){
    SaveDialog sd = new SaveDialog("Output directory and log file name", "log", ".txt");
    String outDirectory = sd.getDirectory();
    this.logName = sd.getFileName();
    if (logName == null) {
        IJ.error((String)"No directory was selected");
    }

    return outDirectory;
  }


}
