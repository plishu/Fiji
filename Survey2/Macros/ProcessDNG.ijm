setBatchMode(true);

args = getArgument();

// Parse argument
path_ff = "";
path_raw = "";
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
  }else if( tmpstr2[0] == "path_raw"){
    path_raw = tmpstr2[1];
  }else if( tmpstr2[0] == "path_out"){
    path_out = tmpstr2[1];
  }
}

print("path_ff: " + path_ff);
print("path_raw: " + path_raw);
print("path_out: " + path_out);
print("filter_radius: " + filter_radius);

/* Dev mode: comment out */
/*
path_ff = File.openDialog("Select Flat-Field");
path_raw = File.openDialog("Select DNG File");
path_out = File.directory;
filter_radius = 0;
*/

fileName_ff = "";
fileNameNoExt_ff = "";
fileName_raw = "";
fileNameNoExt_raw = "";

// Open flat-field image
if( filter_radius != 0 ){
  print("Opening Flat-Field: " + path_ff);
  run("DCRaw Reader...", "open=["+path_ff+"] white_balance=None output_colorspace=raw read_as=8-bit interpolation=[High-speed, low-quality bilinear]");
  fileName_ff = getTitle();
  tmp = split(fileName_ff, ".");
  fileNameNoExt_ff = tmp[0];

  print("Splitting channels " + getTitle());
  run("Split Channels");
  wait(1000);

  selectWindow(fileName_ff + " (red)");
  print("Applying mean on " + getTitle());
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");

  selectWindow(fileName_ff + " (green)");
  print("Applying mean on " + getTitle());
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");

  selectWindow(fileName_ff + " (blue)");
  print("Applying mean on " + getTitle());
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");

}

// Process RAW image
print("Opening " + path_raw);
run("DCRaw Reader...", "open=["+path_raw+"] white_balance=None output_colorspace=raw read_as=8-bit interpolation=[High-speed, low-quality bilinear]");
fileName_raw = getTitle();
tmp = split(fileName_raw, ".");
fileNameNoExt_raw = tmp[0];

// Don't correct for vignette, just save
if (filter_radius == 0) {
  //run("RGB Color");
  print("Saving " + getTitle());
  saveAs("Tiff", path_out + fileNameNoExt_raw + ".tif");
  close();
} else {
  // Correct for vignette
  selectWindow(fileName_raw);
  print("Splitting channels " + getTitle());
  run("Split Channels");

  run("Calculator Plus", "i1="+"[" + fileName_ff + " (red)" + "]"+" i2="+"[" + fileName_raw + " (red)" + "]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  selectWindow(fileName_ff + " (red)");
  close();
  selectWindow(fileName_raw + " (red)");
  close();
  selectWindow("Result");
  rename("ResultR");

  run("Calculator Plus", "i1="+"[" + fileName_ff + " (green)" + "]"+" i2="+"[" + fileName_raw + " (green)" + "]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  selectWindow(fileName_ff + " (green)");
  close();
  selectWindow(fileName_raw + " (green)");
  close();
  selectWindow("Result");
  rename("ResultG");

  run("Calculator Plus", "i1="+"[" + fileName_ff + " (blue)" + "]"+" i2="+"[" + fileName_raw + " (blue)" + "]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  selectWindow(fileName_ff + " (blue)");
  close();
  selectWindow(fileName_raw + " (blue)");
  close();
  selectWindow("Result");
  rename("ResultB");

  print("Merging channels");
  run("Merge Channels...", "c1=ResultR c2=ResultG c3=ResultB create"); // @TODO <--Remove create to fix tif!!
  print("Finished merging " + getTitle());

  selectWindow("RGB");

  print("Saving " + getTitle());
  saveAs("Tiff", path_out + fileNameNoExt_raw + ".tif");
  print("Closing " + getTitle());
  close();

}

// Clean up
run("Close All");
run("Collect Garbage");
call("java.lang.System.gc");
