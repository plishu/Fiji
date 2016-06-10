/*
 * Decompiled with CFR 0_114.
 *
 * Could not load the following classes:
 *  WriteEXIF
 *  ij.CompositeImage
 *  ij.IJ
 *  ij.ImagePlus
 *  ij.Prefs
 *  ij.gui.DialogListener
 *  ij.gui.GenericDialog
 *  ij.gui.NewImage
 *  ij.io.DirectoryChooser
 *  ij.io.OpenDialog
 *  ij.io.SaveDialog
 *  ij.plugin.ChannelSplitter
 *  ij.plugin.LutLoader
 *  ij.plugin.PlugIn
 *  ij.process.ImageProcessor
 *  ij.process.LUT
 */
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

public class ApplyCalibration implements PlugIn, DialogListener {
    public void run(String arg) {
        double gamma;
        Boolean createIndexFloat;
        double percentToSubtract;
        Boolean subtractNIR;
        ImagePlus inImagePlus;
        double minColorScale;
        double maxColorScale;
        int counter;
        ImagePlus indexImage;
        String calibrationFileName;
        String lutLocation;
        String outDirectory;
        File[] inputImages;
        File outFile;
        String outFileBase;
        File tempFile;
        ImagePlus colorIndex;
        String[] indexTypes;
        int nirBand;
        String indexType;
        Boolean createIndexColor;
        int visBand;
        String lutName;
        double[] calibrationCoefs;
        String logName;
        Boolean removeGamma;


        indexTypes = new String[]{"NDVI (NIR-Vis)/(NIR+Vis)", "DVI NIR-Vis"};
        lutLocation = IJ.getDirectory((String)"luts");
        File lutDirectory = new File(lutLocation);
        String[] lutNames = lutDirectory.list();
        logName = "log.txt";
        outFile = null;
        tempFile = null;
        inImagePlus = null;
        indexImage = null;
        outFileBase = "";
        visBand = 0;
        nirBand = 0;
        colorIndex = null;
        calibrationCoefs = new double[4];
        subtractNIR = null;
        percentToSubtract = 0.0;
        removeGamma = null;
        gamma = 0.0;
        Boolean saveParameters = true;
        Boolean useDefaults = false;
        indexType = Prefs.get((String)"pm.fromSBImage.indexType", (String)indexTypes[0]);
        createIndexColor = Prefs.get((String)"pm.ac.createIndexColor", (boolean)true);
        createIndexFloat = Prefs.get((String)"pm.ac.createIndexFloat", (boolean)true);
        maxColorScale = Prefs.get((String)"pm.ac.maxColorScale", (double)1.0);
        minColorScale = Prefs.get((String)"pm.ac.minColorScale", (double)-1.0);
        lutName = Prefs.get((String)"pm.ac.lutName", (String)lutNames[0]);
        GenericDialog dialog = new GenericDialog("Enter variables");
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
            return;
        }
        useDefaults = dialog.getNextBoolean();
        if (useDefaults.booleanValue()) {
            dialog = null;
            dialog = new GenericDialog("Enter variables");
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
                return;
            }
        }
        if (useDefaults.booleanValue()) {
            dialog.getNextBoolean();
        }
        indexType = dialog.getNextChoice();
        createIndexColor = dialog.getNextBoolean();
        minColorScale = dialog.getNextNumber();
        maxColorScale = dialog.getNextNumber();
        createIndexFloat = dialog.getNextBoolean();
        lutName = dialog.getNextChoice();
        saveParameters = dialog.getNextBoolean();
        if (saveParameters.booleanValue()) {
            Prefs.set((String)"pm.ac.indexType", (String)indexType);
            Prefs.set((String)"pm.ac.createIndexColor", (boolean)createIndexColor);
            Prefs.set((String)"pm.ac.createIndexFloat", (boolean)createIndexFloat);
            Prefs.set((String)"pm.ac.maxColorScale", (double)maxColorScale);
            Prefs.set((String)"pm.ac.minColorScale", (double)minColorScale);
            Prefs.set((String)"pm.ac.lutName", (String)lutName);
            Prefs.savePreferences();
        }
        OpenDialog od = new OpenDialog("Select calibration file", arg);
        String calibrationDirectory = od.getDirectory();
        calibrationFileName = od.getFileName();
        if (calibrationFileName == null) {
            IJ.error((String)"No file was selected");
            return;
        }
        DirectoryChooser inDirChoose = new DirectoryChooser("Input image directory");
        String inDir = inDirChoose.getDirectory();
        if (inDir == null) {
            IJ.error((String)"Input image directory was not selected");
            return;
        }
        File inFolder = new File(inDir);
        inputImages = inFolder.listFiles();
        SaveDialog sd = new SaveDialog("Output directory and log file name", "log", ".txt");
        outDirectory = sd.getDirectory();
        logName = sd.getFileName();
        if (logName == null) {
            IJ.error((String)"No directory was selected");
            return;
        }
        BufferedReader fileReader = null;
        try {
            try {
                String fullLine = "";
                fileReader = new BufferedReader(new FileReader(String.valueOf(calibrationDirectory) + calibrationFileName));
                counter = 1;
                while ((fullLine = fileReader.readLine()) != null) {
                    String[] dataValues;
                    if (counter == 8) {
                        dataValues = fullLine.split(":");
                        calibrationCoefs[0] = Double.parseDouble(dataValues[1]);
                    }
                    if (counter == 9) {
                        dataValues = fullLine.split(":");
                        calibrationCoefs[1] = Double.parseDouble(dataValues[1]);
                    }
                    if (counter == 11) {
                        dataValues = fullLine.split(":");
                        calibrationCoefs[2] = Double.parseDouble(dataValues[1]);
                    }
                    if (counter == 12) {
                        dataValues = fullLine.split(":");
                        calibrationCoefs[3] = Double.parseDouble(dataValues[1]);
                    }
                    if (counter == 14) {
                        dataValues = fullLine.split(":");
                        subtractNIR = Boolean.parseBoolean(dataValues[1]);
                    }
                    if (counter == 15) {
                        dataValues = fullLine.split(":");
                        percentToSubtract = Double.parseDouble(dataValues[1]);
                    }
                    if (counter == 16) {
                        dataValues = fullLine.split(":");
                        removeGamma = Boolean.parseBoolean(dataValues[1]);
                    }
                    if (counter == 17) {
                        dataValues = fullLine.split(":");
                        gamma = Double.parseDouble(dataValues[1]);
                    }
                    if (counter == 19) {
                        dataValues = fullLine.split(":");
                        visBand = Integer.parseInt(dataValues[1].trim()) - 1;
                    }
                    if (counter == 20) {
                        dataValues = fullLine.split(":");
                        nirBand = Integer.parseInt(dataValues[1].trim()) - 1;
                    }
                    ++counter;
                }
            }
            catch (Exception e) {
                IJ.error((String)"Error reading calibration coefficient file", (String)e.getMessage());
                try {
                    fileReader.close();
                }
                catch (IOException f) {
                    f.printStackTrace();
                }
                return;
            }
        }
        finally {
            try {
                fileReader.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter(String.valueOf(outDirectory) + logName));
            bufWriter.write("PARAMETER SETTINGS:\n");
            bufWriter.write("File name for calibration coeficients: " + calibrationFileName + "\n");
            bufWriter.write("Select index type for calculation: " + indexType + "\n\n");
            bufWriter.write("Output Color Index image? " + createIndexColor + "\n");
            bufWriter.write("Minimum Index value for scaling color Index image: " + minColorScale + "\n");
            bufWriter.write("Maximum Index value for scaling color Index image: " + maxColorScale + "\n");
            bufWriter.write("Output floating point Index image? " + createIndexFloat + "\n");
            bufWriter.write("Channel from visible image to use for Red band to create Index: " + (visBand + 1) + "\n");
            bufWriter.write("Channel from IR image to use for IR band to create Index: " + (nirBand + 1) + "\n");
            bufWriter.write("Subtract NIR from visible?" + subtractNIR + "\n");
            bufWriter.write("Percent of NIR to subtract: " + percentToSubtract + "\n");
            bufWriter.write("Remove gamma effect? " + removeGamma + "\n");
            bufWriter.write("Gammafactor: " + gamma + "\n");
            bufWriter.write("Visible band: " + (visBand + 1) + "\n");
            bufWriter.write("Near-infrared band: " + (nirBand + 1) + "\n");
            bufWriter.write("Select output color table for color Index image: " + lutName + "\n\n");
            bufWriter.close();
        }
        catch (Exception e) {
            IJ.error((String)"Error writing log file", (String)e.getMessage());
            return;
        }
        percentToSubtract /= 100.0;
        File[] arrfile = inputImages;
        int dataValues = arrfile.length;
        counter = 0;
        while (counter < dataValues) {
            File inImage = arrfile[counter];
            inImagePlus = new ImagePlus(inImage.getAbsolutePath());
            if (inImagePlus.getImage() != null) {

                outFileBase = inImagePlus.getTitle().replaceFirst("[.][^.]+$", "");

                String[] inImageParts = (inImage.getName()).split("\\.(?=[^\\.]+$)");
                String inImageExt = null;

                if( inImageParts.length < 2 ){
                  continue;
                } else{
                  inImageExt = inImageParts[1];
                }


                inImagePlus.show();
                double visPixel = 0.0;
                double nirPixel = 0.0;
                if (inImagePlus.getNChannels() == 1) {
                    inImagePlus = new CompositeImage(inImagePlus);
                }
                ImagePlus[] imageBands = ChannelSplitter.split((ImagePlus)inImagePlus);
                ImagePlus visImage = this.scaleImage(imageBands[visBand], "visImage");
                ImagePlus nirImage = this.scaleImage(imageBands[nirBand], "nirImage");
                if (removeGamma.booleanValue()) {
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
                }
                if (subtractNIR.booleanValue()) {
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
                }
                if (indexType == indexTypes[0]) {
                    indexImage = this.makeNDVI(visImage, nirImage, calibrationCoefs);
                    indexImage.show();
                } else if (indexType == indexTypes[1]) {
                    indexImage = this.makeDVI(visImage, nirImage, calibrationCoefs);
                }

                // Save image
                if (createIndexFloat.booleanValue()) {
                    if (indexType == indexTypes[0]) {
                        IJ.save((ImagePlus)indexImage, (String)(String.valueOf(outDirectory) + outFileBase + "_NDVI_Float." + "tif"));
                    } else if (indexType == indexTypes[1]) {
                        IJ.save((ImagePlus)indexImage, (String)(String.valueOf(outDirectory) + outFileBase + "_DVI_Float." + "tif"));
                    }
                }

                if (createIndexColor.booleanValue()) {
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
                        cm = LutLoader.open((String)(String.valueOf(lutLocation) + lutName));
                    }
                    catch (IOException e) {
                        IJ.error((String)((Object)e));
                    }
                    LUT lut = new LUT(cm, 255.0, 0.0);
                    colorIndex.getProcessor().setLut(lut);
                    colorIndex.show();

                    // Save calibrated section of code
                    String tempFileName = String.valueOf(outDirectory) + outFileBase + "IndexColorTemp." + inImageExt;
                    tempFile = new File(tempFileName);
                    IJ.save((ImagePlus)colorIndex, (String)tempFileName);
                    if (indexType == indexTypes[0]) {
                        outFile = new File(String.valueOf(outDirectory) + outFileBase + "_NDVI_Color." + inImageExt);
                    } else if (indexType == indexTypes[1]) {
                        outFile = new File(String.valueOf(outDirectory) + outFileBase + "_DVI_Color." + inImageExt);
                    }
                }
                IJ.run((String)"Close All");
                WriteEXIF exifWriter = new WriteEXIF(inImage, outFile, tempFile);
                exifWriter.copyEXIF();
            }
            ++counter;
        }
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
}
