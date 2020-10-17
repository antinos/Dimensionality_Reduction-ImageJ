package dimred;

import ij.*;
import ij.gui.Plot;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import com.jujutsu.utils.MatrixUtils;
import tagbio.umap.Umap;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;

/**version 1.0.0*/

/**
 * uMAP java implementation from https://github.com/tag-bio/umap-java
 * Process an image-stack, pass a directory of images to the plugin, or choose one when a dialog opens.
 */
public class Umap_ implements PlugIn {
	//Top level initialisation
	double[][] imageMatrix;
	String[] Filelist;
	String[] labelsArray;
	private boolean processingStack = false;
	
    // Options to use during the run. Defaults for some but otherwise populated when parseOptions() is called.
    private String inputFolderPath;
    private String inputIndexPath; //show the plugin where sample labels are located.. used to colour the final output.
    private String nNeighbours;
    	private int neighbours = 15;
    private String nThreads;
    	private int threads = 1;
	private String metric;
    int availableThreads = Runtime.getRuntime().availableProcessors(); //Get available threads so that we can default to 1 if the user selects more.
    private boolean outputCSV = false; //create a CSV copy of the tSNE 2D output on the users desktop.
    boolean cancelled; //JOption cancel flag.
    
    // Variables and main() method for testing in IDEs.
    private static String debugOptions = null;
    private static double[][] debugArray = null;
    public static void main(String[] args) {
        // To debug, set a CSV file to open, we'll use that in the ResultsTable for the run.
        debugArray = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
        new Umap_().run("");
    }
    

    public void run(String arg) {
        // Fill in the options we'll use during this run.
        parseOptions();
        processingStack = false;
        
        if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack()) {
        	int type = WindowManager.getCurrentImage().getType();
            int choice = JOptionPane.showConfirmDialog(null, "Do you want to process the image stack?", "UMAP option",JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION) {return;}
            else if (choice == JOptionPane.OK_OPTION) {
            	processingStack = true;
            	ImagePlus stack = WindowManager.getCurrentImage();
            	Filelist = new String[stack.getStackSize()];
            	int height = stack.getHeight();
            	int width = stack.getWidth();
            	imageMatrix = new double[stack.getStackSize()][width*height]; //2D array to hold all images in stack [slice number][pixels as 1D array]
            	for (int s = 0; s < stack.getStackSize(); s++) {
            		Filelist[s] = Integer.toString(s+1);
            		stack.setSlice(s+1);
            		ImageProcessor sip = stack.getProcessor();  	
                	float[] image1DArray = new float[width*height];	//try double, then float
                	for (int j = 0; j < height; j++) {
                		for (int k = 0; k < width; k++) {
                			if (type == ImagePlus.COLOR_RGB) {
                				image1DArray[k+(j*width)] = sip.get(k, j);
                			} else {
                				image1DArray[k+(j*width)] = (float) sip.getInterpolatedValue(k,j); //not sure if casting is appropriate here
                			}
                		}
                	}
                	for(int l = 0; l < (width*height); l++) {
                		imageMatrix[s][l] = image1DArray[l];
                	}
            	}
            	//stack.close();
            	stack.setSlice(0);
            	stack.updateAndDraw();
            } else {
            	DirectoryChooser dc = new DirectoryChooser("Select a folder");
                inputFolderPath = dc.getDirectory();
                if (inputFolderPath == null) {
                	return;
                }
            }
        }
        //specify a folder to perform uMAP on
        else if (inputFolderPath.isEmpty() && debugArray == null) {
        DirectoryChooser dc = new DirectoryChooser("Select a folder of images to process");
        inputFolderPath = dc.getDirectory();
	        if (inputFolderPath == null) {
	        	return;
	        }
    	}
    	
