import ij.ImagePlus;
import java.util.Iterator;
import java.util.Map;
import ij.plugin.ChannelSplitter;
import ij.gui.NewImage;
import ij.IJ;

public class NDVIIndexLegacy extends NDVIIndex{

    @Override
    public ImagePlus calculateIndex(ImagePlus inImg){

        int x = 0;
        int y = 0;

        ImagePlus visImg = getVisImage(inImg,2);
        ImagePlus nirImg = getNIRImage(inImg,0);
        ImagePlus newImage = NewImage.createFloatImage("ndviIndex", inImg.getWidth(), inImg.getHeight(), 1, 1);

        double visPixel = 0.0;
        double nirPixel = 0.0;
        double outPixel = 0.0;

        while( y < inImg.getHeight() ){
            x = 0; // Reset for new row of pixels
            while( x < inImg.getWidth() ){
                visPixel = visImg.getProcessor().getPixelValue(x,y);
                nirPixel = nirImg.getProcessor().getPixelValue(x,y);

                outPixel = calculate(visPixel, nirPixel);

                newImage.getProcessor().putPixelValue(x, y, outPixel);

                x++;
            }
            y++;
        }

        return newImage;
    }

}
