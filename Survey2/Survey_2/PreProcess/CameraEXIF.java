public class CameraEXIF{

    public EXIFReader reader = null;
    public EXIFContainer exifcontainer = null;

    private String exposureTime;
    private String isoSpeed;
    private String cameraModel;
    private String cameraMaker;
    private String fStop;
    private String exposureBias;
    private String focalLength;

    public CameraEXIF(EXIFReader nreader){
        reader = nreader;
        exifcontainer = reader.readExif();
    }

    public String getExposureTime(){
        return exifcontainer.getExposureTime();
    }

    public String getISOSpeed(){
        return exifcontainer.getIsoSpeed();
    }
    public String getCameraModel(){
        return exifcontainer.getCameraModel();
    }

    public String getCameraMaker(){
        return exifcontainer.getCameraMaker();
    }

    public String getFStop(){
        return exifcontainer.getFStop();
    }

    public String getExposureBias(){
        return exifcontainer.getExposureBias();
    }

    public String getFocalLength(){
        return exifcontainer.getFocalLength();
    }

    public boolean equals(Object obj){
        if( !(obj instanceof CameraEXIF) ){
            return false;
        }

        EXIFContainer objCast = (EXIFContainer)obj;
        boolean condition = objCast.getExposureTime().equals(exifcontainer.getExposureTime())
            && objCast.getIsoSpeed().equals(exifcontainer.getIsoSpeed())
            && objCast.getCameraModel().equals(exifcontainer.getCameraModel())
            && objCast.getCameraMaker().equals(exifcontainer.getCameraMaker())
            && objCast.getFStop().equals(exifcontainer.getFStop())
            && objCast.getExposureBias().equals(exifcontainer.getExposureBias())
            && objCast.getFocalLength().equals(exifcontainer.getFocalLength());

        return condition;
    }

    // Return all differences in both containers
    public void diff(EXIFContainer otherContainer){

    }

}
