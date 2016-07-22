import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.TextField;

import java.io.IOException;


public class IndexerMain implements PlugIn{

    public String NDVISINGLE = "NDVI (Single Camera)";
    public String NDVIDUAL = "NDVI (Dual Camera)";

    public String indexes[] = {NDVISINGLE, NDVIDUAL};

    private String indexOptionSelected = null;
    private Index index = null;

    private boolean keepAlive = true;

    public void run(String arg){
        GenericDialog mainDialog = null;
        while( keepAlive ){

            mainDialog = showMainDialog();
            if( mainDialog.wasCanceled() ){
                keepAlive = false;
                continue;
            }

            indexOptionSelected = mainDialog.getNextChoice();

            if( indexOptionSelected.equals(NDVISINGLE) ){
                index = new NDVIIndex();
            }else if( indexOptionSelected.equals(NDVIDUAL) ){
                index = new NDVIIndex();
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
}
