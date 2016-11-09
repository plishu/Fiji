import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Iterator;
import java.lang.Runtime;
import java.lang.ProcessBuilder;
import java.lang.Process;
import ij.io.*;

public class Post_Process_Batch implements PlugIn{
  private String inDirStr = null;
  private String outDirStr = null;

  private String PATH_TO_EXIFTOOL = null;

  private final int FILTER_RADIUS_RAW = 1;
  private final int FILTER_RADIUS_JPG = 10;
  private String filter_radius_raw = "1";
  private String filter_radius_jpg = "10";
  private String normalize = "0";

  private String OS = null;

  private String CurrentModel = null;

  private String WorkingDirectory = null;
  private String FLAT_FIELD_DIRECTORY = null;

  private Boolean DeleteOriginals = true;
  private boolean removeVignette = true;

  private final String VERSION = "1.3.8";


  class FileComparator implements Comparator<File>{
    @Override
    public int compare(File a, File b){
      return a.getName().compareTo(b.getName());
    }
  }


  public void run(String args){
      IJ.log("Build: " + VERSION);
    //IJ.showMessage("Post_Process", "Hello world");
    WorkingDirectory = IJ.getDirectory("imagej");
    OS = System.getProperty("os.name");
    // WINDOWS SUPPORT ONLY
    PATH_TO_EXIFTOOL = WorkingDirectory+"Survey2\\EXIFTool\\";
    //PATH_TO_EXIFTOOL = "/usr/bin/exiftool";
    FLAT_FIELD_DIRECTORY = WorkingDirectory+"Survey2\\Flat-Fields\\";

    YesNoCancelDialog removeVignetteDialog = new YesNoCancelDialog(null, "Attention", "Do you want to apply vignette removal to the pre-process procedure?");
    if( removeVignetteDialog.yesPressed() ){
        filter_radius_raw = "1";
        filter_radius_jpg = "10";
        removeVignette = true;
    }else if( removeVignetteDialog.cancelPressed() ){
        return;
    }else{
        filter_radius_raw = "0";
        filter_radius_jpg = "0";
        removeVignette = false;
    }

    // @TODO: Camera settings changed detection

    inDirStr = IJ.getDirectory("Select Folder with Images to Process");
    outDirStr = IJ.getDirectory("Select Output Folder");
    //GenericDialog gd = new GenericDialog("");
    //gd.addMessage("You have selected the same directory for the input and output. This will replace the original JPG images in the input directory. Do you wish to backup the original JPG images?");
    //gd.enableYesNoCancel();
    if( outDirStr.equals(inDirStr) ){
      YesNoCancelDialog dialog = new YesNoCancelDialog(null, "Caution", "The output directory is the same as the input directory. Continuing will replace the original JPG images.\n\nDo you want to backup the original JPG images?" );
      //Backup original images even if user said no bc we need the exif data
      backUpImages(inDirStr);
       if( dialog.yesPressed() ){
         DeleteOriginals = false;
       }else if( dialog.cancelPressed() ){
         return;
       }
       //outDirStr = IJ.getDirectory("Select Output Folder");
    }


    // Check input directory. If directory only contains JPG,
    // then remove vignette and save the output.
    // If directory contains RAW & JPG, then remove vignette from RAW,
    // save as TIFF, and copy JPG EXIF data to TIFF.
    //runMacro("macro","args");

    //IJ.log("Input Directory: " + inDirStr);
    //IJ.log("Output Directory: " + outDirStr);

    // Get all files to process
    File inDir = new File(inDirStr);
    File[] filesToProcess = inDir.listFiles();

    /*
    for( int i=0; i<filesToProcess.length; i++ ){
      IJ.log(filesToProcess[i].getName());
    }*/

    //IJ.log("---------------------------------------------------------");

    // Put files in cronological order (ascending)
    Arrays.sort(filesToProcess, new FileComparator());

    /*
    for( int i=0; i<filesToProcess.length; i++ ){
      IJ.log(filesToProcess[i].getName());
    }*/


    List<File> jpgBatchToProcess = new ArrayList<File>();
    List<File> raw_jpgBatchToProcess = new ArrayList<File>();

    // Following assumptions are made:
    // 1) RAW always comes before JPG
    // 2) All files sorted in chronological order first
    // 3) RAW always followed by JPG, and are the same image
    if( filesToProcess.length > 0){
    for( int i=0; i<filesToProcess.length; i++ ){
      String[] inImageParts = (filesToProcess[i].getName()).split("\\.(?=[^\\.]+$)");
      String inImageExt = null;

      if( inImageParts.length < 2 ){
        continue;
      } else{
        inImageExt = inImageParts[1];
      }

      // If RAW is found, then add RAW+JPG to raw_jpgBatchToProcess and skip following JPG
      if( inImageExt.toUpperCase().equals("RAW") ){
        raw_jpgBatchToProcess.add(filesToProcess[i]);

        if( filesToProcess[i+1].getName().contains("jpg") || filesToProcess[i+1].getName().contains("JPG") ){
            raw_jpgBatchToProcess.add(filesToProcess[i+1]);
            i++;
        }else{
            // Does not comply to directory file structure
            if( showNotCorrectFileStructure().wasOKed() ){
                IJ.log("Current File: " + filesToProcess[i].getName());
                IJ.log("Error --> " + filesToProcess[i+1].getName());
                IJ.log("File structure detected: ");
                for( int j=0; j<filesToProcess.length; j++ ){
                    IJ.log(filesToProcess[j].getName());
                }
            }
            i++;
            return;
        }

         // Skip following JPG in the search
      }else if( inImageExt.toUpperCase().equals("DNG") ){
        raw_jpgBatchToProcess.add(filesToProcess[i]);

        //IJ.log(filesToProcess[i].getName());
        if( containsDNGOnly( raw_jpgBatchToProcess.iterator()) ){
            IJ.log("Contains DNGs only");
        }
        else
        {
            if( filesToProcess[i+1].getName().contains("jpg") || filesToProcess[i+1].getName().contains("JPG") ){
                raw_jpgBatchToProcess.add(filesToProcess[i+1]);
                i++;
            }else{
                // Does not comply to directory file structure

                if( showNotCorrectFileStructure().wasOKed() ){
                    IJ.log("Current File: " + filesToProcess[i].getName());
                    IJ.log("Error --> " + filesToProcess[i+1].getName());
                    IJ.log("File structure detected: ");
                    for( int j=0; j<filesToProcess.length; j++ ){
                        IJ.log(filesToProcess[j].getName());
                    }
                    i++;
                    return;
                }
            }

      }
      }else if( inImageExt.toUpperCase().equals("JPG") ){
        jpgBatchToProcess.add(filesToProcess[i]);
      }
    }
}
    Collections.sort(jpgBatchToProcess);
    Collections.sort(raw_jpgBatchToProcess);
    if( !raw_jpgBatchToProcess.isEmpty())
    {
    if( GetCameraModel(raw_jpgBatchToProcess.get(1).getAbsolutePath()) == "Survey2_RGB")
    {
        YesNoCancelDialog normal = new YesNoCancelDialog(null, "Attention", "Do you want to normalize the RGB color photos? This will make the colors more even (less green).");
        if(normal.yesPressed())
        {
            normalize = "1";
        }
        else if(normal.cancelPressed())
        {
            return;
        }
        else
        {
            normalize = "0";
        }

    }
}
    /*
    IJ.log("jpg:" + jpgBatchToProcess.size());
    IJ.log("raw:" + raw_jpgBatchToProcess.size());
    */

    //IJ.log("------------------------------------------------------------");
    /*
    IJ.log("Images that will be proccessed: ");
    for( int i=0; i<raw_jpgBatchToProcess.size(); i++ ){
      IJ.log(raw_jpgBatchToProcess.get(i).getName());
    }
    for( int i=0; i<jpgBatchToProcess.size(); i++ ){
      IJ.log(jpgBatchToProcess.get(i).getName());
    }*/

    //IJ.log("-----------------------------------------------------------------");
    IJ.log("I will begin to process the images. Please wait.");


    // Begin to process all images in input directory
    //CurrentModel = ( GetCameraModel(jpgFilesToProcess.get(0).getAbsolutePath()) );
    //IJ.log("Working Directory: " + WorkingDirectory);


    CurrentModel = "";
    String NextModel = null;
    String FlatField = null;
    ImagePlus[] rawFactorArray = null;
    ImagePlus[] jpgFactorArray = null;
    ImagePlus RAWProcessed = null;
    ImagePlus JPGProcessed = null;

    String[] inImageParts = null;
    String inImageExt = null;
    String inImageNoExt = null;

    CameraEXIF imageEXIFData = null;
    CameraEXIF defaultEXIFData = null;
    String pathToCSV = null;


    int imgcounter = 1;
    String tmp_f_r_raw = filter_radius_raw;
    String tmp_f_r_jpg = filter_radius_jpg;

    if( !raw_jpgBatchToProcess.isEmpty() ){
      // Process RAW + JPG

      // I think you will need to create new loop for DNG only


      // Iterate through each RAW file only!
      // Index JPG by i+1 each time.
      for( int i=0; i<raw_jpgBatchToProcess.size(); i++ ){
        if( (i != 0) && !containsDNGOnly(raw_jpgBatchToProcess.iterator())){
          i = i+1;
        }
        if( i >= raw_jpgBatchToProcess.size()  )
        {
            continue;
        }
        filter_radius_raw = tmp_f_r_raw;
        filter_radius_jpg = tmp_f_r_jpg;

        //inImageParts = (filesToProcess[i].getName()).split("\\.(?=[^\\.]+$)");
        inImageParts = (raw_jpgBatchToProcess.get(i).getName()).split("\\.(?=[^\\.]+$)");
        if( inImageParts.length < 2 ){
          IJ.log("Could not find extension of " + filesToProcess[i].getName());
          continue;
        } else{
          inImageNoExt = inImageParts[0];
          inImageExt = inImageParts[1];
        }

        // Support for DNG only (DNG has metadata embedded within image)
        if( containsDNGOnly(raw_jpgBatchToProcess.iterator()) ){
            IJ.log("Entering SUPPORT FOR DNG ONLY");
            //if( i != 0 ){
            //    i = i-1;
            //}
            String margs = "";
            margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".RAW";
            margs += "|";
            margs += "path_raw="+raw_jpgBatchToProcess.get(i).getAbsolutePath();
            margs += "|";
            margs += "path_out="+outDirStr;
            margs += "|";
            margs += "filter_radius="+filter_radius_raw;
            //IJ.log(margs);
            // Clear log for exif data
            //IJ.deleteRows(0, IJ.getLog().length()-1);
            //IJ.selectWindow("Log");
            //IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\GetDNGEXIF.ijm", margs);
            // Get camera model (dng version)
            imageEXIFData = new CameraEXIF( new EXIFToolsReader(PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i).getAbsolutePath()) );
            NextModel = getDNGCameraModel( imageEXIFData );
        }else{
            NextModel = GetCameraModel(raw_jpgBatchToProcess.get(i+1).getAbsolutePath());
            imageEXIFData = new CameraEXIF( new EXIFToolsReader(PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i+1).getAbsolutePath()) );
        }
        pathToCSV = GetEXIFCSV(NextModel);
        //IJ.log("Path To CSV: " + pathToCSV);
        defaultEXIFData = new CameraEXIF( new EXIFCSVReader(pathToCSV) );

        //IJ.log("EXIF data match up? " + imageEXIFData.equals(defaultEXIFData) );
        if( removeVignette && !imageEXIFData.equals(defaultEXIFData) ){
            if( !containsDNGOnly(raw_jpgBatchToProcess.iterator()) ){
                GenericDialog dialog = showCameraSettingsNotEqualDialog(defaultEXIFData.printEXIFData(), imageEXIFData.printEXIFData(), raw_jpgBatchToProcess.get(i+1).getName());
                if( dialog.wasOKed() ){

                }else if( dialog.wasCanceled() ){
                    return;
                }else{
                    // Don't remove vignette
                    filter_radius_raw = "0";
                    filter_radius_jpg = "0";
                    //removeVignette = false;
                }
            }
        }
        //IJ.log(imageEXIFData.printEXIFData());
        //IJ.log(defaultEXIFData.printEXIFData());
        //IJ.log("Camera Model for " + raw_jpgBatchToProcess.get(i+1).getAbsolutePath() + ": " + NextModel);

        if( NextModel.equals("CAMERA_NOT_SUPPORTED") ){
          IJ.log("The image you are trying to process was not taken with a compatable camera. I will skip this image");
          continue;
        }
          CurrentModel = NextModel;
          FlatField = getFFFile(CurrentModel);

        // Process RAW
        IJ.log( (String)"Processing image " + raw_jpgBatchToProcess.get(i).getName() + " (" + imgcounter + " of " + raw_jpgBatchToProcess.size() + " - " + (int)((double)imgcounter/((double)raw_jpgBatchToProcess.size())*100) + "% complete" + ")" );
        imgcounter++;
        // CALL MACRO HERE
        if( inImageExt.toUpperCase().equals("RAW") ){
          String margs = "";
          margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".RAW";
          margs += "|";
          margs += "path_raw="+raw_jpgBatchToProcess.get(i).getAbsolutePath();
          margs += "|";
          margs += "path_out="+outDirStr;
          margs += "|";
          margs += "filter_radius="+filter_radius_raw;
          margs += "|";
          margs += "normalize_photos="+normalize;
          //IJ.log(margs);
          IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessRAW.ijm", margs);
        }

        // CALL MACRO FOR DNG HERE
        if( inImageExt.toUpperCase().equals("DNG") ){
          String margs = "";
          margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".DNG";
          margs += "|";
          margs += "path_raw="+raw_jpgBatchToProcess.get(i).getAbsolutePath();
          margs += "|";
          margs += "path_out="+outDirStr;
          margs += "|";
          margs += "filter_radius="+filter_radius_raw;
          //IJ.log(margs);
          IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessDNG.ijm", margs);
        }

        if( containsDNGOnly(raw_jpgBatchToProcess.iterator()) ){
            // Get camera model (dng version)
        //    CopyEXIFDNGData(imageEXIFData, PATH_TO_EXIFTOOL, outDirStr+inImageNoExt+".tif");
            CopyEXIFData(OS, PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i).getAbsolutePath(), outDirStr+inImageNoExt+".tif");
            //IJ.log("Done copying EXIF data");
        }else{
            CopyEXIFData(OS, PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i+1).getAbsolutePath(), outDirStr+inImageNoExt+".tif");
        }

        if( containsDNGOnly(raw_jpgBatchToProcess.iterator()) ){
            continue;
        }


        // Process JPG
        // CALL MACRO HERE
        IJ.log( (String)"Processing image " + raw_jpgBatchToProcess.get(i+1).getName() + " (" + imgcounter + " of " + raw_jpgBatchToProcess.size() + " - " + (int)((double)imgcounter/((double)raw_jpgBatchToProcess.size())*100) + "% complete" + ")" );
        imgcounter++;
        String margs = "";
        margs = "";
        margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".JPG";
        margs += "|";
        margs += "path_jpg="+raw_jpgBatchToProcess.get(i+1).getAbsolutePath();
        margs += "|";
        margs += "path_out="+outDirStr;
        margs += "|";
        margs += "filter_radius="+filter_radius_jpg;
        //IJ.log(margs);
        if ( filter_radius_jpg != "0" )
        {
            IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessJPG.ijm", margs);
        }

        inImageParts = (raw_jpgBatchToProcess.get(i+1).getName()).split("\\.(?=[^\\.]+$)");
        inImageNoExt = inImageParts[0];

        // This copies the already cleared exif data
        if( outDirStr.equals(inDirStr) ){
          // Get the exif data from original Folder if output directory is input directory
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, outDirStr+"original\\"+inImageNoExt+".jpg", outDirStr+inImageNoExt+".jpg");
        }else{
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i+1).getAbsolutePath(), outDirStr+inImageNoExt+".jpg");
        }

      }

    }

    else if( !jpgBatchToProcess.isEmpty() ){
      // JPG only case

      for( int i=0; i<jpgBatchToProcess.size(); i++ ){
          filter_radius_raw = tmp_f_r_raw;
          filter_radius_jpg = tmp_f_r_jpg;

        inImageParts = (filesToProcess[i].getName()).split("\\.(?=[^\\.]+$)");
        if( inImageParts.length < 2 ){
          IJ.log("Could not find extension of " + filesToProcess[i].getName());
          continue;
        } else{
          inImageNoExt = inImageParts[0];
          inImageExt = inImageParts[1];
        }


        NextModel = GetCameraModel(jpgBatchToProcess.get(i).getAbsolutePath());
        //IJ.log("Camera Model for " + jpgBatchToProcess.get(i).getAbsolutePath() + ": " + NextModel);
        pathToCSV = GetEXIFCSV(NextModel);
        imageEXIFData = new CameraEXIF( new EXIFToolsReader(PATH_TO_EXIFTOOL, jpgBatchToProcess.get(i).getAbsolutePath()) );
        //IJ.log("Path To CSV: " + pathToCSV);
        defaultEXIFData = new CameraEXIF( new EXIFCSVReader(pathToCSV) );


        //IJ.log("EXIF data match up? " + imageEXIFData.equals(defaultEXIFData) );
        if( removeVignette && !imageEXIFData.equals(defaultEXIFData) ){
            GenericDialog dialog = showCameraSettingsNotEqualDialog(defaultEXIFData.printEXIFData(), imageEXIFData.printEXIFData(), jpgBatchToProcess.get(i).getName() );
            if( dialog.wasOKed() ){

            }else if( dialog.wasCanceled() ){
                return;
            }else{
                // Don't remove vignette
                filter_radius_raw = "0";
                filter_radius_jpg = "0";
                //removeVignette = false;
            }
        }

        if( NextModel.equals("CAMERA_NOT_SUPPORTED") ){
          IJ.log("The image you are trying to process was not taken with a compatable camera. I will skip this image");
          continue;
        }
          CurrentModel = NextModel;
          FlatField = getFFFile(CurrentModel);

        // Process JPG
        // CALL MACRO HERE
        IJ.log( (String)"Processing image " + jpgBatchToProcess.get(i).getName() + " (" + imgcounter + " of " + jpgBatchToProcess.size() + " - " + (int)((double)imgcounter/((double)jpgBatchToProcess.size())*100) + "% complete" + ")" );
        imgcounter++;
        String margs = "";
        margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".JPG";
        margs += "|";
        margs += "path_jpg="+jpgBatchToProcess.get(i).getAbsolutePath();
        margs += "|";
        margs += "path_out="+outDirStr;
        margs += "|";
        margs += "filter_radius="+filter_radius_jpg;
        //IJ.log(margs);
        IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessJPG.ijm", margs);

        //String[] inImageParts = (jpgBatchToProcess.get(i).getName()).split("\\.(?=[^\\.]+$)");
        //String inImageNoExt = inImageParts[0];

        // This copies the already cleared exif data
        if( outDirStr.equals(inDirStr) ){
          // Get the exif data from original Folder if output directory is input directory
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, outDirStr+"original\\"+inImageNoExt+".jpg", outDirStr+inImageNoExt+".jpg");
        }else{
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, jpgBatchToProcess.get(i).getAbsolutePath(), outDirStr+inImageNoExt+".jpg");
        }

      }

    }

    if( !outDirStr.equals(inDirStr) ){
      DeleteOriginals = false;
    }
    if( DeleteOriginals == true ){
      deleteOriginals(outDirStr+"original");
    }

    // Delete any tmp files that might exists
    deleteTmps(outDirStr);
    IJ.log("I am done processing images. Goodbye!");

  }

  public String GetEXIFCSV(String model){
      String workingDirectory = IJ.getDirectory("imagej");
      String valuesDirectory = workingDirectory+"Survey2\\Values\\";
      String csvpath = null;

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
          csvpath = valuesDirectory+"FC330_ndvi\\FC330_ndvi.csv";
      }else if( model.equals("FC300X") ){
          csvpath = valuesDirectory+"FC300X_ndvi\\FC300X_ndvi.csv";
      }else if( model.equals("FC300S") ){
          csvpath = valuesDirectory+"FC300S_ndvi\\FC300S_ndvi.csv";
      }else if( model.equals("Survey2_RGB") ){
          csvpath = valuesDirectory+"rgb\\rgb.csv";
      }

      return csvpath;
  }

  /*
   * Return the camera model that the img was taken with.
   * @param img - full path to image
   * @return String - camera model
   */
  public String GetCameraModel(String img){
    String console = null;
    String c_arg = null;
    String command = null;

    if( this.OS.contains("Windows") ){
      console = "cmd";
      c_arg = "/c";
      command = PATH_TO_EXIFTOOL + "exiftool.exe" + " " + "\""+img+"\"";
    }else{
      console = "sh";
      c_arg = "-c";
      command = PATH_TO_EXIFTOOL + "exiftool" + " " + "\'"+img+"\'";
    }


    String line = null;
    try{
      //IJ.log(PATH_TO_EXIFTOOL);
      //IJ.log(command);
      //IJ.log(this.OS);
      ProcessBuilder bob = new ProcessBuilder(console, c_arg, command);
      bob.redirectErrorStream(true);
      final Process proc = bob.start();

      BufferedReader proc_out = new BufferedReader( new InputStreamReader(proc.getInputStream()));

      do{
        line = proc_out.readLine();
        //IJ.log(line);


        if( line != null ){
          if( line.matches( ".*Survey2_(BLUE|RED|GREEN|RGB|IR|NDVI)|.*FC350|.*FC300(S|X)|.*FC330" )){
            break;
          }
        }

        //IJ.log(line);
      }while( line != null );

    }catch( IOException e){
      e.printStackTrace();
    }

    if( line.matches(".*Survey2_BLUE") ){
      return "Survey2_BLUE";
    }else if( line.matches(".*Survey2_RGB") ){
      return "Survey2_RGB";
    }else if( line.matches(".*Survey2_RED") ){
      return "Survey2_RED";
    }else if( line.matches(".*Survey2_GREEN") ){
      return "Survey2_GREEN";
    }else if( line.matches(".*Survey2_RGB") ){
      return "Survey2_RGB";
    }else if( line.matches(".*Survey2_IR") ){
      return "Survey2_IR";
    }else if( line.matches(".*Survey2_NDVI") ){
      return "Survey2_NDVI";
    }else if( line.matches(".*FC350") ){
      return "FC350";
    }else if( line.matches(".*FC330") ){
      return "FC330";
    }else if( line.matches(".*FC300X") ){
      return "FC300X";
    }else if( line.matches(".*FC300S") ){
      return "FC300S";
    }else{
      return "CAMERA_NOT_SUPPORTED";
    }
  }

  public String getDNGCameraModel(CameraEXIF exifdata){
      // Open logfile
      String line = exifdata.getCameraModel();
      //String line = "Survey2_RED";

      if( line.matches(".*Survey2_BLUE") ){
        return "Survey2_BLUE";
      }else if( line.matches(".*Survey2_RED") ){
        return "Survey2_RED";
      }else if( line.matches(".*Survey2_RGB") ){
        return "Survey2_RGB";
      }else if( line.matches(".*Survey2_GREEN") ){
        return "Survey2_GREEN";
      }else if( line.matches(".*Survey2_RGB") ){
        return "Survey2_RGB";
      }else if( line.matches(".*Survey2_IR") ){
        return "Survey2_IR";
      }else if( line.matches(".*Survey2_NDVI") ){
        return "Survey2_NDVI";
      }else if( line.matches(".*FC350") ){
        return "FC350";
      }else if( line.matches(".*FC330") ){
        return "FC330";
      }else if( line.matches(".*FC300X") ){
        return "FC300X";
      }else if( line.matches(".*FC300S") ){
        return "FC300S";
      }else{
        return "CAMERA_NOT_SUPPORTED";
      }
  }

  public GenericDialog showNotCorrectFileStructure(){
      GenericDialog dialog = new GenericDialog("Problem with Input Directory");
      dialog.addMessage("It seems RAW images are not followed by JPG images (or no JPG images exist in the directory). ");
      dialog.addMessage("If you supply RAW images, they must be followed by its corrisponding JPG version.");
      dialog.addMessage("Please fix the input file directory by supplying JPG files.");
      //dialog.hideCancelButton();
      dialog.setOKLabel("Debug");
      dialog.setCancelLabel("Quit");

      dialog.showDialog();

      return dialog;
  }

  public ImagePlus[] OpenFF(String path, int filterRadius){
    String[] inImageParts = path.split("\\.(?=[^\\.]+$)");
    String inImageExt = inImageParts[1];

    String flatFieldImage = null;


    ImagePlus fredff = null;
    ImagePlus fgreenff = null;
    ImagePlus fblueff = null;


  	if( (inImageExt.toUpperCase()).equals("JPG") ) {
      //IJ.log("Opening " + path);

      IJ.run("Open [Image IO]", "image="+ "[" + path + "]");
      ImagePlus jpg = IJ.getImage();
      String jpgname = jpg.getTitle();

      IJ.run(jpg, "Split Channels", "");

      // Process red channel
      IJ.selectWindow(jpgname + " (red)");
      ImagePlus jpgR = IJ.getImage();
      IJ.run(jpgR, "Mean...", "radius="+String.valueOf(filterRadius));
      IJ.run(jpgR, "32-bit", "");
      ImageStatistics redstat = jpgR.getStatistics();
      IJ.run(jpgR, "Macro...", "code=v=" + String.valueOf(redstat.max) + "/v");
      IJ.selectWindow(jpgname + " (red)");
      fredff = IJ.getImage();

      // Process green channel
      IJ.selectWindow(jpgname + " (green)");
      ImagePlus jpgG = IJ.getImage();
      IJ.run(jpgG, "Mean...", "radius="+String.valueOf(filterRadius));
      IJ.run(jpgG, "32-bit", "");
      ImageStatistics greenstat = jpgG.getStatistics();
      IJ.run(jpgG, "Macro...", "code=v=" + String.valueOf(greenstat.max) + "/v");
      IJ.selectWindow(jpgname + " (green)");
      fgreenff = IJ.getImage();

      // Process blue channel
      IJ.selectWindow(jpgname + " (blue)");
      ImagePlus jpgB = IJ.getImage();
      IJ.run(jpgB, "Mean...", "radius="+String.valueOf(filterRadius));
      IJ.run(jpgB, "32-bit", "");
      ImageStatistics bluestat = jpgB.getStatistics();
      IJ.run(jpgB, "Macro...", "code=v=" + String.valueOf(bluestat.max) + "/v");
      IJ.selectWindow(jpgname + " (blue)");
      fblueff = IJ.getImage();

    }

    if( (inImageExt.toUpperCase()).equals("RAW") ){
      //IJ.log("Opening " + path);

      IJ.run("Raw...", "open="+ "[" + path + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
      ImagePlus raw = IJ.getImage();


      IJ.run(raw, "Debayer Image", "order=G-B-G-B demosaicing=Replication radius=2 radius=2");
      ImagePlus debayed = IJ.getImage();
      raw.changes = false;
      raw.close();

      IJ.run(debayed, "Make Composite", "display=or");
      IJ.wait(1000);
      //debayed.changes = false;
      //debayed.close();
      //debayed.changes = true;
      IJ.selectWindow("RGB Stack");
      debayed = IJ.getImage();

      ImageStack imgstk = debayed.getImageStack();
      imgstk.setSliceLabel("RedFlat", 1);
      imgstk.setSliceLabel("GreenFlat", 2);
      imgstk.setSliceLabel("BlueFlat", 3);
      debayed.setStack(imgstk);

      IJ.run(debayed, "Stack to Images", "");


      // Process red stack
      IJ.selectWindow("RedFlat");
      ImagePlus redff = IJ.getImage();
      IJ.run(redff, "Mean...", "radius="+filterRadius);
      IJ.run(redff, "32-bit", "");
      ImageStatistics redstat = redff.getStatistics();
      IJ.run(redff, "Macro...", "code=v=" + String.valueOf(redstat.max) + "/v");
      IJ.selectWindow("RedFlat");
      fredff = IJ.getImage();

      // Process green stack
      IJ.selectWindow("GreenFlat");
      ImagePlus greenff = IJ.getImage();
      IJ.run(greenff, "Mean...", "radius="+filterRadius);
      IJ.run(greenff, "32-bit", "");
      ImageStatistics greenstat = greenff.getStatistics();
      IJ.run(greenff, "Macro...", "code=v=" + String.valueOf(greenstat.max) + "/v");
      IJ.selectWindow("GreenFlat");
      fgreenff = IJ.getImage();

      // Process blue stack
      IJ.selectWindow("BlueFlat");
      ImagePlus blueff = IJ.getImage();
      IJ.run(blueff, "Mean...", "radius="+filterRadius);
      IJ.run(blueff, "32-bit", "");
      ImageStatistics bluestat = blueff.getStatistics();
      IJ.run(blueff, "Macro...", "code=v=" + String.valueOf(bluestat.max) + "/v");
      IJ.selectWindow("BlueFlat");
      fblueff = IJ.getImage();

      //debayed.changes = false;
      //debayed.close();

    }

    ImagePlus[] farray = new ImagePlus[3];
    farray[0] = fredff;
    farray[1] = fgreenff;
    farray[2] = fblueff;

    return farray;

  }

  /*
   * Extracts the camera lens used from the Camera Model and returns it.
   * @param: model - Model of camera
   */
  public String getFFFile(String model ){
    String file = null;

    if( model.equals("Survey2_RED") ){
      file = "red";
    }else if( model.equals("Survey2_BLUE") ){
      file = "blue";
    }else if( model.equals("Survey2_GREEN") ){
      file = "green";
    }else if( model.equals("Survey2_RGB") ){
      file = "rgb";
    }else if( model.equals("Survey2_IR") ){
      file = "ir";
    }else if( model.equals("Survey2_NDVI") ){
      file = "ndvi";
    }else if( model.equals("FC330") ){
        // Phantom 4
      file = "FC330_ndvi";
    }else if( model.equals("FC300X") ){
        // Using model S for now
      file = "FC300S_ndvi";
    }else if( model.equals("FC300S") ){
        // Phantom 3
      file = "FC300S_ndvi";
    }else if( model.equals("FC350") ){
      file = "FC350_ndvi";
    }

    return file;
  }

  public ImagePlus ProcessRAW(String path, int filterRadius, ImagePlus[] flatField){
    IJ.run("Raw...", "open="+ "[" + path + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
    ImagePlus raw = IJ.getImage();

    IJ.run(raw, "Debayer Image", "order=G-B-G-B demosaicing=Replication radius=2 radius=2");
    IJ.wait(1000);
    raw.changes = false;
    raw.close();
    IJ.selectWindow("RGB Stack");
    ImagePlus debayed = IJ.getImage();


    IJ.run(debayed, "Make Composite", "display=Color");
    IJ.wait(1000);
    debayed.changes = false;
    debayed.close();
    IJ.selectWindow("RGB Stack");
    debayed = IJ.getImage();


    ImageStack imgstk = debayed.getImageStack();
    if( filterRadius == 0){
      imgstk.setSliceLabel("Red", 1);
      imgstk.setSliceLabel("Green", 2);
      imgstk.setSliceLabel("Blue", 3);
    }else{
      imgstk.setSliceLabel("RedIn", 1);
      imgstk.setSliceLabel("GreenIn", 2);
      imgstk.setSliceLabel("BlueIn", 3);
    }
    debayed.setStack(imgstk);


    // Remove vignette
    if( filterRadius != 0 ){
      IJ.run(debayed, "Stack to Images", "");
      IJ.wait(1000);
      //debayed.changes = false;
      //debayed.close();

      // Process red channel
      IJ.selectWindow("RedIn");
      ImagePlus red = IJ.getImage();
      IJ.run(red, "Calculator Plus", "i1="+"["+flatField[0].getTitle()+"]"+" i2="+"["+red.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
      IJ.wait(1000);
      ImagePlus result1 = IJ.getImage();
      result1.setTitle("Result1");
      red.changes = false;
      red.close();

      // Process green channel
      IJ.selectWindow("GreenIn");
      ImagePlus green = IJ.getImage();
      IJ.run(green, "Calculator Plus", "i1="+"["+flatField[1].getTitle()+"]"+" i2="+"["+green.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
      IJ.wait(1000);
      ImagePlus result2 = IJ.getImage();
      result2.setTitle("Result2");
      green.changes = false;
      green.close();

      // Process blue channel
      IJ.selectWindow("BlueIn");
      ImagePlus blue = IJ.getImage();
      IJ.run(blue, "Calculator Plus", "i1="+"["+flatField[2].getTitle()+"]"+" i2="+"["+blue.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
      IJ.wait(1000);
      ImagePlus result3 = IJ.getImage();
      result3.setTitle("Result3");
      blue.changes = false;
      blue.close();

      IJ.run("Merge Channels...", "c1=Result1 c2=Result2 c3=Result3 create");
      IJ.wait(1000);
      IJ.selectWindow("Composite");
      ImagePlus composite = IJ.getImage();
      ImageStack cmpstk = composite.getImageStack();
      cmpstk.setSliceLabel("Red", 1);
      cmpstk.setSliceLabel("Green", 2);
      cmpstk.setSliceLabel("Blue", 3);
      composite.setStack(cmpstk);

      debayed = composite;
    }

    return debayed;

  }

  public ImagePlus ProcessJPG(String path, int filterRadius, ImagePlus[] flatField){
    ImagePlus jpg = IJ.openImage(path);

    IJ.run(jpg, "Split Channels", "");
    IJ.wait(1000);


    // Get Red channel
    IJ.selectWindow(jpg.getTitle() + " (red)");
    ImagePlus red = IJ.getImage();
    IJ.run(red, "Calculator Plus", "i1="+"["+flatField[0].getTitle()+"]"+" i2="+"["+red.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
    IJ.wait(1000);
    IJ.selectWindow("Result");
    ImagePlus result1 = IJ.getImage();
    result1.setTitle("Result1");
    red.changes = false;
    red.close();

    // Get Green channel
    IJ.selectWindow(jpg.getTitle() + " (green)");
    ImagePlus green = IJ.getImage();
    IJ.run(green, "Calculator Plus", "i1="+"["+flatField[1].getTitle()+"]"+" i2="+"["+green.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
    IJ.wait(1000);
    IJ.selectWindow("Result");
    ImagePlus result2 = IJ.getImage();
    result2.setTitle("Result2");
    green.changes = false;
    green.close();

    // Get Blue channel
    IJ.selectWindow(jpg.getTitle() + " (blue)");
    ImagePlus blue = IJ.getImage();
    IJ.run(blue, "Calculator Plus", "i1="+"["+flatField[2].getTitle()+"]"+" i2="+"["+blue.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
    IJ.wait(1000);
    IJ.selectWindow("Result");
    ImagePlus result3 = IJ.getImage();
    result3.setTitle("Result3");
    blue.changes = false;
    blue.close();

    IJ.run("Merge Channels...", "c1=Result1 c2=Result2 c3=Result3 create");
    IJ.wait(1000);
    IJ.selectWindow("Composite");
    ImagePlus composite = IJ.getImage();

    return composite;
  }

  public void CopyEXIFDNGData(CameraEXIF mdata, String exiftoolpath, String imgpath){
      // Tags
      final String TAG_EXPOSURE_TIME = "ExposureTime";
      final String TAG_ISO = "ISO";
      final String TAG_CMODEL = "Model";
      final String TAG_CMAKE = "Make";
      final String TAG_FSTOP = "FNumber";
      final String TAG_EXPOSURE_BIAS = "ExposureCompensation";
      final String TAG_FOCAL_LENGTH = "FocalLength";
      final String TAG_WHITE_BALANCE = "WhiteBalance";
      final String TAG_TIME_STAMP = "CreateDate";
      final String TAG_APERTURE = "ApertureValue";
      final String TAG_THUMB_SIZE = "";
      final String TAG_FULL_SIZE = "";
      final String TAG_IMAGE_SIZE = "ImageSize";
      final String TAG_OUTPUT_SIZE = "";
      final String TAG_FILTER_PATTERN = "";
      final String TAG_DAYLIGHT_MULTIPLYER = "";
      final String TAG_CAMERA_MULTIPLYER = "";

      Formatter formatter = new Formatter();

      // Start running exiftool
      String command = null;

      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_EXPOSURE_TIME, mdata.getExposureTime(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_ISO, mdata.getISOSpeed(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_CMAKE, mdata.getCameraMaker(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_CMODEL, mdata.getCameraModel(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_FSTOP, mdata.getFStop(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_EXPOSURE_BIAS, mdata.getExposureBias(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_FOCAL_LENGTH, mdata.getFocalLength(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_WHITE_BALANCE, mdata.getWhiteBalance(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_TIME_STAMP, mdata.getTimeStamp(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_APERTURE, mdata.getApeture(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      formatter = new Formatter();
      formatter = formatter.format(exiftoolpath + "exiftool.exe -%1$s=%2$s \"%3$s\"", TAG_IMAGE_SIZE, mdata.getImageSize(), imgpath);
      command = formatter.toString();
      runCommand(command);
      IJ.log("Writting EXIF to " + imgpath);
      IJ.log(command);

      // Repeat this for all tags
  }

  public void runCommand(String command){
      String console = null;
      String c_arg = null;
      ProcessBuilder bob = null;
      Process proc = null;

      if( System.getProperty("os.name").contains("Windows") ){
          console = "cmd";
          c_arg = "/c";
      }else{
          console = "sh";
          c_arg = "-c";
      }

      try{
        bob = new ProcessBuilder(console, c_arg, command);
        bob.redirectErrorStream(false);
        proc = bob.start();
        proc.waitFor();

      }catch( IOException e){
        e.printStackTrace();
      }catch( InterruptedException i ){
        i.printStackTrace();
      }
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
        command = exiftoolpath + "exiftool.exe" + " -overwrite_original -tagsfromfile " + "\""+refimg+"\"" + " " + "\""+targimg+"\"";
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
        command = exiftoolpath + "exiftool" + " -overwrite_original -tagsfromfile " + "\'"+refimg+"\'" + " " + "\'"+targimg+"\'";
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


  public void backUpImages( String dir ){
    String console = null;
    String c_arg = null;
    String command = null;
    ProcessBuilder bob = null;
    Process proc = null;

    if( this.OS.contains("Windows") ){
      console = "cmd";
      c_arg = "/c";
      command = "xcopy /Y " + "\""+dir+"*.jpg"+"\" " + "\""+dir+"original\\\"";
    }else{
      console = "sh";
      c_arg = "-c";
      command = "cp -f " + "\'"+dir+"*.jpg\' " + "\'"+dir+"original\\\'";
    }

    try{
      //command = "xcopy " + "\""+dir+"*.jpg"+"\" " + "\""+dir+"original\\\"";
      //IJ.log("Executing Command: " + command);
      bob = new ProcessBuilder(console, c_arg, command);
      bob.redirectErrorStream(false);
      proc = bob.start();
      //proc.waitFor();

      BufferedReader proc_out = new BufferedReader( new InputStreamReader(proc.getInputStream()));
      String line = null;
      do{
        line = proc_out.readLine();
        IJ.log(line);
      }while( line != null );


    }catch( IOException e ){
      e.printStackTrace();
    }/*catch( InterruptedException i ){
      i.printStackTrace();
    }*/

    IJ.log("Finished backing up images");

  }

  public void deleteTmps(String path){
      File dir = new File(path);

      String[] fileStr = dir.list();
      File deleteFile = null;

      for( int i=0; i<fileStr.length; i++ ){
          deleteFile = new File(fileStr[i]);
          if( deleteFile.getName().contains("tmp") || deleteFile.getName().contains("temp") || deleteFile.getName().contains("_original") ){
              deleteFile.delete();
          }
      }
      return;
  }

  public void deleteOriginals(String path){
    /*
    String console = null;
    String c_arg = null;
    String command = null;
    ProcessBuilder bob = null;
    Process proc = null;

    if( this.OS.contains("Windows") ){
      console = "cmd";
      c_arg = "/c";
      command = "\"Rmdir /s /q " + path + "\"";
    }else{
      console = "sh";
      c_arg = "-c";
      command = "rm -rf " + path;
    }

    try{
      //command = "xcopy " + "\""+dir+"*.jpg"+"\" " + "\""+dir+"original\\\"";
      IJ.log("Executing Command: " + command);
      bob = new ProcessBuilder(console, c_arg, command);
      bob.redirectErrorStream(false);
      proc = bob.start();
      //proc.waitFor();

      BufferedReader proc_out = new BufferedReader( new InputStreamReader(proc.getInputStream()));
      String line = null;
      do{
        line = proc_out.readLine();
        IJ.log(line);
      }while( line != null );


    }catch( IOException e ){
      e.printStackTrace();
    }/*catch( InterruptedException i ){
      i.printStackTrace();
    }*/
    File directory = new File(path);
    if( directory.exists() ){
      IJ.log("Deleting backup images: " + path);

      File[] files = directory.listFiles();
      if( files != null ){
        for( int i=0; i<files.length; i++ ){
          files[i].delete();
        }
      }

      if( directory.delete() ){
        IJ.log("Delete successful");
      }else{
        IJ.log("Delete not soccessful");
      }
    }


    IJ.log("Finished deleting backups");
  }

  public GenericDialog showCameraSettingsNotEqualDialog(String exifdata1, String exifdata2, String image){
      GenericDialog dialog = new GenericDialog("Attention!");

      dialog.addMessage("The camera settings of the current image to process");
      dialog.addMessage("does not match the camera settings the flat-field image was taken with.");
      dialog.addMessage("Attempting vignette removal might cause undesired results.");
      dialog.addMessage("Do you wish to continue?");
      dialog.enableYesNoCancel("Continue anyway", "Don't remove vignette");

      dialog.addTextAreas("Flat-Field EXIF Data:\n" + exifdata1, "Image EXIF Data: " + image + "\n" + exifdata2, 5, 30);
      dialog.setCancelLabel("Quit");

      dialog.showDialog();

      return dialog;
  }

  public boolean containsDNGOnly(Iterator<File> it){
      boolean hasDNGOnly = false;
      File nextFile = null;
      String ext = null;
      while (it.hasNext()){
          nextFile = it.next();
          // Get extension
          ext = getFileExtension(nextFile);
          // Only check for extension if the file not a folder
          if( ext != null ){
              hasDNGOnly = true;// Assume true if there are files with extension
              if( !ext.toUpperCase().equals("DNG") ){
                  hasDNGOnly = false;
                  break;
              }
          }
      }
      return hasDNGOnly;
  }

  public String getFileExtension(File file){
      String filename = file.getName();
      String[] parts = filename.split("\\.(?=[^\\.]+$)");

      if( parts.length != 2 ){
        return null;
      }
      return parts[1];
  }

}
