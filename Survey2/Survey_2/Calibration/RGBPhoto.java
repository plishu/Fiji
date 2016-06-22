import ij.ImagePlus;
import ij.IJ;

import java.io.File;
import java.util.HashMap;

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

  public RGBPhoto(HashMap<String, String> valueMap){
    imageDir = valueMap.get(CalibrationPrompt.MAP_IMAGEDIR);
    imageName = valueMap.get(CalibrationPrompt.MAP_IMAGEFILENAME);
    imagePath = valueMap.get(CalibrationPrompt.MAP_IMAGEPATH);
    imageExt = getExtension(imagePath);

    image = new ImagePlus(imagePath);

  }

  public String getExtension(String path){
    return "yo";
  }

  public String getExtension(){
    return "yo";
  }

  public void show(){
    image.show();
  }

}
