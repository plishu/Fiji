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



// Open flat-field image
if( filter_radius != 0 ){
  print("Opening raw: " + path_ff);
  run("Raw...", "open="+ "[" + path_ff + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
  print("Finished opening raw: " + path_ff);

  rawImage = getTitle();
  print("Debayering " + rawImage);
  run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
  print("Done Debayering " + rawImage);

  selectWindow(rawImage);
  close();
  print("Closed " + rawImage);
  wait(1000);


  selectWindow("RGB Stack");
  print("Making composite of " + getTitle());
  run("Make Composite", "display=Color");
  print("Finished composite of " + getTitle());
  labels = newArray("RedFlat", "GreenFlat", "BlueFlat");
  for (l=0; l<3; l++) {
    setSlice(l+1);
    setMetadata("label", labels[l]);
  }

  print("Making images from stack of " + getTitle());
  run("Stack to Images");
  selectWindow("RedFlat");
  print("Applying mean on " + getTitle());
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");
  factorRed = getTitle();
  print(factorRed + " is prepared");

  selectWindow("GreenFlat");
  print("Applying mean on " + getTitle());
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");
  factorGreen = getTitle();
  print(factorGreen + " is prepared");

  selectWindow("BlueFlat");
  print("Applying mean on " + getTitle());
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");
  factorBlue = getTitle();
  print(factorBlue + " is prepared");

}

// Process RAW image
print("Opening " + path_raw);
run("Raw...", "open="+ "[" +  path_raw + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
rawimgfname = getInfo("image.filename");
print("Done Opening" + getTitle());

print("Debayering " + getTitle());
run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
print("Finished Debayering " + getTitle());
selectWindow(rawimgfname);
print("Closed " + getTitle());
close();
wait(1000);

selectWindow("RGB Stack");
print("Making composite of " + getTitle());
run("Make Composite", "display=Color");
print("Finished composite of " + getTitle());

if (filter_radius == 0) {
  labels = newArray("Red", "Green", "Blue");
} else {
  labels = newArray("RedIn", "GreenIn", "BlueIn");
}

for (j=0; j<3; j++) {
  setSlice(j+1);
  setMetadata("label", labels[j]);
}

//outputFileNameParts = split(rawFilesToProcess[i], ".");
//outputFileName = outputFileNameParts[0] + ".tif";


if (filter_radius == 0) {
  run("RGB Color");
  print("Saving " + getTitle());
  saveAs("Tiff", path_out + File.nameWithoutExtension + ".tif");
  close();
} else {
  print("Making images from stack of " + getTitle());
  run("Stack to Images");
  print("Made images");
  selectWindow("RedIn");
  toProcessBand = getTitle();
  run("Calculator Plus", "i1="+"["+factorRed+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  print("Processed " + getTitle());
  rename("Result1");
  selectWindow("RedIn");
  print("closed " + getTitle());
  close();


  selectWindow("GreenIn");
  toProcessBand = getTitle();
  run("Calculator Plus", "i1="+"["+factorGreen+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  print("Processed " + getTitle());
  rename("Result2");
  selectWindow("GreenIn");
  print("closed " + getTitle());
  close();

  selectWindow("BlueIn");
  toProcessBand = getTitle();
  run("Calculator Plus", "i1="+"["+factorBlue+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  print("Processed " + getTitle());
  rename("Result3");
  selectWindow("BlueIn");
  print("closed " + getTitle());
  close();

  print("Merging channels of " + getTitle());
  run("Merge Channels...", "c1=Result1 c2=Result2 c3=Result3 create");
  print("Finished merging " + getTitle());

  selectWindow("Composite");
  labels = newArray("Red", "Green", "Blue");
  for (k=0; k<3; k++) {
    setSlice(k+1);
    setMetadata("label", labels[k]);
  }
  run("RGB Color");
  print("Saving " + getTitle());
  saveAs("Tiff", path_out + File.nameWithoutExtension + ".tif");
  print("Closing " + getTitle());
  close();

}

// Clean up
run("Close All");
run("Collect Garbage");
call("java.lang.System.gc");
