args = getArgument();

filterRadius = "";
path = ""; // flat-field path
exiftoolPath = "";
exiftoolDirectory = "";
inDirectory = "";
outDirectory = "";

outputFlatFieldNameParts = ""
outputFlatFieldExtension = "";

tmpstr1 = split(args, " | ");

// Parse argument
for( i=0; i<tmpstr1.length; i++ ){
  tmpstr2 = split(tmpstr1[i], "=");
  if( tmpstr2[0] == "fRadius" ){
    filterRadius = tmpstr2[1];
    filterRadius = parseInt(filterRadius);
  }else if( tmpstr2[0] == "ffPath" ){
    path = tmpstr2[1];
  }else if( tmpstr2[0] == "exifPath"){
    exiftoolPath = tmpstr2[1];
  }else if( tmpstr2[0] == "exifDir"){
    exiftoolDirectory = tmpstr2[1];
  }else if( tmpstr2[0] == "inDir"){
    inDirectory = tmpstr2[1];
  }else if( tmpstr2[0] == "outDir"){
    outDirectory = tmpstr2[1];
  }
}

print(filterRadius);
print(path);
print(exiftoolPath);
print(exiftoolDirectory);
print(inDirectory);
print(outDirectory);


filesToProcess = getFileList(inDirectory);
rawFilesToProcess = newArray();
jpgFilesToProcess = newArray();


// Seperate raw and jpg
for( i=0; i<filesToProcess.length; i++ ){
	inImageParts = split(filesToProcess[i], ".");
	inImageExt = inImageParts[1];

	if( toUpperCase(inImageExt) == "JPG"){
		jpgFilesToProcess = Array.concat(jpgFilesToProcess, filesToProcess[i]);
	}else{
		rawFilesToProcess = Array.concat(rawFilesToProcess, filesToProcess[i]);
	}
}


print("Raw files to process: " + rawFilesToProcess.length);
print("JPG files to process: " + jpgFilesToProcess.length);



