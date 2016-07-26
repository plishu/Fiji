import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;
import ij.plugin.PlugIn;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.LutLoader;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.image.IndexColorModel;

import java.io.IOException;
import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;


public class IndexerMain implements PlugIn{

    public String NDVISINGLE = "NDVI (Single Camera)";
    public String NDVIDUAL = "NDVI (Dual Camera)";
    public String SAVEFOLDER = "Index";
    public String PATHTOLUTS = IJ.getDirectory("luts");

    public final int DEBUGHIGH = 2;
    public final int DEBUGLOW = 1;
    public final int DEBUGOFF = 0;

    public final int debugMode = 0;

    public String indexes[] = {NDVISINGLE, NDVIDUAL};

    private String indexOptionSelected = null;
    private Index index = null;

    private boolean keepAlive = true;
    private boolean saveIndex = false;

    private String inputDir = null;
    private String inputVisDir = null;
    private String inputNirDir = null;
    private String[] luts = null;

    private final String VERSION = "1.3.0";

    // @TODO maintain transparency layer (4th layer)
    public void run(String arg){
        IJ.log("Build: " + VERSION);
        GenericDialog mainDialog = null;
        luts = getLutNames(PATHTOLUTS);
        String lutChoice = null;
        HashMap<String, ImagePlus> imageMap = null;

        // Allow user to process another indice
        while( keepAlive ){
            // Prompt user for indice
            mainDialog = showMainDialog(luts);
            if( mainDialog.wasCanceled() ){
                // Terminate keepAlive loop and go to cleanup stage (after loop)
                keepAlive = false;
                continue;
            }

            // Get which indice to process
            indexOptionSelected = mainDialog.getNextChoice();
            saveIndex = mainDialog.getNextBoolean();
            lutChoice = mainDialog.getNextChoice();


            // Get images to process
            // @TODO fail-safe check
            List<File> images = null;

            if( indexOptionSelected.equals(NDVISINGLE) ){
                index = new NDVIIndex();
            }else if( indexOptionSelected.equals(NDVIDUAL) ){
                index = new NDVIIndex();
            }

            // Select strategy
            if( indexOptionSelected.equals(NDVISINGLE) ){
                inputDir = showInputDirectoryDialog("The First Image").getDirectory();
                while( inputDir == null || inputDir == "" ){
                    if( showCantOpenDirectory(inputDir).wasCanceled() ){
                        keepAlive = false;
                        return;
                    }
                    inputDir = showInputDirectoryDialog("The First Image").getDirectory();
                }
                images = getImagesToProcess(inputDir);

            }else if( indexOptionSelected.equals(NDVIDUAL) ){
                inputVisDir = showInputDirectoryDialog("The Visible Light Captured Image").getPath();
                while( inputVisDir == null || inputVisDir == "" ){
                    if( showCantOpenDirectory(inputVisDir).wasCanceled() ){
                        keepAlive = false;
                        return;
                    }
                    inputVisDir = showInputDirectoryDialog("The Visible Light Captured Image").getPath();
                }

                inputNirDir = showInputDirectoryDialog("The NIR Captured Image").getPath();
                while( inputNirDir == null || inputNirDir == "" ){
                    if( showCantOpenDirectory(inputNirDir).wasCanceled() ){
                        keepAlive = false;
                        return;
                    }
                    inputNirDir = showInputDirectoryDialog("The NIR Captured Image").getPath();
                }
                inputDir = showSaveDirectoryDialog("Where Do You Want To Save The Output Image?").getDirectory();
                images = new ArrayList<File>();
                // Dumb fix so that one iteration of processing occurs
                images.add(new File(inputNirDir));
            }

            Iterator<File> imageIterator = images.iterator();
            if( debugMode == DEBUGHIGH ){
                printDirectoryContents(imageIterator);
            }
            // Rasture calculate
            IJ.log( "I will begin to process the images in the selected directory. Please wait.");
            ImagePlus imageToIndex = null;
            ImagePlus indexedImage = null;
            ImagePlus lutImage = null;
            File nextImage = null;
            imageIterator = images.iterator(); // Reset iterator for next command
            while( imageIterator.hasNext() ){
                nextImage = imageIterator.next();
                IJ.log("Applying index formula to " + nextImage.getAbsolutePath() );
                imageToIndex = new ImagePlus(nextImage.getAbsolutePath());

                if( debugMode == DEBUGHIGH ){
                    imageToIndex.show();
                }


                // Put images in map as needed for index selected
                if( indexOptionSelected.equals(NDVISINGLE) ){
                    indexedImage = index.calculateIndex( imageToIndex );
                }else if( indexOptionSelected.equals(NDVIDUAL) ){
                    imageMap = new HashMap<String, ImagePlus>();
                    imageMap.put("visImg", new ImagePlus(inputVisDir));
                    imageMap.put("nirImg", new ImagePlus(inputNirDir));
                    indexedImage = index.calculateIndex( imageMap );
                }

                if( debugMode == DEBUGLOW ){
                    IJ.log("Done calculating index");
                }

                if( debugMode == DEBUGHIGH ){
                    indexedImage.show();
                }

                // Save indexed image
                IJ.log("Saving image");
                if( saveIndex ){
                    saveIndexImageToDir(inputDir, nextImage.getName(), index.getIndexType(), indexedImage);
                }

                // Apply lut
                IJ.log("Path to lut: " + PATHTOLUTS+lutChoice);
                lutImage = applyLut(indexedImage, PATHTOLUTS+lutChoice);
                saveLutImageToDir(inputDir, nextImage.getName(), index.getIndexType(), lutImage);
            }

            if( showFinishedDialog().wasCanceled() ){
                keepAlive = false;
            }

        }


        // Clean up stuff if needed
        IJ.log("Goodbye!");
    }

