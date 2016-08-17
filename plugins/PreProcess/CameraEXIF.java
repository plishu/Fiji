import ij.IJ;

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

    public String getTimeStamp(){
        return exifcontainer.getTimeStamp();
    }

    public String getApeture(){
        return exifcontainer.getApeture();
    }

    public String getThumbSize(){
        return exifcontainer.getThumbSize();
    }

    public String getFullSize(){
        return exifcontainer.getFullSize();
    }

    public String getImageSize(){
        return exifcontainer.getImageSize();
    }

    public String getOutputSize(){
        return exifcontainer.getOutputSize();
    }

    public String getFilterPattern(){
        return exifcontainer.getFilterPattern();
    }

    public String getDaylightMultiplyer(){
        return exifcontainer.getDaylightMultiplyer();
    }

    public String getCameraMultiplyer(){
        return exifcontainer.getCameraMultiplyer();
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

    public String getWhiteBalance(){
        return exifcontainer.getWhiteBalance();
    }


    public boolean equals(Object obj){
        if( !(obj instanceof CameraEXIF) ){
            return false;
        }

        CameraEXIF objCast = (CameraEXIF)obj;

        /*
        boolean condition = objCast.getExposureTime().equals(exifcontainer.getExposureTime())
            && objCast.getISOSpeed().equals(exifcontainer.getIsoSpeed())
            && objCast.getCameraModel().equals(exifcontainer.getCameraModel())
            && objCast.getCameraMaker().equals(exifcontainer.getCameraMaker())
            && objCast.getFStop().equals(exifcontainer.getFStop())
            && objCast.getExposureBias().equals(exifcontainer.getExposureBias())
            && objCast.getFocalLength().equals(exifcontainer.getFocalLength())
            && objCast.getWhiteBalance().equals(exifcontainer.getFocalLength());
            */


        boolean whatNolanWants = objCast.getExposureTime().equals(exifcontainer.getExposureTime())
            && objCast.getISOSpeed().equals(exifcontainer.getIsoSpeed())
            && objCast.getCameraModel().equals(exifcontainer.getCameraModel())
            && objCast.getExposureBias().equals(exifcontainer.getExposureBias());
            //&& objCast.getWhiteBalance().equals(exifcontainer.getWhiteBalance());



        return whatNolanWants;
    }

    // Return all differences in both containers
    public void diff(EXIFContainer otherContainer){

    }

    public String printEXIFData(){
        String exifdata = "Exposure Time: " + exifcontainer.getExposureTime() + "\n";
        exifdata += "ISO: " + exifcontainer.getIsoSpeed() + "\n";
        exifdata += "Model: " + exifcontainer.getCameraModel() + "\n";
        exifdata += "Exposure Bias: " + exifcontainer.getExposureBias() + "\n";
        exifdata += "White Exposure: " + exifcontainer.getWhiteBalance() + "\n";

        return exifdata;
    }

}
