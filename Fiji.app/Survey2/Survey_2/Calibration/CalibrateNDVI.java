import ij.ImagePlus;
import ij.IJ;
import ij.gui.NewImage;
import ij.plugin.frame.RoiManager;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.process.ImageProcessor;
import ij.measure.CurveFitter;
import ij.gui.Plot;
import ij.gui.PlotWindow;

import java.lang.Math;
import java.util.Map;
import java.util.HashMap;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.Exception;


public class CalibrateNDVI implements CalibrateIndex{

    public ImagePlus createChannelReflectanceMapping(ImagePlus channel, double slope, double intercept ){

        ImagePlus image = NewImage.createFloatImage((String) channel.getTitle() + " Image Reflectance Map", (int)channel.getWidth(), (int)channel.getHeight(), (int)1, (int)1);

        int x = 0;
        int y = 0;

        double pixel = 0.0;

        while( y < channel.getHeight() ){
            x = 0;
            while( x < channel.getWidth() ){
                pixel = channel.getProcessor().getPixelValue(x,y);

                // Apply reflectance mapping
                pixel = pixel * slope + intercept;

                // Create reflectance mapping
                image.getProcessor().putPixelValue(x,y,pixel);
                x++;
            }
            y++;
        }

        return image;
    }

    @Override
    public ImagePlus[] createReflectanceMapping(RGBPhoto photo, double[] calibCoeff){
        ImagePlus img = photo.getImage();
        ImagePlus redImg = photo.getRedChannel();
        ImagePlus greenImg = photo.getGreenChannel();
        ImagePlus blueImg = photo.getBlueChannel();

        boolean condition = (img == null) || (redImg == null) ||
                            (greenImg == null) || (blueImg == null);
        if( condition ){
            IJ.log("One or more channels of the image could not be obtained");
            IJ.log("Are you sure that this is an RGB image with three channels?");
            return null;
        }

        double redSlope = calibCoeff[1];
        double redIntercept = calibCoeff[0];
        double blueSlope = calibCoeff[3];
        double blueIntercept = calibCoeff[2];

        Log.debug("Red Slope Recieved: " + calibCoeff[1]);
        Log.debug("Red Slope Intercept: " + calibCoeff[0]);

        ImagePlus newRedImage = NewImage.createFloatImage((String)"Red Image Reflectance Map", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
        ImagePlus newBlueImage = NewImage.createFloatImage((String)"Blue Image Reflectance Map", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
        ImagePlus newGreenImage = NewImage.createFloatImage((String)"Green Image Reflectance Map", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);

        int x = 0;
        int y = 0;

        double redPixel = 0.0;
        double greenPixel = 0.0;
        double bluePixel = 0.0;

        redImg.show();
        redImg.setTitle("Red before processing");

        while( y < img.getHeight() ){
            x = 0;
            while( x < img.getWidth() ){
                redPixel = redImg.getProcessor().getPixelValue(x,y);
                bluePixel = blueImg.getProcessor().getPixelValue(x,y);

                // Apply reflectance mapping
                redPixel = redPixel * redSlope + redIntercept;
                bluePixel = bluePixel * blueSlope + blueIntercept;

                // Create reflectance mapping
                newRedImage.getProcessor().putPixelValue(x,y,redPixel);
                newGreenImage.getProcessor().putPixelValue(x,y,greenPixel);
                newBlueImage.getProcessor().putPixelValue(x,y,bluePixel);

                x++;
            }
            y++;
        }

        newRedImage.show();
        newRedImage.setTitle("Red after processing");

        ImagePlus[] channels = {newRedImage, newGreenImage, newBlueImage};
        return channels;
    }

    @Override
    public double[] calculateCoeffs(RGBPhoto qrPhoto, Roi[] rois, double[][] baseValues){
        RoiManager manager = null;
        double[] coeff = new double[4];
        List<HashMap<String, String>> bandSummary = null;
        double[] refmeans = new double[3];
        double[] redBaseValues = new double[3];
        double[] blueBaseValues = new double[3];
        Calibrator calibrator = new Calibrator();


        qrPhoto.show();
        qrPhoto.getImage().setTitle("Hello there :)");
        qrPhoto.getRedChannel().show();
        qrPhoto.getRedChannel().setTitle("Hello Red");

        /*
        qrPhoto.getRedChannel().getProcessor().drawRoi(rois[0]);
        qrPhoto.getRedChannel().getProcessor().drawRoi(rois[1]);
        qrPhoto.getRedChannel().getProcessor().drawRoi(rois[2]);
        qrPhoto.getRedChannel().updateAndDraw();
        */


        if( qrPhoto == null ){
            coeff = null;
        }else{

            // Get red channel means
            manager = calibrator.setupROIManager(qrPhoto.getRedChannel(), rois);
            bandSummary = calibrator.processRois(qrPhoto.getRedChannel(), manager);
            manager.reset();
            manager.close();

            double[] redMeans = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};

            Log.debug("Red Mean 87: " + redMeans[0]);
            Log.debug("Red Mean 52: " + redMeans[1]);
            Log.debug("Red Mean 23: " + redMeans[2]);

            // Get blue channel means
            manager = calibrator.setupROIManager(qrPhoto.getBlueChannel(), rois);
            bandSummary = calibrator.processRois(qrPhoto.getBlueChannel(), manager);
            manager.reset();
            manager.close();

            double[] blueMeans = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};

            Log.debug("Blue Mean 87: " + blueMeans[0]);
            Log.debug("Blue Mean 52: " + blueMeans[1]);
            Log.debug("Blue Mean 23: " + blueMeans[2]);


            // Get base values
            redBaseValues[0] = baseValues[0][0];
            redBaseValues[1] = baseValues[1][0];
            redBaseValues[2] = baseValues[2][0];

            blueBaseValues[0] = baseValues[0][2];
            blueBaseValues[1] = baseValues[1][2];
            blueBaseValues[2] = baseValues[2][2];

            // Get coefficients
            double[] redCoeff = calibrator.calculateCalibrationCoefficients( redMeans, redBaseValues );
            double[] blueCoeff = calibrator.calculateCalibrationCoefficients( blueMeans, blueBaseValues );

            coeff[0] = redCoeff[0]; // Red intercept
            coeff[1] = redCoeff[1]; // Red slope
            coeff[2] = blueCoeff[0]; // Blue intercept
            coeff[3] = blueCoeff[1]; // Blue slope

            Log.debug("Red intercept: " + coeff[0]);
            Log.debug("Red slope: " + coeff[1]);
            Log.debug("Blue intercept: " + coeff[2]);
            Log.debug("Blue slope: " + coeff[3]);
        }

        return coeff;
    }

