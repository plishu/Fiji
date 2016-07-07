/*
 * Decompiled with CFR 0_114.
 *
 * Could not load the following classes:
 *  ij.IJ
 *  org.apache.sanselan.ImageReadException
 *  org.apache.sanselan.ImageWriteException
 *  org.apache.sanselan.Sanselan
 *  org.apache.sanselan.common.IImageMetadata
 *  org.apache.sanselan.formats.jpeg.JpegImageMetadata
 *  org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter
 *  org.apache.sanselan.formats.tiff.TiffField
 *  org.apache.sanselan.formats.tiff.TiffImageMetadata
 *  org.apache.sanselan.formats.tiff.write.TiffOutputSet
 */
import ij.IJ;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

public class WriteEXIF {
    File outImageFile = null;
    File originalJpegFile = null;
    File tempImageFile = null;

    public WriteEXIF(File originalJpegFile, File outImageFile, File tempImageFile) {
        this.originalJpegFile = originalJpegFile;
        this.outImageFile = outImageFile;
        this.tempImageFile = tempImageFile;
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
