import ij.ImagePlus;
import java.util.Iterator;
import ij.plugin.ChannelSplitter;
import ij.gui.NewImage;
import ij.IJ;

public class NDVIIndex implements Index{
    @Override
    public double calculate(double visPix, double nirPix){

        double outpixel = 0.0;

        if( visPix + nirPix == 0.0 ){
            outpixel = 0.0;
        }else{
            outpixel = (nirPix - visPix)/(nirPix + visPix);

            // Cap anything outside of range
            /*
            if( outpixel > 1.0 ){
                outpixel = 1.0;
            }
            if( outpixel < -1.0 ){
                outpixel = -1.0;
            }*/
        }

        return outpixel;
    }

    @Override
    public ImagePlus calculateIndex(ImagePlus inImg){

        int x = 0;
        int y = 0;

        ImagePlus visImg = getVisImage(inImg,0);
        ImagePlus nirImg = getNIRImage(inImg,2);
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

    // @TODO For seperate band calculation
    @Override
    public ImagePlus calculateIndex(Iterator<ImagePlus> inImgs){
        return null;
    }

    public ImagePlus getVisImage(ImagePlus inImg, int visIndex){
        ImagePlus[] imageBands = ChannelSplitter.split(inImg);
        return imageBands[visIndex];
    }

    public ImagePlus getNIRImage(ImagePlus inImg, int nirIndex){
        ImagePlus[] imageBands = ChannelSplitter.split(inImg);
        return imageBands[nirIndex];
    }


}
