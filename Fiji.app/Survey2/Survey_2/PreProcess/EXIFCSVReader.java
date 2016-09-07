import java.io.BufferedReader;
import java.lang.Exception;
import java.io.FileReader;
import ij.IJ;

public class EXIFCSVReader implements EXIFReader{

    private final String CSV_EXPOSURE_TIME_STR = "Exposure Time";
    private final String CSV_ISO_SPEED_STR = "ISO";
    private final String CSV_CAMERA_MODEL_STR = "Camera Model Name";
    private final String CSV_CAMERA_MAKER_STR = "Make";
    private final String CSV_F_STOP_STR = "F Number";
    private final String CSV_EXPOSURE_BIAS_STR = "Exposure Compensation";
    private final String CSV_FOCAL_LENGTH_STR = "Focal Length";
    private final String CSV_WHITE_BALANCE_STR = "White Balance";

    private String path_to_csv = null;

    public EXIFCSVReader(String path){
        path_to_csv = path;
    }


    @Override
    public EXIFContainer readExif(){
        String fullLine = "";
        String line = "";
        int numLines = 0;
        String[] split = null;
        BufferedReader fileReader = null;
        String commaDelimiter = ",";
        String valueMeal = null;
        EXIFContainer exifcontainer = new EXIFContainer();
        try{
          fileReader = new BufferedReader( new FileReader(path_to_csv) );


          while( (line = fileReader.readLine()) != null ){
              if( line.length() <= 0 ){
                  continue;
              }
              //IJ.log("CSV says: " + line);

              if( match(line, CSV_EXPOSURE_TIME_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setExposureTime(valueMeal);
              }else if( match(line, CSV_ISO_SPEED_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setIsoSpeed(valueMeal);
              }else if( match(line, CSV_CAMERA_MODEL_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setCameraModel(valueMeal);
              }else if( match(line, CSV_CAMERA_MAKER_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setCameraMaker(valueMeal);
              }else if( match(line, CSV_F_STOP_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setFStop(valueMeal);
              }else if( match(line, CSV_EXPOSURE_BIAS_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setExposureBias(valueMeal);
              }else if( match(line, CSV_FOCAL_LENGTH_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setFocalLength(valueMeal);
              }else if( match(line, CSV_WHITE_BALANCE_STR) ){
                  valueMeal = (line.split(commaDelimiter))[1];
                  exifcontainer.setWhiteBalance(valueMeal);
              }

          }

        }catch( Exception e ){
          e.printStackTrace();
        }


        return exifcontainer;
    }

    private boolean match(String line, String target){
        return line.contains(target);
    }

}
