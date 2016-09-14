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
        if( exifcontainer != null)
        {
            return exifcontainer.getTimeStamp();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getApeture(){
        if( exifcontainer != null)
        {
            return exifcontainer.getApeture();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getThumbSize(){
        if( exifcontainer != null)
        {
            return exifcontainer.getThumbSize();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getFullSize(){
        if( exifcontainer != null)
        {
            return exifcontainer.getFullSize();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getImageSize(){
        if( exifcontainer != null)
        {
            return exifcontainer.getImageSize();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getOutputSize(){
        if( exifcontainer != null)
        {
            return exifcontainer.getOutputSize();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getFilterPattern(){
        if( exifcontainer != null)
        {
            return exifcontainer.getFilterPattern();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getDaylightMultiplyer(){
        if( exifcontainer != null)
        {
            return exifcontainer.getDaylightMultiplyer();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getCameraMultiplyer(){
        if( exifcontainer != null)
        {
            return exifcontainer.getCameraMultiplyer();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getExposureTime(){
        if( exifcontainer != null)
        {
            return exifcontainer.getExposureTime();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getISOSpeed(){
        if( exifcontainer != null)
        {
            return exifcontainer.getIsoSpeed();
        }
        else{
            return "No Exif Data";
        }
    }
    public String getCameraModel(){
        if( exifcontainer != null)
        {
            return exifcontainer.getCameraModel();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getCameraMaker(){
        if( exifcontainer != null)
        {
            return exifcontainer.getCameraMaker();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getFStop(){
        if( exifcontainer != null)
        {
            return exifcontainer.getFStop();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getExposureBias(){
        if( exifcontainer != null)
        {
            return exifcontainer.getExposureBias();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getFocalLength(){
        if( exifcontainer != null)
        {
            return exifcontainer.getFocalLength();
        }
        else{
            return "No Exif Data";
        }
    }

    public String getWhiteBalance(){
        if( exifcontainer != null)
        {
            return exifcontainer.getWhiteBalance();
        }
        else{
            return "No Exif Data";
        }
    }


    public boolean equals(Object obj){
        if( !(obj instanceof CameraEXIF) ){
            return false;
        }
        else if (obj == null)
        {
            return false;
        }

        CameraEXIF objCast = (CameraEXIF)obj;
        boolean camerasMatch = false;
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

        if( (exifcontainer == null) || (objCast == null))
        {
            return camerasMatch;
        }
        else
        {
            // return true;
            camerasMatch = objCast.getExposureTime().equals(exifcontainer.getExposureTime())
                && objCast.getISOSpeed().equals(exifcontainer.getIsoSpeed())
                && objCast.getCameraModel().equals(exifcontainer.getCameraModel())
                && objCast.getExposureBias().equals(exifcontainer.getExposureBias());
                //&& objCast.getWhiteBalance().equals(exifcontainer.getWhiteBalance());
        }


        return camerasMatch;
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
