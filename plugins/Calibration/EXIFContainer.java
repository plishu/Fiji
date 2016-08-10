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
