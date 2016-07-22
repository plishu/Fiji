import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;
import ij.plugin.PlugIn;
import ij.ImagePlus;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;

import java.io.IOException;
import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


public class IndexerMain implements PlugIn{

    public String NDVISINGLE = "NDVI (Single Camera)";
    public String NDVIDUAL = "NDVI (Dual Camera)";

    public final int DEBUGHIGH = 2;
    public final int DEBUGLOW = 1;
    public final int DEBUGOFF = 0;

    public final int debugMode = 2;

    public String indexes[] = {NDVISINGLE, NDVIDUAL};

    private String indexOptionSelected = null;
    private Index index = null;

    private boolean keepAlive = true;

    private String inputDir = null;

    // @TODO maintain transparency layer (4th layer)
    public void run(String arg){
        GenericDialog mainDialog = null;

        // Allow user to process another indice
        while( keepAlive ){
            // Prompt user for indice
            mainDialog = showMainDialog();
            if( mainDialog.wasCanceled() ){
                // Terminate keepAlive loop and go to cleanup stage (after loop)
                keepAlive = false;
                continue;
            }

            // Get which indice to process
            indexOptionSelected = mainDialog.getNextChoice();

            // Select strategy
            if( indexOptionSelected.equals(NDVISINGLE) ){
                index = new NDVIIndex();
            }else if( indexOptionSelected.equals(NDVIDUAL) ){
                index = new NDVIIndex();
            }

            // Get input directory
            // @TODO You will not be able to process a whole directory for NDVI dual cameras
            // In that case, process only 1 pair image at a time (asking user for both images)
            inputDir = showInputDirectoryDialog().getDirectory();
            while( inputDir == null || inputDir == "" ){
                if( showCantOpenDirectory(inputDir).wasCanceled() ){
                    keepAlive = false;
                    return;
                }
                inputDir = showInputDirectoryDialog().getDirectory();
            }

            // Get images to process
            // @TODO fail-safe check
            List<File> images = getImagesToProcess(inputDir);
            Iterator<File> imageIterator = images.iterator();
            if( debugMode == DEBUGHIGH ){
                printDirectoryContents(imageIterator);
            }

            // Rasture calculate
            IJ.log( "I will begin to process the images in the selected directory. Please wait.");
            ImagePlus imageToIndex = null;
            ImagePlus indexedImage = null;
            File nextImage = null;
            imageIterator = images.iterator(); // Reset iterator for next command
            while( imageIterator.hasNext() ){
                nextImage = imageIterator.next();
                IJ.log("Applying index formula to " + nextImage.getAbsolutePath() );
                imageToIndex = new ImagePlus(nextImage.getAbsolutePath());

                if( debugMode == DEBUGHIGH ){
                    imageToIndex.show();
                }

                indexedImage = index.calculateIndex( imageToIndex );

                if( debugMode == DEBUGLOW ){
                    IJ.log("Done calculating index");
                }

                if( debugMode == DEBUGHIGH ){
                    indexedImage.show();
                }
            }


        }


        // Clean up stuff if needed
        IJ.log("Goodbye!");
    }


    public GenericDialog showMainDialog(){
        GenericDialog dialog = new GenericDialog("Create Index Image and Apply LUT");
        dialog.addChoice("Index: ", indexes, indexes[0]);
        dialog.setOKLabel("Begin");
        dialog.setCancelLabel("Quit");
        dialog.showDialog();

        return dialog;
    }

    public OpenDialog showInputDirectoryDialog(){
        OpenDialog dialog = new OpenDialog("Select First Image in Directory of Images You Want to Index Process");
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
            extension = split[1];
            fileToAdd = new File(file);

            if( extension.toUpperCase().equals("JPG") ){
                images.add(fileToAdd);
            }else if( extension.toUpperCase().equals("TIF") ){
                images.add(fileToAdd);
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
