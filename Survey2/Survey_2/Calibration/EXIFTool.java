import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Vector;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import ij.IJ;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;


public class EXIFTool {
    File outImageFile = null;
    File originalJpegFile = null;
    File tempImageFile = null;

    public EXIFTool(File originalJpegFile, File outImageFile, File tempImageFile) {
        this.originalJpegFile = originalJpegFile;
        this.outImageFile = outImageFile;
        this.tempImageFile = tempImageFile;
    }

    public static void copyEXIF(File target, File reference){
      OutputStream outStream = null;
      String extension = getExtension(reference);

      IImageMetadata metadata = null;
      JpegImageMetadata jpegMetadata = null;
      TiffImageMetadata tiffMetadata = null;
      TiffImageMetadata exifData = null;
      TiffOutputSet outputSet = null;

      try{
        metadata = Sanselan.getMetadata(reference);

        /*
        if( extension.toUpperCase().equals("JPG") || extension.toUpperCase().equals("JPEG") ){
          jpegMetadata = (JpegImageMetadata)metadata;
        }else if( extension.toUpperCase().equals("TIF") ){
          tiffMetadata = (TiffImageMetadata)metadata;
        }*/
        jpegMetadata = (JpegImageMetadata)metadata;
        exifData = jpegMetadata.getExif();

        if( exifData == null ){
          return;
        }

        outputSet = exifData.getOutputSet();
        outStream = new FileOutputStream(target);
        outStream = new BufferedOutputStream(outStream);
        IJ.log("The target's name " + target.getAbsolutePath());

        //new (ExifRewriter().updateExifMetadataLossless( target, outStream, outputSet));
        

      }catch (ImageWriteException e) {
          e.printStackTrace();
          IJ.error((String)("ImageWriteException \n" ));
      }
      catch (FileNotFoundException e) {
          e.printStackTrace();
          IJ.error((String)("FileNotFoundException \n" ));
      }
      catch (IOException e) {
          e.printStackTrace();
          IJ.error((String)("IOException \n" ));
      }
      catch (ImageReadException e) {
          e.printStackTrace();
          IJ.error((String)("ImageReadException \n"));
      }

      return;
    }

    public static String getExtension(File filename){
      String fn = filename.getName();
      String[] fnsplit = fn.split("\\.(?=[^\\.]+$)");
      String ext = null;

      if( fnsplit.length == 2 ){
        ext = fnsplit[1];
      }
      return ext;
    }

    public void copyEXIF() {
        OutputStream os = null;
        TiffOutputSet outputSet = null;
        JpegImageMetadata jpegMetadata = null;
        TiffImageMetadata tiffMetadata = null;
        String extension = this.originalJpegFile.getName().substring(this.originalJpegFile.getName().lastIndexOf(".") + 1, this.originalJpegFile.getName().length());

        try {
            IImageMetadata metadata = Sanselan.getMetadata((File)this.originalJpegFile);
            if (metadata instanceof JpegImageMetadata) {
                jpegMetadata = (JpegImageMetadata)metadata;
            } else if (metadata instanceof TiffImageMetadata) {
                tiffMetadata = (TiffImageMetadata)metadata;
            }
            if (extension.equals("tif".toLowerCase())) {
                this.tempImageFile.renameTo(this.outImageFile);
                this.tempImageFile.delete();
            } else if (jpegMetadata != null) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (exif != null) {
                    outputSet = exif.getOutputSet();
                }
                os = new FileOutputStream(this.outImageFile);
                os = new BufferedOutputStream(os);
                new ExifRewriter().updateExifMetadataLossless(this.tempImageFile, os, outputSet);
                this.tempImageFile.delete();
            } else if (tiffMetadata != null) {
                outputSet = tiffMetadata.getOutputSet();
                List tiffList = tiffMetadata.getAllFields();
                ArrayList dirList = new ArrayList();
                dirList = tiffMetadata.getDirectories();
                outputSet = new TiffOutputSet();
                for (Object field : tiffMetadata.getAllFields()) {
                    if (!(field instanceof TiffField)) continue;
                    TiffField tiffField = (TiffField)field;
                    System.out.println(String.valueOf(tiffField.getTagName()) + ": " + tiffField.getValueDescription() + " : " + tiffField.length);
                }
                os = new FileOutputStream(this.outImageFile);
                os = new BufferedOutputStream(os);
                new ExifRewriter().updateExifMetadataLossy(this.tempImageFile, os, outputSet);
                this.tempImageFile.delete();
            } else {
                this.tempImageFile.renameTo(this.outImageFile);
            }
        }
        catch (ImageWriteException e) {
            e.printStackTrace();
            IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
        }
        catch (IOException e) {
            e.printStackTrace();
            IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
        }
        catch (ImageReadException e) {
            e.printStackTrace();
            IJ.error((String)("Error adding GPS metadata to file \n" + this.outImageFile.getAbsolutePath()));
        }
    }
}