    	if (debugArray != null) {
    		Filelist = new String[2500]; //Dirty Filelist creation for debugging
    	} else if (!inputFolderPath.isEmpty()) {
    		//Generate an array containing the file names to process, also allowing us to count them. NOTE: order is not ensured, without sorting.
    		Filelist = (new File(inputFolderPath)).list();
    		// Attempt to sort the Filelist numerically.. Arrays.sort sorts by ASCII values, which results in a strange order.
    		Arrays.sort(Filelist, new Comparator<String>() {
    			public int compare(String s1, String s2) {
    				try {
    		            int i1 = Integer.parseInt((s1).replaceAll("[^0-9]", "")); //Some chance for NumberFormatExceptions using this Regex.
    		            int i2 = Integer.parseInt((s2).replaceAll("[^0-9]", ""));
    		            return i1 - i2;
    		        } catch(NumberFormatException e) {
    		            throw new AssertionError(e);
    				}
    			}
    		});
    		//IJ.log("File list: "+Arrays.toString(Filelist));
    			
    		if (Filelist==null) {
    			IJ.log("The specified directory does not contain images to process.");
    			return;
    		}
    	}
		
    if (debugArray == null && !processingStack) {
		//Open the first image in the folder to get dimensions.. could have done it in the loop
		ImagePlus imp0 = IJ.openImage(inputFolderPath + Filelist[0]);
    	int width = imp0.getWidth();
    	int height = imp0.getHeight();
    	imp0.close();
		
		imageMatrix = new double[Filelist.length][width*height];
		IJ.log("Number of images = "+imageMatrix.length);			//300
		IJ.log("Dimensions (pixels) = "+imageMatrix[0].length);	//900

        for (int i = 0; i < Filelist.length; i++) {
        	if (IJ.escapePressed()) break;
			String path = inputFolderPath + Filelist[i];
        	
        	ImagePlus imp = IJ.openImage(path);
        	if (imp==null) {
				IJ.log("openImage() returned null: "+path);
				continue;
			}
        	
        	int type= imp.getType();
        	ImageProcessor ip = imp.getProcessor();
        	
        	float[] image1DArray = new float[width*height];
        	
        	for (int j = 0; j < height; j++) {
        		for (int k = 0; k < width; k++) {
        			if (type == ImagePlus.COLOR_RGB) {
        				image1DArray[k+(j*width)] = ip.get(k, j);
        			} else {
        				image1DArray[k+(j*width)] = (float) ip.getInterpolatedValue(k,j); //not sure if casting is appropriate here
        			}
        		}
        	}
        	imp.close();
        	
        	for(int l = 0; l < (width*height); l++) {
        		imageMatrix[i][l] = image1DArray[l];
        	}
        	
        }
    }
        
    	//UMAP here
		if (nNeighbours != null && !("").equals(nNeighbours) && !nNeighbours.matches(".*[A-Za-z].*") && Double.valueOf(nNeighbours) > 0) {
			neighbours = Integer.valueOf(nNeighbours);
		}
		if (nThreads != null && !("").equals(nThreads) && !nThreads.matches(".*[A-Za-z].*") && Double.valueOf(nThreads) > 0) {
			if (Double.valueOf(nThreads) > availableThreads) {
				IJ.log("More CPU threads specified than available for use. The default of 1 has been set.");
			} else {
			threads = Integer.valueOf(nThreads);
			}
		}
        final Umap umap = new Umap();
        umap.setNumberComponents(2); // number of dimensions in result
        umap.setNumberNearestNeighbours(neighbours); //Larger values result in more global views of the manifold, while smaller values result in more local data being preserved. In general values should be in the range 2 to 100. The default is 15.
        umap.setThreads(threads); // use > 1 to enable parallelism. Over 1 will prevent a deterministic result.
        umap.setMetric(metric);
        final double[][] Y = umap.fitTransform(imageMatrix);
        
