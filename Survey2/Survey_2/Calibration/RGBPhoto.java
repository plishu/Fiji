import ij.ImagePlus;
import ij.IJ;

import java.io.File;

public class RGBPhoto{
  private String imageDir = null;
  private String imageName = null;
  private String imagePath = null;
  private String imageExt = null;
  private File imageFile = null;

  private ImagePlus image = null;
  private ImagePlus redChannel = null;
  private ImagePlus greenChannel = null;
  private ImagePlus blueChannel = null;

  /*
   * Create RGBPhoto from photo directory
   * @param: dir - Directory of photo
   * @return: none
   */
  public RGBPhoto(String dir, String fname, String path){
    imageDir = dir;
    imageName = fname;
    imagePath = path;
    imageExt = getExtension(imagePath);

    image = new ImagePlus(imagePath);

  }

  public String getExtension(String path){
    return "yo";
  }

  public String getExtension(){
    return "yo";
  }

}
