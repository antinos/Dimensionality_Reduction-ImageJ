package dimred;

import ij.*;
import ij.gui.Plot;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import plot.plot.Fx_Scatter;

import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.MatrixOps;
import com.jujutsu.utils.MatrixUtils;
import com.jujutsu.utils.TSneUtils;

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

/**version 1.0.6*/

/*
 * tSNE java implementation from https://github.com/lejon/T-SNE-Java
 * Process an image-stack, pass a directory of images to the plugin, or choose one when a dialog opens.
 */
public class Tsne_ implements PlugIn {
	//Top level initialisation
	int choice; //JOption 'process the image stack?' choice outcome
	public static boolean processingTable = false;
	public static ImagePlus stack;
	public static double[][] imageMatrix;
	String[] Filelist;
	public static String[] labelsArray;
	public static String[] uniqueArray;
	static String groupLabel;
	public static boolean processingStack = false;
    public static double[] Xarray;
    public static double[] Yarray;
	static Color ranCol;
	public static Color[] groupColours;
	public static int[][] lookupArray;
	public static String xTitle;
	public static String yTitle;
	public static String plotTitle;
	//ImageJ plot initialisation
	Plot scatter = null;
	//Fx top level initialisation
	Fx_Scatter Fx_plot = null;
	
    // Options to use during the run. Defaults for some but otherwise populated when parseOptions() is called.
    private String inputFolderPath;
    public static String inputIndexPath; //show the plugin where sample labels are located.. used to colour the final output.
    private static int initial_dims = 30; //pre-processing PCA variable.. reducing dimensions to specified value.
    private static double perplexity = 50.0;
    private int max_iterations = 1000;
    private boolean outputCSV = false; //create a CSV copy of the tSNE 2D output on the users desktop.
    private boolean parallel = false; //run t-sne using multiple CPU threads, for speed-up.
    private String ranSeed;
		private int seed = 5;
	private boolean suppressStackAsk = false;
	private boolean suppressFx = false;
	private boolean suppressPlot = false;
    private boolean logTransform = false;
    private boolean cenAndScale = false;
    private String colourBy; //accepts pc_1, pc_2, ... pc_n (principal component x); var_1, var_2. ... var_n (variable x);
    
    // Variables and main() method for testing in IDEs.
    private static String debugOptions = null;
    private static double[][] debugArray = null;
    public static void main(String[] args) {
        // To debug, set a CSV file to open, we'll use that in the ResultsTable for the run.
        debugArray = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
        new Tsne_().run("");
        //debugOptions = "output_csv";
        //new T_SNE().run("");
        //debugOptions = "initial_dims=50";
        //new T_SNE().run("");
        //debugOptions = "initial_dims=50 perplexity=40";
        //new T_SNE().run("");
        //debugOptions = "initial_dims=50 max_iterations=500";
        //new T_SNE().run("");
        //debugOptions = "initial_dims=50 max_iterations=1000 output_csv";
        //new T_SNE().run("");
        //debugOptions = "label_path=[C:/Users/asinadin/Desktop/NumbersIndex.csv]";
        //new T_SNE().run("");
    }
    

