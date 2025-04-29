package dimred;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import ij.*;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import plot.plot.Fx_Scatter;

import com.jujutsu.tsne.PrincipalComponentAnalysis;
import com.jujutsu.utils.MatrixUtils;

/**version 1.0.6*/

public class Pca_ implements PlugIn {
	//top level initialisation
	int choice; //JOption 'process the image stack?' choice outcome
	public static boolean processingTable = false;
	public static ImagePlus stack;
	public static PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis();
	public static boolean processingStack = false;
	public static double[][] imageMatrix;
	String[] Filelist;
	public static String[] labelsArray;
	public static String[] uniqueArray;
	public static double[] Pcomp1;
	public static double[] Pcomp2;
	public static double[] pcaMean; //mean of each element across all input samples, taken from PrincipalComponentAnalysis class
	public static double[] W; //diagonal matrix from SVD
	static String groupLabel;
	int bitD;
	int width;
	int height;
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
    private int pca_comp = 2; //was 2
    public static String inputIndexPath; //show the plugin where sample labels are located.. used to colour the final output.
    private String eigenOutString;
    private boolean eigen_out = false; //eigenvector output from those computed
    private String[] eigenOutStringArray;
    private int[] eigenOutArray;
    private static int pcompX = 1;
    private static int pcompY = 2;
    private String ranSeed;
	private int seed = 5;
	private boolean outputCSV = false; //create a CSV copy of the dimensionality-reduction output XYs on the users desktop.
	private boolean suppressStackAsk = false;
	private boolean suppressFx = false;
	private boolean suppressPlot = false;
	private boolean mean_out = false;
	private String colourBy; //accepts pc_1, pc_2, ... pc_n (principal component x); var_1, var_2. ... var_n (variable x);
    //private boolean logTransform = false;
    //private boolean cenAndScale = false;
	
    // Variables and main() method for testing in IDEs.
    private static String debugOptions = null;
    private static double[][] debugArray = null;
    private static double[][] debugArrayx = null;
	public static void main(String[] args) {
        debugArrayx = MatrixUtils.simpleRead2DMatrix(new File("src/main/resources/datasets/mnist2500_X.txt"), "   ");
        	//swap rows and columns of the debugArray (go from 784x2500 to 2500x784)
        	double[][] temp = new double[debugArrayx[0].length][debugArrayx.length];
        	for (int i=0; i<debugArrayx.length; i++) {
        		for (int j=0; j<debugArrayx[0].length; j++) {
        			temp[j][i] = debugArrayx[i][j];
        		}
        	}
        debugArray = temp;
        debugOptions = "label_path=[C:/Users/Anthony/Desktop/mnist_labels.csv]";
        new Pca_().run("");
        //debugOptions = "label_path=[C:/Users/Anthony/Desktop/NumbersIndex.csv]";
        //new PCA_().run("");
	}
	