    /*
    @Override
    public double[] optimizeCoeffs(ImagePlus[] channels, Roi[] rois, double[][] baseValues){
        RoiManager manager = null;
        double[] coeff = new double[4];
        List<HashMap<String, String>> bandSummary = null;
        double[] redBaseValues = new double[3];
        double[] blueBaseValues = new double[3];
        Calibrator calibrator = new Calibrator();

        // START HERE

        double redError87;
        double redError51;
        double redError23;
        double avgRedError;
        double tolerance = 0.01;
        ImagePlus redChannel = channels[0];
        double[] redCoeff = null;

        int attempt = 0;
        int maxAttempts = 5;

        // Calibrate red channel
        do{
            manager = calibrator.setupROIManager(redChannel, rois);
            bandSummary = calibrator.processRois(redChannel, manager);
            manager.reset();
            manager.close();

            double[] redMeans = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};

            Log.debug("Red Mean 87: " + redMeans[0]);
            Log.debug("Red Mean 52: " + redMeans[1]);
            Log.debug("Red Mean 23: " + redMeans[2]);



            // Get base values
            redBaseValues[0] = baseValues[0][0];
            redBaseValues[1] = baseValues[1][0];
            redBaseValues[2] = baseValues[2][0];

            // Calculate average error
            redError87 = Math.abs(redBaseValues[0] - redMeans[0]);
            redError51 = Math.abs(redBaseValues[1] - redMeans[1]);
            redError23 = Math.abs(redBaseValues[2] - redMeans[2]);
            avgRedError = (redError87+redError51+redError23)/3.0;

            Log.debug("Red Average Error: " + avgRedError);

            // Get new coeff
            redCoeff = calibrator.calculateCalibrationCoefficients( redMeans, redBaseValues );

            // Calibrate image
            redChannel = createChannelReflectanceMapping(redChannel, redCoeff[1], redCoeff[0]);
            attempt++;

        }while( avgRedError >= tolerance && attempt < maxAttempts );


        double blueError87;
        double blueError51;
        double blueError23;
        double avgBlueError;
        ImagePlus blueChannel = channels[2];
        double[] blueCoeff = null;

        attempt = 0;
        // Calibrate blue channel
        do{
            manager = calibrator.setupROIManager(blueChannel, rois);
            bandSummary = calibrator.processRois(blueChannel, manager);
            manager.reset();
            manager.close();

            double[] blueMeans = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
                Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};

            Log.debug("Blue Mean 87: " + blueMeans[0]);
            Log.debug("Blue Mean 52: " + blueMeans[1]);
            Log.debug("Blue Mean 23: " + blueMeans[2]);



            // Get base values
            blueBaseValues[0] = baseValues[0][0];
            blueBaseValues[1] = baseValues[1][0];
            blueBaseValues[2] = baseValues[2][0];

            // Calculate average error
            blueError87 = Math.abs(blueBaseValues[0] - blueMeans[0]);
            blueError51 = Math.abs(blueBaseValues[1] - blueMeans[1]);
            blueError23 = Math.abs(blueBaseValues[2] - blueMeans[2]);
            avgBlueError = (blueError87+blueError51+blueError23)/3.0;

            Log.debug("Blue Average Error: " + avgBlueError);

            // Get new coeff
            blueCoeff = calibrator.calculateCalibrationCoefficients( blueMeans, blueBaseValues );

            // Calibrate image
            blueChannel = createChannelReflectanceMapping(blueChannel, blueCoeff[1], blueCoeff[0]);
            attempt++;

        }while( avgBlueError >= tolerance+0.002 && attempt < maxAttempts );

        redChannel.show();
        redChannel.setTitle("Optimized red");
        blueChannel.show();
        blueChannel.setTitle("Optimized blue");

        coeff[0] = redCoeff[0];
        coeff[1] = redCoeff[1];
        coeff[2] = blueCoeff[0];
        coeff[3] = blueCoeff[1];
        return coeff;
    }*/

