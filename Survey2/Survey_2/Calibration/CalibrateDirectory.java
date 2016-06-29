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

public class CalibrateDirectory implements PlugIn{

  private boolean useQR = false;
  private String qrDir = "";
  private String qrFilename = "";
  private String qrPath = "";
  private String qrCamera = "";

  private String cameraType = "";
  private RGBPhoto qrPhoto = null;

  private String inputDir = "";
  private String outputDir = "";

  public void run(String arg){
    CalibrationPrompt prompts = CalibrationPrompt.getPrompt();
    prompts.showFullDialog();


    HashMap<String, String> fullDialogValues = prompts.getFullDialogValues();
    HashMap<String, String> qrFileDialogValues = null;
    useQR = Boolean.parseBoolean( fullDialogValues.get(CalibrationPrompt.MAP_USEQR) );
    IJ.log(fullDialogValues.get(CalibrationPrompt.MAP_USEQR));
    IJ.log( String.valueOf(useQR) );
    cameraType = fullDialogValues.get(CalibrationPrompt.MAP_CAMERA);


    if( useQR == true ){
      prompts.showQRFileDialog();
      qrFileDialogValues = prompts.getQRFileDialogValues();
      qrDir = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR);
      qrPath = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEPATH);
      qrFilename = qrFileDialogValues.get(CalibrationPrompt.MAP_IMAGEFILENAME);

      qrPhoto = new RGBPhoto(qrDir, qrFilename, qrPath, cameraType);
    }else{
      // Use base files

    }

    prompts.showImageFileDialog();
    HashMap<String, String> imageFileDialogValues = prompts.getImageFileDialogValues();
    inputDir = imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR);
    outputDir = inputDir + "\\Calibrated";




  }

}
