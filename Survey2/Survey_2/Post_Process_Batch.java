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

  private String OS = null;

  private String CurrentModel = null;

  private String WorkingDirectory = null;
  private String FLAT_FIELD_DIRECTORY = null;

  private Boolean DeleteOriginals = true;


  class FileComparator implements Comparator<File>{
    @Override
    public int compare(File a, File b){
      return a.getName().compareTo(b.getName());
    }
  }


  public void run(String args){
    //IJ.showMessage("Post_Process", "Hello world");
    WorkingDirectory = IJ.getDirectory("imagej");
    OS = System.getProperty("os.name");
    // WINDOWS SUPPORT ONLY
    PATH_TO_EXIFTOOL = WorkingDirectory+"Survey2\\EXIFTool\\exiftool.exe";
    //PATH_TO_EXIFTOOL = "/usr/bin/exiftool";
    FLAT_FIELD_DIRECTORY = WorkingDirectory+"Survey2\\Flat-Fields\\";

    //@TODO: Ask user if he/she wants to replace original if inDir = outDir
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

    IJ.log("Input Directory: " + inDirStr);
    IJ.log("Output Directory: " + outDirStr);

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
        raw_jpgBatchToProcess.add(filesToProcess[i+1]);
        i++; // Skip following JPG in the search
      }else if( inImageExt.toUpperCase().equals("JPG") ){
        jpgBatchToProcess.add(filesToProcess[i]);
      }
    }

    Collections.sort(jpgBatchToProcess);
    Collections.sort(raw_jpgBatchToProcess);

    /*
    IJ.log("jpg:" + jpgBatchToProcess.size());
    IJ.log("raw:" + raw_jpgBatchToProcess.size());
    */

    //IJ.log("------------------------------------------------------------");
    IJ.log("Images that will be proccessed: ");
    for( int i=0; i<raw_jpgBatchToProcess.size(); i++ ){
      IJ.log(raw_jpgBatchToProcess.get(i).getName());
    }
    for( int i=0; i<jpgBatchToProcess.size(); i++ ){
      IJ.log(jpgBatchToProcess.get(i).getName());
    }
    IJ.log("-----------------------------------------------------------------");
    IJ.log("I will begin to process the images mentioned above. Please wait.");

    IJ.log("\n\n");


    // Begin to process all images in input directory
    //CurrentModel = ( GetCameraModel(jpgFilesToProcess.get(0).getAbsolutePath()) );
    IJ.log("Working Directory: " + WorkingDirectory);


    CurrentModel = "";
    String NextModel = null;
    String FlatField = null;
    ImagePlus[] rawFactorArray = null;
    ImagePlus[] jpgFactorArray = null;
    ImagePlus RAWProcessed = null;
    ImagePlus JPGProcessed = null;

    if( !raw_jpgBatchToProcess.isEmpty() ){
      // Process RAW + JPG

      // Iterate through each RAW file only!
      // Index JPG by i+1 each time.
      for( int i=0; i<raw_jpgBatchToProcess.size(); i=i+2 ){
        IJ.log("\n\n");

        NextModel = GetCameraModel(raw_jpgBatchToProcess.get(i+1).getAbsolutePath());
        IJ.log("Camera Model for " + raw_jpgBatchToProcess.get(i+1).getAbsolutePath() + ": " + NextModel);

        if( NextModel.equals("CAMERA_NOT_SUPPORTED") ){
          IJ.log("The image you are trying to process was not taken with a compatable camera. I will skip this image");
          continue;
        }
          CurrentModel = NextModel;
          FlatField = getFFFile(CurrentModel);

        // Process RAW
        IJ.log("Processing: " + raw_jpgBatchToProcess.get(i).getAbsolutePath());
        // CALL MACRO HERE
        String margs = "";
        margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".RAW";
        margs += "|";
        margs += "path_raw="+raw_jpgBatchToProcess.get(i).getAbsolutePath();
        margs += "|";
        margs += "path_out="+outDirStr;
        margs += "|";
        margs += "filter_radius="+"1";
        IJ.log(margs);
        IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessRAW.ijm", margs);

        String[] inImageParts = (raw_jpgBatchToProcess.get(i).getName()).split("\\.(?=[^\\.]+$)");
        String inImageNoExt = inImageParts[0];

        CopyEXIFData(OS, PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i+1).getAbsolutePath(), outDirStr+inImageNoExt+".tif");


        // Process JPG
        // CALL MACRO HERE
        IJ.log("Processing: " + raw_jpgBatchToProcess.get(i+1).getAbsolutePath());

        margs = "";
        margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".JPG";
        margs += "|";
        margs += "path_jpg="+raw_jpgBatchToProcess.get(i+1).getAbsolutePath();
        margs += "|";
        margs += "path_out="+outDirStr;
        margs += "|";
        margs += "filter_radius="+"10";
        IJ.log(margs);
        IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessJPG.ijm", margs);

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

    if( !jpgBatchToProcess.isEmpty() ){
      // JPG only case

      for( int i=0; i<jpgBatchToProcess.size(); i++ ){
        IJ.log("\n\n");

        NextModel = GetCameraModel(jpgBatchToProcess.get(i).getAbsolutePath());
        IJ.log("Camera Model for " + jpgBatchToProcess.get(i).getAbsolutePath() + ": " + NextModel);

        if( NextModel.equals("CAMERA_NOT_SUPPORTED") ){
          IJ.log("The image you are trying to process was not taken with a compatable camera. I will skip this image");
          continue;
        }
          CurrentModel = NextModel;
          FlatField = getFFFile(CurrentModel);

        // Process JPG
        // CALL MACRO HERE
        IJ.log("Processing: " + jpgBatchToProcess.get(i).getAbsolutePath());
        String margs = "";
        margs += "path_ff="+FLAT_FIELD_DIRECTORY+FlatField+"\\"+FlatField+".JPG";
        margs += "|";
        margs += "path_jpg="+jpgBatchToProcess.get(i).getAbsolutePath();
        margs += "|";
        margs += "path_out="+outDirStr;
        margs += "|";
        margs += "filter_radius="+"10";
        IJ.log(margs);
        IJ.runMacroFile(WorkingDirectory+"Survey2\\Macros\\ProcessJPG.ijm", margs);

        String[] inImageParts = (jpgBatchToProcess.get(i).getName()).split("\\.(?=[^\\.]+$)");
        String inImageNoExt = inImageParts[0];

        // This copies the already cleared exif data
        if( outDirStr.equals(inDirStr) ){
          // Get the exif data from original Folder if output directory is input directory
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, outDirStr+"original\\"+inImageNoExt+".jpg", outDirStr+inImageNoExt+".jpg");
        }else{
          CopyEXIFData(OS, PATH_TO_EXIFTOOL, raw_jpgBatchToProcess.get(i+1).getAbsolutePath(), outDirStr+inImageNoExt+".jpg");
        }

      }

    }

    if( !outDirStr.equals(inDirStr) ){
      DeleteOriginals = false;
    }
    if( DeleteOriginals == true ){
      deleteOriginals(outDirStr+"original");
    }
    IJ.log("I am done processing images. Goodbye!");

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
      command = PATH_TO_EXIFTOOL + " " + "\""+img+"\"";
    }else{
      console = "sh";
      c_arg = "-c";
      command = PATH_TO_EXIFTOOL + " " + "\'"+img+"\'";
    }


    String line = null;
    try{
      IJ.log(PATH_TO_EXIFTOOL);
      IJ.log(command);
      IJ.log(this.OS);
      ProcessBuilder bob = new ProcessBuilder(console, c_arg, command);
      bob.redirectErrorStream(true);
      final Process proc = bob.start();

      BufferedReader proc_out = new BufferedReader( new InputStreamReader(proc.getInputStream()));

      do{
        line = proc_out.readLine();
        //IJ.log(line);


        if( line != null ){
          if( line.matches( ".*Survey2_(BLUE|RED|GREEN|RGB|IR|NDVI)" )){
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
    }else{
      return "CAMERA_NOT_SUPPORTED";
    }
  }

  public ImagePlus[] OpenFF(String path, int filterRadius){
    String[] inImageParts = path.split("\\.(?=[^\\.]+$)");
    String inImageExt = inImageParts[1];

    String flatFieldImage = null;


    ImagePlus fredff = null;
    ImagePlus fgreenff = null;
    ImagePlus fblueff = null;


  	if( (inImageExt.toUpperCase()).equals("JPG") ) {
      IJ.log("Opening " + path);

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
      IJ.log("Opening " + path);

      IJ.run("Raw...", "open="+ "[" + path + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
      ImagePlus raw = IJ.getImage();


      IJ.run(raw, "Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
      ImagePlus debayed = IJ.getImage();
      raw.changes = false;
      raw.close();

      IJ.run(debayed, "Make Composite", "display=Color");
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
    }

    return file;
  }

  public ImagePlus ProcessRAW(String path, int filterRadius, ImagePlus[] flatField){
    IJ.run("Raw...", "open="+ "[" + path + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
    ImagePlus raw = IJ.getImage();

    IJ.run(raw, "Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
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
    IJ.run(red, "Calculator Plus", "i1="+"["+flatField[1].getTitle()+"]"+" i2="+"["+green.getTitle()+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
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
        IJ.log("Executing command: " + command);
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
        IJ.log("Executing command: " + command);
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

    IJ.log("Finished writing EXIF data");

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

    IJ.log("Finished backing up images");

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

}
