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

public class Auto_Calibrate implements PlugIn{

  public void run(String args){

    String imageDir = IJ.getFilePath("Input QR Code image");
    ImagePlus qrimg = IJ.openImage(imageDir);

    QRCalib qr = new QRCalib();

    Result result = qr.decodeQR(qrimg);

    ResultPoint[] points = result.getResultPoints();
    IJ.log( Integer.toString(points.length) );

    IJ.log("X1: " + Float.toString(points[0].getX()) );
    IJ.log("Y1: " + Float.toString(points[0].getY()) );
    IJ.log("X2: " + Float.toString(points[1].getX()) );
    IJ.log("Y2: " + Float.toString(points[1].getY()) );
    IJ.log("X3: " + Float.toString(points[2].getX()) );
    IJ.log("Y3: " + Float.toString(points[2].getY()) );

  }
}
