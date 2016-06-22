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
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.measure.CurveFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;


import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;


import java.io.BufferedReader;
import java.io.BufferedWriter;
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

public class Calibrate implements PlugIn{

  public void run(String arg){
    CalibrationPrompt prompt = CalibrationPrompt.getPrompt();
    prompt.showMainDialog();
    HashMap<String, String> mainDialogValues = prompt.getMainDialogValues();
    HashMap<String, String> dualBandDialogValues = null;
    HashMap<String, String> qrFileDialogValues = null;
    HashMap<String, String> imageFileDialogValues = null;
    printAll( mainDialogValues.values() );

    // Check if camera selected is NDVI
    if( mainDialogValues.get(CalibrationPrompt.MAP_CAMERA).equals("Survey2 NDVI") ){
      prompt.showDualBandDialog();
      dualBandDialogValues = prompt.getDualBandDialogValues();
      printAll( dualBandDialogValues.values() );
    }

    // Check if use wants to provide calibration target image
    RGBPhoto qrphoto = null;
    if( mainDialogValues.get(CalibrationPrompt.MAP_USEQR).equals("true") ){
      prompt.showQRFileDialog();
      qrFileDialogValues = prompt.getQRFileDialogValues();
      printAll( qrFileDialogValues.values() );

      qrphoto = new RGBPhoto( qrFileDialogValues );
      qrphoto.show();

    }

    // Ask user for input image to calibrate
    prompt.showImageFileDialog();
    imageFileDialogValues = prompt.getImageFileDialogValues();
    printAll( imageFileDialogValues.values() );

    // Values!!
    RGBPhoto photo = new RGBPhoto( imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEDIR),
              imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEFILENAME),
              imageFileDialogValues.get(CalibrationPrompt.MAP_IMAGEPATH) );
    photo.show();






  }

  public void printAll(Collection col){
    Iterator itr = col.iterator();
    while( itr.hasNext() ){
      IJ.log( (String)itr.next() );
    }
  }

}