    public ImagePlus applyLut(ImagePlus indexImg, String pathToLut){
        ImagePlus lutImg = NewImage.createByteImage("Lut", indexImg.getWidth(), indexImg.getHeight(), 1, 1);

        int x = 0;
        int y = 0;

        double lutMax = 1.0;
        double lutMin = -1.0;

        double pixel = 0.0;
        IndexColorModel lutModel = null;

        while( y < indexImg.getHeight() ){
            x = 0;
            while( x < indexImg.getWidth() ){
                pixel = (double)indexImg.getProcessor().getPixelValue(x,y);

                // Scale image to [0,1] for LUT

                //pixel = (lutMax - lutMin)/255.0*pixel + lutMin;
                //pixel = (double)Math.round( (pixel - lutMin)/(lutMax - lutMin)/255.0 );
                pixel = (pixel - lutMin)/(lutMax - lutMin)*255.0;
                lutImg.getProcessor().putPixelValue(x, y, pixel);

                x++;
            }
            y++;
        }

        try {
            lutModel = LutLoader.open( pathToLut );
        }catch (IOException e) {
            //IJ.error((String)((Object)e));
            IJ.log("Could not open LUT file");
            e.printStackTrace();
            return null;
        }

        LUT lut = new LUT(lutModel, 255.0, 0.0);
        lutImg.getProcessor().setLut(lut);
        return lutImg;
    }

    public String[] getLutNames(String pathToLutDir){
        File lutDir = new File( pathToLutDir );
        return lutDir.list();
    }

    public void saveLutImageToDir(String saveDir, String filename, String indexType, ImagePlus image){
        String folderName = "Index_Lut\\";
        // If output directory does not exist, create it


        String[] split = splitNameAndExt(filename);
        String outStr = saveDir+folderName+split[1].toUpperCase()+"\\";
        File outDir = new File( outStr );

        if( !outDir.exists() ){
            (new File(saveDir+folderName)).mkdir();
            outDir.mkdir();
        }

        // Replace Calibration in filename to Index
        filename = filename.replaceAll("Calibrated", "");

        filename = split[0] + "_" + indexType + "_Lut." + split[1];

        IJ.log("Saving to: " + outStr);

        IJ.save( (ImagePlus)image, outStr + filename );
        return;
    }