    @Override
    public double[] optimizeCoeffs(ImagePlus[] channels, Roi[] rois, double[][] baseValues){
        RoiManager manager = null;
        double[] coeff = new double[4];
        List<HashMap<String, String>> bandSummary = null;
        double[] redBaseValues = new double[3];
        double[] blueBaseValues = new double[3];
        Calibrator calibrator = new Calibrator();

        // START HERE
        double redError87;
        double redError51;
        double redError23;
        double avgRedError;
        ImagePlus redChannel = channels[0];
        double[] redCoeff = null;

        // Calibrate red channel
        manager = calibrator.setupROIManager(redChannel, rois);
        bandSummary = calibrator.processRois(redChannel, manager);
        manager.reset();
        manager.close();
        double[] redMeans = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
            Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
            Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};
        Log.debug("Red Mean 87: " + redMeans[0]);
        Log.debug("Red Mean 52: " + redMeans[1]);
        Log.debug("Red Mean 23: " + redMeans[2]);

        // Get base values
        redBaseValues[0] = baseValues[0][0];
        redBaseValues[1] = baseValues[1][0];
        redBaseValues[2] = baseValues[2][0];

        // Calculate average error
        redError87 = Math.abs(redBaseValues[0] - redMeans[0]);
        redError51 = Math.abs(redBaseValues[1] - redMeans[1]);
        redError23 = Math.abs(redBaseValues[2] - redMeans[2]);
        avgRedError = (redError87+redError51+redError23)/3.0;

        Log.debug("Red Average Error: " + avgRedError);


