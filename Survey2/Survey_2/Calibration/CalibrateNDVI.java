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

        ImagePlus newRedImage = NewImage.createFloatImage((String)"Red Image Reflectance Map", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
        ImagePlus newBlueImage = NewImage.createFloatImage((String)"Blue Image Reflectance Map", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);
        ImagePlus newGreenImage = NewImage.createFloatImage((String)"Green Image Reflectance Map", (int)img.getWidth(), (int)img.getHeight(), (int)1, (int)1);

        int x = 0;
        int y = 0;

        double redPixel = 0.0;
        double greenPixel = 0.0;
        double bluePixel = 0.0;

        while( y < img.getHeight() ){
            x = 0;
            while( y < img.getWidth() ){
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

        ImagePlus[] channels = {newRedImage, newGreenImage, newBlueImage};
        return channels;
    }
}
