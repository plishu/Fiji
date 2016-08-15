import ij.ImagePlus;
import ij.gui.Roi;

public interface CalibrateIndex{
    public ImagePlus[] createReflectanceMapping(RGBPhoto photo, double[] calibCoeff);
    // To be used only for the calibration target!!
    public double[] optimizeCoeffs(ImagePlus[] channels, Roi[] rois, double[][] baseValues, double[] oldCoeff);
    public double[] calculateCoeffs( RGBPhoto qrPhoto, Roi[] rois, double[][] baseValues);
}
