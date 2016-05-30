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
  run("Raw...", "open="+ "[" + path_ff + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");

  rawImage = getTitle();
  run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
  selectWindow(rawImage);
  close();
  wait(500);


  selectWindow("RGB Stack");
  run("Make Composite", "display=Color");
  labels = newArray("RedFlat", "GreenFlat", "BlueFlat");
  for (l=0; l<3; l++) {
    setSlice(l+1);
    setMetadata("label", labels[l]);
  }

  run("Stack to Images");
  selectWindow("RedFlat");
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");
  factorRed = getTitle();

  selectWindow("GreenFlat");
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");
  factorGreen = getTitle();

  selectWindow("BlueFlat");
  run("Mean...", "radius="+filter_radius);
  run("32-bit");
  getRawStatistics(nPixels, mean, min, max, std, histogram);
  run("Macro...", "code=v=" + max + "/v");
  factorBlue = getTitle();

}

// Process RAW image
run("Raw...", "open="+ "[" +  path_raw + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
rawimgfname = getInfo("image.filename");

run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
selectWindow(rawimgfname);
close();

selectWindow("RGB Stack");
run("Make Composite", "display=Color");

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
  saveAs("Tiff", path_out + File.nameWithoutExtension + ".tif");
  close();
} else {
  run("Stack to Images");
  selectWindow("RedIn");
  toProcessBand = getTitle();
  run("Calculator Plus", "i1="+"["+factorRed+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  rename("Result1");
  selectWindow("RedIn");
  close();

  selectWindow("GreenIn");
  toProcessBand = getTitle();
  run("Calculator Plus", "i1="+"["+factorGreen+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  rename("Result2");
  selectWindow("GreenIn");
  close();

  selectWindow("BlueIn");
  toProcessBand = getTitle();
  run("Calculator Plus", "i1="+"["+factorBlue+"]"+" i2="+"["+toProcessBand+"]"+" operation=[Multiply: i2 = (i1*i2) x k1 + k2] k1=1 k2=0 create");
  rename("Result3");
  selectWindow("BlueIn");
  close();

  run("Merge Channels...", "c1=Result1 c2=Result2 c3=Result3 create");

  selectWindow("Composite");
  labels = newArray("Red", "Green", "Blue");
  for (k=0; k<3; k++) {
    setSlice(k+1);
    setMetadata("label", labels[k]);
  }
  saveAs("Tiff", path_out + File.nameWithoutExtension + ".tif");
  close();

}

// Clean up
run("Close All");
run("Collect Garbage");
call("java.lang.System.gc");
