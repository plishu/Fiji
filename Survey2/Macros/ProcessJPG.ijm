setBatchMode(true);
args = getArgument();

// Parse Arguments
path_ff = "";
path_jpg = "";
path_out = "";
filter_radius = "";

tmpstr1 = split(args, "|");

// format: arg1=v1 | arg2=v2 | arg3=v3
for( i=0; i<tmpstr1.length; i++ ){
  tmpstr2 = split(tmpstr1[i], "=");

  if( tmpstr2[0] == "filter_radius" ){
    filter_radius = tmpstr2[1];
    filter_radius = parseInt(filter_radius);
  }else if( tmpstr2[0] == "path_ff" ){
    path_ff = tmpstr2[1];
  }else if( tmpstr2[0] == "path_jpg"){
    path_jpg = tmpstr2[1];
  }else if( tmpstr2[0] == "path_out"){
    path_out = tmpstr2[1];
  }
}

//print("path_ff: " + path_ff);
//print("path_jpg: " + path_jpg);
//print("path_out: " + path_out);
//print("filter_radius: " + filter_radius);

// Open FF
run("Open [Image IO]", "image="+ "[" + path_ff + "]");
flatFieldImage = getTitle();
run("Split Channels");
selectWindow(flatFieldImage + " (red)");
run("Mean...", "radius="+filter_radius);
run("32-bit");
getRawStatistics(nPixels, mean, min, max, std, histogram);
run("Macro...", "code=v=" + max + "/v");
factorRed = getTitle();

selectWindow(flatFieldImage + " (green)");
run("Mean...", "radius="+filter_radius);
run("32-bit");
getRawStatistics(nPixels, mean, min, max, std, histogram);
run("Macro...", "code=v=" + max + "/v");
factorGreen = getTitle();

selectWindow(flatFieldImage + " (blue)");
run("Mean...", "radius="+filter_radius);
run("32-bit");
getRawStatistics(nPixels, mean, min, max, std, histogram);
run("Macro...", "code=v=" + max + "/v");
factorBlue = getTitle();


// Open JPG image
run("Open [Image IO]", "image="+ "[" + path_jpg + "]");
toProcessImage = getTitle();
run("Split Channels");

selectWindow(toProcessImage + " (red)");
toProcessBand = getTitle();
run("Calculator Plus", "i1="+"["+factorRed+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
rename("Result1");

selectWindow(toProcessImage + " (green)");
toProcessBand = getTitle();
run("Calculator Plus", "i1="+"["+factorGreen+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
rename("Result2");

selectWindow(toProcessImage + " (blue)");
toProcessBand = getTitle();
run("Calculator Plus", "i1="+"["+factorBlue+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
rename("Result3");
run("Merge Channels...", "c1=Result1 c2=Result2 c3=Result3 create");

// Save
saveAs("jpeg", path_out + File.nameWithoutExtension + ".jpg");
close();

selectWindow(toProcessImage + " (red)");
close();
selectWindow(toProcessImage + " (green)");
close();
selectWindow(toProcessImage + " (blue)");
close();

// Clean up
run("Close All");
run("Collect Garbage");
call("java.lang.System.gc");
