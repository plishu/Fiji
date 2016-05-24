setBatchMode(true);
Dialog.create("Processing choices");
Dialog.addChoice("Enter path for exiftool", newArray("no", "yes"));
Dialog.addChoice("Keep copy of original file", newArray("yes", "no"));
Dialog.addNumber("Enter radius for mean filter", 10);
Dialog.show();
exiftoolPath = Dialog.getChoice();
keepOriginal = Dialog.getChoice();
filterRadius = Dialog.getNumber();

// File open dialog for flat flield image
path = File.openDialog("Select the flat-field image");
// Directory with images to correct
inDirectory = getDirectory("Choose directory with images to correct");
filesToProcess = getFileList(inDirectory)
// Directory for output images
outDirectory = getDirectory("Choose the output image directory");


if (exiftoolPath == "yes") {
	// Location of exiftool
	exiftoolDirectory = getDirectory("Choose the directory for exiftool");
} else {
	exiftoolDirectory="";
}

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

for (i=0; i<filesToProcess.length; i++) {
	run("Open [Image IO]", "image="+ "[" + inDirectory + filesToProcess[i] + "]");
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

	outputFileNameParts = split(filesToProcess[i], ".");
	outputFileName = outputFileNameParts[0] + ".jpg";
	saveAs("jpeg", outDirectory + outputFileName);
	close();
	selectWindow(toProcessImage + " (red)");
	close();
	selectWindow(toProcessImage + " (green)");
	close();
	selectWindow(toProcessImage + " (blue)");
	close();
	osType = getInfo("os.name");
	print(osType);
	if (keepOriginal == "yes") {
		if (startsWith(osType, "Windows")) {
			exec("cmd", "/c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + filesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
		} else {
			exec("sh", "-c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + filesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
		}
	} else {
		if (startsWith(osType, "Windows")) {
			exec("cmd", "/c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + filesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
			//exec("cmd", "/c", exiftoolDirectory + "exiftool -delete_original! "  + outDirectory);
			print("del " + outDirectory + outputFileName + ".jpg_original");
			exec("cmd", "/c", "del " + "\"" + outDirectory + outputFileName + "_original" + "\"");
		} else {
			exec("sh", "-c", exiftoolDirectory + "exiftool -tagsfromfile "  + "\"" + inDirectory + filesToProcess[i] + "\"" + " " + "\"" + outDirectory + outputFileName + "\"");
			exec("sh", "-c", exiftoolDirectory + "exiftool -delete_original! "  + "\"" + outDirectory + "\"");
	}
	}
}
run("Close All");


