setBatchMode(false);
Dialog.create("Copy Exif Meta-Data from JPG to TIFF");
Dialog.addChoice("Enter path for exiftool", newArray("yes", "no"));
Dialog.addChoice("Delete original TIFF images after copying Exif meta-data?", newArray("no","yes"));
Dialog.show();
exiftoolPath = Dialog.getChoice();
deleteOriginal = Dialog.getChoice();


if (exiftoolPath == "yes") {
	// Location of exiftool
	exiftoolDirectory = getDirectory("Choose the directory for exiftool");
} else {
	exiftoolDirectory="";
}


inDirectory = getDirectory("Choose directory with JPG images");
filesToProcess = getFileList(inDirectory);

// Directory for output images
outDirectory = getDirectory("Choose directory with TIFF images");
outputFileNames = getFileList(outDirectory);


rawFilesToProcess = newArray();
jpgFilesToProcess = newArray();
// Seperate raw and jpg
for( i=0; i<filesToProcess.length; i++ ){
	inImageParts = split(filesToProcess[i], ".");
	inImageExt = inImageParts[1];
	//print(inImageExt);
	if( toUpperCase(inImageExt) == "JPG"){
		//print("Adding JPG to JPG Array");
		jpgFilesToProcess = Array.concat(jpgFilesToProcess, filesToProcess[i]);
	}else{
		//print("Adding RAW to RAW Array");
		rawFilesToProcess = Array.concat(rawFilesToProcess, filesToProcess[i]);
	}
}

//Array.print(rawFilesToProcess);
//Array.print(jpgFilesToProcess);


osType = getInfo("os.name");
for( i=0; i<jpgFilesToProcess.length; i++ ){
	if (startsWith(osType, "Windows")) {
		exec("cmd", "/c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + jpgFilesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileNames[i] + "\"");
		//exec("cmd", "/c", exiftoolDirectory + "exiftool -delete_original! "  + outDirectory);
		if( deleteOriginal == "yes" ){
			print("del " + outDirectory + outputFileNames[i] + ".tif_original");
			exec("cmd", "/c", "del " + "\"" + outDirectory + outputFileNames[i] + "_original" + "\"");
		}
	} else {
		exec("sh", "-c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + jpgFilesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileNames[i] + "\"");
		if( deleteOriginal == "yes" ){
				exec("sh", "-c", exiftoolDirectory + "exiftool -delete_original! "  + "\"" + outDirectory + "\"");
		}
  }
}
print("Done.");
