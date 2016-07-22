import ij.ImagePlus;
import java.util.Iterator;

public interface Index{
    public double calculate(double pix1, double pix2);
    public ImagePlus calculateIndex(ImagePlus inImg);
    public ImagePlus calculateIndex(Iterator<ImagePlus> inImgs);
}
