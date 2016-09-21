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
import ij.plugin.ContrastEnhancer;
import ij.io.FileSaver;


import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.*;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class CalibrateDirectory implements PlugIn{

  private boolean useQR = false;
  private boolean tifsToJpgs = false;
  private boolean continueAnyway = false;

  private String qrJPGDir = "";
  private String qrJPGFilename = "";
  private String qrJPGPath = "";

  private String qrTIFDir = "";
  private String qrTIFFilename = "";
  private String qrTIFPath = "";

  private String qrCamera = "";

  private String cameraType = "";
  private RGBPhoto qrJPGPhoto = null;
  private RGBPhoto qrTIFPhoto = null;
  private RGBPhoto qrJPGScaled = null;
  private RGBPhoto qrTIFScaled = null;

  private String inputDir = "";
  private String outputDir = "";
  private String jpgOutputDir = "";
  private String tifOutputDir = "";

  private final String CALIBRATEDSAVEFOLDER = "Calibrated";

  private File fileInputDir = null;
  private File fileOutputDir = null;
  private File[] imagesToCalibrate = null;
  private List<File> jpgToCalibrate = null;
  private List<File> tifToCalibrate = null; // Not being used - they all go into jpgToCalibrate

  private EXIFTool exif = null;

  private boolean keepPluginAlive = true;

  private Debugger debugger = Debugger.getInstance();

  // {Intercept, Slope}
  // @TODO Update this
  // @TODO Create progress dialog for user
  private final double[] BASE_COEFF_SURVEY2_RED_JPG = {-2.55421832, 16.01240929};//
  private final double[] BASE_COEFF_SURVEY2_GREEN_JPG = {-0.60437250, 4.82869470};//
  private final double[] BASE_COEFF_SURVEY2_BLUE_JPG = {-0.39268985, 2.67916884};//
  private final double[] BASE_COEFF_SURVEY2_NDVI_JPG = {-0.29870245, 6.51199915, -0.65112026, 10.30416005};//
  private final double[] BASE_COEFF_SURVEY2_NIR_JPG = {-0.46967653, 7.13619139};//

  private final double[] BASE_COEFF_SURVEY1_NDVI_JPG = {0.0, 0.0, 0.0, 0.0};
  private final double[] BASE_COEFF_SURVEY1_NDVI_TIF = {0.0, 0.0, 0.0, 0.0};

  private final double[] BASE_COEFF_SURVEY2_RED_TIF = {-5.09645820, 0.24177528};//
  private final double[] BASE_COEFF_SURVEY2_GREEN_TIF = {-1.39528479, 0.07640011};//
  private final double[] BASE_COEFF_SURVEY2_BLUE_TIF = {-0.67299134, 0.03943339};//
  private final double[] BASE_COEFF_SURVEY2_NDVI_TIF = {-0.60138990, 0.14454211, -3.51691589, 0.21536524};//
  private final double[] BASE_COEFF_SURVEY2_NIR_TIF = {-2.24216724, 0.12962333};//

  // @TODO: We don't have reliable calibration coefficients. Tell user this in prompt.
  private final double[] BASE_COEFF_DJIX3_NDVI_JPG = {-0.34430543, 4.63184993, -0.49413940, 16.36429964};//
  private final double[] BASE_COEFF_DJIX3_NDVI_TIF = {-0.74925346, 0.01350319, -0.77810008, 0.03478272};//

  private final double[] BASE_COEFF_DJIPHANTOM4_NDVI_JPG = {-1.17016961, 0.03333209, -0.99455214, 0.05373502};
  private final double[] BASE_COEFF_DJIPHANTOM4_NDVI_TIF = {-1.17016961, 0.03333209, -0.99455214, 0.05373502}; //

  private final double[] BASE_COEFF_DJIPHANTOM3_NDVI_JPG = {-1.54494979, 3.44708472, -1.40606832, 6.35407929};//
  private final double[] BASE_COEFF_DJIPHANTOM3_NDVI_TIF = {-1.37495554, 0.01752340, -1.41073753, 0.03700812};//

  private final double[] BASE_COEFF_GOPROHERO4_NDVI = {0.0,0.0};

  private String OS = System.getProperty("os.name");
  private String WorkingDirectory = IJ.getDirectory("imagej");
  private String PATH_TO_VALUES = WorkingDirectory+"Calibration\\values.csv";
  private String PATH_TO_EXIFTOOL = WorkingDirectory+"Survey2\\EXIFTool\\";

  private int jpegQuality = 0;

  private boolean thereAreJPGs = false;
  private boolean thereAreTIFs = false;

  private final String VERSION = "1.3.4";
  private final boolean DEBUG = false;

  public enum channelNumbers {
      REDMAX,
      REDMIN,
      GREENMAX,
      GREENMIN,
      BLUEMAX,
      BLUEMIN
  }

  public void run(String arg){
      IJ.log("Build: " + VERSION);
      debugger.DEBUGMODE = false;

      jpegQuality = FileSaver.getJpegQuality();
      FileSaver.setJpegQuality(100);

    while( keepPluginAlive ){
      Calibrator calibrator = new Calibrator();
      Roi[] jpgrois = null;
      Roi[] tifrois = null;
      CalibrationPrompt prompts = new CalibrationPrompt();
      prompts.showFullDialog();
      if( prompts.wasCanceledFullDialog() ){
        IJ.log("Goodbye!");
        return;
      }



      jpgToCalibrate = new ArrayList<File>();
      tifToCalibrate = new ArrayList<File>();


      HashMap<String, String> fullDialogValues = prompts.getFullDialogValues();
      HashMap<String, String> qrFileDialogValues = null;
      useQR = Boolean.parseBoolean( fullDialogValues.get(CalibrationPrompt.MAP_USEQR) );
      //IJ.log(fullDialogValues.get(CalibrationPrompt.MAP_USEQR));
      //IJ.log( String.valueOf(useQR) );
      cameraType = fullDialogValues.get(CalibrationPrompt.MAP_CAMERA);
      tifsToJpgs = Boolean.parseBoolean( fullDialogValues.get(CalibrationPrompt.MAP_TIFFTOJPG) );

      String qrCameraModel = null;

      prompts.showImageFileDialog();
      HashMap<String, String> imageFileDialogValues = prompts.getImageFileDialogValues();
      inputDir = imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR);

      if( inputDir == null ){
        // User selected cancel
        IJ.log("Goodbye!");
        return;
      }
      outputDir = inputDir + "\\"+CALIBRATEDSAVEFOLDER+"\\";

      // Resolve output directory
      int numCalibratedFolders = calibratedFolderExists(inputDir, CALIBRATEDSAVEFOLDER);
      if( numCalibratedFolders == 0 ){
        File out = new File(outputDir);
        if( out.exists() ){
          outputDir = inputDir + "\\" + CALIBRATEDSAVEFOLDER + "_" + Integer.toString(1) + "\\";
        }
      }
      else if( numCalibratedFolders > 0 ){
        outputDir = inputDir + "\\" + CALIBRATEDSAVEFOLDER + "_" + Integer.toString(numCalibratedFolders+1) + "\\";
      }

      jpgOutputDir = outputDir + "\\JPG\\";
      tifOutputDir = outputDir + "\\TIF\\";

      // Scan for calibrated folder

      // Create output Folder





      //IJ.log("I will begin to process the images in ");
      // Get all images to process
      fileInputDir = new File(inputDir);
      imagesToCalibrate = fileInputDir.listFiles();

      for( int i=0; i<imagesToCalibrate.length; i++ ){
        String[] inImageParts = (imagesToCalibrate[i].getName()).split("\\.(?=[^\\.]+$)");
        String inImageExt = null;

        if( inImageParts.length < 2){
          continue;
        }else{
          inImageExt = inImageParts[1];
        }

        if( inImageExt.toUpperCase().equals("JPG") || inImageExt.toUpperCase().equals("TIF") || inImageExt.toUpperCase().equals("DNG") ){
          if( tifsToJpgs && inImageExt.toUpperCase().equals("JPG") ){
            // Don't add original jpgs, only tifs
            thereAreJPGs = false;
            continue;
          }
          jpgToCalibrate.add(imagesToCalibrate[i]);

          // Set flags indicating which image extensions are going to be calibrated
          if( inImageExt.toUpperCase().equals("JPG") ){
            thereAreJPGs = true;
          }

          if( inImageExt.toUpperCase().equals("TIF") ){
            thereAreTIFs = true;
          }

        }


      }



      // Ask for QR images and load them
      RoiManager manager = RoiManager.getInstance();
      if( manager != null ){
          manager.reset();
          manager.close();
      }
      while( useQR == true ){

        if( !tifsToJpgs ){
          // User want to calibrate JPGs present in directory
          if( thereAreJPGs ){

            prompts.showQRJPGFileDialog();
            qrFileDialogValues = prompts.getQRFileDialogValues();
            qrJPGDir = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR);
            qrJPGPath = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEPATH);
            qrJPGFilename = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEFILENAME);

            // Check if user wants to quit program
            if(thereAreJPGs && !tifsToJpgs && qrJPGDir == null){
              // User hit cancel
              IJ.log("Goodbye!");
              return;
            }
            IJ.log("Detecting QR code. Please Wait.");

            qrJPGPhoto = new RGBPhoto(qrJPGDir, qrJPGFilename, qrJPGPath, cameraType, false);
            qrCameraModel = GetEXIFCameraModel("Windows", PATH_TO_EXIFTOOL, qrJPGPath);

            if( !qrJPGPhoto.checkChannels() ){
              IJ.log("There was a problem opening this calibration target image. Please choose another one.");
              continue;
            }

            qrJPGScaled = calibrator.scaleChannels(qrJPGPhoto);
            if( qrJPGPhoto.getCameraType().equals(CalibrationPrompt.SURVEY2_NDVI) ){
            //TODO If a packet hits a pocket
              calibrator.subtractNIR(qrJPGScaled.getBlueChannel(), qrJPGScaled.getRedChannel(), 80 );
            }

            //qrJPGScaled = new RGBPhoto(qrJPGPhoto);
            jpgrois = calibrator.getRois(qrJPGPhoto.getImage());

            if( debugger.getDebugMode() ){
                qrJPGPhoto.show();
                qrJPGScaled.show();
            }

            if( jpgrois == null ){
              //IJ.log("ATTN: QR calibration targets could not be found. I will use default coefficient values.");
              try{

                if( prompts.showQRNotDetectedDialog() ){
                  useQR = true;
                  continue;
                }else{
                  useQR = false;
                }

              }catch(IOException io){
                IJ.log("Goodbye!");
                return;
              }
            }else{
              if( !thereAreTIFs ){
                // Don't ask for Tif QR bc we are not calibrating tifs
                break;
              }
              // QR code found!
            }

          }

        }

        if( thereAreTIFs ){
          prompts.showQRTIFFileDialog();
          qrFileDialogValues = prompts.getQRFileDialogValues();
          qrTIFDir = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR);
          qrTIFPath = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEPATH);
          qrTIFFilename = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEFILENAME);

          if(thereAreTIFs && qrTIFDir == null){
            // User hit cancel
            IJ.log("Goodbye!");
            return;
          }

          IJ.log("Detecting QR code. Please Wait.");
          qrTIFPhoto = new RGBPhoto(qrTIFDir, qrTIFFilename, qrTIFPath, cameraType, true);
          qrCameraModel = GetEXIFCameraModel("Windows", PATH_TO_EXIFTOOL, qrTIFPath);

          if( !qrTIFPhoto.checkChannels() ){
            IJ.log("There was a problem opening this calibration target image. Please choose another one.");
            continue;
          }

          //qrTIFScaled = calibrator.scaleChannels(qrTIFPhoto);
          qrTIFScaled = new RGBPhoto(qrTIFDir, qrTIFFilename, qrTIFPath, cameraType, true);
          if( qrTIFPhoto.getCameraType().equals(CalibrationPrompt.SURVEY2_NDVI) ){
            //TODO If a packet hits a pocket
            calibrator.subtractNIR(qrTIFScaled.getBlueChannel(), qrTIFScaled.getRedChannel(), 80 );
          }
          if( qrTIFPhoto.getCameraType().equals(CalibrationPrompt.DJIPHANTOM4_NDVI) ){
            //TODO If a packet hits a pocket
              calibrator.subtractNIR(qrTIFScaled.getBlueChannel(), qrTIFScaled.getRedChannel(), 80);
          }
          // Enhance for better QR detection
          //(new ContrastEnhancer()).equalize(qrTIFPhoto.getImage());
          tifrois = calibrator.getRois(qrTIFPhoto.getImage());

          if( tifrois == null ){
            //IJ.log("ATTN: QR calibration targets could not be found. I will use default coefficient values.");
            try{

              if( prompts.showQRNotDetectedDialog() ){
                useQR = true;
                continue;
              }else{
                if (cameraType.equals(CalibrationPrompt.DJIX3_NDVI)){
                    IJ.log("ATTENTION: We currently do not have base calibration values for the DJI X3. You must supply a calibation target to proceed.");
                    IJ.log("The plugin will now terminate. Goodbye!");
                    return;
                }
                useQR = false;
              }

            }catch(IOException io){
              IJ.log("Goodbye!");
              return;
            }
          }else{
            // QR code found!
            break;
          }

        }

        // Check if correct camera model QR type found
        if( !qrCameraModel.equals(cameraType) && useQR ){
          if( prompts.showQRNotSameModelDialog(qrCameraModel, cameraType) ){
            // Ask for QR again
            useQR = true;
            continue;
          }else{
            // Quit was selected
            useQR = false;
            IJ.log("Goodbye!");
            return;
          }
        }
    }







      //IJ.log("---------------Images To Calibrate---------------");
      /*
      for( int i=0; i<jpgToCalibrate.size(); i++ ){
        IJ.log(jpgToCalibrate.get(i).getName());
      }
      for( int i=0; i<tifToCalibrate.size(); i++ ){
        //IJ.log(tifToCalibrate.get(i).getName());
      }*/

      IJ.log("I will begin processing the " + jpgToCalibrate.size() + " images that were found.");

      // @TODO Only show this for tifs (not for jpgs bc its fine for jpgs only)
      if( thereAreTIFs ){
          IJ.showMessage("Attention! During the calibration process, windows will popup.\n" +
          "This is part of the calibration process.\n" +
          "Please do not disturb these windows as it might disrupt the calibration process.");
      }

      // Load in photo (only after calibration coefficients have been prepared)
      RGBPhoto photo = null;
      RGBPhoto resultphoto = null;

      Iterator<File> pixelIterator = jpgToCalibrate.iterator();

      double[] dirMinMaxes = new double[18];
      //dirMinMaxes[0-5]: Tiff minimum and maximum channel values
      //dirMinMaxes[6-11]: JPG minimum and maximum channel values
      //dirMinMaxes[12-17]: DNG minimum and maximum channel values
      File tmpfile = null;

      //OpenDialog baseFileDialog = new OpenDialog("Select Base File");
      //File bfs = new File(baseFileDialog.getPath());
      File bfs = new File(PATH_TO_VALUES);

      fileOutputDir = new File(outputDir);
      if( !fileOutputDir.exists() ){
        fileOutputDir.mkdir();
      }

      File jpgout = new File(jpgOutputDir);
      if( !jpgout.exists() && thereAreJPGs ){
        jpgout.mkdir();
      }
      File tifout = new File(tifOutputDir);
      if( !tifout.exists() && thereAreTIFs ){
        tifout.mkdir();
      }

      double[][] baseSummary = null;
      double[] coeffs = new double[4];
      double[] tmpcoeff = null;

      //qrJPGPhoto.show();
      //qrTIFPhoto.show();
      int imgcounter = 0;

      CameraEXIF imageEXIFData = null;
      CameraEXIF qrTIFEXIFData = null;
      CameraEXIF qrJPGEXIFData = null;
      String pathToCSV = null;
      while( pixelIterator.hasNext() ){
        imgcounter++;
        tmpfile = pixelIterator.next();
        IJ.log( (String)"Gathering pixel information for " + tmpfile.getName() + " (" + imgcounter + " of " + jpgToCalibrate.size() + " - " + (int)((double)imgcounter/((double)jpgToCalibrate.size())*100) + "% complete" + ")" );
        photo = new RGBPhoto(inputDir, tmpfile.getName(), tmpfile.getPath(), cameraType, false);

        ImagePlus imgRed = photo.getRedChannel();
        ImagePlus imgGreen = photo.getGreenChannel();
        ImagePlus imgBlue = photo.getBlueChannel();


        double redReflectMax = (double)imgRed.getDisplayRangeMax();
        double redReflectMin = (double)imgRed.getDisplayRangeMin();
        double greenReflectMax = (double)imgGreen.getDisplayRangeMax();
        double greenReflectMin = (double)imgGreen.getDisplayRangeMin();
        double blueReflectMax = (double)imgBlue.getDisplayRangeMax();
        double blueReflectMin = (double)imgBlue.getDisplayRangeMin();
        if(photo.getExtension().toUpperCase().equals("TIF"))
        {
            dirMinMaxes[0] = (dirMinMaxes[0] > redReflectMax) ? dirMinMaxes[0] : redReflectMax;
            dirMinMaxes[1] = (dirMinMaxes[1] < redReflectMin) ? dirMinMaxes[1] : redReflectMin;
            dirMinMaxes[2] = (dirMinMaxes[2] > greenReflectMax) ? dirMinMaxes[2] : greenReflectMax;
            dirMinMaxes[3] = (dirMinMaxes[3] < greenReflectMin) ? dirMinMaxes[3] : greenReflectMin;
            dirMinMaxes[4] = (dirMinMaxes[4] > blueReflectMax) ? dirMinMaxes[4] : blueReflectMax;
            dirMinMaxes[5] = (dirMinMaxes[5] < blueReflectMin) ? dirMinMaxes[5] : blueReflectMin;
        }
        else if(photo.getExtension().toUpperCase().equals("JPG"))
        {
            dirMinMaxes[6] = (dirMinMaxes[6] > redReflectMax) ? dirMinMaxes[6] : redReflectMax;
            dirMinMaxes[7] = (dirMinMaxes[7] < redReflectMin) ? dirMinMaxes[7] : redReflectMin;
            dirMinMaxes[8] = (dirMinMaxes[8] > greenReflectMax) ? dirMinMaxes[8] : greenReflectMax;
            dirMinMaxes[9] = (dirMinMaxes[9] < greenReflectMin) ? dirMinMaxes[9] : greenReflectMin;
            dirMinMaxes[10] = (dirMinMaxes[10] > blueReflectMax) ? dirMinMaxes[10] : blueReflectMax;
            dirMinMaxes[11] = (dirMinMaxes[11] < blueReflectMin) ? dirMinMaxes[11] : blueReflectMin;
        }
        else if(photo.getExtension().toUpperCase().equals("DNG"))
        {
            dirMinMaxes[12] = (dirMinMaxes[12] > redReflectMax) ? dirMinMaxes[12] : redReflectMax;
            dirMinMaxes[13] = (dirMinMaxes[13] < redReflectMin) ? dirMinMaxes[13] : redReflectMin;
            dirMinMaxes[14] = (dirMinMaxes[14] > greenReflectMax) ? dirMinMaxes[14] : greenReflectMax;
            dirMinMaxes[15] = (dirMinMaxes[15] < greenReflectMin) ? dirMinMaxes[15] : greenReflectMin;
            dirMinMaxes[16] = (dirMinMaxes[16] > blueReflectMax) ? dirMinMaxes[16] : blueReflectMax;
            dirMinMaxes[17] = (dirMinMaxes[17] < blueReflectMin) ? dirMinMaxes[17] : blueReflectMin;
        }
     }

      //IJ.log( (String)"Red Max: " + dirMinMaxes[0] + " Red Min: " + dirMinMaxes[1] + " Blue Max: " + dirMinMaxes[2] + "Blue Min: " + dirMinMaxes[3]);
      Iterator<File> jpgIterator = jpgToCalibrate.iterator();
      Iterator<File> tifIterator = tifToCalibrate.iterator();
      imgcounter = 0;
      while( jpgIterator.hasNext() ){
        imgcounter++;
        tmpfile = jpgIterator.next();

        IJ.log( (String)"Processing image " + tmpfile.getName() + " (" + imgcounter + " of " + jpgToCalibrate.size() + " - " + (int)((double)imgcounter/((double)jpgToCalibrate.size())*100) + "% complete" + ")" );


        // Add QR and Image camera settings check here
        imageEXIFData = new CameraEXIF( new EXIFToolsReader(PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath()) );
        if( imageEXIFData == null ){
            IJ.log("Could not find EXIF information on this image. Please make sure it contains EXIF information. I will skip this image.");
            continue;
        }


        //IJ.log("Path To CSV: " + pathToCSV);
        qrTIFEXIFData = new CameraEXIF( new EXIFToolsReader(PATH_TO_EXIFTOOL, qrTIFPath) );
        qrJPGEXIFData = new CameraEXIF( new EXIFToolsReader(PATH_TO_EXIFTOOL, qrJPGPath) );

        //IJ.log("image Exif Data: " + imageEXIFData.printEXIFData());
        //IJ.log(qrTIFEXIFData.printEXIFData());
        //IJ.log("QR Exif Data" + qrJPGEXIFData.printEXIFData());

        if( imageEXIFData != null)
        {
            pathToCSV = GetEXIFCSV(imageEXIFData.getCameraModel());
            CameraEXIF defaultEXIFData = new CameraEXIF( new EXIFCSVReader(pathToCSV));

            if (useQR)
            {
                if( thereAreTIFs && !continueAnyway){
                    if( !imageEXIFData.equals(qrTIFEXIFData) )
                    {
                        GenericDialog dialog = showCameraSettingsNotEqualDialog(qrTIFEXIFData.printEXIFData(),imageEXIFData.printEXIFData());
                        continueAnyway = dialog.getNextBoolean();
                        if( dialog.wasOKed() ){
                            // Continue
                        }else if( dialog.wasCanceled() ){
                            // Quit
                            IJ.log("Goodbye!");
                            return;
                        }
                    }
                }
                if( thereAreJPGs && !continueAnyway){
                    if( !imageEXIFData.equals(qrTIFEXIFData) )
                    {
                        GenericDialog dialog = showCameraSettingsNotEqualDialog(qrTIFEXIFData.printEXIFData(),imageEXIFData.printEXIFData());
                        continueAnyway = dialog.getNextBoolean();
                        if( dialog.wasOKed() ){
                            // Continue
                        }else if( dialog.wasCanceled() ){
                            // Quit
                            IJ.log("Goodbye!");
                            return;
                        }
                    }
                }
            }
            else
            {
                if( thereAreTIFs && !imageEXIFData.equals(defaultEXIFData) && !continueAnyway){
                    GenericDialog dialog = showCameraSettingsNotEqualDialog(defaultEXIFData.printEXIFData(),imageEXIFData.printEXIFData());
                    continueAnyway = dialog.getNextBoolean();
                    if( dialog.wasOKed() ){
                        // Continue
                    }else if( dialog.wasCanceled() ){
                        // Quit
                        IJ.log("Goodbye!");
                        return;
                    }
                }
                if( thereAreJPGs && !imageEXIFData.equals(defaultEXIFData) && !continueAnyway){
                    //IJ.log(qrJPGEXIFData.printEXIFData());
                    //IJ.log(imageEXIFData.printEXIFData());
                    GenericDialog dialog = showCameraSettingsNotEqualDialog(defaultEXIFData.printEXIFData(),imageEXIFData.printEXIFData());
                    continueAnyway = dialog.getNextBoolean();
                    if( dialog.wasOKed() ){
                        // Continue
                    }else if( dialog.wasCanceled() ){
                        // Quit
                        IJ.log("Goodbye!");
                        return;
                    }
                }
            }


        }


        IJ.log("Opening image: " + tmpfile.getName());
        photo = new RGBPhoto(inputDir, tmpfile.getName(), tmpfile.getPath(), cameraType, false);
        //photo = calibrator.scaleChannels(photo);
        if( !photo.checkChannels() ){
          // Could not split channels. Skip image;
          continue;
        }

        // @TODO Undo gamma and subtractNIR logic here
        //photo.show();

        // Fix dumb tif stuff
        /*
        if( photo.getExtension().toUpperCase().equals("TIF") ){
          IJ.log("Tif detected. Fixing tif.");
          IJ.log("HEY WHATS GOING ON");
          photo.fixTif();

        }*/

        IJ.log("Calibrating image: " + tmpfile.getName());
        if( cameraType.equals(CalibrationPrompt.SURVEY2_NDVI) ){
          // Use red channel and blue channel
          baseSummary = calibrator.getRefValues(bfs, "660/850");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_NDVI_TIF[0];
              coeffs[1] = BASE_COEFF_SURVEY2_NDVI_TIF[1];
              coeffs[2] = BASE_COEFF_SURVEY2_NDVI_TIF[2];
              coeffs[3] = BASE_COEFF_SURVEY2_NDVI_TIF[3];
            }

            //photo = calibrator.subtractNIR(photo, 0.8);
            //TODO add calibrated max/min pixel values
            //IJ.log( (String)"Red Max: " + dirMinMaxes[0] + " Red Min: " + dirMinMaxes[1] + " Blue Max: " + dirMinMaxes[2] + "Blue Min: " + dirMinMaxes[3]);
            resultphoto = calibrator.makeNDVI(photo, coeffs, dirMinMaxes);
            //IJ.log( (String)"Red Max: " + dirMinMaxes[0] + " Red Min: " + dirMinMaxes[1] + " Blue Max: " + dirMinMaxes[2] + "Blue Min: " + dirMinMaxes[3]);

          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_NDVI_JPG[0];
              coeffs[1] = BASE_COEFF_SURVEY2_NDVI_JPG[1];
              coeffs[2] = BASE_COEFF_SURVEY2_NDVI_JPG[2];
              coeffs[3] = BASE_COEFF_SURVEY2_NDVI_JPG[3];
            }
            //IJ.log( (String)"Red Max: " + dirMinMaxes[0] + " Red Min: " + dirMinMaxes[1] + " Blue Max: " + dirMinMaxes[2] + "Blue Min: " + dirMinMaxes[3]);
            resultphoto = calibrator.makeNDVI(photo, coeffs, dirMinMaxes);
            //IJ.log( (String)"Red Max: " + dirMinMaxes[0] + " Red Min: " + dirMinMaxes[1] + " Blue Max: " + dirMinMaxes[2] + "Blue Min: " + dirMinMaxes[3]);
          }

          //resultphoto = calibrator.makeNDVI(photo, coeffs);
          //resultphoto = calibrator.makeNDVI(photo, qrTIFPhoto, coeffs, tifrois );

        }else if( cameraType.equals(CalibrationPrompt.SURVEY2_NIR) ){
          baseSummary = calibrator.getRefValues(bfs, "850");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_NIR_TIF[0];
              coeffs[1] = BASE_COEFF_SURVEY2_NIR_TIF[1];
            }

          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_NIR_JPG[0];
              coeffs[1] = BASE_COEFF_SURVEY2_NIR_JPG[1];
            }
          }


          resultphoto = calibrator.makeSingle(photo, coeffs, dirMinMaxes);

        }else if( cameraType.equals(CalibrationPrompt.SURVEY2_RED) ){
          baseSummary = calibrator.getRefValues(bfs, "650");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_RED_TIF[0];
              coeffs[1] = BASE_COEFF_SURVEY2_RED_TIF[1];
            }

          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_RED_JPG[0];
              coeffs[1] = BASE_COEFF_SURVEY2_RED_JPG[1];
            }
          }



          resultphoto = calibrator.makeSingle(photo, coeffs, dirMinMaxes);

        }else if( cameraType.equals(CalibrationPrompt.SURVEY2_GREEN) ){
          baseSummary = calibrator.getRefValues(bfs, "548");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Green");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_GREEN_TIF[0];
              coeffs[1] = BASE_COEFF_SURVEY2_GREEN_TIF[1];
            }
          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Green");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_GREEN_JPG[0];
              coeffs[1] = BASE_COEFF_SURVEY2_GREEN_JPG[1];
            }

          }

          resultphoto = calibrator.makeSingle(photo, coeffs, dirMinMaxes);

        }else if( cameraType.equals(CalibrationPrompt.SURVEY2_BLUE) ){
          baseSummary = calibrator.getRefValues(bfs, "450");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Blue");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_BLUE_TIF[0];
              coeffs[1] = BASE_COEFF_SURVEY2_BLUE_TIF[1];
            }
          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Blue");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY2_BLUE_JPG[0];
              coeffs[1] = BASE_COEFF_SURVEY2_BLUE_JPG[1];
            }
          }


          resultphoto = calibrator.makeSingle(photo, coeffs, dirMinMaxes);

        }else if( cameraType.equals(CalibrationPrompt.DJIX3_NDVI) ){
          baseSummary = calibrator.getRefValues(bfs, "660/850");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_DJIX3_NDVI_TIF[0];
              coeffs[1] = BASE_COEFF_DJIX3_NDVI_TIF[1];
              coeffs[2] = BASE_COEFF_DJIX3_NDVI_TIF[2];
              coeffs[3] = BASE_COEFF_DJIX3_NDVI_TIF[3];
            }
          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_DJIX3_NDVI_JPG[0];
              coeffs[1] = BASE_COEFF_DJIX3_NDVI_JPG[1];
              coeffs[2] = BASE_COEFF_DJIX3_NDVI_JPG[2];
              coeffs[3] = BASE_COEFF_DJIX3_NDVI_JPG[3];
            }
          }
          resultphoto = calibrator.makeNDVI(photo, coeffs, dirMinMaxes);
        }else if( cameraType.equals(CalibrationPrompt.GOPRO_HERO4_NDVI) ){
          /*
          baseSummary = calibrator.getRefValues(bfs, "660/850");

          if( useQR == true && rois != null ){
            tmpcoeff = calculateCoefficients(qrScaled, rois, calibrator, baseSummary, "Red");
            coeffs[0] = tmpcoeff[0];
            coeffs[1] = tmpcoeff[1];
            tmpcoeff = calculateCoefficients(qrScaled, rois, calibrator, baseSummary, "Blue");
            coeffs[2] = tmpcoeff[0];
            coeffs[3] = tmpcoeff[1];
          }else{
            coeffs[0] = BASE_COEFF_DJIX3_NDVI[0];
            coeffs[1] = BASE_COEFF_DJIX3_NDVI[1];
            coeffs[2] = BASE_COEFF_DJIX3_NDVI[2];
            coeffs[3] = BASE_COEFF_DJIX3_NDVI[3];
          }


          resultphoto = calibrator.makeNDVI(photo, coeffs);*/

          IJ.log("GoPro Hero 4 support coming soon!");
        }else if( cameraType.equals(CalibrationPrompt.DJIPHANTOM4_NDVI) ){
          baseSummary = calibrator.getRefValues(bfs, "660/850");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_DJIPHANTOM4_NDVI_TIF[0];
              coeffs[1] = BASE_COEFF_DJIPHANTOM4_NDVI_TIF[1];
              coeffs[2] = BASE_COEFF_DJIPHANTOM4_NDVI_TIF[2];
              coeffs[3] = BASE_COEFF_DJIPHANTOM4_NDVI_TIF[3];
            }


            //photo = calibrator.subtractNIR(photo, 0.8);

          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_DJIPHANTOM4_NDVI_JPG[0];
              coeffs[1] = BASE_COEFF_DJIPHANTOM4_NDVI_JPG[1];
              coeffs[2] = BASE_COEFF_DJIPHANTOM4_NDVI_JPG[2];
              coeffs[3] = BASE_COEFF_DJIPHANTOM4_NDVI_JPG[3];
            }
          }
          resultphoto = calibrator.makeNDVI(photo, coeffs, dirMinMaxes);
      }else if( cameraType.equals(CalibrationPrompt.DJIPHANTOM3_NDVI) ){
          baseSummary = calibrator.getRefValues(bfs, "660/850");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_DJIPHANTOM3_NDVI_TIF[0];
              coeffs[1] = BASE_COEFF_DJIPHANTOM3_NDVI_TIF[1];
              coeffs[2] = BASE_COEFF_DJIPHANTOM3_NDVI_TIF[2];
              coeffs[3] = BASE_COEFF_DJIPHANTOM3_NDVI_TIF[3];
            }


            //photo = calibrator.subtractNIR(photo, 0.8);

          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_DJIPHANTOM3_NDVI_JPG[0];
              coeffs[1] = BASE_COEFF_DJIPHANTOM3_NDVI_JPG[1];
              coeffs[2] = BASE_COEFF_DJIPHANTOM3_NDVI_JPG[2];
              coeffs[3] = BASE_COEFF_DJIPHANTOM3_NDVI_JPG[3];
            }
          }
          resultphoto = calibrator.makeNDVI(photo, coeffs, dirMinMaxes);
      }else if( cameraType.equals(CalibrationPrompt.SURVEY1_NDVI) ){
          // VIS and NIR channels are switched!!
          // VIS = blue
          // NIR = red
          baseSummary = calibrator.getRefValues(bfs, "660/850");

          if( photo.getExtension().toUpperCase().equals("TIF") ){
            if( useQR == true && tifrois != null ){
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrTIFScaled, tifrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY1_NDVI_TIF[0];
              coeffs[1] = BASE_COEFF_SURVEY1_NDVI_TIF[1];
              coeffs[2] = BASE_COEFF_SURVEY1_NDVI_TIF[2];
              coeffs[3] = BASE_COEFF_SURVEY1_NDVI_TIF[3];
            }


            //photo = calibrator.subtractNIR(photo, 0.8);

          }else if( photo.getExtension().toUpperCase().equals("JPG") ){
            if( useQR == true && jpgrois != null ){
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Red");
              coeffs[0] = tmpcoeff[0];
              coeffs[1] = tmpcoeff[1];
              tmpcoeff = calculateCoefficients(qrJPGScaled, jpgrois, calibrator, baseSummary, "Blue");
              coeffs[2] = tmpcoeff[0];
              coeffs[3] = tmpcoeff[1];
            }else{
              coeffs[0] = BASE_COEFF_SURVEY1_NDVI_JPG[0];
              coeffs[1] = BASE_COEFF_SURVEY1_NDVI_JPG[1];
              coeffs[2] = BASE_COEFF_SURVEY1_NDVI_JPG[2];
              coeffs[3] = BASE_COEFF_SURVEY1_NDVI_JPG[3];
            }
          }
          resultphoto = calibrator.makeNDVI(photo, coeffs, dirMinMaxes);
      }

        resultphoto.copyFileData(photo);



        // Calibrated output folder override prevention
        //IJ.log("Saving Image");
        IJ.log( "Saving image to " + outputDir);
        if( resultphoto.getExtension().toUpperCase().equals("TIF") ){
          saveToDir(tifOutputDir, resultphoto.getFileName(), resultphoto.getExtension(), resultphoto.getImage());
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath(), tifOutputDir+resultphoto.getFileName()+"_Calibrated"+"."+resultphoto.getExtension());
        }else if( resultphoto.getExtension().toUpperCase().equals("JPG") ){
          //(new FileSaver(resultphoto.getImage())).saveAsJpeg(jpgOutputDir+resultphoto.getFileName()+resultphoto.getExtension());
          saveToDir(jpgOutputDir, resultphoto.getFileName(), resultphoto.getExtension(), resultphoto.getImage());
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath(), jpgOutputDir+resultphoto.getFileName()+"_Calibrated"+"."+resultphoto.getExtension());
        }
        // Also save tif to jpg
        if( tifsToJpgs ){
          saveToDir( jpgOutputDir, resultphoto.getFileName(), "jpg", resultphoto.getImage() );
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath(), jpgOutputDir+resultphoto.getFileName()+"_Calibrated"+"."+"jpg");
        }




        //photo.close();
        /*
        if( IJ.getInstance() != null ){
          IJ.getImage().close();
        }*/
        if( qrTIFPhoto != null ){
          qrTIFPhoto.close();
        }
        if( photo != null ){
          photo.close();
        }
      }

    IJ.run("Close All");
    if( prompts.showCalibrationFinishedDialog() ){
      keepPluginAlive = true;
      continue;
    }else{
      keepPluginAlive = false;
    }
  } // End plugin execution


    // Reset jpg quality
    FileSaver.setJpegQuality(jpegQuality);
    IJ.log("Goodbye!");

  }


  public double[] calculateCoefficients(RGBPhoto qrphoto, Roi[] rois, Calibrator calibrator, double[][] baseSummary, String channel){
    //Roi[] rois = null;
    RoiManager manager = null;
    double[] coeff = null;
    List<HashMap<String, String>> bandSummary = null;
    double[] refmeans = new double[3];

    //baseSummary = calibrator.getRefValues(bfs, "660/850"); // <-----Change this!!
    //qrphoto.show();
    if( qrphoto == null ){

    }else{
      //IJ.log("Processing band: " + channel);
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
        //qrphoto.getBlueChannel().show();
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

    for( int i=0; i<means.length; i++ ){
      //IJ.log((String)"Mean: " + means[i]);
    }
    coeff = calibrator.calculateCalibrationCoefficients( means, refmeans );

    manager.reset();
    manager.close();
    }
    return coeff;
  }


  public void saveToDir(String outdir, String filename, String ext, ImagePlus image){
    String NDVIAppend = "_Calibrated";

    // If output directory does not exist, create it
    File outd = new File(outdir);
    if( !outd.exists() ){
      outd.mkdir();
    }

    //IJ.log("Output Directory: " + outdir);
    //IJ.log("Filename: " + filename);
    //IJ.log("Save as extension: " + ext);



    IJ.save((ImagePlus)image, (String)(String.valueOf(outdir) + filename + NDVIAppend + "." + ext));
    return;
  }

  public String GetEXIFCameraModel(String osType, String exiftoolpath, String refimg){
      String console = null;
      String c_arg = null;
      String command = null;

      if( this.OS.contains("Windows") ){
        console = "cmd";
        c_arg = "/c";
        command = exiftoolpath + "exiftool.exe -model -S " + "\""+refimg+"\"";
      }else{
        console = "sh";
        c_arg = "-c";
        command = exiftoolpath + "exiftool -model -S " + "\'"+refimg+"\'";
      }


      String line = null;
      String model = "";
      try{
        ProcessBuilder bob = new ProcessBuilder(console, c_arg, command);
        bob.redirectErrorStream(true);
        final Process proc = bob.start();

        BufferedReader proc_out = new BufferedReader( new InputStreamReader(proc.getInputStream()));
        String [] values = null;

        do{
          line = proc_out.readLine();

          if( line != null ){
            values = line.split(".+: ");
            if( values.length == 2 ){
              model = values[1];
            }
          }
        }while( line != null );

      }catch( IOException e){
        e.printStackTrace();
      }

      // Shitty post process
      if( model.equals("Survey2_IR") ){
        // Convert to match camera model selected from dialog
        model = CalibrationPrompt.SURVEY2_NIR;
      }

      // More shitty fixes for DJI
      // Phantom4 option supports phantom 4 and phantom 3
      // Phantom 4 = FC330; Phantom 3 = FC300X
      if( model.contains("FC330") ){
        model = CalibrationPrompt.DJIPHANTOM4_NDVI;
      }

      if( model.contains("FC300") ){
          model = CalibrationPrompt.DJIPHANTOM3_NDVI;
      }

      if( model.contains("FC350") ){
        model = CalibrationPrompt.DJIX3_NDVI;
      }
      return model.replaceAll("_", " ");
  }

  public void CopyEXIFData(String osType, String exiftoolpath, String refimg, String targimg ){
    String console = null;
    String c_arg = null;
    String command = null;
    ProcessBuilder bob = null;
    Process proc = null;
  	if ( osType.contains("Windows") ) {
      console = "cmd";
      c_arg = "/c";
      try{
        command = exiftoolpath + "exiftool.exe -overwrite_original -tagsfromfile " + "\""+refimg+"\"" + " " + "\""+targimg+"\"";
        //IJ.log("Executing command: " + command);
        bob = new ProcessBuilder(console, c_arg, command);
        bob.redirectErrorStream(false);
        proc = bob.start();
        proc.waitFor();

      }catch( IOException e){
        e.printStackTrace();
      }catch( InterruptedException i ){
        i.printStackTrace();
      }

  	} else {
      console = "sh";
      c_arg = "-c";

      try{
        // directory spaces
        command = exiftoolpath + "exiftool -overwrite_original -tagsfromfile " + "\'"+refimg+"\'" + " " + "\'"+targimg+"\'";
        //IJ.log("Executing command: " + command);
        bob = new ProcessBuilder(console, c_arg, command);
        bob.redirectErrorStream(true);
        proc = bob.start();
        proc.waitFor();

      }catch( IOException e){
        e.printStackTrace();
      }catch( InterruptedException i ){
        i.printStackTrace();
      }

    }

    //IJ.log("Finished writing EXIF data");

  }

  /*
   * Checks if any calibration folder exists.
   * @param  inDir  Input directory to check for calibration folder
   * @return Integer value representing which is the latest calibration folder.
   *          Ex: 0 - none exist
   *              1 - one exists; Calibrated
   *              2 - two exists; Calibrated, Calibrated_1
   *              3 = two exists; Calibrated, Calibrated_3; Calibrated_1 & Calibrated_2 were deleted
   *              etc...
   */
  public int calibratedFolderExists(String inDir, final String calibFolderName){
    // Assumptions:
    // 1) Calibrated folder is placed inside the input directory
    // 2) Naming conventions Calibrated, Calibrated_1, Calibrated_2, etc
    File inFolder = new File(inDir);

    String[] calibs = inFolder.list(new FilenameFilter(){
      public boolean accept(File dir, String name){
        if( name.contains(calibFolderName) ){
          return true;
        }
        return false;
        }
      });

    int num = 0;
    int highestNum = 0;
    String[] split = null;
    for( int i=0; i<calibs.length; i++ ){
      split = calibs[i].split(".+_");
      if( split.length == 1 ){
        highestNum = 0;
      }else if( split.length == 2 ){
        num = Integer.parseInt(split[1]);
        highestNum = (num >= highestNum) ? num : highestNum;
        }
    }

    return highestNum;
  }


  public String GetEXIFCSV(String model){
      String workingDirectory = IJ.getDirectory("imagej");
      String valuesDirectory = workingDirectory+"Survey2\\Values\\";
      String csvpath = null;
      if(model != null)
      {
          if( model.equals("Survey2_RED") ){
              csvpath = valuesDirectory+"red\\red.csv";
          }else if( model.equals("Survey2_GREEN") ){
              csvpath = valuesDirectory+"green\\green.csv";
          }else if( model.equals("Survey2_BLUE") ){
              csvpath = valuesDirectory+"blue\\blue.csv";
          }else if( model.equals("Survey2_NDVI") ){
              csvpath = valuesDirectory+"ndvi\\ndvi.csv";
          }else if( model.equals("Survey2_IR") ){
              csvpath = valuesDirectory+"ir\\ir.csv";
          }else if( model.equals("FC350") ){
              csvpath = valuesDirectory+"FC350_ndvi\\FC350_ndvi.csv";
          }else if( model.equals("FC330") ){
              csvpath = valuesDirectory+"FC330_ndvi\\FC300_ndvi.csv";
          }else if( model.equals("FC300X") ){
              csvpath = valuesDirectory+"FC300X_ndvi\\FC300X_ndvi.csv";
          }else if( model.equals("FC300S") ){
              csvpath = valuesDirectory+"FC300S_ndvi\\FC300S_ndvi.csv";
          }else if( model.equals("MAPIR")){
              csvpath = valuesDirectory+"survey1\\survey1.csv";
          }
      }
      else{
          csvpath = "Camera not supported";
      }

      return csvpath;
  }

  public GenericDialog showCameraSettingsNotEqualDialog(String exifdata1, String exifdata2 ){
      GenericDialog dialog = new GenericDialog("Attention!");

      dialog.addMessage("The camera settings of the current image to process");
      dialog.addMessage("does not match the camera default settings or the setings of the calibration target's image.");
      dialog.addMessage("Proceeding will produce undesired results.");
      dialog.addMessage("Do you wish to continue?");
      dialog.addTextAreas("Calibration Target EXIF Data:\n" + exifdata1, "Image EXIF Data:\n" + exifdata2, 5, 30);
      //dialog.enableYesNoCancel("Continue anyway", "Choose another Calibration Target");
      dialog.addCheckbox("Keep this decision for all remaining files.", false);
      dialog.setOKLabel("Continue anyway");
      dialog.setCancelLabel("Quit");

      dialog.showDialog();

      return dialog;
  }



}
