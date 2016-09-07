import java.io.*;
import ij.IJ;
import java.lang.Exception;

public class EXIFDNGReader implements EXIFReader{

    String path_to_metadata = null;

    public EXIFDNGReader(String mdata){
        path_to_metadata = mdata;
    }

    @Override
    public EXIFContainer readExif(){
        EXIFContainer container = new EXIFContainer();

        try{
            FileReader freader = new FileReader(path_to_metadata);
            BufferedReader buffreader = new BufferedReader(freader);
            String line = null;

            while( (line=buffreader.readLine()) != null ){
                try{
                    if( line.contains("File Creation Date/Time") ){
                        container.setTimeStamp(getAfterColon(line));
                    }else if( line.contains("Camera") ){
                        container.setCameraModel(getAfterColon(line));
                    }else if( line.contains("ISO") ){
                        container.setIsoSpeed(getAfterColon(line));
                    }else if( line.contains("Shutter Speed Value") ){
                        container.setExposureTime(getAfterColon(line));
                    }else if( line.contains("Aperture") ){
                        container.setAperture(getAfterColon(line));
                    }else if( line.contains("Focal Length") ){
                        container.setFocalLength(getAfterColon(line));
                    }
                    //else if( line.contains("Thumb size") ){
                    //    container.setThumbSize(getAfterColon(line));
                    //}
                    //else if( line.contains("Full size") ){
                    //    container.setFullSize(getAfterColon(line));
                    //}
                    else if( line.contains("Image Size") ){
                        container.setImageSize(getAfterColon(line));
                    }
                    //else if( line.contains("Output size") ){
                    //    container.setOutputSize(getAfterColon(line));
                    //}
                    else if( line.contains("Filter pattern") ){
                        container.setFilterPattern(getAfterColon(line));
                    }
                    //else if( line.contains("Daylight multipliers") ){
                    //    container.setDaylightMultiplyer(getAfterColon(line));
                    //}
                    //else if( line.contains("Camera multipliers") ){
                    //    container.setCameraMultiplyer(getAfterColon(line));
                    //}
                    else{}
                }catch(Exception e){
                    IJ.log(e.getMessage());
                }
            }

            buffreader.close();


        }catch(FileNotFoundException e){
            IJ.log("File " + path_to_metadata + " not found.");
            //e.printStackTrace();
        }catch(IOException e){
            IJ.log("An error occured reading " + path_to_metadata);
            e.printStackTrace();
        }

        return container;
    }

    public String getAfterColon(String line){
        String[] split = line.split(".+: ");
        String meat = null;

        if( split.length == 2 ){
            meat = split[1];
        }

        return meat;
    }

}
