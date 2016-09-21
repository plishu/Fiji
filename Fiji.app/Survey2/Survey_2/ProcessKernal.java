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

public class ProcessKernal implements PlugIn
{
  private String inDirStr = null;
  private String outDirStr = null;
   private List<File> toProcess = null;
   private String WorkingDirectory = null;
   private String OS = null;
   private File fileInputDir = null;
   private File[] imagesToProcess = null;

    public void run(String args)
    {
        //IJ.showMessage("Post_Process", "Hello world");
        IJ.log("Entering Kernal Test Program");
        WorkingDirectory = IJ.getDirectory("imagej");
        OS = System.getProperty("os.name");
        inDirStr = IJ.getDirectory("Select Input Folder");
        outDirStr = inDirStr + "Processed\\";
        IJ.log("Input Directory: " + inDirStr);
        IJ.log("Output Directory: " + outDirStr);

        fileInputDir = new File(inDirStr);
        IJ.log("Open file");
        imagesToProcess = fileInputDir.listFiles();
        IJ.log("Create List");
        toProcess = new ArrayList<File>();
        for( int i=0; i<imagesToProcess.length; i++ )
        {
            toProcess.add(imagesToProcess[i]);
        }

        RGBPhoto photo = null;
        RGBPhoto result = null;
        File tmpfile = null;
        Iterator<File> pixelIterator = toProcess.iterator();
        int imgcounter = 0;
        while( pixelIterator.hasNext() )
        {
            ++imgcounter;
            tmpfile = pixelIterator.next();
            IJ.log( (String)"Gathering pixel information for " + tmpfile.getName() + " (" + imgcounter + " of " + toProcess.size() + " - " + (int)((double)imgcounter/((double)toProcess.size())*100) + "% complete" + ")" );
            photo = new RGBPhoto(inDirStr, tmpfile.getName(), tmpfile.getPath(), "Kernal", false);
            ImagePlus img = photo.getImage();
            ImagePlus newimg = photo.getImage();
            double pixel = 0.0;
            long pixelbits = 0;
            long tempbits = 0;
            int x = 0;
            int y = 0;
            while (y < img.getHeight())
            {
                x = 0;
                while (x < img.getWidth())
                {
                    // pixel = (double)img.getProcessor().getPixelValue(x, y);
                    // pixelbits = Double.doubleToLongBits(pixel);
                    // tempbits = pixelbits << 4;
                    // tempbits |= (pixelbits & 0x0000000F);
                    // pixel = (double) tempbits;
                     pixel = (double)((Double.doubleToLongBits(img.getProcessor().getPixelValue(x, y)) << 8) | (Double.doubleToLongBits(img.getProcessor().getPixelValue(x, y)) & 0x000F));
                     pixel = (double)((pixel - 0xF0000000) * 16.0);
                     newimg.getProcessor().putPixelValue(x, y, pixel);
                     ++x;
                }
                ++y;
            }
            result = new RGBPhoto(newimg);
            result.copyFileData(photo);
            IJ.log("Saving");
            saveToDir(outDirStr, result.getFileName(), result.getExtension(), result.getImage());



        }
        IJ.log("Goodbye.");
    }
    public void saveToDir(String outdir, String filename, String ext, ImagePlus image){
      String NDVIAppend = "_Processed";

      // If output directory does not exist, create it
      File outd = new File(outdir);
      if( !outd.exists() ){
        outd.mkdir();
      }
      IJ.save((ImagePlus)image, (String)(String.valueOf(outdir) + filename + NDVIAppend + "." + ext));
      return;
    }
}
