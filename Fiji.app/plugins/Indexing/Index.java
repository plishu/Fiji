import ij.ImagePlus;
import java.util.Iterator;
import java.util.Map;

public interface Index{
    public double calculate(double pix1, double pix2);
    public ImagePlus calculateIndex(ImagePlus inImg);
    public ImagePlus calculateIndex(Map<String,ImagePlus> inImgs);
    public String getIndexType();
}