	public void run(String arg) {
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
				choice = JOptionPane.showConfirmDialog(null, "Do you want to process the open table?", "Pca option",JOptionPane.YES_NO_CANCEL_OPTION);
		} else {
			choice = JOptionPane.OK_OPTION;
		}
		if (choice == JOptionPane.CANCEL_OPTION) {return;}
		else if (choice == JOptionPane.OK_OPTION) {
			processingTable = true;
			ResultsTable rt = ResultsTable.getActiveTable();
			int tableN = rt.size();
			int ColN = rt.getLastColumn();
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
	    	pca.setup(tableN, ColN+1);
	    	for (int p = 0; p < tableN; p++) {
	    		pca.addSample(imageMatrix[p]);
	    	}
	    	height = 1;
	    	width = ColN+1;
	    	bitD = 32;
			}
		}
		
		//Check that the requested principal components or eigenvectors are within the to be computed range. If either or both are over, then increase the computed range.
		if (pcompY > pca_comp) {
			pca_comp = pcompY;
		}
		if (pcompX > pca_comp) {
			pca_comp = pcompX;
		}
		if (eigen_out == true && eigenOutArray[eigenOutArray.length-1] > pca_comp) {
			pca_comp = eigenOutArray[eigenOutArray.length-1];
		}
		 
		 if (WindowManager.getCurrentImage() != null && (WindowManager.getCurrentImage()).isStack() && !processingTable) {
			 int type = WindowManager.getCurrentImage().getType();
			 if (!suppressStackAsk) {
				 choice = JOptionPane.showConfirmDialog(null, "Do you want to process the image stack?", "PCA option",JOptionPane.YES_NO_CANCEL_OPTION);
		 } else {
			 choice = JOptionPane.OK_OPTION;
		 }
		 if (choice == JOptionPane.CANCEL_OPTION) {return;}
		 else if (choice == JOptionPane.OK_OPTION) {
		    	processingStack = true;
		    	stack = WindowManager.getCurrentImage();
		    	//Get bitDepth
		bitD = stack.getBitDepth();
		Filelist = new String[stack.getStackSize()];
		height = stack.getHeight();
		width = stack.getWidth();
		imageMatrix = new double[stack.getStackSize()][width*height]; //2D array to hold all stack image data [slice number][1D array of all pixels in the slice image]
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
						image1DArray[k+(j*width)] = sip.getf(k,j);
					}
				}
			}
			for(int l = 0; l < (width*height); l++) {
				imageMatrix[s][l] = image1DArray[l];
			}
		}
		pca.setup(stack.getStackSize(), (width*height));
		for (int p = 0; p < stack.getStackSize(); p++) {
			pca.addSample(imageMatrix[p]);
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
		//specify a folder to perform PCA on
		else if (inputFolderPath.isEmpty() && debugArray == null && !processingTable) {
		DirectoryChooser dc = new DirectoryChooser("Select a folder");
		    inputFolderPath = dc.getDirectory();
		        if (inputFolderPath == null) {
		        	return;
		        }
			}
		 
			if (debugArray != null) {
				// Build pca with data from debug matrix
		Filelist = new String[debugArray[0].length];		//was debugArray.length
			pca.setup(debugArray.length, debugArray[0].length);
			for (int d = 0; d < debugArray.length; d++) {
				pca.addSample(debugArray[d]);
			}
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
		
		if (Filelist==null) {
			IJ.log("The specified directory does not contain images to process.");
				return;
			}
		
		
		 if (debugArray == null && !processingStack && !processingTable) {
				//Open the first image in the folder to get dimensions.. could have done it in the loop
		ImagePlus imp0 = IJ.openImage(inputFolderPath + Filelist[0]);
		width = imp0.getWidth();
		height = imp0.getHeight();
		imp0.close();
		
		pca.setup(Filelist.length, width*height);
		
		IJ.log("Number of images = "+imageMatrix.length);		//300
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
		
		double[] image1DArray = new double[width*height];
		
		for (int j = 0; j < height; j++) {
			for (int k = 0; k < width; k++) {
				if (type == ImagePlus.COLOR_RGB) {
					image1DArray[k+(j*width)] = ip.get(k, j);
				} else {
					image1DArray[k+(j*width)] = (float) ip.getf(k,j); //not sure if casting is appropriate here
		        			}
		        		}
		        	}
		        	imp.close();
		        	pca.addSample(image1DArray);
		        }
		    }
		}
		 
		 //pca computation here
		 pca.computeBasis(pca_comp);
		 
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
		if (labelsArray.length != Filelist.length) {
			IJ.log("The number of labels in the labels file does not match the number of processed images, so it has not been used to assign groupings.");
		IJ.log("Y.length = "+String.valueOf(Filelist.length)+"; Label number = "+String.valueOf(labelsArray.length));
		 		IJ.log(Arrays.toString(labelsArray));
		 		labelsArray = null;
		 	}
		 } else {
		 	labelsArray = null;
		 }
		 
		 // get and plot 2 sample vectors corresponding to specified principal components, or the first 2 if none are specified
		 if (pca_comp >= 0) {
			Pcomp1 = pca.getU(pcompX-1);
			if (pca_comp < 2) {
				Pcomp2 = pca.getU(pcompX-1);
			} else {
				Pcomp2 = pca.getU(pcompY-1);
			}
			if (eigen_out) {
					//Create a stack here
			ImagePlus outputPlus = NewImage.createImage("Eigenvector(s): ["+eigenOutString+"]", width, height, eigenOutArray.length, bitD, NewImage.FILL_BLACK);
			ImageStack eigenStack = outputPlus.getStack();
			outputPlus.setStack(eigenStack);
			ResultsTable eigenTable = new ResultsTable();
			for (int k = 0; k < eigenOutArray.length; k++) {
				//IJ.log("eigenOutArray = "+String.valueOf(eigenOutArray[k]));
			if (eigenOutArray[k] > 0 && eigenOutArray[k] <= pca_comp) { //do I need to check against pca_comp here?
			double[] eigenX = pca.getBasisVector((eigenOutArray[k])-1);
			//IJ.log("BasisVector is composed of "+Double.toString(eigenX.length)+" elements.");
			if (processingTable) {
				for (int i = 0; i < eigenX.length; i++) {
					eigenTable.setValue(i, k, eigenX[i]);
				}
			} else {
				eigenStack.setSliceLabel(""+String.valueOf(eigenOutArray[k]), k+1);
			ImageProcessor outputProc = eigenStack.getProcessor(k+1);
			if (eigenX.length != (width*height)) {
				IJ.log("Error: could not reconstruct eigenvector " + String.valueOf(eigenOutArray[k]) + " as an image, as its length does not equal the orginal image width*height.");
			} else {
				double maxNumber = 0;
				double minNumber = 0;
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {
						if (eigenX[j+(i*width)] > maxNumber) {
							maxNumber = eigenX[j+(i*width)];
						}
						if (eigenX[j+(i*width)] < minNumber) {
							minNumber = eigenX[j+(i*width)];
						}
					}
				}
				//IJ.log("maxNumber = "+Double.toString(maxNumber));
			boolean minUnderZero = false;
			boolean minOverZero = false;
			double scaleFactor = 1;
			//IJ.log("minNumber = "+String.valueOf(minNumber));
			if (minNumber < 0) {
				minUnderZero = true;
				//IJ.log("minUnderZero");
			minNumber = Math.sqrt(minNumber*minNumber);
			maxNumber = maxNumber+minNumber;
			scaleFactor = (Math.pow(2,bitD)-1)/(maxNumber); //bitD will only be available for image processing, perhaps I should set the default value to 1. Table processing is handled differently.
			} else if (minNumber >= 0) {
				minOverZero = true;
				//IJ.log("minOverZero");
				maxNumber = maxNumber-minNumber;
				scaleFactor = (Math.pow(2,bitD)-1)/(maxNumber);
			}
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					//Shift and scale all eigenvector values to span the image bit depth range (e.g. 0-255 for 8-bit)
								if (minUnderZero) {
									eigenX[j+(i*width)] = eigenX[j+(i*width)]+minNumber;
								} else if(minOverZero) {
									eigenX[j+(i*width)] = eigenX[j+(i*width)]-minNumber;
								}
								outputProc.set(j, i, (int) Math.floor(scaleFactor*eigenX[j+(i*width)]));
							}
						}
					}
				}
			
			} else {
				IJ.log("Could not output the specified eigenvector ["+String.valueOf(eigenOutArray[k])+"] as it is less than 1 or greater than the number of computed principal components.");
				}
			}
			if (processingTable) {
				eigenTable.show("Eigenvector(s): ["+eigenOutString+"]");
				} else {
					outputPlus.show();
				}
			}
			if (mean_out) {
				pcaMean = pca.mean;
				if (processingTable) {
					ResultsTable eigenTable = new ResultsTable();
					for (int i = 0; i < pcaMean.length; i++) {
						eigenTable.setValue(i, 0, pcaMean[i]);
					}
					eigenTable.show("Mean of all samples");
				} else {
					ImagePlus outputPlus = NewImage.createImage("Mean image of all input samples", width, height, 1, bitD, NewImage.FILL_BLACK);
					ImageProcessor outputProc = outputPlus.getProcessor();
					if (pcaMean.length != (width*height)) {
						IJ.log("Error: could not reconstruct the mean image vector, as its length does not equal the orginal image width*height.");
					} else {
						pcaMean = pca.mean;
						for (int i = 0; i < height; i++) {
							for (int j = 0; j < width; j++) {
								outputProc.set(j, i, (int) ((pcaMean[j+(i*width)])));
							}
						}
						outputPlus.show();
					}
				}
			}
			
			//print PC1 explains X amount of variance
			//Plot singular values sorted in W, to show how fast singular values decay.
			double[] describedVarianceArray = getDescribedVariance();
			DecimalFormat numFormat = new DecimalFormat("#.00");
			IJ.log("PC "+Integer.toString(pcompX)+" explains "+String.valueOf(numFormat.format(describedVarianceArray[pcompX-1]*100))+"% of the total variance.");
			IJ.log("PC "+Integer.toString(pcompY)+" explains "+String.valueOf(numFormat.format(describedVarianceArray[pcompY-1]*100))+"% of the total variance.");
			
			double totalVarianceDescribed = 0;
			if (describedVarianceArray.length <= 10) {
				for (int i = 0; i < describedVarianceArray.length-1; i++ ) {
					totalVarianceDescribed += describedVarianceArray[i];
					IJ.log("The first "+(i+1)+" PCs explain "+String.valueOf(numFormat.format(totalVarianceDescribed*100))+"% of the total variance.");
				}
			} else {
				for (int i = 0; i < 100; i++ ) {
					totalVarianceDescribed += describedVarianceArray[i];
					if (totalVarianceDescribed < 1 && (i+1)%5 == 0) {
						IJ.log("The first "+(i+1)+" PCs explain "+String.valueOf(numFormat.format(totalVarianceDescribed*100))+"% of the total variance.");
					} else if (totalVarianceDescribed >= 1){
						break;
					}
				}
			}
			
			/* Below are Iris data principle components, for example purpose. Squaring all values shows the contribution of each variable to the corresponding PC. 
			PC1	0.52	0.26	0.58	0.57	
			PC2	0.37	0.93	0.02	0.07
			PC3	0.72	0.24	0.14	0.63
			PC4	0.26	0.12	0.80	0.52
			*
				dimension 1	dimension 2	dimension 3	dimension 4		Row sum values
			PC1	0.272872109	0.069355814	0.337856224	0.31991586		1.000000007		For instance, Variable/dimension-3 contributes 33.8% of the variance to PC1.
			PC2	0.138620961	0.856654816	0.00044499	0.004279223		0.99999999		Summing the multiplication of this by the corresponding eigenvalue will allow one to see the amount of variance explained across the whole dataset by that variable.
			PC3	0.51986524	0.058579915	0.019850629	0.401704215		0.999999999		Ordered Eigenvalues = 0.72770452, 0.23030523, 0.03683832, 0.00515193
			PC4	0.068641689	0.015409451	0.641848164	0.274100697		1.000000001		Therefore for Variable 3 describes (0.72770452*0.337856224 + 0.23030523*0.00044499 + 0.03683832*0.019850629 + 0.00515193*0.641848164) 25% of the total variance. In fact, all variables/dimensions contribute equally. 
			*
			*
				1.00		1.00		1.00		1.00 <-- Column Sum values
			*/
			
			
			if ((!suppressFx || !suppressPlot)) {
				if (labelsArray == null) {
					lookupArray = new int[1][Pcomp1.length];
			    	for (int z = 0; z < Pcomp1.length; z++) {
			    		lookupArray[0][z] = z+1;
			    	}
			    	if (!suppressFx) {
			    		Fx_plot = new Fx_Scatter("PCA output", "PC "+ Integer.toString(pcompX), "PC " + Integer.toString(pcompY));
			groupLabel = "All data";
				Fx_plot.addSeries(Pcomp1, Pcomp2, groupLabel, Color.BLACK);
			}
			if (!suppressPlot) {
			    plotTitle = new String("PCA output");
			xTitle = "PC "+ Integer.toString(pcompX);
			yTitle = "PC " + Integer.toString(pcompY);
			scatter = new Plot(plotTitle, xTitle, yTitle);
			scatter.setLineWidth(5);
			scatter.addPoints(Pcomp1, Pcomp2, 6);
			scatter.show();
			uniqueArray = new String[1];
			uniqueArray[0] = "Data";
			labelsArray = new String[Pcomp1.length];	//Creating and filling the label array is maybe not ideal
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
				Fx_plot = new Fx_Scatter("PCA output", "PC "+ Integer.toString(pcompX), "PC " + Integer.toString(pcompY));
			}
			if (!suppressPlot) {
			    plotTitle = new String("PCA output");
				xTitle = "PC "+ Integer.toString(pcompX);
				yTitle = "PC " + Integer.toString(pcompY);
				scatter = new Plot(plotTitle, xTitle, yTitle);
			}
			StringBuilder sb = new StringBuilder(); //this will be created even if the user has not requested an ImageJ plot... I could initialise it as null and make it global, but overhead is low anyway.
			
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
					XXarray[counter] = Pcomp1[z];
					YYarray[counter] = Pcomp2[z];
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
			}
		 
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
						Fx_plot.colourByVariable(pca.getU(pc-1), Color.RED);
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

    private void parseOptions() {
        // Get the options string we were started using.
        String optionsStr = debugOptions == null ? Macro.getOptions() : debugOptions;
        if (optionsStr == null) optionsStr = "";

        // Input folder path to process.
        inputFolderPath = Macro.getValue(optionsStr, "input_path", "");
        
        // Path to sample labels.
        inputIndexPath = Macro.getValue(optionsStr, "label_path", "");

        // Initial dimensions to specify PCA pre-processing dimensionality-reduction.
        pca_comp = Integer.parseInt(Macro.getValue(optionsStr, "pca_comp", "2"));
        
        // Specify which principal component associated sample vectors to display.
        pcompX = Integer.parseInt(Macro.getValue(optionsStr, "pc_x", "1"));
        pcompY = Integer.parseInt(Macro.getValue(optionsStr, "pc_y", "2"));
        
        // Output specified eigenvectors (eigenfaces).
        //Note: The below function should probably be factorised into a discrete method to clean up the code.
        eigenOutString = Macro.getValue(optionsStr, "eigen_out", "");
        eigenOutString = eigenOutString.trim();
        eigenOutString = eigenOutString.replaceAll("[^\\d,-]+",""); //remove all non-numbers (not including commas or hyphens)
        eigenOutString = eigenOutString.replaceAll("(,)\\1+",","); //replace all consecutive repeating commas with one
        if (eigenOutString.startsWith(",")) {
        	eigenOutString = eigenOutString.substring(1);
        }
        //IJ.log(eigenOutString);
        eigenOutStringArray = eigenOutString.split(",");
        String rangeString = "";
        if (eigenOutStringArray.length > 0 && eigenOutStringArray[0] != "") {
	        for (int i = 0; i < eigenOutStringArray.length; i++) {
	        	if (eigenOutStringArray[i].contains("-")) {
	        		String[] subString = new String[2];
	        		try {
	        			subString = eigenOutStringArray[i].split("-");
	        			int lowNum = Integer.parseInt(subString[0]);
	        			int highNum = Integer.parseInt(subString[1]);
	        			if (highNum <= lowNum) {
	        				IJ.log("The specfied range of 'eigen_out' ["+subString[0]+"-"+subString[1]+"] cannot go from higher to lower, or be the same number.");
	        				continue;
	        			}
	        			for (int j = lowNum; j <= highNum; j++) {
	        				if (j == lowNum && i == 0) {
	        					rangeString = rangeString+String.valueOf(j);
	        				} else {
	        					rangeString = rangeString+","+String.valueOf(j);
	        				}
	        			}
	        		}
	        		catch(Exception e) {
	        			IJ.log("The specified eigen_out range was not valid.\nSingle numbers should be separated with a ','(comma).\nDiscrete ranges should be separated with a '-'(hyphen).\nAvoid using multiple hyphen within the same comma block (e.g. '1-10-15' is not valid).");
	        			IJ.handleException(e);
	        		}
	        	} else if(i==0) {
	        		rangeString = rangeString+String.valueOf(eigenOutStringArray[i]);
	        	} else {
	        		rangeString = rangeString+","+String.valueOf(eigenOutStringArray[i]);
	        	}
	        }
	        rangeString = rangeString.replaceAll("[^\\d,]+",""); //remove all non-numbers (not including commas)
	        rangeString = rangeString.replaceAll("(,)\\1+",","); //replace all consecutive repeating commas with one
	        //the below startsWith and endsWith block is probably not needed
	        if (rangeString.startsWith(",")) {
	        	rangeString = rangeString.substring(1);
	        }
	        if (rangeString.endsWith(",")) {
	        	rangeString = rangeString.substring(0, rangeString.length()-1);
	        }
	        IJ.log("eigen_out range = "+rangeString);
	        eigenOutString = rangeString;
	        eigenOutStringArray = rangeString.split(",");
	        eigenOutArray = new int[eigenOutStringArray.length];
	        for (int i = 0; i < eigenOutStringArray.length; i++) {
	        	eigenOutArray[i] = Integer.parseInt(eigenOutStringArray[i]);
	        }
	        if (eigenOutArray != null || eigenOutArray.length > 0) {
	        	eigen_out = true;
	        }
	        Arrays.sort(eigenOutArray);
        }
        
        // Output the mean sample
        mean_out = optionsStr.contains("mean_out");
        
        // Get a random seed from the user or use the default.
        ranSeed = Macro.getValue(optionsStr, "seed", "5");
        
        // See if a CSV output is requested.
        outputCSV = optionsStr.contains("output_csv");
        
        // Suppress the 'Do you want to run on the image stack' prompt
        suppressStackAsk = optionsStr.contains("no_prompt");
        
        // Suppress the creation of an interactive FX-plot
        suppressFx = optionsStr.contains("no_fx");
        
        // Suppress the creation of the default ImageJ plot
        suppressPlot = optionsStr.contains("no_plot");
        
        // Colour the plot datapoints by a user specified variable.
        colourBy = Macro.getValue(optionsStr, "colour_by", "");

    }
    
    /**
     * Returns the amount of input sample space variance defined by all principal components.
     *
     * @return Variance contribution array.
     */
    private double[] getDescribedVariance() {
    	
    	//double[] variance = new double[pca.getU(0).length];
    	
    	//if the number of images or table entries is lower than the number of dimensions, use this number
    	int maxVal = imageMatrix[0].length;
    	if (Filelist.length < maxVal) {
    		maxVal = Filelist.length;
    	}
    	
    	double[] variance = new double[maxVal];
    	//IJ.log("variance length ="+Integer.toString(variance.length));
    	double eigenValueSum = 0;
    	int eigenVecLength = variance.length;
    	for (int i = 0; i < eigenVecLength; i++) {
    		double eigenValue = pca.getW(i);
    		eigenValueSum += Math.pow(eigenValue, 2);
    		variance[i] = Math.pow(eigenValue, 2);
    	}
    	for (int i = 0; i < eigenVecLength; i++) {
    		variance[i] = variance[i]/eigenValueSum;
    	}
    	return variance;
    }
    
}