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
import java.util.*;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;


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
  private String[] calibDir = null;
  private String inputDir = null;
  private String outputDir = null;
  private File[] inputFiles = null;
  private List<File> inputImages = null;


  private double[] calibrationCoefs = new double[4];
  private Boolean subtractNIR = null;
  private double percentToSubtract = 0;
  private Boolean removeGamma = null;
  private double gamma = 0;
  private int visBand = 0;
  private int nirBand = 0;
  private ImagePlus indexImage = null;
  private ImagePlus colorIndex = null;

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
      DebugPrint("Calibration file: " + calibDir[0]+calibDir[1]);
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

    // Get inputDir files and filter out only the images
    String[] fsplit = null;
    inputFiles = new File(inputDir).listFiles();

    for( int i=0; i<inputFiles.length; i++ ){
      DebugPrint(inputFiles[i].getAbsolutePath());
      getImagesFromFiles( inputFiles[i], inputImages );
    }
    for( int i=0; i<inputImages.size(); i++ ){
      DebugPrint( inputImages.get(i).getName() );
    }
    if( inputImages.size() == 0 ){
      IJ.log( "No images found. Please choose a directory with images to calibrate" );
      return;
    }

    // Set calibration values from calibration file
    setCalibrationValues(calibDir[0]+calibDir[1]); //@TODO failsafe if calib file could not be read
    printCalibrationValues();
    saveCalibrationFile(outputDir, this.logName, calibDir[1] );

    // Begin processing
    this.percentToSubtract /= 100.0;
    ImagePlus inImagePlus = null;
    String imagefilepath = null;
    String imagefile = null;
    String[] imagenameparts = null;
    double visPixel = 0.0;
    double nirPixel = 0.0;

    Iterator<File> imageIterator = inputImages.iterator();
    File fimage = null;

    while( imageIterator.hasNext() ){
      fimage = imageIterator.next();
      imagefilepath = fimage.getAbsolutePath();
      imagefile = fimage.getName();
      inImagePlus = new ImagePlus( imagefilepath );

      if( inImagePlus == null ){
        IJ.log("Could not open image: " + imagefile + ". I will skip it.");
        continue;
      }
      IJ.log("Processing image: " + imagefilepath );

      // Get filename and extension
      imagenameparts = splitFilename(imagefile);

      inImagePlus.show();
      visPixel = 0.0;
      nirPixel = 0.0;
      DebugPrint( "Channels: " + Integer.toString(inImagePlus.getNChannels()) );

      if( inImagePlus.getNChannels() == 1 ){
        inImagePlus = new CompositeImage(inImagePlus);
      }
      IJ.log("Splitting Channels...");
      ImagePlus[] imageBands = ChannelSplitter.split((ImagePlus)inImagePlus);
      IJ.log("Scaling Image...");
      ImagePlus visImage = this.scaleImage(imageBands[visBand], "visImage");
      ImagePlus nirImage = this.scaleImage(imageBands[nirBand], "nirImage");

      if( removeGamma ){
        IJ.log("Removing Gamma...");
        removeGamma(visImage, nirImage, visPixel, nirPixel);
      }

      if( subtractNIR ){
        IJ.log("Subtracting NIR...");
        subtractNIR(visImage, nirImage, visPixel, nirPixel);
      }


      // Make DVI or NDVI
      if (indexType == indexTypes[0]) {
          indexImage = this.makeNDVI(visImage, nirImage, calibrationCoefs);
          indexImage.show();
      } else if (indexType == indexTypes[1]) {
          indexImage = this.makeDVI(visImage, nirImage, calibrationCoefs);
      }

      // Save image step
      if( createIndexFloat.booleanValue() ){
        IJ.log("Creating Index Float image");
        createIndexFloat(outputDir, imagenameparts[0], indexType, indexImage, imagenameparts[1]);
        IJ.log("Saved");
      }

      File[] savedIndexColorImages = null;
      if( createIndexColor.booleanValue() ){
        IJ.log("Creating Index Color");
        createIndexColor(this.colorIndex, indexImage, indexType, indexTypes, lutLocation, lutName, minColorScale, maxColorScale);
        savedIndexColorImages = saveIndexColor(outputDir, imagenameparts[0], imagenameparts[1], this.colorIndex, indexType, indexTypes);
        IJ.log("Saved");
      }

      // Clean up and EXIF data copy
      IJ.run((String)"Close All");

      IJ.log("Writting EXIF data...");
      WriteEXIF exifWriter = new WriteEXIF(fimage, savedIndexColorImages[0] , savedIndexColorImages[1]);
      exifWriter.copyEXIF();
    }

    IJ.log("Done processing!");

  }

  /*
   * Setup directories that will be used
   * @param none
   * @return void
   */
  public void setup(){
    WorkingDirectory = IJ.getDirectory("imagej");
    lutLocation = IJ.getDirectory("luts");
    lutNames = (new File(lutLocation)).list();

    DebugPrint("---------------- Luts Detected ------------------");
    for( int i=0; i<lutNames.length; i++ ){
      DebugPrint(lutNames[i]);
    }

    inputImages = new ArrayList<File>();

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
  public String[] getCalibDir(String arg){
    OpenDialog od = new OpenDialog("Select calibration file", arg);
    String calibrationDirectory = od.getDirectory();
    String calibrationFileName = od.getFileName();
    if (calibrationFileName == null) {
        IJ.error((String)"No file was selected");
    }
    String[] calibDir = {calibrationDirectory, calibrationFileName};
    return calibDir;
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

  /*
   * Splits the filename path into filename and extension. For example
   * MyImage.jpg is split into MyImage and jpg
   * @param filename  The filename to split
   * @return          String array where first index contains the filename
   *                  and second index contains extension.
   *                  Return null if could not be split or extension could not be found
   */
  public String[] splitFilename(String filename){
    String[] fnsplit = filename.split("\\.(?=[^\\.]+$)");
    String[] split = null;

    if( fnsplit.length == 2 ){
      // filename split successfully into filename and extension
      split = fnsplit;
    }

    return split;
  }

  /*
   * Add image fils to the List. Essentially, it filters out non-image-based files.
   * @param fname   File object of the filename to filters
   * @param imgList List to add the image to when found
   * @return        Function returns void. However, imgList should be populated
   *                with Files that link to image-based-files
   */
  public void getImagesFromFiles(File fname, List<File> imglist ){
    String[] fsplit = splitFilename( fname.getName() );
    String ext = null;

    if( fsplit == null ){
      IJ.log("Could not split: " + fname.getName() + ". I am skipping it.");
    }else{
      ext = fsplit[1].toUpperCase();
      if( ext.equals("JPG") ){
        imglist.add(fname);
      }else if( ext.equals("PNG") ){
        imglist.add(fname);
      }else if( ext.equals("TIF") ){
        imglist.add(fname);
      }else if( ext.equals("RAW") ){
        imglist.add(fname);
      }

    }
    return;
  }

  /*
   * Set working calibration values
   * @param calibfile   calibration file to read values from
   * @return none
   */
  public void setCalibrationValues(String calibfile){
    BufferedReader fileReader = null;
    try {
      String fullLine = "";
      fileReader = new BufferedReader(new FileReader(calibfile));
      int counter = 1;
      while ((fullLine = fileReader.readLine()) != null) {
          String[] dataValues;
          if (counter == 8) {
              dataValues = fullLine.split(":");
              this.calibrationCoefs[0] = Double.parseDouble(dataValues[1]);
          }
          if (counter == 9) {
              dataValues = fullLine.split(":");
              this.calibrationCoefs[1] = Double.parseDouble(dataValues[1]);
          }
          if (counter == 11) {
              dataValues = fullLine.split(":");
              this.calibrationCoefs[2] = Double.parseDouble(dataValues[1]);
          }
          if (counter == 12) {
              dataValues = fullLine.split(":");
              this.calibrationCoefs[3] = Double.parseDouble(dataValues[1]);
          }
          if (counter == 14) {
              dataValues = fullLine.split(":");
              this.subtractNIR = Boolean.parseBoolean(dataValues[1]);
          }
          if (counter == 15) {
              dataValues = fullLine.split(":");
              this.percentToSubtract = Double.parseDouble(dataValues[1]);
          }
          if (counter == 16) {
              dataValues = fullLine.split(":");
              this.removeGamma = Boolean.parseBoolean(dataValues[1]);
          }
          if (counter == 17) {
              dataValues = fullLine.split(":");
              this.gamma = Double.parseDouble(dataValues[1]);
          }
          if (counter == 19) {
              dataValues = fullLine.split(":");
              this.visBand = Integer.parseInt(dataValues[1].trim()) - 1;
          }
          if (counter == 20) {
              dataValues = fullLine.split(":");
              this.nirBand = Integer.parseInt(dataValues[1].trim()) - 1;
          }
          ++counter;
      }
    }catch (Exception e) {
        IJ.error((String)"Error reading calibration coefficient file", (String)e.getMessage());
        try {
            fileReader.close();
        }
        catch (IOException f) {
            f.printStackTrace();
        }
        return;
    }finally {
      try {
          fileReader.close();
      }catch (IOException e) {
          e.printStackTrace();
      }
    }

    return;
  }

  /*
   * Save calibration settings to text fileReader
   * @param outDirectory          output directory to save calibration settings to
   * @param logName               name of the settings fileReader
   * @param calibrationFileName   name of the calibration file used
   * @return  none
   */
  public void saveCalibrationFile(String outDirectory, String logName, String calibrationFileName){
    try {
        BufferedWriter bufWriter = new BufferedWriter(new FileWriter(String.valueOf(outDirectory) + logName));
        bufWriter.write("PARAMETER SETTINGS:\n");
        bufWriter.write("File name for calibration coeficients: " + calibrationFileName + "\n");
        bufWriter.write("Select index type for calculation: " + this.indexType + "\n\n");
        bufWriter.write("Output Color Index image? " + this.createIndexColor + "\n");
        bufWriter.write("Minimum Index value for scaling color Index image: " + this.minColorScale + "\n");
        bufWriter.write("Maximum Index value for scaling color Index image: " + this.maxColorScale + "\n");
        bufWriter.write("Output floating point Index image? " + this.createIndexFloat + "\n");
        bufWriter.write("Channel from visible image to use for Red band to create Index: " + (this.visBand + 1) + "\n");
        bufWriter.write("Channel from IR image to use for IR band to create Index: " + (this.nirBand + 1) + "\n");
        bufWriter.write("Subtract NIR from visible?" + this.subtractNIR + "\n");
        bufWriter.write("Percent of NIR to subtract: " + this.percentToSubtract + "\n");
        bufWriter.write("Remove gamma effect? " + this.removeGamma + "\n");
        bufWriter.write("Gammafactor: " + this.gamma + "\n");
        bufWriter.write("Visible band: " + (this.visBand + 1) + "\n");
        bufWriter.write("Near-infrared band: " + (this.nirBand + 1) + "\n");
        bufWriter.write("Select output color table for color Index image: " + this.lutName + "\n\n");
        bufWriter.close();
    }
    catch (Exception e) {
        IJ.error((String)"Error writing log file", (String)e.getMessage());
        return;
    }
  }

  /*
   * Print calibration values in effect
   * @param   none
   * @return  none
   */
  public void printCalibrationValues(){
    IJ.log("Calibration Coefficient 1: " + Double.toString(this.calibrationCoefs[0]) );
    IJ.log("Calibration Coefficient 2: " + Double.toString(this.calibrationCoefs[1]) );
    IJ.log("Calibration Coefficient 3: " + Double.toString(this.calibrationCoefs[2]) );
    IJ.log("Calibration Coefficient 4: " + Double.toString(this.calibrationCoefs[3]) );

    IJ.log( "subtractNIR: " + this.subtractNIR );
    IJ.log( "percentToSubtract: " + Double.toString(this.percentToSubtract) );
    IJ.log( "removeGamma: " + this.removeGamma );
    IJ.log( "gamma: " +  this.gamma );
    IJ.log( "visBand: " + this.visBand );
    IJ.log( "nirBand: " + this.nirBand );
  }

  /*
   * Scale input imageName
   * @param inImage   image to scale
   * @param imageName image filename
   * @return scaled inImage
   */
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
   * Apply gamma removal to ImagePlus
   * @param visImage  visImage of ImagePlus
   * @param nirImage   nirImage of ImagePlus
   * @param visPixel
   * @param nirPixel
   * @return Image should have gamma removed
   */

  public void removeGamma(ImagePlus visImage, ImagePlus nirImage, double visPixel, double nirPixel){
    double undoGamma = 1.0 / gamma;
    int y = 0;
    while (y < nirImage.getHeight()) {
        int x = 0;
        while (x < nirImage.getWidth()) {
            nirPixel = Math.pow(nirImage.getProcessor().getPixelValue(x, y), undoGamma);
            visPixel = Math.pow(visImage.getProcessor().getPixelValue(x, y), undoGamma);
            visImage.getProcessor().putPixelValue(x, y, visPixel);
            nirImage.getProcessor().putPixelValue(x, y, nirPixel);
            ++x;
        }
        ++y;
    }
    return;
  }

  /*
   * Apply subtractNIR
   * @param visImage
   * @param nirImage  nir image to subtractNIR
   * @param visPixel
   * @param nirPixel
   * @return          none
   */
  public void subtractNIR(ImagePlus visImage, ImagePlus nirImage, double visPixel, double nirPixel){
    int y = 0;
    while (y < nirImage.getHeight()) {
        int x = 0;
        while (x < nirImage.getWidth()) {
            nirPixel = nirImage.getProcessor().getPixelValue(x, y);
            visPixel = (double)visImage.getProcessor().getPixelValue(x, y) - percentToSubtract * nirPixel;
            visImage.getProcessor().putPixelValue(x, y, visPixel);
            ++x;
        }
        ++y;
    }
    return;
  }

  public ImagePlus makeNDVI(ImagePlus visImage, ImagePlus nirImage, double[] calibrationCeofs) {
      double outPixel = 0.0;
      ImagePlus newImage = NewImage.createFloatImage((String)"ndviImage", (int)nirImage.getWidth(), (int)nirImage.getHeight(), (int)1, (int)1);
      int y = 0;
      while (y < nirImage.getHeight()) {
          int x = 0;
          while (x < nirImage.getWidth()) {
              double visPixel;
              double nirPixel = (double)nirImage.getProcessor().getPixelValue(x, y) * calibrationCeofs[3] + calibrationCeofs[2];
              if (nirPixel + (visPixel = (double)visImage.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0]) == 0.0) {
                  outPixel = 0.0;
              } else {
                  outPixel = (nirPixel - visPixel) / (nirPixel + visPixel);
                  if (outPixel > 1.0) {
                      outPixel = 1.0;
                  }
                  if (outPixel < -1.0) {
                      outPixel = -1.0;
                  }
              }
              newImage.getProcessor().putPixelValue(x, y, outPixel);
              ++x;
          }
          ++y;
      }
      return newImage;
  }

  public ImagePlus makeDVI(ImagePlus visImage, ImagePlus nirImage, double[] calibrationCeofs) {
      double outPixel = 0.0;
      ImagePlus newImage = NewImage.createFloatImage((String)"ndviImage", (int)nirImage.getWidth(), (int)nirImage.getHeight(), (int)1, (int)1);
      int y = 0;
      while (y < nirImage.getHeight()) {
          int x = 0;
          while (x < nirImage.getWidth()) {
              double nirPixel = (double)nirImage.getProcessor().getPixelValue(x, y) * calibrationCeofs[3] + calibrationCeofs[2];
              double visPixel = (double)visImage.getProcessor().getPixelValue(x, y) * calibrationCeofs[1] + calibrationCeofs[0];
              outPixel = nirPixel - visPixel;
              newImage.getProcessor().putPixelValue(x, y, outPixel);
              ++x;
          }
          ++y;
      }
      newImage.show();
      return newImage;
  }

  public void createIndexFloat(String outDirectory, String outFileBase, String indexType, ImagePlus indexImage, String extension){
    String NDVIAppend = "_NDVI_Float";
    String DVIAppend = "_DVI_Float";

    DebugPrint("Output Directory:" + outDirectory);
    DebugPrint("Filename: " + outFileBase);
    DebugPrint("Index Type: " + indexType);
    DebugPrint("Save as extension: " + extension);

    if ( indexType.equals(indexTypes[0]) ) {
        IJ.save((ImagePlus)indexImage, (String)(String.valueOf(outDirectory) + outFileBase + NDVIAppend + "." + extension));
    } else if (indexType == indexTypes[1]) {
        IJ.save((ImagePlus)indexImage, (String)(String.valueOf(outDirectory) + outFileBase + DVIAppend + "." + extension));
    }
    return;
  }

  public void createIndexColor(ImagePlus colorIndex, ImagePlus indexImage, String indexType, String[] indexTypes, String lutLocation, String lutName, double minColorScale, double maxColorScale ){
    DebugPrint("Index Type: " + indexType);
    DebugPrint("Lut Location: " + lutLocation);
    DebugPrint("Lut Name: " + lutName);
    DebugPrint("Min Color Scale: " + Double.toString(minColorScale));
    DebugPrint("Max Color Scale: " + Double.toString(maxColorScale));


    IndexColorModel cm = null;
    colorIndex = null;

    if (indexType == indexTypes[0]) {
        colorIndex = NewImage.createByteImage((String)"Color NDVI", (int)indexImage.getWidth(), (int)indexImage.getHeight(), (int)1, (int)1);
    } else if (indexType == indexTypes[1]) {
        colorIndex = NewImage.createByteImage((String)"Color DVI", (int)indexImage.getWidth(), (int)indexImage.getHeight(), (int)1, (int)1);
    }

    float[] pixels = (float[])indexImage.getProcessor().getPixels();
    int y = 0;
    while (y < indexImage.getHeight()) {
        int offset = y * indexImage.getWidth();
        int x = 0;
        while (x < indexImage.getWidth()) {
            int pos = offset + x;
            colorIndex.getProcessor().putPixelValue(x, y, (double)Math.round(((double)pixels[pos] - minColorScale) / ((maxColorScale - minColorScale) / 255.0)));
            ++x;
        }
        ++y;
    }

    try {
        cm = LutLoader.open( (String)(String.valueOf(lutLocation) + lutName) );
    }catch (IOException e) {
        //IJ.error((String)((Object)e));
        e.printStackTrace();
    }

    if( cm == null ){
      IJ.log("Could not open LUT file.");
      return;
    }
    LUT lut = new LUT(cm, 255.0, 0.0);
    colorIndex.getProcessor().setLut(lut);
    colorIndex.show();

  }

  public File[] saveIndexColor(String outDirectory, String outFileBase, String inImageExt, ImagePlus colorIndex, String indexType, String[] indexTypes){
    DebugPrint("Index Type: " + indexType);
    DebugPrint("Save Directory: " + outDirectory);
    DebugPrint("Save Filename: " + outFileBase);
    DebugPrint("Save Image Extension: " + inImageExt);

    String tempFileName = String.valueOf(outDirectory) + outFileBase + "IndexColorTemp." + inImageExt;
    File tempFile = new File(tempFileName);
    IJ.save((ImagePlus)colorIndex, (String)tempFileName);
    File outFile = null;
    if (indexType == indexTypes[0]) {
        outFile = new File(String.valueOf(outDirectory) + outFileBase + "_NDVI_Color." + inImageExt);
    } else if (indexType == indexTypes[1]) {
        outFile = new File(String.valueOf(outDirectory) + outFileBase + "_DVI_Color." + inImageExt);
    }

    File[] outputs = {outFile, tempFile};
    return outputs;
  }





  public class WriteEXIF {
      File outImageFile = null;
      File originalJpegFile = null;
      File tempImageFile = null;

      public WriteEXIF(File originalJpegFile, File outImageFile, File tempImageFile) {
          this.originalJpegFile = originalJpegFile;
          this.outImageFile = outImageFile;
          this.tempImageFile = tempImageFile;
      }

      public void copyEXIF() {
          OutputStream os = null;
          TiffOutputSet outputSet = null;
          JpegImageMetadata jpegMetadata = null;
          TiffImageMetadata tiffMetadata = null;
          String extension = this.originalJpegFile.getName().substring(this.originalJpegFile.getName().lastIndexOf(".") + 1, this.originalJpegFile.getName().length());
          try {
              IImageMetadata metadata = Sanselan.getMetadata((File)this.originalJpegFile);
              if (metadata instanceof JpegImageMetadata) {
                  jpegMetadata = (JpegImageMetadata)metadata;
              } else if (metadata instanceof TiffImageMetadata) {
                  tiffMetadata = (TiffImageMetadata)metadata;
              }
              if (extension.equals("tif".toLowerCase())) {
                  this.tempImageFile.renameTo(this.outImageFile);
                  this.tempImageFile.delete();
              } else if (jpegMetadata != null) {
                  TiffImageMetadata exif = jpegMetadata.getExif();
                  if (exif != null) {
                      outputSet = exif.getOutputSet();
                  }
                  os = new FileOutputStream(this.outImageFile);
                  os = new BufferedOutputStream(os);
                  new ExifRewriter().updateExifMetadataLossless(this.tempImageFile, os, outputSet);
                  this.tempImageFile.delete();
              } else if (tiffMetadata != null) {
                  outputSet = tiffMetadata.getOutputSet();
                  List tiffList = tiffMetadata.getAllFields();
                  ArrayList dirList = new ArrayList();
                  dirList = tiffMetadata.getDirectories();
                  outputSet = new TiffOutputSet();
                  for (Object field : tiffMetadata.getAllFields()) {
                      if (!(field instanceof TiffField)) continue;
                      TiffField tiffField = (TiffField)field;
                      System.out.println(String.valueOf(tiffField.getTagName()) + ": " + tiffField.getValueDescription() + " : " + tiffField.length);
                  }
                  os = new FileOutputStream(this.outImageFile);
                  os = new BufferedOutputStream(os);
                  new ExifRewriter().updateExifMetadataLossy(this.tempImageFile, os, outputSet);
                  this.tempImageFile.delete();
              } else {
                  this.tempImageFile.renameTo(this.outImageFile);
              }
          }
          catch (ImageWriteException e) {
              e.printStackTrace();
              IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
          }
          catch (FileNotFoundException e) {
              e.printStackTrace();
              IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
          }
          catch (IOException e) {
              e.printStackTrace();
              IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
          }
          catch (ImageReadException e) {
              e.printStackTrace();
              IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
          }
      }
  }


}
