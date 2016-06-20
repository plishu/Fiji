/*
 * Decompiled with CFR 0_114.
 *
 * Could not load the following classes:
 *  ij.CompositeImage
 *  ij.IJ
 *  ij.ImagePlus
 *  ij.Prefs
 *  ij.gui.DialogListener
 *  ij.gui.GenericDialog
 *  ij.gui.NewImage
 *  ij.gui.Plot
 *  ij.gui.PlotWindow
 *  ij.gui.Roi
 *  ij.io.OpenDialog
 *  ij.io.SaveDialog
 *  ij.measure.CurveFitter
 *  ij.plugin.ChannelSplitter
 *  ij.plugin.PlugIn
 *  ij.plugin.frame.RoiManager
 *  ij.process.ImageProcessor
 */
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
//import java.io.Reader;
import java.io.Writer;
import java.util.Vector;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;

import java.awt.image.BufferedImage;
import java.util.Hashtable;
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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.ResultPoint;

public class NewCalculateCalibration
implements PlugIn, DialogListener {
    public void run(String arg) {
        double sum;
        Rectangle r;
        int visBandIndex;
        int x;
        Roi[] rois;
        double[] visRegressionParams;
        double[] nirRegressionParams;
        double[] visRefValues;
        double nirR_Squared;
        Boolean subtractNIR;
        Boolean removeGamma;
        double percentToSubtract;
        int numLines;
        ImagePlus imp;
        double[] nirRefValues;
        double visR_Squared;
        double[] visImageValues;
        int count;
        double gamma;
        double[] nirImageValues;
        String outDirectory;
        int y;
        int nirBandIndex;
        String logName;
        String[] IndexBands = new String[]{"red", "green", "blue"};
        Boolean saveParameters = true;
        Boolean useDefaults = false;
        rois = null;
        logName = "log.txt";
        visImageValues = null;
        nirImageValues = null;
        visRefValues = null;
        nirRefValues = null;
        visRegressionParams = null;
        nirRegressionParams = null;
        visR_Squared = 0.0;
        nirR_Squared = 0.0;


        // Create Roi
        String imageDir = IJ.getFilePath("Input QR Code image");
        ImagePlus qrimg = IJ.openImage(imageDir);
        qrimg.show();

        QRCalib qr = new QRCalib();

        Result result = qr.attemptDecode(qrimg); // Blocking

        if( result == null ){
          IJ.log( "Could not find QR code. Skipping this image." );
          return;
        }

        ImagePlus resimg = qr.getAttemptImg(); // Needed for processing

        ResultPoint[] points = result.getResultPoints();

        //PolygonRoi qrRoi = qr.createPolygon( polyXCoords, polyYCoords );
        PolygonRoi qrRoi = qr.createPolygon( points );
        qr.drawPolygonOn(qrRoi, resimg);
        qr.setTargetsCenter(qrRoi);

        float[] target1Center = qr.getTarget1Center();
        float[] target2Center = qr.getTarget2Center();
        float[] target3Center = qr.getTarget3Center();
        float targetLength = qr.getTargetSize();

        Roi target1Roi = qr.createRectangleRoi( target1Center, 0.3f );
        qr.drawPolygonOn( target1Roi, resimg );
        Roi target2Roi = qr.createRectangleRoi( target2Center, 0.3f );
        qr.drawPolygonOn( target2Roi, resimg );
        Roi target3Roi = qr.createRectangleRoi( target3Center, 0.3f );
        qr.drawPolygonOn( target3Roi, resimg );

        target1Roi = qr.mapRoiTo( qrimg, resimg, target1Center, 0.6f );
        qr.drawPolygonOn( target1Roi, qrimg );
        target2Roi = qr.mapRoiTo( qrimg, resimg, target2Center, 0.6f );
        qr.drawPolygonOn( target2Roi, qrimg );
        target3Roi = qr.mapRoiTo( qrimg, resimg, target3Center, 0.6f );
        qr.drawPolygonOn( target3Roi, qrimg );

        // Create Roi manager and add Roi
        RoiManager mgr = new RoiManager();
        //mgr.addRoi(target1Roi);
        //mgr.addRoi(target2Roi);
        //mgr.addRoi(target3Roi);
        mgr.add(qrimg, target1Roi, 0);
        mgr.add(qrimg, target2Roi, 1);
        mgr.add(qrimg, target3Roi, 2);

        // Assumes RoiManager has been created and at least two Roi exist
        RoiManager manager = RoiManager.getInstance();
        if (manager == null) {
            IJ.error((String)"At least 2 ROIs must be added to the ROI Tool before running plugin");
            return;
        }
        rois = manager.getRoisAsArray();
        if (rois.length < 2) {
            IJ.error((String)"At least 2 ROIs must be added to the ROI Tool before running plugin");
            return;
        }
        //imp = IJ.getImage();
        imp = qrimg;
        visBandIndex = (int)Prefs.get((String)"pm.calibrate.visBandIndex", (double)0.0);
        nirBandIndex = (int)Prefs.get((String)"pm.calibrate.nirBandIndex", (double)2.0);
        subtractNIR = Prefs.get((String)"pm.calibrate.subtractNIR", (boolean)true);
        percentToSubtract = Prefs.get((String)"pm.calibrate.percentToSubtract", (double)80.0);
        removeGamma = Prefs.get((String)"pm.calibrate.removeGamma", (boolean)false);
        gamma = Prefs.get((String)"pm.calibrate.gamma", (double)2.2);
        GenericDialog dialog = new GenericDialog("Enter variables");
        dialog.addCheckbox("Load default parameters (click OK below to reload)", false);
        dialog.addChoice("Channel for Visible band to create Index", IndexBands, IndexBands[visBandIndex]);
        dialog.addChoice("Channel for IR band to create Index", IndexBands, IndexBands[nirBandIndex]);
        dialog.addCheckbox("Subtract NIR from visible?", subtractNIR.booleanValue());
        dialog.addNumericField("Percent of NIR to subtract (enter values between 0 and 100)", percentToSubtract, 3);
        dialog.addCheckbox("Remove gamma effect?", removeGamma.booleanValue());
        dialog.addNumericField("Gamma value", gamma, 5);
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
            dialog.addChoice("Channel for Visible band to create Index", IndexBands, IndexBands[0]);
            dialog.addChoice("Channel for IR band to create Index", IndexBands, IndexBands[2]);
            dialog.addCheckbox("Subtract NIR from visible?", true);
            dialog.addNumericField("Percent of NIR to subtract (enter values between 0 and 100)", 80.0, 3);
            dialog.addCheckbox("Remove gamma effect?", false);
            dialog.addNumericField("Factor for removing gamma", 2.2, 5);
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
        visBandIndex = dialog.getNextChoiceIndex();
        nirBandIndex = dialog.getNextChoiceIndex();
        subtractNIR = dialog.getNextBoolean();
        percentToSubtract = dialog.getNextNumber();
        removeGamma = dialog.getNextBoolean();
        gamma = dialog.getNextNumber();
        saveParameters = dialog.getNextBoolean();
        if (saveParameters.booleanValue()) {
            Prefs.set((String)"pm.calibrate.visBandIndex", (int)visBandIndex);
            Prefs.set((String)"pm.calibrate.nirBandIndex", (int)nirBandIndex);
            Prefs.set((String)"pm.calibrate.subtractNIR", (boolean)subtractNIR);
            Prefs.set((String)"pm.calibrate.percentToSubtract", (double)percentToSubtract);
            Prefs.set((String)"pm.calibrate.removeGamma", (boolean)removeGamma);
            Prefs.set((String)"pm.calibrate.gamma", (double)gamma);
            Prefs.savePreferences();
        }
        SaveDialog sd = new SaveDialog("Output calibration file name and directory", "calibration", ".txt");
        outDirectory = sd.getDirectory();
        logName = sd.getFileName();
        if (logName == null) {
            IJ.error((String)"No directory was selected");
            return;
        }
        OpenDialog od = new OpenDialog("Target reference data", arg);
        String targetDirectory = od.getDirectory();
        String targetFileName = od.getFileName();
        if (targetFileName == null) {
            IJ.error((String)"No file was selected");
            return;
        }
        BufferedReader fileReader = null;
        numLines = 0;
        String line = null;
        try {
            try {
                String fullLine = "";
                fileReader = new BufferedReader(new FileReader(String.valueOf(targetDirectory) + targetFileName));
                while ((line = fileReader.readLine()) != null) {
                    if (line.length() <= 0) continue;
                    ++numLines;
                }
                fileReader.close();
                fileReader = new BufferedReader(new FileReader(String.valueOf(targetDirectory) + targetFileName));
                visImageValues = new double[numLines];
                nirImageValues = new double[numLines];
                visRefValues = new double[numLines];
                nirRefValues = new double[numLines];
                int counter = 0;
                int i = 0;
                while (i < numLines) {
                    fullLine = fileReader.readLine();
                    String[] dataValues = fullLine.split(",");
                    visRefValues[counter] = Double.parseDouble(dataValues[0]);
                    nirRefValues[counter] = Double.parseDouble(dataValues[1]);
                    ++counter;
                    ++i;
                }
            }
            catch (Exception e) {
                IJ.error((String)"Error reading target reference data", (String)e.getMessage());
                try {
                    fileReader.close();
                }
                catch (IOException f) {
                    e.printStackTrace();
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
        double visPixel = 0.0;
        double nirPixel = 0.0;
        double outPixel = 0.0;
        if (imp.getNChannels() == 1) {
            imp = new CompositeImage(imp);
        }
        ImagePlus[] imageBands = ChannelSplitter.split((ImagePlus)imp);
        ImagePlus visImage = this.scaleImage(imageBands[visBandIndex], "visImage");
        ImagePlus nirImage = this.scaleImage(imageBands[nirBandIndex], "nirImage");
        if (removeGamma.booleanValue()) {
            double undoGamma = 1.0 / gamma;
            int y2 = 0;
            while (y2 < nirImage.getHeight()) {
                int x2 = 0;
                while (x2 < nirImage.getWidth()) {
                    nirPixel = Math.pow(nirImage.getProcessor().getPixelValue(x2, y2), undoGamma);
                    visPixel = Math.pow(visImage.getProcessor().getPixelValue(x2, y2), undoGamma);
                    visImage.getProcessor().putPixelValue(x2, y2, visPixel);
                    nirImage.getProcessor().putPixelValue(x2, y2, nirPixel);
                    ++x2;
                }
                ++y2;
            }
        }
        if (subtractNIR.booleanValue()) {
            percentToSubtract /= 100.0;
            int y3 = 0;
            while (y3 < nirImage.getHeight()) {
                int x3 = 0;
                while (x3 < nirImage.getWidth()) {
                    nirPixel = nirImage.getProcessor().getPixelValue(x3, y3);
                    visPixel = (double)visImage.getProcessor().getPixelValue(x3, y3) - percentToSubtract * nirPixel;
                    visImage.getProcessor().putPixelValue(x3, y3, visPixel);
                    ++x3;
                }
                ++y3;
            }
        }
        int i = 0;
        while (i < rois.length) {
            Roi roi = rois[i];
            if (roi != null && !roi.isArea()) {
                roi = null;
            }
            ImageProcessor ip = visImage.getProcessor();
            ImageProcessor mask = roi != null ? roi.getMask() : null;
            r = roi != null ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
            sum = 0.0;
            count = 0;
            y = 0;
            while (y < r.height) {
                x = 0;
                while (x < r.width) {
                    if (mask == null || mask.getPixel(x, y) != 0) {
                        ++count;
                        // Is this staying at 0?
                        sum += (double)ip.getPixelValue(x + r.x, y + r.y);
                    }
                    ++x;
                }
                ++y;
            }
            IJ.log((String)("count: " + count));
            IJ.log((String)("sum: " + sum));
            IJ.log((String)("mean: " + IJ.d2s((double)(sum / (double)count), (int)4)));
            visImageValues[i] = sum / (double)count;
            ++i;
        }
        i = 0;
        while (i < rois.length) {
            Roi roi = rois[i];
            if (roi != null && !roi.isArea()) {
                roi = null;
            }
            ImageProcessor ip = nirImage.getProcessor();
            ImageProcessor mask = roi != null ? roi.getMask() : null;
            r = roi != null ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
            sum = 0.0;
            count = 0;
            y = 0;
            while (y < r.height) {
                x = 0;
                while (x < r.width) {
                    if (mask == null || mask.getPixel(x, y) != 0) {
                        ++count;
                        sum += (double)ip.getPixelValue(x + r.x, y + r.y);
                    }
                    ++x;
                }
                ++y;
            }
            IJ.log((String)("count: " + count));
            IJ.log((String)("mean: " + IJ.d2s((double)(sum / (double)count), (int)4)));
            nirImageValues[i] = sum / (double)count;
            ++i;
        }
        CurveFitter visRegression = new CurveFitter(visImageValues, visRefValues);
        visRegression.doFit(0, true);
        visRegressionParams = visRegression.getParams();
        visR_Squared = visRegression.getRSquared();
        IJ.log((String)("intercept: " + IJ.d2s((double)visRegressionParams[0], (int)8)));
        IJ.log((String)("slope: " + IJ.d2s((double)visRegressionParams[1], (int)8)));
        PlotWindow.noGridLines = false;
        Plot visPlot = new Plot("Visible band regression", "Image values", "Reflectance values");
        visPlot.setLimits(0.0, 1.0, 0.0, 1.0);
        visPlot.setColor(Color.RED);
        visPlot.addPoints(visImageValues, visRefValues, 0);
        visPlot.draw();
        double[] xVis = new double[]{0.0, 1.0};
        double[] yVis = new double[]{visRegressionParams[0], visRegressionParams[1] + visRegressionParams[0]};
        visPlot.addPoints(xVis, yVis, 2);
        visPlot.addLabel(0.05, 0.1, "R squared = " + Double.toString(visR_Squared));
        visPlot.show();
        CurveFitter nirRegression = new CurveFitter(nirImageValues, nirRefValues);
        nirRegression.doFit(0, true);
        nirRegressionParams = nirRegression.getParams();
        nirR_Squared = nirRegression.getRSquared();
        IJ.log((String)("intercept: " + IJ.d2s((double)nirRegressionParams[0], (int)8)));
        IJ.log((String)("slope: " + IJ.d2s((double)nirRegressionParams[1], (int)8)));
        PlotWindow.noGridLines = false;
        Plot nirPlot = new Plot("NIR band regression", "Image values", "Reflectance values");
        nirPlot.setLimits(0.0, 1.0, 0.0, 1.0);
        nirPlot.setColor(Color.RED);
        nirPlot.addPoints(nirImageValues, nirRefValues, 0);
        nirPlot.draw();
        double[] xNir = new double[]{0.0, 1.0};
        double[] yNir = new double[]{nirRegressionParams[0], nirRegressionParams[1] + nirRegressionParams[0]};
        nirPlot.addPoints(xNir, yNir, 2);
        nirPlot.addLabel(0.05, 0.1, "R squared = " + Double.toString(nirR_Squared));
        nirPlot.show();
        try {
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter(String.valueOf(outDirectory) + logName));
            bufWriter.write("Calibration information for " + imp.getTitle() + "\n");
            bufWriter.write("\n");
            bufWriter.write("Number of data points for regression: " + numLines + "\n");
            bufWriter.write("R squared for visible band: " + visR_Squared + "\n");
            bufWriter.write("R squared for NIR band: " + nirR_Squared + "\n");
            bufWriter.write("\n");
            bufWriter.write("Visible band slope (gain) and intercept (offest) \n");
            bufWriter.write("   intercept: " + IJ.d2s((double)visRegressionParams[0], (int)8) + "\n");
            bufWriter.write("   slope: " + IJ.d2s((double)visRegressionParams[1], (int)8) + "\n");
            bufWriter.write("NIR band slope (gain) and intercept (offest) \n");
            bufWriter.write("   intercept: " + IJ.d2s((double)nirRegressionParams[0], (int)8) + "\n");
            bufWriter.write("   slope: " + IJ.d2s((double)nirRegressionParams[1], (int)8) + "\n");
            bufWriter.write("\n");
            bufWriter.write("Subtract NIR from visible:" + subtractNIR + "\n");
            bufWriter.write("Percent of NIR to subtract: " + percentToSubtract * 100.0 + "\n");
            bufWriter.write("Remove gamma effect:" + removeGamma + "\n");
            bufWriter.write("Gamma factor: " + gamma + "\n");
            bufWriter.write("\n");
            bufWriter.write("Visible band: " + (visBandIndex + 1) + "\n");
            bufWriter.write("Near-infrared band: " + (nirBandIndex + 1) + "\n");
            bufWriter.write("\n");
            int i2 = 0;
            while (i2 < numLines) {
                bufWriter.write("Mean for target " + (i2 + 1) + " for visible band: " + visImageValues[i2] + "\n");
                bufWriter.write("Mean for target " + (i2 + 1) + " for NIR band: " + nirImageValues[i2] + "\n");
                ++i2;
            }
            bufWriter.close();
        }
        catch (Exception e) {
            IJ.error((String)"Error writing log file", (String)e.getMessage());
            return;
        }
        ImagePlus newImage = NewImage.createFloatImage((String)"ndviImage", (int)nirImage.getWidth(), (int)nirImage.getHeight(), (int)1, (int)1);
        int y4 = 0;
        while (y4 < nirImage.getHeight()) {
            int x4 = 0;
            while (x4 < nirImage.getWidth()) {
                nirPixel = (double)nirImage.getProcessor().getPixelValue(x4, y4) * nirRegressionParams[1] + nirRegressionParams[0];
                if (nirPixel + (visPixel = (double)visImage.getProcessor().getPixelValue(x4, y4) * visRegressionParams[1] + visRegressionParams[0]) == 0.0) {
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
                newImage.getProcessor().putPixelValue(x4, y4, outPixel);
                ++x4;
            }
            ++y4;
        }
        //newImage.show();
        imp.changes = false;
        imp.close();
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
        Checkbox SubtractNIRCheckbox = (Checkbox)gd.getCheckboxes().get(1);
        Checkbox RemoveGammaCheckbox = (Checkbox)gd.getCheckboxes().get(2);
        Vector numericChoices = gd.getNumericFields();
        if (SubtractNIRCheckbox.getState()) {
            ((TextField)numericChoices.get(0)).setEnabled(true);
        } else {
            ((TextField)numericChoices.get(0)).setEnabled(false);
        }
        if (RemoveGammaCheckbox.getState()) {
            ((TextField)numericChoices.get(1)).setEnabled(true);
        } else {
            ((TextField)numericChoices.get(1)).setEnabled(false);
        }
        return true;
    }
}