        //Get and use labels if a .csv file is specified.
        if (!inputIndexPath.isEmpty()) {
        	labelsArray = null;
        	try {
				labelsArray = getLabels(inputIndexPath);
			} catch (IOException e) {
				IJ.handleException(e);
			}
        	if (labelsArray == null) {
        		IJ.log("The specified labels file is empty or corrupt.");
        	}
        	if (labelsArray.length != Y.length) {
        		IJ.log("The number of labels in the labels file does not match the number of processed images, so it has not been used to assign groupings.");
        		IJ.log("Y.length = "+String.valueOf(Y.length)+"; Label number = "+String.valueOf(labelsArray.length));
        		IJ.log(Arrays.toString(labelsArray));
        		labelsArray = null;
        	}
        }
        
        
    	//If the outputCSV toggle is set, output the UMAP result to the users desktop
        if (outputCSV) {
        	try {
        		toCSV(Y, Filelist);
        	} catch (IOException e) {
        		IJ.handleException(e);
        	}
        }
        
        double[] Xarray = new double[Y.length];
        double[] Yarray = new double[Y.length];
        for (int m = 0; m < Y[0].length; m++) { //columns
        	for (int n = 0; n < Y.length; n++) { //rows
        		if (m==0) {
        			Xarray[n] = Y[n][m];
        		} else {
        			Yarray[n] = Y[n][m];
        		}
        	}
        }
        String plotTitle = new String("UMAP output (nNeighbours = "+String.valueOf(neighbours)+"; metric = "+metric+")");
    	Plot scatter = new Plot(plotTitle, "UMAP_1", "UMAP_2");
    	if (labelsArray == null) {
    		scatter.setLineWidth(5);
    		scatter.addPoints(Xarray, Yarray, 6);
    		scatter.show();
    	} else {
    		//some method to count unique entries (groups) in the labels array here. I expect processing overhead time-loss associated with this 'else' code.
    		ArrayList<String> uniqueArrayList = new ArrayList<>();
    	    for(int i=0; i<labelsArray.length; i++){
    	        if(!uniqueArrayList.contains(labelsArray[i])){
    	            uniqueArrayList.add(labelsArray[i]);
    	        }
    	    }
    		int groupN = uniqueArrayList.size();
    		String[] uniqueArray = new String[groupN];
    		uniqueArrayList.toArray(uniqueArray);
    		
    		for (int y = 0; y < groupN; y++) {
    			int counter = 0;
    			for (int z = 0; z < labelsArray.length; z++) {
    				if (labelsArray[z].equals(uniqueArray[y])) {
    					counter++;
    				}
    			}
    				//IJ.log(uniqueArray[y]+" is present "+String.valueOf(counter)+" time(s) in the labelsArray.");
    			double[] XXarray = new double[counter];
    			double[] YYarray = new double[counter];
    			counter = 0;
    			for (int z = 0; z < labelsArray.length; z++) {
    				if (labelsArray[z].equals(uniqueArray[y])) {
    					XXarray[counter] = Xarray[z];
    					YYarray[counter] = Yarray[z];
    					counter++;
    				}
    			}
        		//Randomly generate an rgb colour and add the datapoint of that colour to the plot.
    			Random rand = new Random();
    			int r = rand.nextInt(230);
    			int g = rand.nextInt(230);
    			int b = rand.nextInt(230);
    			Color ranCol = new Color(r,g,b);
    			/* some explict colour assignment for dirty debugging.
    			if (y == 0) {
    				scatter.setColor(Color.BLACK);
    			/} else {
    				scatter.setColor(Color.RED);
    			}
    			*/
    			scatter.setColor(ranCol);
    			scatter.setLineWidth(5);
    			scatter.addPoints(XXarray, YYarray, 6);
    			//scatter.addLegend(uniqueArray[y]);
    		}
    		StringBuilder sb = new StringBuilder();
    		for (int y = 0; y < groupN; y++) {
    			if (y != groupN-1) {
    				sb.append(uniqueArray[y]);
    				sb.append(System.getProperty("line.separator"));
    			} else {
    				sb.append(uniqueArray[y]);
    			}
    		}
    		String legendLabels = sb.toString();
    		scatter.addLegend(legendLabels);
    			//scatter.addLegend((uniqueArray.toString())); //gave odd results, depsite simplicity
    		scatter.show();
    		scatter.setLimitsToFit(true);
    		//IJ.log("File list: "+Arrays.toString(Filelist));
    	}
    }

    /*
     * https://stackoverflow.com/questions/34958829/how-to-save-a-2d-array-into-a-text-file-with-bufferedwriter
     */
    public static void toCSV(double array[][], String Filelist[]) throws IOException {
  	  StringBuilder builder = new StringBuilder();
  	  for(int i = 0; i < array.length; i++)//for each row
  	  {
  	     for(int j = 0; j < array[0].length; j++)//for each column
  	     {
  	    	 if (j == 0) {
  	    		 builder.append(Filelist[i]);
  	    		 builder.append(",");
  	    	 }
  	    	 builder.append(array[i][j]+"");//append to the output string
  	    	 if(j < array.length - 1)//if this is not the last row element
  	    		 builder.append(",");//then add comma (if you don't like commas you can use spaces)
  	     }
  	     builder.append("\n");//append new line at the end of the row
  	  }
  	  String desktopPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "tSNE_result.csv";
  	  BufferedWriter writer = new BufferedWriter(new FileWriter(""+desktopPath));
  	  writer.write(builder.toString());//save the string representation of the board
  	  writer.close();
  	  }
    
    public String[] getLabels( String inputIndexPath) throws IOException {
    	BufferedReader br = null;
    	List<String> labelList = new ArrayList<String>();
    	
    	if (inputIndexPath.endsWith(".csv")) {
    		try {
    			String line = "";
    			br = new BufferedReader(new FileReader(inputIndexPath));
    			int lineCounter = 0;
    			while ((line = br.readLine()) != null) {
    				if (lineCounter == 0) {
    					lineCounter++;
    					continue;
    				} else {
    					lineCounter++;
    					labelList.add(line.replaceAll(",", ""));
    				}
    				
    			}
    			} catch (FileNotFoundException e) {
    			IJ.handleException(e);
    			} catch (IOException e) {
    			IJ.handleException(e);} finally {
    				if (br != null) {
    					try {
    						br.close(); 
    						} catch (IOException e) {
    							IJ.handleException(e);
    						}
    				}
    			}
    	} else {
    		IJ.log("The user has specified a labels file but it is not a .csv.");
    	}
    	String[] labels = new String[labelList.size()];
    	//IJ.log("labelList size is: "+String.valueOf(labelList.size()));
    	labels = labelList.toArray(labels);
    	//IJ.log("getLabels produces this: "+Arrays.toString(labels));
    	//IJ.log("getLabels result contains "+String.valueOf((labels.length)+" elements"));
    	return labels;
    }
    
    /**
     * Parses the options string used to run the plugin and extracts options for it. Uses defaults for any which aren't
     * specified.
     */
    private void parseOptions() {
        // Get the options string we were started using.
        String optionsStr = debugOptions == null ? Macro.getOptions() : debugOptions;
        if (optionsStr == null) optionsStr = "";

        // Input folder path to process.
        inputFolderPath = Macro.getValue(optionsStr, "input_path", "");
        
        // Path to sample labels.
        inputIndexPath = Macro.getValue(optionsStr, "label_path", "");
        
        //
        nNeighbours = Macro.getValue(optionsStr, "n_nearest", "");
        
        //
        nThreads = Macro.getValue(optionsStr, "n_threads", "");
        
        // See if a CSV output is requested.
        outputCSV = optionsStr.contains("output_csv");
        
	    /** 
	     * Set the metric to use to compute distances in high dimensional space by name.
	     * Valid string metrics include:
	     * euclidean,
	     * manhattan,
	     * chebyshev,
	     * minkowski,
	     * canberra,
	     * braycurtis,
	     * cosine,
	     * correlation,
	     * haversine,
	     * hamming,
	     * jaccard,
	     * dice,
	     * russelrao,
	     * kulsinski,
	     * rogerstanimoto,
	     * sokalmichener,
	     * sokalsneath,
	     * yule.
	     * @param metric metric function specified by name
	     */
        metric = Macro.getValue(optionsStr, "metric", "euclidean");

    }
    
}
