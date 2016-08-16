import java.io.IOException;
import java.lang.ProcessBuilder;
import java.lang.Process;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import ij.IJ;

public class EXIFToolsReader implements EXIFReader{

    // Strings as they appear on EXIFTool
    private final String EXIF_EXPOSURE_TIME_STR = "Exposure Time";
    private final String EXIF_ISO_SPEED_STR = "ISO";
    private final String EXIF_CAMERA_MODEL_STR = "Camera Model Name";
    private final String EXIF_CAMERA_MAKER_STR = "Make";
    private final String EXIF_F_STOP_STR = "F Number";
    private final String EXIF_EXPOSURE_BIAS_STR = "Exposure Compensation";
    private final String EXIF_FOCAL_LENGTH_STR = "Focal Length";
    private final String EXIF_WHITE_BALANCE_STR = "White Balance";

    private String path_to_exiftool = null;
    private String path_to_img = null;

    public EXIFToolsReader(String path_tool, String path_img){
        path_to_exiftool = path_tool;
        path_to_img = path_img;
    }

    @Override
    public EXIFContainer readExif(){

        Process exifProcess = null;
        BufferedReader output = null;
        EXIFContainer exifcontainer = new EXIFContainer();

        String line = null;
        String colonDelimiter = ": ";
        String valueMeal = null;
        try{
            exifProcess = callEXIFTools();

            //output = new BufferedReader( new InputStreamReader(exifProcess.getInputStream()) );
            output = new BufferedReader( new InputStreamReader(exifProcess.getInputStream()) );
            BufferedReader err = new BufferedReader( new InputStreamReader(exifProcess.getErrorStream()) );
            String errstr;

          while( (line = output.readLine()) != null ){

            //IJ.log("EXIFTool says: " + line);
            //errstr = err.readLine();
            //IJ.log("Error: " + errstr);
            if( match(line, EXIF_EXPOSURE_TIME_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setExposureTime(valueMeal);
            }else if( match(line, EXIF_ISO_SPEED_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setIsoSpeed(valueMeal);
            }else if( match(line, EXIF_CAMERA_MODEL_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setCameraModel(valueMeal);
            }else if( match(line, EXIF_CAMERA_MAKER_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setCameraMaker(valueMeal);
            }else if( match(line, EXIF_F_STOP_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setFStop(valueMeal);
            }else if( match(line, EXIF_EXPOSURE_BIAS_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setExposureBias(valueMeal);
            }else if( match(line, EXIF_FOCAL_LENGTH_STR) ){
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setFocalLength(valueMeal);
            }else if( match(line, EXIF_WHITE_BALANCE_STR) ){
                //@TODO White Balance not showing in EXIFTool output for some reason
                valueMeal = (line.split(colonDelimiter))[1];
                exifcontainer.setWhiteBalance(valueMeal);
            }

          }

        }catch( IOException e){
          e.printStackTrace();
        }catch( Exception j){
          j.printStackTrace();
        }

        return exifcontainer;
    }

    private boolean match(String line, String target){
        return line.contains(target);
    }

    private Process callEXIFTools(){
        String console = null;
        String c_arg = null;
        String command = null;
        ProcessBuilder bob = null;
        Process proc = null;

        if( System.getProperty("os.name").contains("Windows") ){
          console = "cmd";
          c_arg = "/c";
          command = path_to_exiftool + " " + "\""+path_to_img+"\"";
        }else{
          console = "sh";
          c_arg = "-c";
          command = path_to_exiftool + " " + "\'"+path_to_img+"\'";
        }
        //IJ.log("Executing command: " + command);


        try{
            bob = new ProcessBuilder(console, c_arg, command);
            bob.redirectErrorStream(true);
            proc = bob.start();

        }catch(IOException e){
            e.printStackTrace();
        }
        return proc;
    }
}
