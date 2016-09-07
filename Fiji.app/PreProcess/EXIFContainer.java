import java.lang.Exception;

// **Read Only container**
public class EXIFContainer{
    private String exposureTime = null;
    private String isoSpeed = null;
    private String cameraModel = null;
    private String cameraMaker = null;
    private String fStop = null;
    private String exposureBias = null;
    private String focalLength = null;
    private String whiteBalance = null;

    private String timeStamp = null;
    private String aperture = null;
    private String thumbSize = null;
    private String fullSize = null;
    private String imageSize = null;
    private String outputSize = null;
    private String filterPattern = null;
    private String dayLightMultiplyer = null;
    private String cameraMultiplyer = null;

    public void setTimeStamp(String var) throws Exception{
        if( timeStamp == null ){
            timeStamp = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setAperture(String var) throws Exception{
        if( aperture == null ){
            aperture = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setThumbSize(String var) throws Exception{
        if( thumbSize == null ){
            thumbSize = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setFullSize(String var) throws Exception{
        if( fullSize == null ){
            fullSize = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setImageSize(String var) throws Exception{
        if( imageSize == null ){
            imageSize = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setOutputSize(String var) throws Exception{
        if( outputSize == null ){
            outputSize = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setFilterPattern(String var) throws Exception{
        if( filterPattern == null ){
            filterPattern = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setDaylightMultiplyer(String var) throws Exception{
        if( dayLightMultiplyer == null ){
            dayLightMultiplyer = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setCameraMultiplyer(String var) throws Exception{
        if( cameraMultiplyer == null ){
            cameraMultiplyer = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }

    public void setExposureTime(String var) throws Exception{
        if( exposureTime == null ){
            exposureTime = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setIsoSpeed(String var) throws Exception{
        if( isoSpeed == null ){
            isoSpeed = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setCameraModel(String var) throws Exception{
        if( cameraModel == null ){
            cameraModel = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setCameraMaker(String var) throws Exception{
        if( cameraMaker == null ){
            cameraMaker = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setFStop(String var) throws Exception{
        if( fStop == null ){
            fStop = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setExposureBias(String var) throws Exception{
        if( exposureBias == null ){
            exposureBias = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setFocalLength(String var) throws Exception{
        if( focalLength == null ){
            focalLength = var;
        }else{
            throw new Exception("Container class is READ ONLY once value has been set.");
        }
    }
    public void setWhiteBalance(String var) throws Exception{
        if( whiteBalance == null ){
            whiteBalance = var;
        }else{
            throw new Exception("Container class is READ ONLY once a value has been set");
        }
    }

    public String getTimeStamp(){
        return timeStamp;
    }

    public String getApeture(){
        return aperture;
    }

    public String getThumbSize(){
        return thumbSize;
    }

    public String getFullSize(){
        return fullSize;
    }

    public String getImageSize(){
        return imageSize;
    }

    public String getOutputSize(){
        return outputSize;
    }

    public String getFilterPattern(){
        return filterPattern;
    }

    public String getDaylightMultiplyer(){
        return dayLightMultiplyer;
    }

    public String getCameraMultiplyer(){
        return cameraMultiplyer;
    }

    public String getExposureTime(){
        return exposureTime;
    }
    public String getIsoSpeed(){
        return isoSpeed;
    }
    public String getCameraModel(){
        return cameraModel;
    }
    public String getCameraMaker(){
        return cameraMaker;
    }
    public String getFStop(){
        return fStop;
    }
    public String getExposureBias(){
        return exposureBias;
    }
    public String getFocalLength(){
        return focalLength;
    }
    public String getWhiteBalance(){
        return whiteBalance;
    }

}