if (filterRadius != 0) {
  outputFlatFieldNameParts = split(path, ".");
  outputFlatFieldExtension = outputFlatFieldNameParts[1];
  if (toUpperCase(outputFlatFieldExtension) == "JPG") {
    run("Open [Image IO]", "image="+ "[" + path + "]");
    flatFieldImage = getTitle();
    run("Split Channels");
    selectWindow(flatFieldImage + " (red)");
    run("Mean...", "radius="+filterRadius);
    run("32-bit");
    getRawStatistics(nPixels, mean, min, max, std, histogram);
    run("Macro...", "code=v=" + max + "/v");
    factorRed = getTitle();

    selectWindow(flatFieldImage + " (green)");
    run("Mean...", "radius="+filterRadius);
    run("32-bit");
    getRawStatistics(nPixels, mean, min, max, std, histogram);
    run("Macro...", "code=v=" + max + "/v");
    factorGreen = getTitle();

    selectWindow(flatFieldImage + " (blue)");
    run("Mean...", "radius="+filterRadius);
    run("32-bit");
    getRawStatistics(nPixels, mean, min, max, std, histogram);
    run("Macro...", "code=v=" + max + "/v");
    factorBlue = getTitle();
  }
  if (toUpperCase(outputFlatFieldExtension) == "RAW") {
    run("Raw...", "open="+ "[" + path + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
    rawImage = getTitle();
    run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
    selectWindow(rawImage);
    close();
    selectWindow("RGB Stack");
    run("Make Composite", "display=Color");
      labels = newArray("RedFlat", "GreenFlat", "BlueFlat");
      for (l=0; l<3; l++) {
         setSlice(l+1);
         setMetadata("label", labels[l]);
       }
       run("Stack to Images");
       selectWindow("RedFlat");
    run("Mean...", "radius="+filterRadius);
    run("32-bit");
    getRawStatistics(nPixels, mean, min, max, std, histogram);
    run("Macro...", "code=v=" + max + "/v");
    factorRed = getTitle();

    selectWindow("GreenFlat");
    run("Mean...", "radius="+filterRadius);
    run("32-bit");
    getRawStatistics(nPixels, mean, min, max, std, histogram);
    run("Macro...", "code=v=" + max + "/v");
    factorGreen = getTitle();

    selectWindow("BlueFlat");
    run("Mean...", "radius="+filterRadius);
    run("32-bit");
    getRawStatistics(nPixels, mean, min, max, std, histogram);
    run("Macro...", "code=v=" + max + "/v");
    factorBlue = getTitle();
  }
}



if( rawFilesToProcess.length > 0 ){
  // RAW + JPG case

  for( i=0; i<rawFilesToProcess.length; i++){
    run("Raw...", "open="+ "[" + inDirectory + rawFilesToProcess[i] + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
  	wait(1000);

  	run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
  	wait(5000);

  	selectWindow(rawFilesToProcess[i]);
  	close();
  	selectWindow("RGB Stack");
  	run("Make Composite", "display=Color");

  	if (filterRadius == 0) {
  		labels = newArray("Red", "Green", "Blue");
  	} else {
  		labels = newArray("RedIn", "GreenIn", "BlueIn");
  	}

  	for (j=0; j<3; j++) {
  		setSlice(j+1);
  		setMetadata("label", labels[j]);
  	}

  	outputFileNameParts = split(rawFilesToProcess[i], ".");
  	outputFileName = outputFileNameParts[0] + ".tif";
  	// Just save as Tiff
  	if (filterRadius == 0) {
  		saveAs("Tiff", outDirectory + outputFileName);
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
  		saveAs("Tiff", outDirectory + outputFileName);
  		close();
  	}


    // Transfer EXIF data
  	osType = getInfo("os.name");
  	print(osType);

  	if (startsWith(osType, "Windows")) {
  		exec("cmd", "/c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + jpgFilesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
  		//exec("cmd", "/c", exiftoolDirectory + "exiftool -delete_original! "  + outDirectory);
  		print("del " + outDirectory + outputFileName + ".jpg_original");
  		exec("cmd", "/c", "del " + "\"" + outDirectory + outputFileName + "_original" + "\"");
  	} else {
  		exec("sh", "-c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + jpgFilesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
  		exec("sh", "-c", exiftoolDirectory + "exiftool -delete_original! "  + "\"" + outDirectory + "\"");
    }

  }

}else{
  // JPG case
}

//Array.print(rawFilesToProcess);
//Array.print(jpgFilesToProcess);

for (i=0; i<rawFilesToProcess.length; i++) {
// Use the flat-field
if (filterRadius != 0) {
	outputFlatFieldNameParts = split(path, ".");
	outputFlatFieldExtension = outputFlatFieldNameParts[1];
	if (toUpperCase(outputFlatFieldExtension) == "JPG") {
		run("Open [Image IO]", "image="+ "[" + path + "]");
		flatFieldImage = getTitle();
		run("Split Channels");
		selectWindow(flatFieldImage + " (red)");
		run("Mean...", "radius="+filterRadius);
		run("32-bit");
		getRawStatistics(nPixels, mean, min, max, std, histogram);
		run("Macro...", "code=v=" + max + "/v");
		factorRed = getTitle();

		selectWindow(flatFieldImage + " (green)");
		run("Mean...", "radius="+filterRadius);
		run("32-bit");
		getRawStatistics(nPixels, mean, min, max, std, histogram);
		run("Macro...", "code=v=" + max + "/v");
		factorGreen = getTitle();

		selectWindow(flatFieldImage + " (blue)");
		run("Mean...", "radius="+filterRadius);
		run("32-bit");
		getRawStatistics(nPixels, mean, min, max, std, histogram);
		run("Macro...", "code=v=" + max + "/v");
		factorBlue = getTitle();
	}
	if (toUpperCase(outputFlatFieldExtension) == "RAW") {
		run("Raw...", "open="+ "[" + path + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
		rawImage = getTitle();
		run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
		selectWindow(rawImage);
		close();
		selectWindow("RGB Stack");
		run("Make Composite", "display=Color");
  		labels = newArray("RedFlat", "GreenFlat", "BlueFlat");
  		for (l=0; l<3; l++) {
     		setSlice(l+1);
     		setMetadata("label", labels[l]);
   		}
   		run("Stack to Images");
   		selectWindow("RedFlat");
		run("Mean...", "radius="+filterRadius);
		run("32-bit");
		getRawStatistics(nPixels, mean, min, max, std, histogram);
		run("Macro...", "code=v=" + max + "/v");
		factorRed = getTitle();

		selectWindow("GreenFlat");
		run("Mean...", "radius="+filterRadius);
		run("32-bit");
		getRawStatistics(nPixels, mean, min, max, std, histogram);
		run("Macro...", "code=v=" + max + "/v");
		factorGreen = getTitle();

		selectWindow("BlueFlat");
		run("Mean...", "radius="+filterRadius);
		run("32-bit");
		getRawStatistics(nPixels, mean, min, max, std, histogram);
		run("Macro...", "code=v=" + max + "/v");
		factorBlue = getTitle();
	}
}

// Process each image in directory
//for (i=0; i<rawFilesToProcess.length; i++) {

	// Import RAW file image
	run("Raw...", "open="+ "[" + inDirectory + rawFilesToProcess[i] + "]" + "image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
	wait(1000);

/*
	while( getTitle() != rawFilesToProcess[i]){
		print(getTitle());
		print("-->" + rawFilesToProcess[1]);
		wait(1000);
	}
*/

	run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
	wait(5000);
	/*
	while( getTitle() != "RGB Stack"){
		print("~~ "+getTitle());
		wait(1000);
	}*/


	selectWindow(rawFilesToProcess[i]);
	close();
	selectWindow("RGB Stack");
	run("Make Composite", "display=Color");

	if (filterRadius == 0) {
		labels = newArray("Red", "Green", "Blue");
	} else {
		labels = newArray("RedIn", "GreenIn", "BlueIn");
	}

	for (j=0; j<3; j++) {
		setSlice(j+1);
		setMetadata("label", labels[j]);
	}

	outputFileNameParts = split(rawFilesToProcess[i], ".");
	outputFileName = outputFileNameParts[0] + ".tif";
	// Just save as Tiff
	if (filterRadius == 0) {
		saveAs("Tiff", outDirectory + outputFileName);
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
		saveAs("Tiff", outDirectory + outputFileName);
		close();
	}

/*
	osType = getInfo("os.name");
	print(osType);

	if (startsWith(osType, "Windows")) {
		exec("cmd", "/c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + filesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
		//exec("cmd", "/c", exiftoolDirectory + "exiftool -delete_original! "  + outDirectory);
		print("del " + outDirectory + outputFileName + ".jpg_original");
		exec("cmd", "/c", "del " + "\"" + outDirectory + outputFileName + "_original" + "\"");
	} else {
		exec("sh", "-c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + filesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
		exec("sh", "-c", exiftoolDirectory + "exiftool -delete_original! "  + "\"" + outDirectory + "\"");
}
*/
	run("Close All");
	run("Collect Garbage");
	call("java.lang.System.gc");
	wait(1000);
	print("Finished processing " + rawFilesToProcess[i] + " [" + i + "]");
}
run("Close All");
print("Done");
