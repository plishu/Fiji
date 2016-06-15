import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;

import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.awt.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.lang.Runtime;
import java.lang.ProcessBuilder;
import java.lang.Process;
import ij.io.*;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.ResultPoint;

public class QRCalib{

  private Reader qrReader = null;

  public QRCalib(){
    qrReader = new QRCodeReader();
  }

  public Result decodeQR(ImagePlus inImg){
    BufferedImage bfimg = inImg.getBufferedImage();
    LuminanceSource source = new BufferedImageLuminanceSource(bfimg);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    String resultstr = "";
    print("Decoding...");

    try{
      Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      Result result = qrReader.decode(bitmap, hints);

    } catch (NotFoundException nf){
      print( "Could not find QR code.");
    } catch (ChecksumException cs){
      print("Checksum Error.");
    } catch (FormatException fe){
      print("Could not find QR format.");
    }

    return result;
  }

  public ImagePlus resize(ImagePlus inImg){
    return null;
  }

  public void print(String str){
    // @TODO Make it that it is not limited to IJ.log, but any output stream
    // (command prompt, log file, etc)
    IJ.log(str);
  }



}
