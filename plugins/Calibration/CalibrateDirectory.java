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

  // {Intercept, Slope}
  // @TODO Update this
  // @TODO Create progress dialog for user
  private final double[] BASE_COEFF_SURVEY2_RED_JPG = {-2.55421832, 16.01240929};//
  private final double[] BASE_COEFF_SURVEY2_GREEN_JPG = {-0.60437250, 4.82869470};//
  private final double[] BASE_COEFF_SURVEY2_BLUE_JPG = {-0.39268985, 2.67916884};//
  private final double[] BASE_COEFF_SURVEY2_NDVI_JPG = {-0.29870245, 6.51199915, -0.65112026, 10.30416005};//
  private final double[] BASE_COEFF_SURVEY2_NIR_JPG = {-0.46967653, 7.13619139};//

  private final double[] BASE_COEFF_SURVEY2_RED_TIF = {-5.09645820, 0.24177528};//
  private final double[] BASE_COEFF_SURVEY2_GREEN_TIF = {-1.39528479, 0.07640011};//
  private final double[] BASE_COEFF_SURVEY2_BLUE_TIF = {-0.67299134, 0.03943339};//
  private final double[] BASE_COEFF_SURVEY2_NDVI_TIF = {-0.60138990, 0.14454211, -3.51691589, 0.21536524};//
  private final double[] BASE_COEFF_SURVEY2_NIR_TIF = {-2.24216724, 0.12962333};//

  private final double[] BASE_COEFF_DJIX3_NDVI_JPG = {-0.11216727, 44.37533995, -0.11216727, 497.19423086};

  private final double[] BASE_COEFF_DJIX3_NDVI_TIF = {-0.11216727, 44.37533995, -0.11216727, 497.19423086};
  private final double[] BASE_COEFF_GOPROHERO4_NDVI = {0,0};

  private String OS = System.getProperty("os.name");
  private String WorkingDirectory = IJ.getDirectory("imagej");
  private String PATH_TO_VALUES = WorkingDirectory+"Calibration\\values.csv";
  private String PATH_TO_EXIFTOOL = WorkingDirectory+"Survey2\\EXIFTool\\exiftool.exe";

  private boolean thereAreJPGs = false;
  private boolean thereAreTIFs = false;



  public void run(String arg){

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

      jpgOutputDir = outputDir + "\\Jpgs\\";
      tifOutputDir = outputDir + "\\Tifs\\";

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

        if( inImageExt.toUpperCase().equals("JPG") || inImageExt.toUpperCase().equals("TIF") ){
          if( tifsToJpgs && inImageExt.toUpperCase().equals("JPG") ){
            // Don't add original jpgs, only tifs
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
      while( useQR == true ){

        if( !tifsToJpgs ){
          // User want to calibate JPGs present in directory
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
              calibrator.subtractNIR(qrJPGScaled.getBlueChannel(), qrJPGScaled.getRedChannel(), 80 );
            }
            //qrJPGScaled = new RGBPhoto(qrJPGPhoto);
            jpgrois = calibrator.getRois(qrJPGPhoto.getImage());

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
            calibrator.subtractNIR(qrTIFScaled.getBlueChannel(), qrTIFScaled.getRedChannel(), 80 );
          }
          // Enhance for better QR detection
          (new ContrastEnhancer()).equalize(qrTIFPhoto.getImage());
          tifrois = calibrator.getRois(qrTIFPhoto.getImage());

          if( tifrois == null ){
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

      // Load in photo (only after calibration coefficients have been prepared)
      RGBPhoto photo = null;
      RGBPhoto resultphoto = null;
      Iterator<File> jpgIterator = jpgToCalibrate.iterator();
      Iterator<File> tifIterator = tifToCalibrate.iterator();
      File tmpfile = null;

      //OpenDialog baseFileDialog = new OpenDialog("Select Base File");
      //File bfs = new File(baseFileDialog.getPath());
      File bfs = new File(PATH_TO_VALUES);

      fileOutputDir = new File(outputDir);
      if( !fileOutputDir.exists() ){
        fileOutputDir.mkdir();
      }

      File jpgout = new File(jpgOutputDir);
      if( !jpgout.exists() ){
        jpgout.mkdir();
      }
      File tifout = new File(tifOutputDir);
      if( !tifout.exists() ){
        tifout.mkdir();
      }

      double[][] baseSummary = null;
      double[] coeffs = new double[4];
      double[] tmpcoeff = null;

      //qrJPGPhoto.show();
      //qrTIFPhoto.show();
      int imgcounter = 0;
      while( jpgIterator.hasNext() ){
        imgcounter++;
        tmpfile = jpgIterator.next();

        IJ.log( (String)"Processing image " + tmpfile.getName() + " (" + imgcounter + " of " + jpgToCalibrate.size() + " - " + (int)((double)imgcounter/((double)jpgToCalibrate.size())*100) + "%" + ")" );

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
          }

          resultphoto = calibrator.makeNDVI(photo, coeffs);

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


          resultphoto = calibrator.makeSingle(photo, coeffs);

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



          resultphoto = calibrator.makeSingle(photo, coeffs);

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

          resultphoto = calibrator.makeSingle(photo, coeffs);

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


          resultphoto = calibrator.makeSingle(photo, coeffs);

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


          resultphoto = calibrator.makeNDVI(photo, coeffs);
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
        }

        resultphoto.copyFileData(photo);



        // Calibrated output folder override prevention
        IJ.log("Saving Image");

        if( resultphoto.getExtension().toUpperCase().equals("TIF") ){
          saveToDir(tifOutputDir, resultphoto.getFileName(), resultphoto.getExtension(), resultphoto.getImage());
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath(), tifOutputDir+resultphoto.getFileName()+"_Calibrated"+"."+resultphoto.getExtension());
        }else if( resultphoto.getExtension().toUpperCase().equals("JPG") ){
          saveToDir(jpgOutputDir, resultphoto.getFileName(), resultphoto.getExtension(), resultphoto.getImage());
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath(), jpgOutputDir+resultphoto.getFileName()+"_Calibrated"+"."+resultphoto.getExtension());
        }
        // Also save tif to jpg
        if( tifsToJpgs ){
          saveToDir( jpgOutputDir, resultphoto.getFileName(), "jpg", resultphoto.getImage() );
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, tmpfile.getAbsolutePath(), jpgOutputDir+resultphoto.getFileName()+"_Calibrated"+"."+"jpg");
        }
        IJ.log( "Saved image to " + outputDir);



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

    if( prompts.showCalibrationFinishedDialog() ){
      keepPluginAlive = true;
      continue;
    }else{
      keepPluginAlive = false;
    }
  } // End plugin execution
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
        command = exiftoolpath + " -model -S " + "\""+refimg+"\"";
      }else{
        console = "sh";
        c_arg = "-c";
        command = exiftoolpath + " -model -S " + "\'"+refimg+"\'";
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
        model = "Survey2_NIR";
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
        command = exiftoolpath + " -overwrite_original -tagsfromfile " + "\""+refimg+"\"" + " " + "\""+targimg+"\"";
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
        command = exiftoolpath + " -overwrite_original -tagsfromfile " + "\'"+refimg+"\'" + " " + "\'"+targimg+"\'";
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



}
