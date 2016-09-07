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

if( isOpen("Log") ){
    selectWindow("Log");
    run("Close");
}
run("DCRaw Reader...", "open=["+path_raw+"] white_balance=None output_colorspace=raw read_as=8-bit interpolation=[High-speed, low-quality bilinear] show_metadata");
//print("Saving output to " + path_out+"\\MetaData.txt");
if( isOpen("Log") ){
    selectWindow("Log");
    saveAs("Text", path_out + "\\MetaData.txt");
    //run("Close");
}
