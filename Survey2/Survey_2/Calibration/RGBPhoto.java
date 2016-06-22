import ij.ImagePlus;
import ij.IJ;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.CompositeImage;
import ij.gui.NewImage;

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

  public RGBPhoto( ImagePlus image ){
    this.image = image;
  }

  public RGBPhoto( ImagePlus[] channels ){
    redChannel = channels[0];
    greenChannel = channels[1];
    blueChannel = channels[2];

    RGBStackMerge merger = new RGBStackMerge();
    image = merger.mergeChannels(channels, false);
    //image = new CompositeImage(image);
  }

  public RGBPhoto(HashMap<String, String> valueMap){
    imageDir = valueMap.get(CalibrationPrompt.MAP_IMAGEDIR);
    imageName = valueMap.get(CalibrationPrompt.MAP_IMAGEFILENAME);
    imagePath = valueMap.get(CalibrationPrompt.MAP_IMAGEPATH);
    imageExt = getExtension(imagePath);

    image = new ImagePlus(imagePath);

  }

  public ImagePlus[] splitStack(){
    ImagePlus imp = image;
    if (imp.getNChannels() == 1) {
        imp = new CompositeImage(image);
    }
    ImagePlus[] imageBands = ChannelSplitter.split((ImagePlus)imp);
    /*
    redChannel = scaleImage(imageBands[0], "Red");
    greenChannel = scaleImage(imageBands[1], "Green");
    blueChannel = scaleImage(imageBands[2], "Blue");

    imageBands = new ImagePlus[]{redChannel, greenChannel, blueChannel};
    //ImagePlus visImage = scaleImage(imageBands[visBandIndex], "visImage");
    //ImagePlus nirImage = scaleImage(imageBands[nirBandIndex], "nirImage");
    */
    redChannel = imageBands[0];
    blueChannel = imageBands[1];
    greenChannel = imageBands[2];

    return imageBands;
  }

  public String getExtension(String path){
    return "yo";
  }

  public String getExtension(){
    return "yo";
  }

  public ImagePlus getImage(){
    return image;
  }

  public ImagePlus getRedChannel(){
    return redChannel;
  }

  public ImagePlus getGreenChannel(){
    return greenChannel;
  }

  public ImagePlus getBlueChannel(){
    return blueChannel;
  }

  public void show(){
    image.show();
  }

}