    public void saveIndexImageToDir(String saveDir, String filename, String indexType, ImagePlus image){
        String folderName = "Index_Lut\\";
        // If output directory does not exist, create it


        String[] split = splitNameAndExt(filename);
        String outStr = saveDir+folderName+split[1].toUpperCase()+"\\";
        File outDir = new File( outStr );

        if( !outDir.exists() ){
            (new File(saveDir+folderName)).mkdir();
            outDir.mkdir();
        }

        // Replace Calibration in filename to Index
        filename = filename.replaceAll("Calibrated", "");

        filename = split[0] + "_" + indexType + "_Index." + split[1];

        IJ.log("Saving to: " + outStr);

        IJ.save( (ImagePlus)image, outStr + filename );
        return;
    }


    public GenericDialog showMainDialog(String[] luts){
        GenericDialog dialog = new GenericDialog("Create Index Image and Apply LUT");
        dialog.addChoice("Index: ", indexes, indexes[0]);
        dialog.addCheckbox("Save index image", false);
        dialog.addChoice("Luts: ", luts, luts[0] );
        dialog.setOKLabel("Begin");
        dialog.setCancelLabel("Quit");
        dialog.showDialog();

        return dialog;
    }

    public SaveDialog showSaveDirectoryDialog( String ctext ){
        SaveDialog dialog = new SaveDialog(ctext, "image", "tif");
        return dialog;
    }

    public OpenDialog showInputDirectoryDialog( String ctext ){
        OpenDialog dialog = new OpenDialog("Select " + ctext + " You Want to Process");
        return dialog;
    }

    public GenericDialog showCantOpenDirectory(String pathToOpen){
        GenericDialog dialog = new GenericDialog("Attention!");
        dialog.addMessage("Cannot open directory: " + pathToOpen + ". It might not exists or not be a valid directory.");
        dialog.setOKLabel("Choose another directory");
        dialog.setCancelLabel("Quit");
        dialog.showDialog();

        return dialog;
    }

    public GenericDialog showFinishedDialog(){
        GenericDialog dialog = new GenericDialog("Indexing Finished!");
        dialog.addMessage("The images have been processed");
        dialog.setOKLabel("Process more images");
        dialog.setCancelLabel("Quit");
        dialog.showDialog();

        return dialog;
    }

    // Both jpg and raw/dng
    public List<File> getImagesToProcess(String inDir){

        String[] allFiles = (new File(inDir)).list();
        // Fix pathname???
        for( int i=0; i<allFiles.length; i++ ){
            allFiles[i] = inDir + allFiles[i];
        }

        String file = null;

        String[] split = null;
        String filename = null;
        String extension = null;

        File fileToAdd = null;

        List<File> images = new ArrayList<File>();

        for( int i=0; i<allFiles.length; i++ ){
            file = allFiles[i];
            // Get only JPG, TIF ext
            split = splitNameAndExt(file);

            if( split != null ){
                extension = split[1];
                fileToAdd = new File(file);

                if( extension.toUpperCase().equals("JPG") ){
                    images.add(fileToAdd);
                }else if( extension.toUpperCase().equals("TIF") ){
                    images.add(fileToAdd);
                }
            }
        }

        return images;
    }

    public String[] splitNameAndExt(String filename){
        String[] split = filename.split("\\.(?=[^\\.]+$)");

        if( split.length != 2 ){
            // No extension found or something weird happened
            return null;
        }
        // split[0] = filename
        // split[1] = extension
        return split;
    }

    public void printDirectoryContents(Iterator<File> it){
        while( it.hasNext() ){
            IJ.log(it.next().getName());
        }
        return;
    }
}
