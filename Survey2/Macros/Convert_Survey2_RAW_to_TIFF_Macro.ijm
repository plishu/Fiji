run("Raw...", "open=/media/nedhorning/684EE5FF4EE5C642/AMNH/PhotoMonitoring/NolanCameras/red+NIR/2015_0202_164730_001.RAW image=[16-bit Unsigned] width=4608 height=3456 offset=0 number=1 gap=0 little-endian");
run("Debayer Image", "order=R-G-R-G demosaicing=Replication radius=2 radius=2");
selectWindow("2015_0202_164730_001.RAW");
close();
selectWindow("RGB Stack");
run("Make Composite", "display=Color");
  labels = newArray("Red", "Green", "Blue"); 
  for (i=0; i<3; i++) { 
     setSlice(i+1); 
     setMetadata("label", labels[i]); 
}
saveAs("Tiff", "/media/nedhorning/684EE5FF4EE5C642/AMNH/PhotoMonitoring/NolanCameras/red+NIR/2015_0202_164730_001.tif");

 