        double blueError87;
        double blueError51;
        double blueError23;
        double avgBlueError;
        ImagePlus blueChannel = channels[2];
        double[] blueCoeff = null;


        // Calibrate blue channel
        redChannel.show();
        redChannel.setTitle("Optimized red");
        blueChannel.show();
        blueChannel.setTitle("Optimized blue");

        coeff[0] = redCoeff[0];
        coeff[1] = redCoeff[1];
        coeff[2] = blueCoeff[0];
        coeff[3] = blueCoeff[1];
        return coeff;
    }

    public ImagePlus[] subtractNIR(RGBPhoto photo, RGBPhoto refphoto, Roi[] rois, double[] coeff ){
        ImagePlus img = photo.getImage();
        ImagePlus redImg = photo.getRedChannel();
        ImagePlus greenImg = photo.getGreenChannel();
        ImagePlus blueImg = photo.getBlueChannel();

        ImagePlus refImg = refphoto.getImage();
        ImagePlus refImgRed = refphoto.getRedChannel();
        ImagePlus refImgGreen = refphoto.getGreenChannel();
        ImagePlus refImgBlue = refphoto.getBlueChannel();
        Calibrator calibrator = new Calibrator();



        RoiManager manager = calibrator.setupROIManager(refImgRed, rois);
        List<HashMap<String, String>> bandSummary = null;
        double[] refmeans = new double[3];

        bandSummary = calibrator.processRois(refImgRed, manager); // <--- This is the key
        double[] redRefMean = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
            Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
            Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};
        manager.reset();
        manager.close();


        manager = calibrator.setupROIManager(refImgBlue, rois);
        // bandSummary[0] = 87% target summary
        bandSummary = calibrator.processRois(refImgBlue, manager);
        double[] blueRefMean = {Double.parseDouble(bandSummary.get(0).get(Calibrator.MAP_MEAN)),
            Double.parseDouble(bandSummary.get(1).get(Calibrator.MAP_MEAN)),
            Double.parseDouble(bandSummary.get(2).get(Calibrator.MAP_MEAN))};
        manager.reset();
        manager.close();

        // coeff[0] = red intercept
        // coeff[1] = red slope
        // coeff[2] = blue intercept
        // coeff[3] = blue slope
        double NDVI87 = -0.0117532235;
        double NDVI51 = 0.0031666667;
        double NDVI23 = -0.0233926435;

        double nirSubtract87 = (coeff[3]/coeff[1] + coeff[2]/(coeff[1]*blueRefMean[0]))*((NDVI87-1)/(NDVI87+1)) + (coeff[0]/(coeff[1]*blueRefMean[0])) + (redRefMean[0]/blueRefMean[0]);
        double nirSubtract51 = (blueRefMean[1]*coeff[3]+coeff[2])/(blueRefMean[1]*coeff[1]) * (NDVI51-1)/(NDVI51+1) + (redRefMean[1]*coeff[1]+coeff[0])/(blueRefMean[1]*coeff[1]);
        double nirSubtract23 = (blueRefMean[2]*coeff[3]+coeff[2])/(blueRefMean[2]*coeff[1]) * (NDVI23-1)/(NDVI23+1) + (redRefMean[2]*coeff[1]+coeff[0])/(blueRefMean[2]*coeff[1]);

        double nirSubtractAvg = (nirSubtract23 + nirSubtract51 + nirSubtract87)/3.0;

        Log.debug("NIR subtraction %: " + nirSubtractAvg);

        double redPixel = 0.0;
        double bluePixel = 0.0;

        int x = 0;
        int y = 0;
        while( y < img.getHeight() ){
            x = 0;
            while( x < img.getWidth() ){
                redPixel = redImg.getProcessor().getPixelValue(x,y);
                bluePixel = blueImg.getProcessor().getPixelValue(x,y);

                // Apply nir subtraction
                redPixel = redPixel - nirSubtractAvg * bluePixel;
                redImg.getProcessor().putPixelValue(x,y,redPixel);
                x++;
            }
            y++;
        }

        ImagePlus[] channels = {redImg, greenImg, blueImg};
        return channels;
    }
}
