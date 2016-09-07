import ij.ImagePlus;
import ij.IJ;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.plugin.RGBStackMerge;
import ij.CompositeImage;
import ij.gui.NewImage;
import ij.plugin.RGBStackConverter;
import ij.process.StackConverter;
import ij.plugin.frame.ContrastAdjuster;

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

  // Nice to have, but not required for basic functions
  private String camera = null;
  private String filter = null;

  /*
   * Create RGBPhoto from photo directory
   * @param: dir - Directory of photo
   * @return: none
   */
  public RGBPhoto(String dir, String fname, String path, String cmodel, boolean convertTo8Bit){
    imageDir = dir;
    imageName = getFileName(fname);
    imagePath = path;
    imageExt = getExtension(imagePath);

    //image = new ImagePlus(imagePath);
    image = IJ.openImage(imagePath);

    //IJ.log( Integer.toString(image.getNChannels()) );
    // Fix tifs that come in with 3 channels only (from raw to tif conversion of pre-process)
    if( imageExt.toUpperCase().equals("TIF") && image.getNChannels() != 1 ){
      //RGBStackConverter.convertToRGB(image);
      //(new StackConverter(image)).convertToRGB();
      fixTif(convertTo8Bit);
    }


    // YOU NEED THIS FOR IT TO WORK!!
    //image.show();

    // Get channels
    /*
    ImagePlus imp = image;
    if (imp.getNChannels() == 1) {
        IJ.log("THIS IS A COMPOSITE IMAGE");
        imp = new CompositeImage(image);
    }*/

    /*
    ImagePlus[] imageBands = ChannelSplitter.split(image);
    redChannel = imageBands[0];
    greenChannel = imageBands[1];
    blueChannel = imageBands[2];
    */

    splitStack(image);

    //ImageStack stack = image.getImageStack();
    //ImageStack[] stk = ChannelSplitter.splitRGB(stack, false);

    //redChannel.setStack(stk[0]);
    //greenChannel.setStack(stk[1]);
    //blueChannel.setStack(stk[2]);


    //redChannel.show();
    //greenChannel.show();
    //blueChannel.show();

    camera = cmodel;

    if( camera == null ){
      // Usually happens when the correct valueMap is not supplied. AKA
      // qrFileDialogValues because the camera model is not set there.
      // This is ok for qr code because it is not needed.
    }

    /*
    if( camera != null ){
      if( camera.equals(CalibrationPrompt.SURVEY2_RED) ){
        filter = "650";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_GREEN) ){
        filter = "548";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_BLUE) ){
        filter = "450";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_NDVI) ){
        filter = "660/850";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_NIR) ){
        filter = "850";
      }else if( camera.equals(CalibrationPrompt.OTHER_CAMERA) ){
        //@TODO Support selection of filter
        //filter = valueMap.get(CalibrationPrompt.MAP_FILTER);
      }else{
        IJ.log("Camera not supported");
      }
    }*/
    setFilter(camera);
  }

  public RGBPhoto( ImagePlus image ){
    this.image = image;

    // Get channels
    /*
    ImagePlus imp = image;
    if (imp.getNChannels() == 1) {
        imp = new CompositeImage(image);
    }
    ImagePlus[] imageBands = ChannelSplitter.split((ImagePlus)imp);
    redChannel = imageBands[0];
    greenChannel = imageBands[1];
    blueChannel = imageBands[2];
    */

    if( imageExt.toUpperCase().equals("TIF") ){
      //RGBStackConverter.convertToRGB(image);
      //(new StackConverter(image)).convertToRGB();
      fixTif(false);
    }

    splitStack(image);

  }

  public RGBPhoto( RGBPhoto ref){
    this.image = ref.getImage();

    imageDir = ref.getDir();
    imageName = ref.getFileName();
    imagePath = ref.getPath();
    imageExt = ref.getExtension();

    splitStack(image);
  }

  public RGBPhoto( ImagePlus[] channels, String cam, RGBPhoto ref ){
    redChannel = channels[0];
    greenChannel = channels[1];
    blueChannel = channels[2];


    RGBStackMerge merger = new RGBStackMerge();
    image = merger.mergeChannels(channels, false);
    //image.flattenStack();
    //image.show();
    RGBStackConverter.convertToRGB(image);
    //(new StackConverter(image)).convertToRGB();


    camera = cam;
    imageDir = ref.getDir();
    imageName = ref.getFileName();
    imagePath = ref.getPath();
    imageExt = ref.getExtension();
    setFilter(camera);
  }


  public RGBPhoto(HashMap<String, String> valueMap){
    imageDir = valueMap.get(CalibrationPrompt.MAP_IMAGEDIR);
    imageName = getFileName(valueMap.get(CalibrationPrompt.MAP_IMAGEFILENAME));
    imagePath = valueMap.get(CalibrationPrompt.MAP_IMAGEPATH);
    imageExt = getExtension(imagePath);

    image = new ImagePlus(imagePath);

    // Get channels
    /*
    ImagePlus imp = image;
    if (imp.getNChannels() == 1) {
        imp = new CompositeImage(image);
    }
    ImagePlus[] imageBands = ChannelSplitter.split((ImagePlus)imp);
    redChannel = imageBands[0];
    greenChannel = imageBands[1];
    blueChannel = imageBands[2];*/

    if( imageExt.toUpperCase().equals("TIF") ){
      //RGBStackConverter.convertToRGB(image);
      //(new StackConverter(image)).convertToRGB();
      fixTif(false);
    }

    splitStack(image);

    camera = valueMap.get(CalibrationPrompt.MAP_CAMERA);

    if( camera == null ){
      // Usually happens when the correct valueMap is not supplied. AKA
      // qrFileDialogValues because the camera model is not set there.
      // This is ok for qr code because it is not needed.
    }
    /*
    if( camera != null ){
      if( camera.equals(CalibrationPrompt.SURVEY2_RED) ){
        filter = "650";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_GREEN) ){
        filter = "548";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_BLUE) ){
        filter = "450";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_NDVI) ){
        filter = "660/850";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_NIR) ){
        filter = "850";
      }else if( camera.equals(CalibrationPrompt.OTHER_CAMERA) ){
        filter = valueMap.get(CalibrationPrompt.MAP_FILTER);
      }else{
        IJ.log("Camera not supported");
      }
    }*/
    setFilter(camera);
  }

  public void copyFileData( RGBPhoto refphoto ){
    imageDir = refphoto.getDir();
    imageName = refphoto.getFileName();
    imagePath = refphoto.getPath();
    imageExt = refphoto.getExtension(imagePath);

    camera = refphoto.getCameraType();
    setFilter(camera);
  }

  public String getDir(){
    return imageDir;
  }

  public String getPath(){
    return imagePath;
  }

  public ImagePlus[] splitStack(ImagePlus img){
    ImagePlus[] imageBands = ChannelSplitter.split(img);

    if( imageBands.length != 3 ){
      IJ.log("Could not split bands for this image. I am skipping this image.");
      return null;
    }

    redChannel = imageBands[0];
    greenChannel = imageBands[1];
    blueChannel = imageBands[2];

    return imageBands;
  }

  public void setFilter( String camera ){
    if( camera != null ){
      if( camera.equals(CalibrationPrompt.SURVEY2_RED) ){
        this.filter = "650";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_GREEN) ){
        this.filter = "548";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_BLUE) ){
        this.filter = "450";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_NDVI) ){
        this.filter = "660/850";
      }else if( camera.equals(CalibrationPrompt.SURVEY2_NIR) ){
        this.filter = "850";
      }else if( camera.equals(CalibrationPrompt.DJIPHANTOM4_NDVI) ){
        this.filter = "660/850";
      }else if( camera.equals(CalibrationPrompt.DJIX3_NDVI) ){
        this.filter = "660/850";
      }else if( camera.equals(CalibrationPrompt.DJIPHANTOM3_NDVI) ){
        this.filter = "660/850";
      }else if( camera.equals(CalibrationPrompt.OTHER_CAMERA) ){
        //this.filter = valueMap.get(CalibrationPrompt.MAP_FILTER);
        filter = null; // User will input filter
      }else{
        IJ.log("Camera not supported");
      }
    }
  }

  public String getExtension(String path){
    String[] fnsplit = path.split("\\.(?=[^\\.]+$)");
    String[] split = null;

    if( fnsplit.length == 2 ){
      // filename split successfully into filename and extension
      split = fnsplit;
    }
    imageExt = split[1];
    return split[1];
  }

  public String getExtension(){
    String[] fnsplit = imagePath.split("\\.(?=[^\\.]+$)");
    String[] split = null;

    if( fnsplit.length == 2 ){
      // filename split successfully into filename and extension
      split = fnsplit;
    }
    imageExt = split[1];
    return split[1];
  }

  public String getFileName(String fname){
    String[] fnsplit = fname.split("\\.(?=[^\\.]+$)");
    String[] split = null;

    if( fnsplit.length == 2 ){
      // filename split successfully into filename and extension
      split = fnsplit;
    }
    return split[0];
  }

  public String getFileName(){
    return imageName;
  }

  public ImagePlus getImage(){
    return image;
  }

  public String getCameraType(){
    return camera;
  }

  public String getFilterType(){
    return filter;
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

  public void close(){
    image.close();
  }

  public void fixTif(boolean convertTo8Bit){
    //IJ.log("Fixing tif image");
    image.getProcessor().setMinAndMax(0,65535);
    image.setDefault16bitRange(16);
    ContrastAdjuster.update();


    image.show();
    ImagePlus tmpimg = IJ.getImage();
    IJ.run("RGB Color");
    image = IJ.getImage();
    tmpimg.close();
    //image.close();
    //(new StackConverter(image)).convertToRGB();
    /*
    if( convertTo8Bit ){
      RGBStackConverter.convertToRGB(image);
    }*/
    //image.show();
  }

  public boolean checkChannels(){
    if( redChannel == null || greenChannel == null || blueChannel == null ){
      return false;
    }else{
      return true;
    }
  }

}
