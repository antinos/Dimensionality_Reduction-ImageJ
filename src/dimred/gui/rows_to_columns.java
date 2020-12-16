package dimred.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;

public class rows_to_columns implements PlugIn {
	ImagePlus stack;
	ImagePlus resultsImage;
	
	public void run(String arg) {
		
		if (Analyzer.getResultsTable() == null) {
			IJ.showMessage("'Results to Stack' only works on a 'Results' Table containing numeric data only. Try renaming a table to 'Results'.");
			return;
		}
		
		IJ.run("Results to Image", "");
			resultsImage = WindowManager.getImage("Results Table");
			resultsImage.hide();
		IJ.run(resultsImage, "Rotate 90 Degrees Right", "");
		IJ.run(resultsImage, "Flip Horizontally", "");
		IJ.run(resultsImage, "Image to Results", "");
		resultsImage.close();
		//ImageProcessor resultsProcessor = resultsImage.getProcessor();
		//resultsImage.hide();
		
		ResultsTable rt = Analyzer.getResultsTable();
		rt.showRowNumbers(true);
		rt.updateResults();
		String[] headings = rt.getHeadings();
	 	int rows = rt.size();
	 	//Table.create("transposed results");
	 	ResultsTable rt_transposed = new ResultsTable(rows); //api option to instantiate table with set number of row entries... seems redundant, but have adopted incase the memory allocation is helpful
		for (int i=0; i<rows; i++) {
			for (int j=1; j<headings.length; j++) {
				rt_transposed.setValue(headings[j], i, rt.getValue(headings[j], i)); //check data integrity (e.g. numeric precision as double or however it is encoded here)
			}
		}
		rt.reset();
		rt = null;
		Analyzer.setResultsTable(rt_transposed);
		rt_transposed.show("Results");
		rt_transposed.updateResults();

		IJ.log("The results data has been transposed.");
     }
}