    public void run(String arg) {
    	double startTime = System.currentTimeMillis();
        // Fill in the options we'll use during this run.
        parseOptions();
        processingStack = false;
        processingTable = false;
        if (ranSeed != null && !("").equals(ranSeed) && !ranSeed.matches(".*[A-Za-z].*") && Double.valueOf(ranSeed) > 0) {
        	seed = Integer.valueOf(ranSeed);
        }
		Random rand = new Random();
		rand.setSeed(seed);
        
		if (WindowManager.getActiveTable() != null && WindowManager.getActiveTable() instanceof TextWindow) {
        	if (!suppressStackAsk) {
        		choice = JOptionPane.showConfirmDialog(null, "Do you want to process the open table?", "UMAP option",JOptionPane.YES_NO_CANCEL_OPTION);
        	} else {
        		choice = JOptionPane.OK_OPTION;
        	}
			if (choice == JOptionPane.CANCEL_OPTION) {return;}
			else if (choice == JOptionPane.OK_OPTION) {
				processingTable = true;
				ResultsTable rt = ResultsTable.getActiveTable(); //may need to make this a higher-level object
				int tableN = rt.size(); //we could keep calling rt.size below, but fpr very large tables this may add noticable cpu + memory overhead
				int ColN = rt.getLastColumn(); //as above
				Filelist = new String[tableN];
				imageMatrix = new double[tableN][ColN+1]; //Hopefully this works to count all headings... otherwise we will have to create and then enumerate a potentially very large string array (below)
				//imageMatrix = new double[rt.size()][rt.getHeadings().length];
				for (int y = 0; y < tableN; y++) {
					Filelist[y] = Integer.toString(y+1);
					for (int x = 0; x < ColN+1; x++) {
						imageMatrix[y][x] = rt.getValueAsDouble(x, y);
						//IJ.log(""+Double.toString(rt.getValueAsDouble(x, y)));
					}
				}
			}
		}
		
        if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && !processingTable) {
        	int type = WindowManager.getCurrentImage().getType();
        	if (!suppressStackAsk) {
        		choice = JOptionPane.showConfirmDialog(null, "Do you want to process the image stack?", "t-SNE option",JOptionPane.YES_NO_CANCEL_OPTION);
        	} else {
        		choice = JOptionPane.OK_OPTION;
        	}
            if (choice == JOptionPane.CANCEL_OPTION) {return;}
            else if (choice == JOptionPane.OK_OPTION) {
            	processingStack = true;
            	stack = WindowManager.getCurrentImage();
            	Filelist = new String[stack.getStackSize()];
            	int height = stack.getHeight();
            	int width = stack.getWidth();
            	imageMatrix = new double[stack.getStackSize()][width*height]; //2D array to hold all images in stack [slice number][pixels as 1D array]
            	for (int s = 0; s < stack.getStackSize(); s++) {
            		Filelist[s] = Integer.toString(s+1);
            		stack.setSlice(s+1);
            		ImageProcessor sip = stack.getProcessor();  	
                	double[] image1DArray = new double[width*height];	//try double, then float
                	for (int j = 0; j < height; j++) {
                		for (int k = 0; k < width; k++) {
                			if (type == ImagePlus.COLOR_RGB) {
                				image1DArray[k+(j*width)] = sip.get(k, j);
                			} else {
                				image1DArray[k+(j*width)] = sip.getInterpolatedValue(k,j);
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
            	DirectoryChooser dc = new DirectoryChooser("Select a folder of images to process");
                inputFolderPath = dc.getDirectory();
	                if (inputFolderPath == null) {
	                	return;
	                }
            }
        }
        //specify a folder to perform tSNE on
        else if (inputFolderPath.isEmpty() && debugArray == null && !processingTable) {
        DirectoryChooser dc = new DirectoryChooser("Select a folder");
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
		
    if (debugArray == null && !processingStack && !processingTable) {
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
        
    if (logTransform) {
    	imageMatrix = MatrixOps.log(imageMatrix, true);
    }
    if (cenAndScale) {
    	imageMatrix = MatrixOps.centerAndScale(imageMatrix);
    }
    
    	//tSNE here
        double[][] Y = tSNE_reduction((debugArray == null? imageMatrix : debugArray), initial_dims, perplexity, max_iterations);
        
        //Get and use labels if a .csv file is specified.
        if (!inputIndexPath.isEmpty() && inputIndexPath != null) {
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
        } else {
        	labelsArray = null;
        }
        
    	//If the outputCSV toggle is set, output the tSNE result to the users desktop
        if (outputCSV) {
        	try {
        		toCSV(Y, Filelist);
        	} catch (IOException e) {
        		IJ.handleException(e);
        	}
        }
        
        Xarray = new double[Y.length];
        Yarray = new double[Y.length];
        for (int m = 0; m < Y[0].length; m++) { //columns
        	for (int n = 0; n < Y.length; n++) { //rows
        		if (m==0) {
        			Xarray[n] = Y[n][m];
        		} else {
        			Yarray[n] = Y[n][m];
        		}
        	}
        }
    	
        //plotting here
		if ((!suppressFx || !suppressPlot)) {
			plotTitle = new String("tSNE output (perplexity ="+String.valueOf(perplexity)+"; initial dimensions ="+String.valueOf(initial_dims)+")");
			xTitle = "tSNE 1";
			yTitle = "tSNE 2";
			if (labelsArray == null) {
				lookupArray = new int[1][Xarray.length];
		    	for (int z = 0; z < Xarray.length; z++) {
		    		lookupArray[0][z] = z+1;
		    	}
		    	if (!suppressFx) {
		    		Fx_plot = new Fx_Scatter(plotTitle, xTitle, yTitle);
		    		groupLabel = "All data";
		    		Fx_plot.addSeries(Xarray, Yarray, groupLabel, Color.BLACK);
		    	}
		    	if (!suppressPlot) {
					scatter = new Plot(plotTitle, xTitle, yTitle);
					scatter.setLineWidth(5);
					scatter.addPoints(Xarray, Yarray, 6);
					scatter.show();
					uniqueArray = new String[1];
					uniqueArray[0] = "Data";
					labelsArray = new String[Xarray.length];	//Creating and filling the label array is maybe not ideal
					Arrays.fill(labelsArray, "Data");
					groupColours = new Color[1];
					groupColours[0] = Color.BLACK;
		    	}
			} else if (labelsArray != null) {
				//some method to count unique entries (groups) in the labels array.
				ArrayList<String> uniqueArrayList = new ArrayList<>();
				for(int i=0; i<labelsArray.length; i++){
					if(!uniqueArrayList.contains(labelsArray[i])){
						uniqueArrayList.add(labelsArray[i]);
					}
				}
				int groupN = uniqueArrayList.size();
				uniqueArray = new String[groupN];
				uniqueArrayList.toArray(uniqueArray);
				lookupArray = new int[groupN][];	// Set the number of label groups to a staggered lookup table 2D array... will be used to find the position of images in the original stack
				
				//generate seed-based random colours for datapoint allocation and also establish the lookup array map.
				groupColours = new Color[groupN];
		
				//Create the plot(s) outside of the processing loop below.
				if (!suppressFx) {
					Fx_plot = new Fx_Scatter(plotTitle, xTitle, yTitle);
				}
				if (!suppressPlot) {
					scatter = new Plot(plotTitle, xTitle, yTitle);
				}
				StringBuilder sb = new StringBuilder();
				
				for (int y = 0; y < groupColours.length; y++) {
					int r = rand.nextInt(230);
					int g = rand.nextInt(230);
					int b = rand.nextInt(230);
					ranCol = new Color(r,g,b);
					groupColours[y] = ranCol;
			
					int counter = 0;
					for (int z = 0; z < labelsArray.length; z++) {
						if (labelsArray[z].equals(uniqueArray[y])) {
							counter++;
						}
					}
					lookupArray[y] = new int[counter];
					//IJ.log(uniqueArray[y]+" is present "+String.valueOf(counter)+" time(s) in the labelsArray.");
					//IJ.log("debugArray.length = "+Integer.toString(debugArray.length)+", debugArray[0].length = "+Integer.toString(debugArray[0].length));
					double[] XXarray = new double[counter];
					double[] YYarray = new double[counter];
					counter = 0;
					for (int z = 0; z < labelsArray.length; z++) {
						if (labelsArray[z].equals(uniqueArray[y])) {
								//IJ.log("unique array contents = "+uniqueArray[y]+", XXarray length = "+Integer.toString(XXarray.length)+", YYarray length = "+Integer.toString(YYarray.length)+", y = "+Integer.toString(y)+", Pcomp1 length = "+Integer.toString(Pcomp1.length)+", Pcomp2 length = "+Integer.toString(Pcomp2.length)+"");
							XXarray[counter] = Xarray[z];
							YYarray[counter] = Yarray[z];
							lookupArray[y][counter] = z+1;
							counter++;
						}
					}
					if (!suppressFx) {
						groupLabel = uniqueArray[y];
						final int w = y;
						Fx_plot.addSeries(XXarray, YYarray, groupLabel, groupColours[w]);
					}
					if (!suppressPlot) {
						scatter.setColor(groupColours[y]);
						scatter.setLineWidth(5);
						scatter.addPoints(XXarray, YYarray, 6);
						if (y != groupColours.length-1) {
							sb.append(uniqueArray[y]);
							sb.append(System.getProperty("line.separator"));
						} else {
							sb.append(uniqueArray[y]);
						}
					//IJ.log("File list: "+Arrays.toString(Filelist));
					}
				}
	    		String legendLabels = sb.toString();
	    		if (!suppressPlot) {
	        		scatter.addLegend(legendLabels);
	        		scatter.show();
	        		scatter.setLimitsToFit(true);
		    	}
	    	}
		}
    	
    	double endTime = System.currentTimeMillis();
    	IJ.log("t-SNE took "+String.valueOf((endTime-startTime)/1000)+" seconds to complete.");
    	
    	processingStack = false;
    	processingTable = false;
    	
		//if requested, colour datapoints of the fx_plot by principal component or variable
		if (!colourBy.isEmpty() && colourBy != null) {
			if (colourBy.contains("pc_")) {
				if (colourBy.substring(3).matches("\\d+")) {
			    	int maxVal = imageMatrix[0].length;
			    	if (Filelist.length < maxVal) {
			    		maxVal = Filelist.length;
			    	}
					int pc = Integer.valueOf(colourBy.substring(3));
					if (pc <= maxVal) {
						Fx_plot.recolourByPC(pc-1);
						//Fx_plot.colourByVariable(pca.getU(pc-1), Color.RED);
					}
				} else {
					IJ.log("colour_by argument should match 'pc_x'. For example colour_by=pc_3 .");
				}
			} else if (colourBy.contains("var_")) {
				if (colourBy.substring(4).matches("\\d+")) {
					int maxVal = imageMatrix[0].length;
					int var = Integer.valueOf(colourBy.substring(4));
					if (var <= maxVal) {
						double[] variableArray = new double[Filelist.length];
						for (int i = 0; i < variableArray.length; i++) {
							variableArray[i] = imageMatrix[i][var-1];
						}
						Fx_plot.colourByVariable(variableArray, Color.RED);
					}
				} else {
					IJ.log("colour_by argument should match 'var_x'. For example colour_by=var_10 .");
				}
			}
		}
    	
    }
    
    public double[][] tSNE_reduction(double inputArray[][], int initial_dims, double perplexity, int max_iterations) {

        IJ.log(MatrixOps.doubleArrayToPrintString(inputArray, ", ", 50,10));
        
        BarnesHutTSne tsne;
        //parallel = false;
    	if(parallel) {			
    		tsne = new ParallelBHTsne();
    	} else {
    		tsne = new BHTSne();
    	}
    	
    	TSneConfiguration config = TSneUtils.buildConfig(inputArray, 2, initial_dims, perplexity, 1000);
    	double [][] Y = tsne.tsne(config);
    	
    	return Y;
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
    
    public String[] getLabels( String labelIndexPath) throws IOException {
    	inputIndexPath = labelIndexPath;
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
    	inputIndexPath = null;
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

        // Initial dimensions to specify PCA pre-processing dimensionality-reduction.
        initial_dims = Integer.parseInt(Macro.getValue(optionsStr, "initial_dims", "30"));

        // Get the perplexity value or use the default.
        perplexity = Double.parseDouble(Macro.getValue(optionsStr, "perplexity", "50"));
        
        // Get the maximum iterations for tSNE error approximation or use the default.
        max_iterations = Integer.parseInt(Macro.getValue(optionsStr, "max_iterations ", "1000"));
        
        // See if a CSV output is requested.
        outputCSV = optionsStr.contains("output_csv");
        
        // See if a CSV output is requested.
        parallel = optionsStr.contains("parallel");
        
        // Get a random seed from the user or use the default.
        ranSeed = Macro.getValue(optionsStr, "seed", "5");
        
        // Suppress the 'Do you want to run on the image stack' prompt
        suppressStackAsk = optionsStr.contains("no_prompt");
        
        // Suppress the creation of an interactive FX-plot
        suppressFx = optionsStr.contains("no_fx");
        
        // Log transform the DR input data. Data shaping isn't as important for image data, which is usually sampled across the same input space.
        logTransform = optionsStr.contains("log_transform");
        
        // Centre and Scale the DR input data. Data shaping isn't as important for image data, which is usually sampled across the same input space.
        cenAndScale = optionsStr.contains("centre_and_scale");
        
        // Colour the plot datapoints by a user specified variable.
        colourBy = Macro.getValue(optionsStr, "colour_by", "");

    }
    
}