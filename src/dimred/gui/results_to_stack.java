package dimred.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;

public class results_to_stack implements PlugIn {
	ImagePlus stack;
	ImagePlus resultsImage;
	
	public void run(String arg) {
		
		if (Analyzer.getResultsTable() == null) {
			IJ.showMessage("'Results to Stack' only works on a 'Results' Table containing numeric data only. Try renaming a table to 'Results'.");
			return;
		}
		
		IJ.run("Results to Image", "");
			resultsImage = WindowManager.getImage("Results Table");
		IJ.run(resultsImage, "Rotate 90 Degrees Right", "");
		ImageProcessor resultsProcessor = resultsImage.getProcessor();
		resultsImage.hide();
		ResultsTable rt = Analyzer.getResultsTable();
		int nResults = rt.getCounter();
			IJ.log("nResults = "+nResults);
		String[] headingsArray = rt.getHeadings();
			IJ.log("headingsArray length = "+headingsArray.length);
		
		stack = IJ.createImage("stack", "32-bit black", nResults, 1, headingsArray.length);
		stack.hide();
		ImageProcessor stackProcessor = stack.getProcessor();
		
		for (int j = 0; j < headingsArray.length; j++) {
			stack.setSlice(j+1);
			stackProcessor = stack.getProcessor();
			for (int i = 0; i < nResults; i++) {
				double pix = resultsProcessor.getInterpolatedValue(i, j);
				stackProcessor.putPixelValue(i, 0, (pix));
			}
			if (IJ.escapePressed()){
				return;
			}
		}
		
		resultsImage.close();
		IJ.run(stack, "Rotate 90 Degrees Left", "");
		IJ.run(stack, "Rotate 90 Degrees Left", "");
		stack.show();
			IJ.log("Finished.");
     }
}