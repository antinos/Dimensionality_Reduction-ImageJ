package plot.plot;
import dimred.Umap_;
import dimred.Tsne_;
import dimred.Pca_;
import ij.IJ;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SaveFxPlot implements PlugIn {
	//Top level initialisation
	int choice; //JOption 'process the image stack?' choice outcome
	String[] labelsArray;					//required
	private String[] uniqueArray;			//required
	static double[] Xarray;					//required
    static double[] Yarray;					//required
	private static Color[] groupColours;	//required
	private static int[][] lookupArray;		//required
	private static String xTitle;			//required
	private static String yTitle;			//required
	private static String plotTitle;		//required
	private static int stackSize;			//required
	
	public void run(String arg) {
		
		int plotExists = 1;
		ArrayList<Object> fxObjects = new ArrayList<Object>(10);
		
		if (Pca_.Pcomp1 != null) {
			//fxPanelRef = Pca_.fxPanelRef;
			labelsArray = Pca_.labelsArray;
			uniqueArray = Pca_.uniqueArray;
			Xarray = Pca_.Pcomp1;
			Yarray = Pca_.Pcomp2;
			groupColours = Pca_.groupColours;
			lookupArray = Pca_.lookupArray;
			xTitle = Pca_.xTitle;
			yTitle = Pca_.yTitle;
			plotTitle = Pca_.plotTitle;
			stackSize = Pca_.stack.getStackSize();
		} else if (Tsne_.Xarray != null) {
			//fxPanelRef = Tsne_.fxPanelRef;
			labelsArray = Tsne_.labelsArray;
			uniqueArray = Tsne_.uniqueArray;
			Xarray = Tsne_.Xarray;
			Yarray = Tsne_.Yarray;
			groupColours = Tsne_.groupColours;
			lookupArray = Tsne_.lookupArray;
			xTitle = Tsne_.xTitle;
			yTitle = Tsne_.yTitle;
			plotTitle = Tsne_.plotTitle;
			stackSize = Tsne_.stack.getStackSize();
		} else if (Umap_.Xarray != null) {
			//fxPanelRef = Umap_.fxPanelRef;
			labelsArray = Umap_.labelsArray;
			uniqueArray = Umap_.uniqueArray;
			Xarray = Umap_.Xarray;
			Yarray = Umap_.Yarray;
			groupColours = Umap_.groupColours;
			lookupArray = Umap_.lookupArray;
			xTitle = Umap_.xTitle;
			yTitle = Umap_.yTitle;
			plotTitle = Umap_.plotTitle;
			stackSize = Umap_.stack.getStackSize();
		} else {
			plotExists = 0;
		}
		fxObjects.add(labelsArray);
		fxObjects.add(uniqueArray);
		fxObjects.add(Xarray);
		fxObjects.add(Yarray);
		fxObjects.add(groupColours);
		fxObjects.add(lookupArray);
		fxObjects.add(xTitle);
		fxObjects.add(yTitle);
		fxObjects.add(plotTitle);
		fxObjects.add(stackSize);
		
		if (plotExists == 1) {
	        JFileChooser fileChooser = new JFileChooser("Save plot");
	        FileNameExtensionFilter filter = new FileNameExtensionFilter("PLOT files", "plot");
	        fileChooser.setFileFilter(filter);
	        fileChooser.setSelectedFile(new File("myPlot.plot"));
	        int returnVal = fileChooser.showSaveDialog(null);
	        if(returnVal == JFileChooser.APPROVE_OPTION) {
	        	File file = fileChooser.getSelectedFile();
		        if (file.getName().endsWith(".plot")) {
					try {
						FileOutputStream f = new FileOutputStream(file);
						ObjectOutputStream o = new ObjectOutputStream(f);
						
						o.writeObject(fxObjects);
						
						o.close();
						f.close();
						
					} catch (IOException e) {
						IJ.log("Error initializing stream");
						IJ.handleException(e);
					}
		       } else {
		    	   IJ.log("The plot file was not saved, as the .plot suffix was not added.");
		       }
	        } else {
	        	return;
	        }
		} else {
			IJ.log("A dimenionality reduction plot must be open to save.");
		}
	}
	
}